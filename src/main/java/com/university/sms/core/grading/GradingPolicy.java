package com.university.sms.core.grading;

import java.util.List;

/**
 * UNIT 2 (Prit Patel) - <b>runtime polymorphism through an interface</b>.
 *
 * <p>The rest of the system only knows this interface. Which implementation runs
 * ({@link AbsoluteGradingPolicy}, {@link RelativeGradingPolicy} or
 * {@link ModerationGradingPolicy}) is decided at run time from the admin's choice -
 * dynamic method dispatch. New policies can be added without changing any caller
 * (open/closed principle).</p>
 *
 * <p>The interface is <b>sealed</b> (Java 17+): only the listed classes may implement it.</p>
 */
public sealed interface GradingPolicy permits AbstractGradingPolicy {

    /** Short name shown in the UI. */
    String getName();

    /** One-line explanation of how this policy grades. */
    String getDescription();

    /** Grade one score. */
    Grade assign(double percentage);

    /** Grade a whole class (some policies need the class statistics first). */
    List<Grade> assignAll(List<Double> percentages);

    /**
     * Static factory: converts the text coming from the web form into an object.
     * Uses a switch <b>expression</b> (Java 14+).
     */
    static GradingPolicy of(String key) {
        String k = key == null ? "absolute" : key.trim().toLowerCase();
        return switch (k) {
            case "relative", "curve" -> new RelativeGradingPolicy();
            case "moderation", "grace" -> new ModerationGradingPolicy(5.0);
            default -> new AbsoluteGradingPolicy();
        };
    }
}
