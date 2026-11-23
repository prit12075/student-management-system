package com.university.sms.core.concurrent;

import com.university.sms.core.model.StudentRecord;
import com.university.sms.core.model.UniversityInfo;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * UNIT 3 - Multithreading (Vivekanand).
 *
 * <p>Runs the attendance-alert pipeline on two threads that talk through an {@link AlertQueue}:</p>
 * <ul>
 *   <li><b>Producer</b> - created from a {@link Runnable} lambda: scans the students and
 *       puts an alert for everyone below 75%.</li>
 *   <li><b>Consumer</b> - {@link AlertDispatcher}, a class that <b>extends Thread</b>: takes alerts
 *       and "dispatches" them (mail/SMS to student and guardian in a real deployment).</li>
 * </ul>
 * A special POISON message tells the consumer to stop. {@code join()} makes the caller wait
 * for both threads, and thread priorities/states are written into the event log.
 */
public class AttendanceAlertMonitor {

    private static final String POISON = "__END__";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /** synchronizedList: both threads add to this log safely. */
    private final List<String> events = Collections.synchronizedList(new ArrayList<>());

    /** Consumer thread - demonstrates extending the Thread class. */
    private final class AlertDispatcher extends Thread {
        private final AlertQueue<String> queue;
        private int dispatched;

        AlertDispatcher(AlertQueue<String> queue) {
            super("alert-dispatcher");
            this.queue = queue;
        }

        @Override
        public void run() {
            log("consumer started (priority " + getPriority() + ")");
            try {
                while (true) {
                    String alert = queue.take();          // blocks (WAITING) when queue is empty
                    if (POISON.equals(alert)) {
                        break;
                    }
                    dispatched++;
                    log("DISPATCHED -> " + alert);
                    Thread.sleep(15);                     // simulate sending an SMS/e-mail
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log("consumer interrupted");
            }
            log("consumer finished, " + dispatched + " alert(s) sent");
        }
    }

    /** Runs the full pipeline and returns the ordered event log. */
    public List<String> run(List<StudentRecord> students) throws InterruptedException {
        events.clear();
        AlertQueue<String> queue = new AlertQueue<>(2);   // small buffer so the producer has to wait too

        Runnable producerTask = () -> {
            log("producer started (priority " + Thread.currentThread().getPriority() + ")");
            try {
                for (StudentRecord s : students) {
                    if (s.getAttendancePercentage() < UniversityInfo.MIN_ATTENDANCE_PERCENT) {
                        int need = neededClasses(s.getAttendancePercentage());
                        String alert = String.format("%s %s (%s) at %.1f%% - attend ~%d more classes",
                                s.getRoll(), s.getFullName(), s.getDepartmentCode(), s.getAttendancePercentage(), need);
                        log("queued   -> " + s.getRoll());
                        queue.put(alert);                // blocks when the buffer is full
                    }
                }
                queue.put(POISON);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            log("producer finished");
        };

        Thread producer = new Thread(producerTask, "attendance-scanner");
        AlertDispatcher consumer = new AlertDispatcher(queue);
        producer.setPriority(Thread.MAX_PRIORITY);
        consumer.setPriority(Thread.NORM_PRIORITY);

        log("state before start: producer=" + producer.getState() + ", consumer=" + consumer.getState());
        consumer.start();
        producer.start();
        producer.join();                                   // main thread waits for producer
        consumer.join(5000);                               // ...and for consumer (max 5 s)
        log("state after join:   producer=" + producer.getState() + ", consumer=" + consumer.getState());
        return new ArrayList<>(events);
    }

    /** Assumes a typical 40-class semester to estimate classes still needed. */
    static int neededClasses(double attendancePercentage) {
        int total = 40;
        int present = (int) Math.round(attendancePercentage * total / 100.0);
        return UniversityInfo.classesNeededFor75(present, total);
    }

    private void log(String message) {
        events.add(LocalTime.now().format(TIME) + " [" + Thread.currentThread().getName() + "] " + message);
    }
}
