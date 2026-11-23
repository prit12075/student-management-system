package com.university.sms.core.grading;

import java.util.ArrayList;
import java.util.List;

/**
 * UNIT 2 (Prit Patel) - <b>abstract class implementing an interface</b> and the
 * <b>Template Method</b> pattern.
 *
 * <p>{@link #assignAll(List)} is {@code final}: the algorithm "prepare, then grade every
 * score" is fixed, while the steps {@link #prepare(List)} (a hook with an empty default) and
 * {@link #assign(double)} (abstract) are supplied by subclasses.</p>
 */
public abstract sealed class AbstractGradingPolicy implements GradingPolicy
        permits AbsoluteGradingPolicy, RelativeGradingPolicy, ModerationGradingPolicy {

    private final String name;
    private final String description;

    protected AbstractGradingPolicy(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /** Hook method: subclasses that need class statistics override it. */
    protected void prepare(List<Double> percentages) {
        // default: nothing to prepare
    }

    @Override
    public final List<Grade> assignAll(List<Double> percentages) {
        prepare(percentages);
        List<Grade> grades = new ArrayList<>(percentages.size());
        for (double p : percentages) {
            grades.add(assign(p));        // dynamic dispatch to the subclass version
        }
        return grades;
    }

    /** Shared helper for subclasses. */
    protected static double clamp(double value) {
        return Math.max(0, Math.min(100, value));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name;
    }
}
