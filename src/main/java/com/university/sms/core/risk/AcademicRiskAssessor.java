package com.university.sms.core.risk;

import com.university.sms.core.model.StudentRecord;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * NOVEL FEATURE - Academic Risk Radar / early-warning system (Prit Patel).
 *
 * <p>Combines several {@link RiskFactor}s with weights into one 0-100 score, classifies it with
 * {@link RiskLevel}, and explains <i>why</i> in plain language. Universities usually notice a
 * struggling student only after the end-semester result; this flags them weeks earlier.</p>
 */
public class AcademicRiskAssessor {

    /** factor name -> (factor, weight). LinkedHashMap keeps the display order. */
    private final Map<String, Weighted> factors = new LinkedHashMap<>();

    private record Weighted(RiskFactor factor, double weight) {
    }

    /** Default model used by the ERP. Weights add up to 1.0. */
    public AcademicRiskAssessor() {
        register("Attendance shortage", RiskFactor.attendanceGap(), 0.40);
        register("Low marks", RiskFactor.lowMarks(), 0.25);
        register("Backlogs", RiskFactor.backlogs(), 0.25);
        register("Borderline subjects", RiskFactor.borderlineSubjects(), 0.10);
    }

    /** Plug in any extra factor (lambda, method reference or class). Returns this for chaining. */
    public final AcademicRiskAssessor register(String name, RiskFactor factor, double weight) {
        factors.put(name, new Weighted(factor, weight));
        return this;
    }

    public RiskReport assess(StudentRecord s) {
        double score = 0;
        List<String> reasons = new ArrayList<>();
        for (Map.Entry<String, Weighted> e : factors.entrySet()) {
            double points = e.getValue().factor().evaluate(s);
            double contribution = points * e.getValue().weight();
            score += contribution;
            if (contribution >= 1) {
                reasons.add(String.format("%s (+%.0f)", e.getKey(), contribution));
            }
        }
        score = Math.round(score * 10.0) / 10.0;
        if (reasons.isEmpty()) {
            reasons.add("No risk signals");
        }
        return new RiskReport(s.getRoll(), s.getFullName(), s.getDepartmentCode(), s.getAttendancePercentage(),
                s.getAveragePercentage(), s.getSgpa(), score, RiskLevel.of(score), reasons);
    }

    /** Assesses everybody, highest risk first. */
    public List<RiskReport> rank(List<StudentRecord> students) {
        List<RiskReport> reports = new ArrayList<>();
        for (StudentRecord s : students) {
            reports.add(assess(s));
        }
        reports.sort(Comparator.comparingDouble(RiskReport::score).reversed());
        return reports;
    }

    public List<String> factorNames() {
        return new ArrayList<>(factors.keySet());
    }
}
