package com.university.sms.core.model;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * UNIT 1 + UNIT 2 (Prit Patel) - concrete subclass of {@link Person} (single inheritance).
 *
 * <ul>
 *   <li>{@code extends Person}: inherits fields/methods, calls the parent constructor with {@code super(...)}.</li>
 *   <li><b>Method overriding</b>: {@link #getRoleTitle()}, {@link #describe()}, {@link #toReportLine()}.</li>
 *   <li><b>Method overloading</b>: three versions of {@link #addSubjectScore}.</li>
 *   <li><b>Composition</b>: HAS-A {@link RollNumber} object.</li>
 *   <li>Implements {@link Comparable} - natural ordering by SGPA (highest first), then roll number.</li>
 * </ul>
 * This plain object is what the analytics engine, CSV/serialization I/O, JDBC archive and
 * JavaFX client all work on, so those modules do not depend on JPA/Hibernate.
 */
public class StudentRecord extends Person implements Comparable<StudentRecord> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final RollNumber rollNumber;
    private int semester;
    private double attendancePercentage;
    private double averagePercentage;
    private double sgpa;
    private int backlogs;
    private final ArrayList<Double> subjectScores = new ArrayList<>();

    public StudentRecord(Long id, String rollNumber, String firstName, String lastName, String email,
                         String departmentCode, int semester) {
        super(id, firstName, lastName, email, departmentCode);   // call parent constructor first
        this.rollNumber = RollNumber.of(rollNumber);
        this.semester = semester;
    }

    /** Builder-style "with" method returning this, so calls can be chained. */
    public StudentRecord withPerformance(double attendancePercentage, double averagePercentage,
                                         double sgpa, int backlogs) {
        this.attendancePercentage = attendancePercentage;
        this.averagePercentage = averagePercentage;
        this.sgpa = sgpa;
        this.backlogs = backlogs;
        return this;
    }

    @Override
    public String getRoleTitle() {
        return "Student";
    }

    @Override
    public String describe() {
        // super.describe() re-uses the parent version and extends it
        return super.describe() + " [" + rollNumber + ", Sem " + semester + ", SGPA " + sgpa + "]";
    }

    @Override
    public String toReportLine() {
        return Reportable.join(rollNumber.getValue(), getFullName(), departmentCode,
                String.format("%.1f%%", attendancePercentage), String.format("SGPA %.2f", sgpa));
    }

    // ----------- method overloading: same name, different parameter lists -----------

    public void addSubjectScore(double percentage) {
        subjectScores.add(percentage);
    }

    public void addSubjectScore(double obtained, double maximum) {
        addSubjectScore(maximum == 0 ? 0 : obtained * 100.0 / maximum);
    }

    public void addSubjectScore(double internal, double assignment, double practical, double external) {
        addSubjectScore(internal + assignment + practical + external, 100);
    }

    public boolean isAttendanceShort() {
        return !UniversityInfo.isAttendanceEligible(attendancePercentage);
    }

    @Override
    public int compareTo(StudentRecord other) {
        int bySgpa = Double.compare(other.sgpa, this.sgpa);   // descending SGPA
        return bySgpa != 0 ? bySgpa : this.rollNumber.compareTo(other.rollNumber);
    }

    public RollNumber getRollNumber() {
        return rollNumber;
    }

    public String getRoll() {
        return rollNumber.getValue();
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public double getAveragePercentage() {
        return averagePercentage;
    }

    public double getSgpa() {
        return sgpa;
    }

    public int getBacklogs() {
        return backlogs;
    }

    /** Returns a read-only view so callers cannot modify the internal list (encapsulation). */
    public List<Double> getSubjectScores() {
        return Collections.unmodifiableList(subjectScores);
    }
}
