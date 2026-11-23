package com.university.sms.core.risk;

import com.university.sms.core.grading.Grade;
import com.university.sms.core.grading.GradeCalculator;

import java.util.ArrayList;
import java.util.List;

/**
 * NOVEL FEATURE - "What-if" SGPA planner for students (Prit Patel).
 * <ul>
 *   <li>{@link #project(List, List)} - SGPA if the student scores the given percentages.</li>
 *   <li>{@link #minimumUniformPercentage(List, double)} - the lowest percentage that, scored in
 *       every subject, reaches the target SGPA (walks the {@link Grade} enum from C upwards).</li>
 * </ul>
 */
public final class SgpaGoalPlanner {

    private SgpaGoalPlanner() {
    }

    public record Projection(List<Grade> grades, double sgpa) {
    }

    public static Projection project(List<Double> expectedPercentages, List<Integer> credits) {
        List<Grade> grades = new ArrayList<>();
        for (double p : expectedPercentages) {
            grades.add(Grade.fromPercentage(p));
        }
        return new Projection(grades, GradeCalculator.sgpa(grades, credits));
    }

    /** @return minimum percentage to score in every subject, or -1 if the target is above 10 */
    public static double minimumUniformPercentage(List<Integer> credits, double targetSgpa) {
        Grade[] ascending = {Grade.C, Grade.B, Grade.B_PLUS, Grade.A, Grade.A_PLUS, Grade.O};
        for (Grade g : ascending) {
            List<Grade> all = new ArrayList<>();
            for (int i = 0; i < credits.size(); i++) {
                all.add(g);
            }
            if (GradeCalculator.sgpa(all, credits) >= targetSgpa) {
                return g.getMinPercentage();
            }
        }
        return -1;
    }
}
