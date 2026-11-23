package com.university.sms.core.concurrent;

/**
 * UNIT 3 - Multithreading (Vivekanand).
 * Result produced by one report-generation task. {@code generatedBy} records the name of the
 * worker thread that produced it, so the UI can prove the work really ran in parallel.
 */
public record ReportCard(String rollNumber, String studentName, String department, double sgpa,
                         double attendance, String verdict, String generatedBy, long millis) {
}
