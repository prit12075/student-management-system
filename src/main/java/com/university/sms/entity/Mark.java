package com.university.sms.entity;

import com.university.sms.core.grading.Grade;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "marks",
    uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "subject_id"})
)
public class Mark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(nullable = false)
    private Integer semester = 5;

    @Column(name = "internal_marks", nullable = false)
    private Double internalMarks = 0.0; // Max 30

    @Column(name = "assignment_marks", nullable = false)
    private Double assignmentMarks = 0.0; // Max 20

    @Column(name = "practical_marks", nullable = false)
    private Double practicalMarks = 0.0; // Max 20

    @Column(name = "external_marks", nullable = false)
    private Double externalMarks = 0.0; // Max 30

    @Column(name = "total_marks", nullable = false)
    private Double totalMarks = 0.0; // Max 100

    @Column(nullable = false)
    private Double percentage = 0.0;

    @Column(nullable = false, length = 10)
    private String grade = "F";

    @Column(name = "grade_points", nullable = false)
    private Integer gradePoints = 0;

    @Column(name = "result_status", nullable = false, length = 20)
    private String resultStatus = "FAIL";

    @Column(length = 250)
    private String remarks;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "entered_by_faculty_id")
    private Faculty enteredBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Mark() {
    }

    public void calculateGradeAndTotal() {
        double i = internalMarks != null ? Math.max(0, Math.min(30, internalMarks)) : 0.0;
        double a = assignmentMarks != null ? Math.max(0, Math.min(20, assignmentMarks)) : 0.0;
        double p = practicalMarks != null ? Math.max(0, Math.min(20, practicalMarks)) : 0.0;
        double e = externalMarks != null ? Math.max(0, Math.min(30, externalMarks)) : 0.0;

        this.internalMarks = Math.round(i * 10.0) / 10.0;
        this.assignmentMarks = Math.round(a * 10.0) / 10.0;
        this.practicalMarks = Math.round(p * 10.0) / 10.0;
        this.externalMarks = Math.round(e * 10.0) / 10.0;

        this.totalMarks = Math.round((this.internalMarks + this.assignmentMarks + this.practicalMarks + this.externalMarks) * 10.0) / 10.0;
        int maxTotal = subject != null ? subject.getTotalMaxMarks() : 100;
        this.percentage = maxTotal > 0 ? Math.round((this.totalMarks / maxTotal) * 1000.0) / 10.0 : 0.0;

        // Unit 1/2 (Prit): grading delegated to the Grade enum - one source of truth for the bands
        Grade g = Grade.fromPercentage(this.percentage);
        this.grade = g.getLabel();
        this.gradePoints = g.getGradePoints();
        this.resultStatus = g.isPass() ? "PASS" : "FAIL";
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public Double getInternalMarks() {
        return internalMarks;
    }

    public void setInternalMarks(Double internalMarks) {
        this.internalMarks = internalMarks;
    }

    public Double getAssignmentMarks() {
        return assignmentMarks;
    }

    public void setAssignmentMarks(Double assignmentMarks) {
        this.assignmentMarks = assignmentMarks;
    }

    public Double getPracticalMarks() {
        return practicalMarks;
    }

    public void setPracticalMarks(Double practicalMarks) {
        this.practicalMarks = practicalMarks;
    }

    public Double getExternalMarks() {
        return externalMarks;
    }

    public void setExternalMarks(Double externalMarks) {
        this.externalMarks = externalMarks;
    }

    public Double getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(Double totalMarks) {
        this.totalMarks = totalMarks;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public Integer getGradePoints() {
        return gradePoints;
    }

    public void setGradePoints(Integer gradePoints) {
        this.gradePoints = gradePoints;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Faculty getEnteredBy() {
        return enteredBy;
    }

    public void setEnteredBy(Faculty enteredBy) {
        this.enteredBy = enteredBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

