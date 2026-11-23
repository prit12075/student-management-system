package com.university.sms.entity;

public enum AttendanceStatus {
    PRESENT("Present"),
    ABSENT("Absent"),
    LEAVE("On Leave");

    private final String label;

    AttendanceStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

