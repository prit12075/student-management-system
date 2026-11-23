package com.university.sms.core.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * UNIT 1 (Prit Patel) - an <b>immutable value object</b>.
 * <ul>
 *   <li>{@code final class} + {@code private final} field = the object can never change after construction.</li>
 *   <li>Validation in the constructor (encapsulation: invalid objects cannot exist).</li>
 *   <li>Overrides {@link Object#equals}, {@link Object#hashCode} and {@link Object#toString}.</li>
 *   <li>Implements {@link Comparable} so roll numbers sort naturally.</li>
 * </ul>
 */
public final class RollNumber implements Comparable<RollNumber>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Letters/digits, 4 to 30 characters (e.g. STU2026001 or AP24110011578). */
    private static final Pattern FORMAT = Pattern.compile("[A-Z0-9]{4,30}");

    private final String value;

    public RollNumber(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Roll number cannot be empty");
        }
        String clean = raw.trim().toUpperCase();
        if (!FORMAT.matcher(clean).matches()) {
            throw new IllegalArgumentException("Invalid roll number format: " + raw);
        }
        this.value = clean;
    }

    /** Static factory method - an alternative to calling the constructor. */
    public static RollNumber of(String raw) {
        return new RollNumber(raw);
    }

    public static boolean isValid(String raw) {
        return raw != null && FORMAT.matcher(raw.trim().toUpperCase()).matches();
    }

    public String getValue() {
        return value;
    }

    /** Extracts the admission year when the roll number follows STUyyyy### or APyy### patterns. */
    public int admissionYear() {
        if (value.startsWith("STU") && value.length() >= 7) {
            return Integer.parseInt(value.substring(3, 7));
        }
        if (value.startsWith("AP") && value.length() >= 4 && Character.isDigit(value.charAt(2))) {
            return 2000 + Integer.parseInt(value.substring(2, 4));
        }
        return -1;
    }

    @Override
    public int compareTo(RollNumber other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RollNumber other)) return false;   // pattern matching for instanceof
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
