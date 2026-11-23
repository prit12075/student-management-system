package com.university.sms.core.grading;

/**
 * UNIT 1 + UNIT 2 (Prit Patel) - an <b>enum with fields, a constructor and methods</b>.
 * Each constant is a full object holding its label, minimum percentage and grade points.
 * The UGC 10-point scale used by SRM-style universities.
 */
public enum Grade {

    O("O", 90, 10, "Outstanding"),
    A_PLUS("A+", 80, 9, "Excellent"),
    A("A", 70, 8, "Very Good"),
    B_PLUS("B+", 60, 7, "Good"),
    B("B", 50, 6, "Above Average"),
    C("C", 40, 5, "Pass"),
    F("F", 0, 0, "Fail") {
        // constant-specific body: only F overrides isPass()
        @Override
        public boolean isPass() {
            return false;
        }
    };

    private final String label;
    private final double minPercentage;
    private final int gradePoints;
    private final String remark;

    /** Enum constructors are always private. */
    Grade(String label, double minPercentage, int gradePoints, String remark) {
        this.label = label;
        this.minPercentage = minPercentage;
        this.gradePoints = gradePoints;
        this.remark = remark;
    }

    /** Absolute grading: walks the constants from best to worst (values() keeps declaration order). */
    public static Grade fromPercentage(double percentage) {
        for (Grade g : values()) {
            if (percentage >= g.minPercentage) {
                return g;
            }
        }
        return F;
    }

    /** Converts the printed label ("A+") back to the constant. */
    public static Grade fromLabel(String label) {
        for (Grade g : values()) {
            if (g.label.equalsIgnoreCase(label)) {
                return g;
            }
        }
        throw new IllegalArgumentException("Unknown grade label: " + label);
    }

    public boolean isPass() {
        return true;
    }

    public String getLabel() {
        return label;
    }

    public double getMinPercentage() {
        return minPercentage;
    }

    public int getGradePoints() {
        return gradePoints;
    }

    public String getRemark() {
        return remark;
    }

    @Override
    public String toString() {
        return label;
    }
}
