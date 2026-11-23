package com.university.sms.core.grading;

import com.university.sms.core.model.UniversityInfo;

import java.util.List;

/**
 * UNIT 2 (Prit Patel) - <b>relative grading (bell curve)</b>.
 *
 * <p>Overrides the {@link #prepare(List)} hook to compute the class mean and standard
 * deviation, then grades each student by how far they are from the mean (z-score).
 * A student below the absolute pass mark still fails, so the curve can never pass
 * someone who has not earned 40%.</p>
 */
public final class RelativeGradingPolicy extends AbstractGradingPolicy {

    private double mean;
    private double standardDeviation;

    public RelativeGradingPolicy() {
        super("Relative (bell curve)", "Grades by z-score from class mean: z>=1.5 O, >=1 A+, >=0.5 A, >=0 B+, >=-0.5 B, else C; below 40% always F");
    }

    @Override
    protected void prepare(List<Double> percentages) {
        if (percentages.isEmpty()) {
            mean = 0;
            standardDeviation = 1;
            return;
        }
        double sum = 0;
        for (double p : percentages) {
            sum += p;
        }
        mean = sum / percentages.size();

        double squared = 0;
        for (double p : percentages) {
            squared += (p - mean) * (p - mean);
        }
        standardDeviation = Math.sqrt(squared / percentages.size());
        if (standardDeviation == 0) {
            standardDeviation = 1;   // avoid division by zero when everybody has the same score
        }
    }

    @Override
    public Grade assign(double percentage) {
        double p = clamp(percentage);
        if (p < UniversityInfo.PASS_PERCENT) {
            return Grade.F;
        }
        double z = (p - mean) / standardDeviation;
        if (z >= 1.5) return Grade.O;
        if (z >= 1.0) return Grade.A_PLUS;
        if (z >= 0.5) return Grade.A;
        if (z >= 0.0) return Grade.B_PLUS;
        if (z >= -0.5) return Grade.B;
        return Grade.C;
    }

    public double getMean() {
        return mean;
    }

    public double getStandardDeviation() {
        return standardDeviation;
    }
}
