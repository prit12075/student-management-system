package com.university.sms.core.concurrent;

import java.util.ArrayList;
import java.util.List;

/**
 * UNIT 3 - Thread life cycle (Vivekanand).
 * Drives a real thread through every {@link Thread.State} and records what it observes:
 * NEW → RUNNABLE → TIMED_WAITING (sleep) → WAITING (wait) → BLOCKED (lock held by another
 * thread) → TERMINATED. Shown on the Java Lab page.
 */
public class ThreadLifecycleTracer {

    private final Object monitor = new Object();
    private volatile boolean sleptOnce;
    private volatile boolean waiting;

    public List<String> trace() throws InterruptedException {
        List<String> states = new ArrayList<>();

        Thread worker = new Thread(() -> {
            long end = System.currentTimeMillis() + 50;
            while (System.currentTimeMillis() < end) {
                // busy work -> RUNNABLE
            }
            try {
                Thread.sleep(150);                     // TIMED_WAITING
                sleptOnce = true;
                synchronized (monitor) {
                    waiting = true;
                    monitor.wait();                    // WAITING until notified
                }
                synchronized (monitor) {               // BLOCKED while main holds the lock
                    // re-acquired the lock
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "lifecycle-demo");

        states.add("1. after new Thread(...)          : " + worker.getState());
        worker.start();
        states.add("2. after start() (busy working)   : " + worker.getState());
        Thread.sleep(90);
        states.add("3. during Thread.sleep(150)       : " + worker.getState());
        while (!waiting) {
            Thread.sleep(5);
        }
        Thread.sleep(20);
        states.add("4. inside monitor.wait()          : " + worker.getState());
        synchronized (monitor) {
            monitor.notify();                          // wake the worker...
            Thread.sleep(40);                          // ...but keep holding the lock
            states.add("5. notified, lock still held      : " + worker.getState());
        }
        worker.join();
        states.add("6. after join()                   : " + worker.getState());
        return states;
    }

    boolean hasSlept() {
        return sleptOnce;
    }
}
