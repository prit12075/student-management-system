package com.university.sms.dto;

import com.university.sms.entity.Student;
import com.university.sms.entity.Subject;

public class AttendanceSummaryDto {

    private Student student;
    private Subject subject;
    private int totalClasses;
    private int presentCount;
    private int absentCount;
    private int leaveCount;
    private double attendancePercentage;
    private boolean lowAttendance; // true when < 75.0%
    private int classesNeededFor75;

    public AttendanceSummaryDto() {
    }

    public AttendanceSummaryDto(Student student, Subject subject, int totalClasses, int presentCount, int absentCount, int leaveCount) {
        this.student = student;
        this.subject = subject;
        this.totalClasses = totalClasses;
        this.presentCount = presentCount;
        this.absentCount = absentCount;
        this.leaveCount = leaveCount;

        if (totalClasses > 0) {
            this.attendancePercentage = Math.round(((double) presentCount / totalClasses) * 1000.0) / 10.0;
            this.lowAttendance = this.attendancePercentage < 75.0;
            if (this.lowAttendance) {
                // Solve (presentCount + x) / (totalClasses + x) >= 0.75 => x >= 3*totalClasses - 4*presentCount
                int needed = (3 * totalClasses) - (4 * presentCount);
                this.classesNeededFor75 = Math.max(1, needed);
            } else {
                this.classesNeededFor75 = 0;
            }
        } else {
            this.attendancePercentage = 100.0;
            this.lowAttendance = false;
            this.classesNeededFor75 = 0;
        }
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public int getTotalClasses() {
        return totalClasses;
    }

    public void setTotalClasses(int totalClasses) {
        this.totalClasses = totalClasses;
    }

    public int getPresentCount() {
        return presentCount;
    }

    public void setPresentCount(int presentCount) {
        this.presentCount = presentCount;
    }

    public int getAbsentCount() {
        return absentCount;
    }

    public void setAbsentCount(int absentCount) {
        this.absentCount = absentCount;
    }

    public int getLeaveCount() {
        return leaveCount;
    }

    public void setLeaveCount(int leaveCount) {
        this.leaveCount = leaveCount;
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }

    public boolean isLowAttendance() {
        return lowAttendance;
    }

    public void setLowAttendance(boolean lowAttendance) {
        this.lowAttendance = lowAttendance;
    }

    public int getClassesNeededFor75() {
        return classesNeededFor75;
    }

    public void setClassesNeededFor75(int classesNeededFor75) {
        this.classesNeededFor75 = classesNeededFor75;
    }
}

