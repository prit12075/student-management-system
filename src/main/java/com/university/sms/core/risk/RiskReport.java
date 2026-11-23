package com.university.sms.core.risk;

import java.util.List;

/** NOVEL FEATURE - one student's risk assessment (immutable record). */
public record RiskReport(String rollNumber, String name, String department, double attendance,
                         double average, double sgpa, double score, RiskLevel level, List<String> reasons) {
}
