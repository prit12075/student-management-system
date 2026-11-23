package com.university.sms.core.risk;

/**
 * NOVEL FEATURE - Academic Risk Radar (Prit Patel, Units 1/2).
 * Enum with fields and a static classifier; each level carries a UI colour and advice.
 */
public enum RiskLevel {
    LOW(0, "success", "On track - keep it up"),
    MODERATE(20, "warning", "Needs attention - meet the mentor this month"),
    HIGH(40, "danger", "At risk - mandatory mentor meeting and remedial classes"),
    CRITICAL(65, "dark", "Critical - inform guardian, attendance and backlog recovery plan");

    private final int minScore;
    private final String colour;
    private final String advice;

    RiskLevel(int minScore, String colour, String advice) {
        this.minScore = minScore;
        this.colour = colour;
        this.advice = advice;
    }

    /** Highest level whose threshold the score reaches (iterates values() in reverse). */
    public static RiskLevel of(double score) {
        RiskLevel[] all = values();
        for (int i = all.length - 1; i >= 0; i--) {
            if (score >= all[i].minScore) {
                return all[i];
            }
        }
        return LOW;
    }

    public int getMinScore() { return minScore; }
    public String getColour() { return colour; }
    public String getAdvice() { return advice; }
}
