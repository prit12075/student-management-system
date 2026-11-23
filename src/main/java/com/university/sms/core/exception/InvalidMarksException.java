package com.university.sms.core.exception;

/**
 * UNIT 2 - Exception Handling (Anjay).
 * Checked exception thrown by {@link MarksValidator} when a faculty member types a mark that is
 * negative or above the component maximum (e.g. 35 out of 30 in the internal exam).
 */
public class InvalidMarksException extends SmsException {

    private static final long serialVersionUID = 1L;

    private final String component;
    private final double enteredValue;
    private final double maximum;

    public InvalidMarksException(String component, double enteredValue, double maximum) {
        super("SMS-MARKS-001", String.format("%s marks must be between 0 and %.0f, but %.1f was entered",
                component, maximum, enteredValue));
        this.component = component;
        this.enteredValue = enteredValue;
        this.maximum = maximum;
    }

    public String getComponent() {
        return component;
    }

    public double getEnteredValue() {
        return enteredValue;
    }

    public double getMaximum() {
        return maximum;
    }
}
