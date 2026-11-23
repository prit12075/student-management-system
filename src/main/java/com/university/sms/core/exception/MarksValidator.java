package com.university.sms.core.exception;

/**
 * UNIT 2 - Exception Handling (Anjay).
 * Validates the four mark components before they are saved. Demonstrates
 * <ul>
 *   <li>{@code throws} in a method signature (declaring a checked exception),</li>
 *   <li>{@code throw} to raise a new exception object,</li>
 *   <li>an inner helper that throws for the first invalid value.</li>
 * </ul>
 */
public final class MarksValidator {

    public static final double MAX_INTERNAL = 30;
    public static final double MAX_ASSIGNMENT = 20;
    public static final double MAX_PRACTICAL = 20;
    public static final double MAX_EXTERNAL = 30;

    private MarksValidator() {
    }

    public static void validate(Double internal, Double assignment, Double practical, Double external)
            throws InvalidMarksException {
        check("Internal exam", internal, MAX_INTERNAL);
        check("Assignment", assignment, MAX_ASSIGNMENT);
        check("Practical", practical, MAX_PRACTICAL);
        check("External exam", external, MAX_EXTERNAL);
    }

    private static void check(String component, Double value, double max) throws InvalidMarksException {
        if (value == null) {
            return;                                  // blank means "not entered yet" = 0
        }
        if (value.isNaN() || value < 0 || value > max) {
            throw new InvalidMarksException(component, value, max);
        }
    }
}
