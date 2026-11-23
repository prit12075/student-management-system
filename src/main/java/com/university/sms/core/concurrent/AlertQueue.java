package com.university.sms.core.concurrent;

import java.util.LinkedList;
import java.util.Queue;

/**
 * UNIT 3 - Inter-thread communication (Vivekanand).
 *
 * <p>A bounded buffer written by hand with {@code synchronized}, {@code wait()} and
 * {@code notifyAll()} - the classic <b>producer-consumer</b> problem:</p>
 * <ul>
 *   <li>{@link #put} waits while the buffer is full,</li>
 *   <li>{@link #take} waits while the buffer is empty,</li>
 *   <li>after every change the other side is woken with {@code notifyAll()}.</li>
 * </ul>
 * {@code wait()} is always called inside a {@code while} loop to guard against spurious wake-ups.
 *
 * @param <T> type of the messages (generic class)
 */
public class AlertQueue<T> {

    private final Queue<T> buffer = new LinkedList<>();
    private final int capacity;

    public AlertQueue(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1");
        }
        this.capacity = capacity;
    }

    public synchronized void put(T item) throws InterruptedException {
        while (buffer.size() == capacity) {
            wait();                       // releases the lock and sleeps until notified
        }
        buffer.add(item);
        notifyAll();                      // wake up a waiting consumer
    }

    public synchronized T take() throws InterruptedException {
        while (buffer.isEmpty()) {
            wait();
        }
        T item = buffer.poll();
        notifyAll();                      // wake up a waiting producer
        return item;
    }

    public synchronized int size() {
        return buffer.size();
    }

    public int getCapacity() {
        return capacity;
    }
}
