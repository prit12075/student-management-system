package com.university.sms.core.verify;

/** NOVEL FEATURE - one subject line that is sealed into the verification code (Anjay). */
public record MarksheetLine(String subjectCode, double total, String grade) {
}
