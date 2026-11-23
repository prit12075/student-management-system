package com.university.sms.core.concurrent;

import com.university.sms.core.model.StudentRecord;
import com.university.sms.core.model.UniversityInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * UNIT 3 - Multithreading (Vivekanand).
 *
 * <p>Generates a report card for every student, first one-by-one on the calling thread
 * (sequential) and then with a <b>thread pool</b> ({@link ExecutorService}). Each student is a
 * {@link Callable} task that returns a {@link ReportCard}; the {@link Future} objects are
 * collected and {@code get()} waits for each result. The UI compares the two timings.</p>
 *
 * <p>Report rendering in a real ERP (PDF layout, DB reads) takes time; {@link #WORK_MILLIS}
 * simulates that latency so the speed-up is visible even with a small demo dataset.</p>
 */
public class ConcurrentReportEngine {

    public static final long WORK_MILLIS = 60;

    /** Outcome of one run. */
    public record ReportRun(String mode, List<ReportCard> cards, Set<String> threadsUsed, long elapsedMillis) {
    }

    /** Sequential version: one thread does all the work. */
    public ReportRun generateSequential(List<StudentRecord> students) {
        long start = System.nanoTime();
        List<ReportCard> cards = new ArrayList<>();
        for (StudentRecord s : students) {
            cards.add(buildCard(s));
        }
        return finish("Sequential (1 thread)", cards, start);
    }

    /** Parallel version: a fixed pool of worker threads executes one Callable per student. */
    public ReportRun generateParallel(List<StudentRecord> students, int threads) throws InterruptedException {
        long start = System.nanoTime();
        ExecutorService pool = Executors.newFixedThreadPool(threads, namedThreadFactory("report-worker"));
        try {
            List<Callable<ReportCard>> tasks = new ArrayList<>();
            for (StudentRecord s : students) {
                tasks.add(() -> buildCard(s));          // lambda implementing Callable<ReportCard>
            }
            List<Future<ReportCard>> futures = pool.invokeAll(tasks);   // submits all, waits for all
            List<ReportCard> cards = new ArrayList<>();
            for (Future<ReportCard> f : futures) {
                try {
                    cards.add(f.get());
                } catch (ExecutionException e) {
                    throw new IllegalStateException("Report task failed", e.getCause());
                }
            }
            return finish("Parallel (" + threads + " threads)", cards, start);
        } finally {
            pool.shutdown();                                     // always release the threads
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        }
    }

    /** The work done for one student (runs on whichever thread executes the task). */
    ReportCard buildCard(StudentRecord s) {
        long t0 = System.nanoTime();
        try {
            Thread.sleep(WORK_MILLIS);                           // simulated rendering latency
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();                  // restore the interrupt flag
        }
        String verdict;
        if (s.getBacklogs() > 0) {
            verdict = "Backlog in " + s.getBacklogs() + " subject(s)";
        } else if (!UniversityInfo.isAttendanceEligible(s.getAttendancePercentage())) {
            verdict = "Detained - attendance shortage";
        } else if (s.getSgpa() >= 9.0) {
            verdict = "Distinction";
        } else {
            verdict = "Promoted";
        }
        return new ReportCard(s.getRoll(), s.getFullName(), s.getDepartmentCode(), s.getSgpa(),
                s.getAttendancePercentage(), verdict, Thread.currentThread().getName(),
                (System.nanoTime() - t0) / 1_000_000);
    }

    private ReportRun finish(String mode, List<ReportCard> cards, long startNanos) {
        Set<String> threads = new TreeSet<>();
        for (ReportCard c : cards) {
            threads.add(c.generatedBy());
        }
        return new ReportRun(mode, cards, threads, (System.nanoTime() - startNanos) / 1_000_000);
    }

    /** Custom ThreadFactory: gives worker threads readable names and makes them daemon threads. */
    static ThreadFactory namedThreadFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger(1);
        return runnable -> {
            Thread t = new Thread(runnable, prefix + "-" + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        };
    }
}
