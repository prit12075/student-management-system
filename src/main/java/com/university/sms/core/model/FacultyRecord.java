package com.university.sms.core.model;

import java.io.Serial;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * UNIT 1 + UNIT 2 (Prit Patel) - second subclass of {@link Person} (hierarchical inheritance:
 * one parent, two children). Overrides {@link #getRoleTitle()} differently from
 * {@link StudentRecord}, so the same call {@code person.getRoleTitle()} gives different
 * results at run time - <b>dynamic method dispatch</b>.
 */
public class FacultyRecord extends Person {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String employeeId;
    private final String designation;
    private final LinkedHashSet<String> subjectCodes = new LinkedHashSet<>();

    public FacultyRecord(Long id, String employeeId, String firstName, String lastName,
                         String email, String departmentCode, String designation) {
        super(id, firstName, lastName, email, departmentCode);
        this.employeeId = employeeId;
        this.designation = designation == null ? "Faculty" : designation;
    }

    @Override
    public String getRoleTitle() {
        return designation;
    }

    @Override
    public String toReportLine() {
        return Reportable.join(employeeId, getFullName(), departmentCode, String.join(",", subjectCodes));
    }

    /** Varargs parameter: accepts any number of subject codes. */
    public FacultyRecord teaches(String... codes) {
        for (String code : codes) {
            subjectCodes.add(code.toUpperCase());
        }
        return this;
    }

    public int getTeachingLoad() {
        return subjectCodes.size();
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getDesignation() {
        return designation;
    }

    public Set<String> getSubjectCodes() {
        return Set.copyOf(subjectCodes);
    }
}
