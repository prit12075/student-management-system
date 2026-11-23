package com.university.sms.core.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * UNIT 1 + UNIT 2 (Prit Patel) - the <b>abstract base class</b> of the plain-Java domain model.
 *
 * <p>Concepts shown:</p>
 * <ul>
 *   <li><b>Abstraction</b>: abstract class with an abstract method {@link #getRoleTitle()}.</li>
 *   <li><b>Encapsulation</b>: private fields, public getters, validation in setters.</li>
 *   <li><b>Constructors</b>: parameterised constructor and constructor chaining with {@code this(...)}.</li>
 *   <li><b>static</b> member: an object counter shared by all objects of every subclass.</li>
 *   <li><b>final</b> method: {@link #getFullName()} cannot be overridden by subclasses.</li>
 *   <li><b>protected</b> member: visible to subclasses (inheritance).</li>
 *   <li>Implements three interfaces at once: {@link Identifiable}, {@link Reportable}, {@link Serializable}.</li>
 * </ul>
 * Subclasses: {@link StudentRecord}, {@link FacultyRecord}.
 */
public abstract class Person implements Identifiable<Long>, Reportable, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** static: one copy for the whole class, counts every Person-type object ever created. */
    private static int objectsCreated = 0;

    private final Long id;            // final: set once in the constructor
    private String firstName;
    private String lastName;
    private String email;
    protected String departmentCode;  // protected: directly usable by subclasses

    /** Full constructor. */
    protected Person(Long id, String firstName, String lastName, String email, String departmentCode) {
        this.id = id;                  // 'this' separates the field from the parameter
        setFirstName(firstName);
        setLastName(lastName);
        this.email = email;
        this.departmentCode = departmentCode;
        objectsCreated++;
    }

    /** Constructor chaining: delegates to the full constructor using this(...). */
    protected Person(Long id, String firstName, String lastName) {
        this(id, firstName, lastName, null, "GEN");
    }

    /** Abstract method - every concrete subclass MUST implement it (runtime polymorphism). */
    public abstract String getRoleTitle();

    /** final method - same behaviour for every subclass, cannot be overridden. */
    public final String getFullName() {
        return firstName + " " + lastName;
    }

    /** Overridable method - subclasses add their own details and call super.describe(). */
    public String describe() {
        return getRoleTitle() + ": " + getFullName() + " (" + departmentCode + ")";
    }

    public static int getObjectsCreated() {
        return objectsCreated;
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public final void setFirstName(String firstName) {
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First name is required");
        }
        this.firstName = firstName.trim();
    }

    public String getLastName() {
        return lastName;
    }

    public final void setLastName(String lastName) {
        this.lastName = lastName == null ? "" : lastName.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return Objects.equals(id, person.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return describe();
    }
}
