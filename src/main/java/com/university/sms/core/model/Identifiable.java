package com.university.sms.core.model;

/**
 * UNIT 1 + UNIT 5 (Prit Patel)
 * A generic interface: any object that owns a unique identifier of type {@code ID}.
 * Used as the upper bound of the generic repository ({@code T extends Identifiable<ID>}).
 *
 * @param <ID> type of the identifier (Long, String, ...)
 */
public interface Identifiable<ID> {

    /** @return the unique identifier of this object */
    ID getId();
}
