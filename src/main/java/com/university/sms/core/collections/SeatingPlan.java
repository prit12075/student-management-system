package com.university.sms.core.collections;

import java.util.List;
import java.util.Map;

/**
 * NOVEL FEATURE - result of the seating generator (Arindam).
 * {@code grids}: room name → rows → seats (null = intentionally empty seat).
 */
public record SeatingPlan(String examName, Map<String, List<List<Seat>>> grids, Map<String, Seat> seatByRoll,
                          List<String> unseated, int sameDepartmentNeighbours, int capacity) {

    public int seated() {
        return seatByRoll.size();
    }
}
