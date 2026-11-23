package com.university.sms.core.generic;

import com.university.sms.core.model.Identifiable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * UNIT 5 - Generics (Prit Patel).
 * A <b>generic interface with a bounded type parameter</b>: {@code T} must implement
 * {@link Identifiable}&lt;ID&gt;. The same contract works for StudentRecord, FacultyRecord
 * or any future type - written once, type-safe everywhere (no casts).
 * This mirrors what Spring Data's {@code JpaRepository<T, ID>} does for the database layer.
 *
 * @param <T>  the stored type
 * @param <ID> the identifier type
 */
public interface GenericRepository<T extends Identifiable<ID>, ID> {

    T save(T item);

    void saveAll(Collection<? extends T> items);   // upper-bounded wildcard: any subtype of T

    Optional<T> findById(ID id);

    List<T> findAll();

    List<T> findWhere(Predicate<? super T> condition);  // lower-bounded wildcard: any supertype of T

    boolean deleteById(ID id);

    int count();
}
