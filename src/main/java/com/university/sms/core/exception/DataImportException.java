package com.university.sms.core.exception;

/**
 * UNIT 2 + UNIT 3 (Anjay).
 * Checked exception raised while reading a CSV file. It remembers the line number so the
 * admin sees exactly which row is wrong, and it can wrap the original cause
 * (NumberFormatException, IllegalArgumentException, IOException ...).
 */
public class DataImportException extends SmsException {

    private static final long serialVersionUID = 1L;

    private final int lineNumber;

    public DataImportException(int lineNumber, String message) {
        super("SMS-IMPORT-001", "Line " + lineNumber + ": " + message);
        this.lineNumber = lineNumber;
    }

    public DataImportException(int lineNumber, String message, Throwable cause) {
        super("SMS-IMPORT-001", "Line " + lineNumber + ": " + message, cause);
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
