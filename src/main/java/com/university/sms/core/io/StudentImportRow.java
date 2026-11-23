package com.university.sms.core.io;

/**
 * UNIT 3 - I/O (Anjay).
 * One validated row of a bulk-admission CSV file (Java record = immutable data class with
 * auto-generated constructor, getters, equals, hashCode and toString).
 */
public record StudentImportRow(int lineNumber, String rollNumber, String firstName, String lastName,
                               String email, String phone, String departmentCode, int semester, String section) {
}
