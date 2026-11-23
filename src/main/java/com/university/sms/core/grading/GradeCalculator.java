package com.university.sms.core.grading;

import java.util.List;
import java.util.Map;

/**
 * UNIT 2 (Prit Patel) - <b>compile-time polymorphism (method overloading)</b>.
 * The compiler picks the right {@code percentage(...)} / {@code sgpa(...)} version from the
 * number and types of the arguments.
 */
public final class GradeCalculator {

    private GradeCalculator() {
    }

    /** Version 1: obtained marks out of a maximum. */
    public static double percentage(double obtained, double maximum) {
        if (maximum <= 0) {
            return 0;
        }
        return round1(obtained * 100.0 / maximum);
    }

    /** Version 2: the four university components (internal 30 + assignment 20 + practical 20 + external 30). */
    public static double percentage(double internal, double assignment, double practical, double external) {
        return percentage(internal + assignment + practical + external, 100);
    }

    /** Version 3: an int array of component marks with one shared maximum. */
    public static double percentage(int[] componentMarks, int maximum) {
        int total = 0;
        for (int m : componentMarks) {
            total += m;
        }
        return percentage(total, maximum);
    }

    /** SGPA from parallel lists of grades and credits: sum(points x credits) / sum(credits). */
    public static double sgpa(List<Grade> grades, List<Integer> credits) {
        if (grades.size() != credits.size()) {
            throw new IllegalArgumentException("Grades and credits must have the same size");
        }
        double weighted = 0;
        int totalCredits = 0;
        for (int i = 0; i < grades.size(); i++) {
            weighted += grades.get(i).getGradePoints() * credits.get(i);
            totalCredits += credits.get(i);
        }
        return totalCredits == 0 ? 0 : round2(weighted / totalCredits);
    }

    /** Overloaded SGPA: every subject has the same credit value. */
    public static double sgpa(List<Grade> grades) {
        return sgpa(grades, grades.stream().map(g -> 4).toList());
    }

    /** Overloaded SGPA from a map grade -> credits (e.g. built by the analytics engine). */
    public static double sgpa(Map<Grade, Integer> creditsPerGrade) {
        double weighted = 0;
        int totalCredits = 0;
        for (Map.Entry<Grade, Integer> e : creditsPerGrade.entrySet()) {
            weighted += e.getKey().getGradePoints() * e.getValue();
            totalCredits += e.getValue();
        }
        return totalCredits == 0 ? 0 : round2(weighted / totalCredits);
    }

    public static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    public static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
