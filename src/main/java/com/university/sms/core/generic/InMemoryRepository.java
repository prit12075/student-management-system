package com.university.sms.core.generic;

import com.university.sms.core.model.Identifiable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * UNIT 5 - Generics (Prit Patel).
 * Thread-safe, in-memory implementation of {@link GenericRepository}. It is used as a
 * cache of {@code StudentRecord} objects by the Java Lab console and in unit tests.
 * The generic types flow from the class declaration into every method.
 */
public class InMemoryRepository<T extends Identifiable<ID>, ID> implements GenericRepository<T, ID> {

    private final Map<ID, T> store = new LinkedHashMap<>();   // keeps insertion order

    @Override
    public synchronized T save(T item) {
        if (item == null || item.getId() == null) {
            throw new IllegalArgumentException("Item and its id must not be null");
        }
        store.put(item.getId(), item);
        return item;
    }

    @Override
    public synchronized void saveAll(Collection<? extends T> items) {
        for (T item : items) {
            save(item);
        }
    }

    @Override
    public synchronized Optional<T> findById(ID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public synchronized List<T> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public synchronized List<T> findWhere(Predicate<? super T> condition) {
        List<T> matches = new ArrayList<>();
        for (T item : store.values()) {
            if (condition.test(item)) {
                matches.add(item);
            }
        }
        return matches;
    }

    @Override
    public synchronized boolean deleteById(ID id) {
        return store.remove(id) != null;
    }

    @Override
    public synchronized int count() {
        return store.size();
    }
}
