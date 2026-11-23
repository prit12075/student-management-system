package com.university.sms.core.risk;

import com.university.sms.core.model.StudentRecord;
import com.university.sms.core.model.UniversityInfo;

/**
 * NOVEL FEATURE - Academic Risk Radar (Prit Patel, Unit 2 polymorphism + functional interfaces).
 *
 * <p>A <b>functional interface</b>: each risk factor is one behaviour "student → 0..100 points".
 * The four built-in factors are created with <b>lambda expressions</b> in static factory methods,
 * so new factors can be plugged in without changing {@link AcademicRiskAssessor}.</p>
 */
@FunctionalInterface
public interface RiskFactor {

    /** 0 = no risk from this factor, 100 = maximum risk. */
    double evaluate(StudentRecord student);

    /** Attendance risk starts 5 points above the 75% rule (80%) and is maximum at 60%. */
    static RiskFactor attendanceGap() {
        return s -> clamp((UniversityInfo.MIN_ATTENDANCE_PERCENT + 5 - s.getAttendancePercentage()) * 5);
    }

    /** Low average marks - below 70% starts adding risk, 30% or less is maximum. */
    static RiskFactor lowMarks() {
        return s -> s.getSubjectScores().isEmpty() ? 0 : clamp((70 - s.getAveragePercentage()) * 2.5);
    }

    /** Each backlog (failed subject) adds 50 points. */
    static RiskFactor backlogs() {
        return s -> clamp(s.getBacklogs() * 50.0);
    }

    /** Subjects scraping through: scores between 40 and 50 count as "borderline". */
    static RiskFactor borderlineSubjects() {
        return s -> {
            long borderline = s.getSubjectScores().stream().filter(p -> p >= 40 && p < 50).count();
            return clamp(borderline * 35.0);
        };
    }

    static double clamp(double v) {
        return Math.max(0, Math.min(100, v));
    }
}
