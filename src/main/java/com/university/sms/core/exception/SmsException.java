package com.university.sms.core.exception;

/**
 * UNIT 2 - Exception Handling (Anjay).
 * Root of the project's own <b>checked</b> exception hierarchy.
 *
 * <pre>
 * Throwable
 *  └─ Exception
 *      ├─ SmsException                    (checked - caller must catch or declare)
 *      │   ├─ InvalidMarksException
 *      │   ├─ DataImportException
 *      │   └─ AttendanceShortageException
 *      └─ RuntimeException
 *          └─ RecordNotFoundException     (unchecked)
 * </pre>
 * Every SmsException carries a machine-readable {@link #getErrorCode() error code}.
 */
public class SmsException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String errorCode;

    public SmsException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /** Exception chaining: keeps the low-level cause (e.g. an IOException) inside. */
    public SmsException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return "[" + errorCode + "] " + getMessage();
    }
}
