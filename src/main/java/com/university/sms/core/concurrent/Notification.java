package com.university.sms.core.concurrent;

import java.time.LocalDateTime;

/** NOVEL FEATURE - one inbox message for a student (Vivekanand). */
public record Notification(long id, LocalDateTime createdAt, String type, String title, String message) {

    /** Bootstrap colour used by the inbox page for this notification type. */
    public String colour() {
        return switch (type) {
            case "ATTENDANCE" -> "danger";
            case "MARKS" -> "success";
            case "RISK" -> "warning";
            default -> "primary";
        };
    }
}
