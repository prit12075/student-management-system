package com.university.sms.core.grading;

import com.university.sms.core.model.UniversityInfo;

/**
 * UNIT 2 (Prit Patel) - <b>moderation / grace marks</b> policy.
 * A student who is short of the pass mark by at most {@code graceMarks} is lifted to the
 * pass mark; everybody else is graded with the absolute bands. Shows a subclass that keeps
 * its own state (a final field set through the constructor).
 */
public final class ModerationGradingPolicy extends AbstractGradingPolicy {

    private final double graceMarks;

    public ModerationGradingPolicy(double graceMarks) {
        super("Moderation (+" + graceMarks + " grace)",
                "Absolute bands, but students within " + graceMarks + " marks of 40% are moderated up to a pass");
        if (graceMarks < 0) {
            throw new IllegalArgumentException("Grace marks cannot be negative");
        }
        this.graceMarks = graceMarks;
    }

    @Override
    public Grade assign(double percentage) {
        double p = clamp(percentage);
        double pass = UniversityInfo.PASS_PERCENT;
        if (p < pass && pass - p <= graceMarks) {
            p = pass;
        }
        return Grade.fromPercentage(p);
    }

    public double getGraceMarks() {
        return graceMarks;
    }
}
