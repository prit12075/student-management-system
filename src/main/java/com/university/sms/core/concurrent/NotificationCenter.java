package com.university.sms.core.concurrent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * NOVEL FEATURE - thread-safe student notification inbox (Vivekanand, Unit 3).
 *
 * <p>Written by the background {@link AlertScheduler} thread and by faculty request threads,
 * read by student request threads - all at the same time. It therefore uses only concurrent
 * structures: {@link ConcurrentHashMap}, {@link CopyOnWriteArrayList} (readers never block),
 * a concurrent key-set for "read" ids and an {@link AtomicLong} id generator.</p>
 */
public class NotificationCenter {

    private static final int MAX_PER_STUDENT = 50;
    private final Map<String, CopyOnWriteArrayList<Notification>> inbox = new ConcurrentHashMap<>();
    private final Set<Long> readIds = ConcurrentHashMap.newKeySet();
    private final AtomicLong nextId = new AtomicLong(1);

    public Notification send(String rollNumber, String type, String title, String message) {
        Notification n = new Notification(nextId.getAndIncrement(), LocalDateTime.now(), type, title, message);
        CopyOnWriteArrayList<Notification> list =
                inbox.computeIfAbsent(rollNumber.toUpperCase(), k -> new CopyOnWriteArrayList<>());
        list.add(0, n);                                    // newest first
        while (list.size() > MAX_PER_STUDENT) {
            list.remove(list.size() - 1);
        }
        return n;
    }

    public List<Notification> inboxOf(String rollNumber) {
        return List.copyOf(inbox.getOrDefault(rollNumber.toUpperCase(), new CopyOnWriteArrayList<>()));
    }

    public long unreadCount(String rollNumber) {
        return inboxOf(rollNumber).stream().filter(n -> !readIds.contains(n.id())).count();
    }

    public boolean isRead(long id) {
        return readIds.contains(id);
    }

    public void markAllRead(String rollNumber) {
        inboxOf(rollNumber).forEach(n -> readIds.add(n.id()));
    }

    /** Latest notifications of every student, for the admin monitor. */
    public List<String> recent(int limit) {
        List<String> all = new ArrayList<>();
        List<Map.Entry<String, Notification>> flat = new ArrayList<>();
        inbox.forEach((roll, list) -> list.forEach(n -> flat.add(Map.entry(roll, n))));
        flat.sort(Comparator.comparingLong((Map.Entry<String, Notification> e) -> e.getValue().id()).reversed());
        for (Map.Entry<String, Notification> e : flat.subList(0, Math.min(limit, flat.size()))) {
            all.add(e.getValue().createdAt().toLocalTime().withNano(0) + "  " + e.getKey()
                    + "  [" + e.getValue().type() + "] " + e.getValue().title());
        }
        return all;
    }

    public int totalSent() {
        return (int) (nextId.get() - 1);
    }
}
