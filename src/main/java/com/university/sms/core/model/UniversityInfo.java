package com.university.sms.core.model;

import java.time.Year;

/**
 * UNIT 1 (Prit Patel) - the <b>static</b> and <b>final</b> keywords.
 * <ul>
 *   <li>{@code final class}: cannot be inherited.</li>
 *   <li>{@code private} constructor: no objects can be created (utility class).</li>
 *   <li>{@code static final} fields: compile-time constants shared by the whole program.</li>
 *   <li>{@code static} block: runs exactly once when the class is loaded by the JVM.</li>
 *   <li>{@code static} nested class: a class that lives inside another class.</li>
 * </ul>
 */
public final class UniversityInfo {

    public static final String UNIVERSITY_NAME = "Apex University";
    public static final double MIN_ATTENDANCE_PERCENT = 75.0;
    public static final double PASS_PERCENT = 40.0;
    public static final int MAX_MARKS = 100;

    /** Assigned once in the static block, can never change afterwards. */
    public static final int ACADEMIC_YEAR;

    private static int classLoadCount;

    static {
        // static initializer block - executed once, when the JVM loads this class
        ACADEMIC_YEAR = Year.now().getValue();
        classLoadCount++;
    }

    private UniversityInfo() {
        throw new UnsupportedOperationException("UniversityInfo is a utility class");
    }

    public static int getClassLoadCount() {
        return classLoadCount;
    }

    /** Static method: true when the attendance percentage meets the university rule. */
    public static boolean isAttendanceEligible(double percentage) {
        return percentage >= MIN_ATTENDANCE_PERCENT;
    }

    /**
     * Classes a student must attend continuously (without missing any) to reach 75%.
     * Solves (present + x) / (total + x) >= 0.75 for the smallest whole x.
     */
    public static int classesNeededFor75(int present, int total) {
        if (total == 0 || isAttendanceEligible(present * 100.0 / total)) {
            return 0;
        }
        int needed = 0;
        while ((present + needed) * 100.0 / (total + needed) < MIN_ATTENDANCE_PERCENT) {
            needed++;
        }
        return needed;
    }

    /** Static nested class - grouped with its outer class, but needs no outer object. */
    public static final class Contact {
        public static final String EMAIL = "registrar@apex.edu.in";
        public static final String PHONE = "+91-863-000-0000";

        private Contact() {
        }
    }
}
