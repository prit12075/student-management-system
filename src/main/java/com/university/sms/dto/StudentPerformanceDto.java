package com.university.sms.dto;

import com.university.sms.entity.Student;

public class StudentPerformanceDto {

    private Student student;
    private int totalClasses;
    private int presentClasses;
    private double attendancePercentage;
    private boolean lowAttendanceAlert;
    private double averagePercentage;
    private double sgpa;
    private int subjectsEvaluated;
    private int subjectsPassed;
    private int backlogs;
    private String overallGrade;

    public StudentPerformanceDto() {
    }

    public StudentPerformanceDto(Student student, int totalClasses, int presentClasses, double attendancePercentage,
                                 boolean lowAttendanceAlert, double averagePercentage, double sgpa,
                                 int subjectsEvaluated, int subjectsPassed, int backlogs, String overallGrade) {
        this.student = student;
        this.totalClasses = totalClasses;
        this.presentClasses = presentClasses;
        this.attendancePercentage = attendancePercentage;
        this.lowAttendanceAlert = lowAttendanceAlert;
        this.averagePercentage = averagePercentage;
        this.sgpa = sgpa;
        this.subjectsEvaluated = subjectsEvaluated;
        this.subjectsPassed = subjectsPassed;
        this.backlogs = backlogs;
        this.overallGrade = overallGrade;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public int getTotalClasses() {
        return totalClasses;
    }

    public void setTotalClasses(int totalClasses) {
        this.totalClasses = totalClasses;
    }

    public int getPresentClasses() {
        return presentClasses;
    }

    public void setPresentClasses(int presentClasses) {
        this.presentClasses = presentClasses;
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }

    public boolean isLowAttendanceAlert() {
        return lowAttendanceAlert;
    }

    public void setLowAttendanceAlert(boolean lowAttendanceAlert) {
        this.lowAttendanceAlert = lowAttendanceAlert;
    }

    public double getAveragePercentage() {
        return averagePercentage;
    }

    public void setAveragePercentage(double averagePercentage) {
        this.averagePercentage = averagePercentage;
    }

    public double getSgpa() {
        return sgpa;
    }

    public void setSgpa(double sgpa) {
        this.sgpa = sgpa;
    }

    public int getSubjectsEvaluated() {
        return subjectsEvaluated;
    }

    public void setSubjectsEvaluated(int subjectsEvaluated) {
        this.subjectsEvaluated = subjectsEvaluated;
    }

    public int getSubjectsPassed() {
        return subjectsPassed;
    }

    public void setSubjectsPassed(int subjectsPassed) {
        this.subjectsPassed = subjectsPassed;
    }

    public int getBacklogs() {
        return backlogs;
    }

    public void setBacklogs(int backlogs) {
        this.backlogs = backlogs;
    }

    public String getOverallGrade() {
        return overallGrade;
    }

    public void setOverallGrade(String overallGrade) {
        this.overallGrade = overallGrade;
    }
}

