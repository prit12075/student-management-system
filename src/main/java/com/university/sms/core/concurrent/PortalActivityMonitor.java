package com.university.sms.core.concurrent;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * UNIT 3 - Thread safety / synchronization (Vivekanand).
 *
 * <p>The embedded Tomcat server handles every HTTP request on a different thread, so these
 * counters are updated by many threads at the same time. Three techniques are shown side by side:</p>
 * <ol>
 *   <li>a {@code synchronized} method guarding a plain {@code long} field,</li>
 *   <li>{@link AtomicLong} (lock-free compare-and-set),</li>
 *   <li>{@link ConcurrentHashMap} + {@link LongAdder} for per-portal counters.</li>
 * </ol>
 * Without synchronization, {@code count++} (read-modify-write) would lose updates - a race condition.
 */
public class PortalActivityMonitor {

    private long totalRequests;                                  // guarded by 'this'
    private final AtomicLong slowRequests = new AtomicLong();
    private final AtomicLong peakConcurrent = new AtomicLong();
    private final AtomicLong inFlight = new AtomicLong();
    private final Map<String, LongAdder> perPortal = new ConcurrentHashMap<>();
    private final long startedAt = System.currentTimeMillis();

    public synchronized void incrementTotal() {
        totalRequests++;
    }

    public synchronized long getTotalRequests() {
        return totalRequests;
    }

    public void requestStarted(String portal) {
        incrementTotal();
        perPortal.computeIfAbsent(portal, k -> new LongAdder()).increment();
        long now = inFlight.incrementAndGet();
        peakConcurrent.accumulateAndGet(now, Math::max);         // atomic "max" update
    }

    public void requestFinished(long millis) {
        inFlight.decrementAndGet();
        if (millis > 500) {
            slowRequests.incrementAndGet();
        }
    }

    public Map<String, Long> getPerPortalCounts() {
        Map<String, Long> snapshot = new TreeMap<>();
        perPortal.forEach((k, v) -> snapshot.put(k, v.sum()));
        return snapshot;
    }

    public long getSlowRequests() {
        return slowRequests.get();
    }

    public long getPeakConcurrent() {
        return peakConcurrent.get();
    }

    public long getUptimeSeconds() {
        return (System.currentTimeMillis() - startedAt) / 1000;
    }

    /** Stress test used in the Java Lab page and unit tests: many threads hammer the counter. */
    public static long raceConditionDemo(int threads, int incrementsPerThread, boolean useSync)
            throws InterruptedException {
        final long[] unsafeCounter = {0};
        PortalActivityMonitor safe = new PortalActivityMonitor();
        Thread[] workers = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            workers[i] = new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    if (useSync) {
                        safe.incrementTotal();
                    } else {
                        unsafeCounter[0]++;                       // NOT thread-safe on purpose
                    }
                }
            }, "race-" + i);
            workers[i].start();
        }
        for (Thread t : workers) {
            t.join();
        }
        return useSync ? safe.getTotalRequests() : unsafeCounter[0];
    }
}
