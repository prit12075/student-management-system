package com.university.sms.core.jdbc;

/**
 * UNIT 4 - JDBC (Arindam).
 * One row of the {@code archived_students} table, read back from SQLite through a ResultSet.
 * Used by the web console and the JavaFX desktop client.
 */
public record ArchivedStudent(String rollNumber, String fullName, String email, String department,
                              int semester, double attendance, double average, double sgpa,
                              int backlogs, String status) {
}
