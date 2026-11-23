package com.university.sms.core.generic;

/**
 * UNIT 5 - Generics (Prit Patel).
 * A generic class with <b>two type parameters</b>, written as a Java record (immutable data carrier).
 * Example: {@code Pair<String, Double> topper = Pair.of("Ananya", 9.8);}
 *
 * @param <A> type of the first value
 * @param <B> type of the second value
 */
public record Pair<A, B>(A first, B second) {

    /** Generic static factory method - the compiler infers A and B from the arguments. */
    public static <A, B> Pair<A, B> of(A first, B second) {
        return new Pair<>(first, second);
    }

    /** Returns a new pair with the values swapped: Pair<A,B> becomes Pair<B,A>. */
    public Pair<B, A> swap() {
        return new Pair<>(second, first);
    }
}
