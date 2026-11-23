package com.university.sms.core.exception;

import com.university.sms.core.model.UniversityInfo;

/**
 * UNIT 2 - Exception Handling (Anjay).
 * Checked exception thrown when a student below 75% attendance asks for an exam hall ticket.
 * Handled centrally by {@code GlobalExceptionHandler}, which shows a friendly page with the
 * number of classes still needed.
 */
public class AttendanceShortageException extends SmsException {

    private static final long serialVersionUID = 1L;

    private final String rollNumber;
    private final double attendancePercentage;
    private final int classesNeeded;

    public AttendanceShortageException(String rollNumber, double attendancePercentage, int classesNeeded) {
        super("SMS-ATT-075", String.format("%s has %.1f%% attendance (minimum %.0f%% required). Attend %d more class(es) to become eligible.",
                rollNumber, attendancePercentage, UniversityInfo.MIN_ATTENDANCE_PERCENT, classesNeeded));
        this.rollNumber = rollNumber;
        this.attendancePercentage = attendancePercentage;
        this.classesNeeded = classesNeeded;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public int getClassesNeeded() {
        return classesNeeded;
    }
}
