package com.university.sms.core.generic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * UNIT 5 - Generics (Prit Patel).
 * A generic "result" container returned by long operations (CSV import, archive export,
 * parallel report generation). It carries either a value of type {@code T} or a list of
 * error messages, plus a summary - so the UI can show partial success.
 *
 * <p>Concepts: generic class, generic static factory methods, generic method with a second
 * type parameter ({@link #map(Function)}), and the functional interface {@link Function}.</p>
 *
 * @param <T> type of the value produced by the operation
 */
public final class Result<T> {

    private final T value;
    private final List<String> errors;
    private final String summary;

    private Result(T value, List<String> errors, String summary) {
        this.value = value;
        this.errors = errors == null ? List.of() : List.copyOf(errors);
        this.summary = summary;
    }

    public static <T> Result<T> success(T value, String summary) {
        return new Result<>(value, Collections.emptyList(), summary);
    }

    public static <T> Result<T> partial(T value, List<String> errors, String summary) {
        return new Result<>(value, new ArrayList<>(errors), summary);
    }

    public static <T> Result<T> failure(String error) {
        return new Result<>(null, List.of(error), error);
    }

    /** Generic method: transforms Result&lt;T&gt; into Result&lt;R&gt; keeping the errors. */
    public <R> Result<R> map(Function<? super T, ? extends R> mapper) {
        R mapped = value == null ? null : mapper.apply(value);
        return new Result<>(mapped, errors, summary);
    }

    public boolean isSuccess() {
        return value != null && errors.isEmpty();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public T getValue() {
        return value;
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getSummary() {
        return summary;
    }
}
