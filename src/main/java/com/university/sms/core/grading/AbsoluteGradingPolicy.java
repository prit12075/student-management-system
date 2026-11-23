package com.university.sms.core.grading;

/**
 * UNIT 2 (Prit Patel) - the default university policy: fixed percentage bands
 * (90+ = O, 80+ = A+, ... below 40 = F). Overrides only {@link #assign(double)}.
 */
public final class AbsoluteGradingPolicy extends AbstractGradingPolicy {

    public AbsoluteGradingPolicy() {
        super("Absolute (fixed bands)", "Fixed UGC bands: 90+ O, 80+ A+, 70+ A, 60+ B+, 50+ B, 40+ C, below 40 F");
    }

    @Override
    public Grade assign(double percentage) {
        return Grade.fromPercentage(clamp(percentage));
    }
}
