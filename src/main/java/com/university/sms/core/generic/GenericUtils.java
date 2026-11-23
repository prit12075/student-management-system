package com.university.sms.core.generic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * UNIT 5 - Generic methods, bounded types and wildcards (Prit Patel).
 */
public final class GenericUtils {

    private GenericUtils() {
    }

    /** Bounded type parameter: T must be comparable to itself (or a supertype). */
    public static <T extends Comparable<? super T>> T max(Collection<? extends T> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Collection is empty");
        }
        T best = null;
        for (T item : items) {
            if (best == null || item.compareTo(best) > 0) {
                best = item;
            }
        }
        return best;
    }

    /** Upper-bounded wildcard: accepts List&lt;Integer&gt;, List&lt;Double&gt;, ... */
    public static double average(Collection<? extends Number> numbers) {
        if (numbers.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (Number n : numbers) {
            sum += n.doubleValue();
        }
        return Math.round(sum / numbers.size() * 100.0) / 100.0;
    }

    /** Lower-bounded wildcard: can add Integers into List&lt;Integer&gt;, List&lt;Number&gt; or List&lt;Object&gt;. */
    public static void fillRange(List<? super Integer> target, int fromInclusive, int toExclusive) {
        for (int i = fromInclusive; i < toExclusive; i++) {
            target.add(i);
        }
    }

    /** Unbounded wildcard: works with a list of anything, only reads Objects. */
    public static int countNonNull(List<?> list) {
        int count = 0;
        for (Object o : list) {
            if (o != null) count++;
        }
        return count;
    }

    /** Generic grouping: groups items of type T by a key of type K, keeping first-seen key order. */
    public static <T, K> Map<K, List<T>> groupBy(Collection<T> items, Function<? super T, ? extends K> keyFn) {
        Map<K, List<T>> groups = new LinkedHashMap<>();
        for (T item : items) {
            groups.computeIfAbsent(keyFn.apply(item), k -> new ArrayList<>()).add(item);
        }
        return groups;
    }

    /** Generic top-N with any comparator. */
    public static <T> List<T> topN(Collection<T> items, Comparator<? super T> order, int n) {
        List<T> copy = new ArrayList<>(items);
        copy.sort(order);
        return copy.subList(0, Math.min(n, copy.size()));
    }
}
