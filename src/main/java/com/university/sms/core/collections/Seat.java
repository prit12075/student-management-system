package com.university.sms.core.collections;

/** NOVEL FEATURE - one allocated exam seat (Arindam). Label example: "HALL-1 B3". */
public record Seat(String room, int row, int column, String rollNumber, String studentName, String department) {

    public String label() {
        return room + " " + (char) ('A' + row) + (column + 1);
    }
}
