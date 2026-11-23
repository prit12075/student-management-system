package com.university.sms.core.verify;

/**
 * NOVEL FEATURE - result of checking a marksheet code (Anjay).
 * Nested enum + record: VALID, MISMATCH (forged, or marks changed after the PDF was issued) or UNKNOWN_ROLL.
 */
public record VerificationResult(Status status, String rollNumber, String studentName, String department,
                                 double sgpa, int subjects, String message) {

    public enum Status { VALID, MISMATCH, UNKNOWN_ROLL }

    public boolean isValid() {
        return status == Status.VALID;
    }
}
