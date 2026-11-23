package com.university.sms.core.exception;

/**
 * UNIT 2 - Exception Handling (Anjay).
 * <b>Unchecked</b> exception (extends RuntimeException): the compiler does not force callers to
 * catch it. Used when a requested record (student, archive file, backup) does not exist.
 */
public class RecordNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RecordNotFoundException(String entity, Object id) {
        super(entity + " not found: " + id);
    }
}
