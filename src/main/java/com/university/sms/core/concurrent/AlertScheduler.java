package com.university.sms.core.concurrent;

import com.university.sms.core.model.StudentRecord;
import com.university.sms.core.model.UniversityInfo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * NOVEL FEATURE - automatic attendance alert engine (Vivekanand, Unit 3).
 *
 * <p>A {@link ScheduledExecutorService} runs a scan every {@code periodSeconds} on its own daemon
 * thread ("alert-scheduler"). Each scan pulls fresh student data through a {@link Supplier}
 * (a functional interface, so this class does not depend on Spring or JPA), and sends a
 * notification when a student <b>newly</b> drops below 75% - or recovers. The last known state per
 * student is kept in a {@link ConcurrentHashMap} so alerts are not repeated every scan.</p>
 */
public class AlertScheduler {

    private final Supplier<List<StudentRecord>> source;
    private final NotificationCenter center;
    private final long periodSeconds;
    private final Map<String, Boolean> lastShortState = new ConcurrentHashMap<>();
    private final AtomicInteger scans = new AtomicInteger();
    private final AtomicInteger alertsRaised = new AtomicInteger();
    private volatile LocalDateTime lastScanAt;
    private volatile String lastScanThread = "-";
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> task;

    public AlertScheduler(Supplier<List<StudentRecord>> source, NotificationCenter center, long periodSeconds) {
        this.source = source;
        this.center = center;
        this.periodSeconds = periodSeconds;
    }

    public synchronized void start() {
        if (executor != null) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "alert-scheduler");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);          // background work must not slow down web requests
            return t;
        });
        task = executor.scheduleAtFixedRate(this::safeScan, 10, periodSeconds, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    /** Never let an exception kill the scheduled task (it would silently stop repeating). */
    private void safeScan() {
        try {
            scanNow();
        } catch (RuntimeException e) {
            System.err.println("Alert scan failed: " + e.getMessage());
        }
    }

    /** One scan; also called directly by the admin "Run scan now" button. Returns new alerts. */
    public synchronized int scanNow() {
        int raised = 0;
        for (StudentRecord s : source.get()) {
            boolean shortNow = s.getAttendancePercentage() < UniversityInfo.MIN_ATTENDANCE_PERCENT;
            Boolean before = lastShortState.put(s.getRoll(), shortNow);
            if (shortNow && !Boolean.TRUE.equals(before)) {
                int need = AttendanceAlertMonitor.neededClasses(s.getAttendancePercentage());
                center.send(s.getRoll(), "ATTENDANCE", "Attendance below 75%",
                        String.format("Your overall attendance is %.1f%%. Attend about %d more classes without absence "
                                + "to become eligible for the end-semester exam.", s.getAttendancePercentage(), need));
                raised++;
            } else if (!shortNow && Boolean.TRUE.equals(before)) {
                center.send(s.getRoll(), "INFO", "Attendance recovered",
                        String.format("Great - your attendance is back to %.1f%%.", s.getAttendancePercentage()));
                raised++;
            }
        }
        scans.incrementAndGet();
        alertsRaised.addAndGet(raised);
        lastScanAt = LocalDateTime.now().withNano(0);
        lastScanThread = Thread.currentThread().getName();
        return raised;
    }

    public boolean isRunning() {
        return executor != null && task != null && !task.isCancelled();
    }

    public int getScans() { return scans.get(); }
    public int getAlertsRaised() { return alertsRaised.get(); }
    public LocalDateTime getLastScanAt() { return lastScanAt; }
    public String getLastScanThread() { return lastScanThread; }
    public long getPeriodSeconds() { return periodSeconds; }
}
