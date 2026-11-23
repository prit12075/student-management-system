package com.university.sms.core.model;

/**
 * UNIT 1 / UNIT 2 (Prit Patel)
 * Interface showing abstraction through an interface: every academic member
 * must be able to produce a one-line report. Demonstrates
 * <ul>
 *   <li>an abstract method ({@link #toReportLine()}),</li>
 *   <li>a <b>default</b> method that subclasses may override,</li>
 *   <li>a <b>static</b> utility method that belongs to the interface itself,</li>
 *   <li>an interface constant (implicitly {@code public static final}).</li>
 * </ul>
 */
public interface Reportable {

    /** Interface constant: implicitly public, static and final. */
    String REPORT_SEPARATOR = " | ";

    /** Every implementing class must say how it is reported. */
    String toReportLine();

    /** Default method - inherited by all implementing classes. */
    default String toReportHeader() {
        return "=== " + getClass().getSimpleName().toUpperCase() + " REPORT ===";
    }

    /** Static method of an interface - called as Reportable.join(...). */
    static String join(String... parts) {
        return String.join(REPORT_SEPARATOR, parts);
    }
}
