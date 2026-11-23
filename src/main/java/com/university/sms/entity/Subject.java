package com.university.sms.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "subjects")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private Integer credits = 4;

    @Column(nullable = false)
    private Integer semester = 5;

    @Column(name = "class_section", length = 10)
    private String section = "A";

    @Column(name = "subject_type", length = 30)
    private String subjectType = "THEORY"; // THEORY, LAB, ELECTIVE

    @Column(name = "max_internal_marks")
    private Integer maxInternalMarks = 30;

    @Column(name = "max_assignment_marks")
    private Integer maxAssignmentMarks = 20;

    @Column(name = "max_practical_marks")
    private Integer maxPracticalMarks = 20;

    @Column(name = "max_external_marks")
    private Integer maxExternalMarks = 30;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "faculty_id")
    private Faculty assignedFaculty;

    public Subject() {
    }

    public int getTotalMaxMarks() {
        return (maxInternalMarks != null ? maxInternalMarks : 30)
                + (maxAssignmentMarks != null ? maxAssignmentMarks : 20)
                + (maxPracticalMarks != null ? maxPracticalMarks : 20)
                + (maxExternalMarks != null ? maxExternalMarks : 30);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCredits() {
        return credits;
    }

    public void setCredits(Integer credits) {
        this.credits = credits;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public Integer getMaxInternalMarks() {
        return maxInternalMarks;
    }

    public void setMaxInternalMarks(Integer maxInternalMarks) {
        this.maxInternalMarks = maxInternalMarks;
    }

    public Integer getMaxAssignmentMarks() {
        return maxAssignmentMarks;
    }

    public void setMaxAssignmentMarks(Integer maxAssignmentMarks) {
        this.maxAssignmentMarks = maxAssignmentMarks;
    }

    public Integer getMaxPracticalMarks() {
        return maxPracticalMarks;
    }

    public void setMaxPracticalMarks(Integer maxPracticalMarks) {
        this.maxPracticalMarks = maxPracticalMarks;
    }

    public Integer getMaxExternalMarks() {
        return maxExternalMarks;
    }

    public void setMaxExternalMarks(Integer maxExternalMarks) {
        this.maxExternalMarks = maxExternalMarks;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Faculty getAssignedFaculty() {
        return assignedFaculty;
    }

    public void setAssignedFaculty(Faculty assignedFaculty) {
        this.assignedFaculty = assignedFaculty;
    }
}

