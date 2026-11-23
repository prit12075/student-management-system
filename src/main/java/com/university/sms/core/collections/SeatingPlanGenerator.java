package com.university.sms.core.collections;

import com.university.sms.core.model.StudentRecord;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * NOVEL FEATURE - anti-malpractice exam seating plan (Arindam, Unit 4 Collections).
 *
 * <p>Students of the same department write the same paper, so they must not sit next to each
 * other. The generator:</p>
 * <ol>
 *   <li>deals students to the halls round-robin so each hall gets a mix of departments;</li>
 *   <li>inside a hall, groups students by department into a {@link TreeMap} of {@link ArrayDeque} queues;</li>
 *   <li>fills every room row by row; for each seat it picks the department with the <b>most students
 *       left</b> that is different from the left and front neighbours (greedy, sorted with a Comparator);</li>
 *   <li>if no department fits, the seat is left empty instead of breaking the rule; overflow moves on to the next hall;</li>
 *   <li>builds a {@link HashMap} roll → seat for O(1) lookup on the hall ticket.</li>
 * </ol>
 */
public class SeatingPlanGenerator {

    /** One examination hall. */
    public record ExamRoom(String name, int rows, int columns) {
        public int capacity() {
            return rows * columns;
        }
    }

    public SeatingPlan generate(String examName, List<StudentRecord> students, List<ExamRoom> rooms) {
        // 1. sort by department then roll, and deal students to halls like cards (round-robin),
        //    so every hall gets a mix of departments
        List<StudentRecord> ordered = new ArrayList<>(students);
        ordered.sort(Comparator.comparing(StudentRecord::getDepartmentCode).thenComparing(StudentRecord::getRoll));
        List<List<StudentRecord>> perRoom = new ArrayList<>();
        rooms.forEach(r -> perRoom.add(new ArrayList<>()));
        for (int i = 0; i < ordered.size() && !rooms.isEmpty(); i++) {
            perRoom.get(i % rooms.size()).add(ordered.get(i));
        }

        Map<String, List<List<Seat>>> grids = new LinkedHashMap<>();
        Map<String, Seat> byRoll = new HashMap<>();
        Deque<StudentRecord> carryOver = new ArrayDeque<>();          // students a full hall could not take
        int capacity = 0;

        for (int k = 0; k < rooms.size(); k++) {
            ExamRoom room = rooms.get(k);
            capacity += room.capacity();
            // 2. per-hall department queues (TreeMap of ArrayDeque)
            Map<String, Deque<StudentRecord>> queues = new TreeMap<>();
            List<StudentRecord> mine = new ArrayList<>(carryOver);
            mine.addAll(perRoom.get(k));
            carryOver.clear();
            for (StudentRecord s : mine) {
                queues.computeIfAbsent(s.getDepartmentCode(), d -> new ArrayDeque<>()).addLast(s);
            }
            Seat[][] grid = new Seat[room.rows()][room.columns()];
            for (int r = 0; r < room.rows(); r++) {
                for (int c = 0; c < room.columns(); c++) {
                    String left = c > 0 && grid[r][c - 1] != null ? grid[r][c - 1].department() : null;
                    String front = r > 0 && grid[r - 1][c] != null ? grid[r - 1][c].department() : null;
                    String dept = pickDepartment(queues, left, front);
                    if (dept == null) {
                        continue;                                   // leave the seat empty rather than break the rule
                    }
                    StudentRecord s = queues.get(dept).pollFirst();
                    if (queues.get(dept).isEmpty()) {
                        queues.remove(dept);
                    }
                    Seat seat = new Seat(room.name(), r, c, s.getRoll(), s.getFullName(), dept);
                    grid[r][c] = seat;
                    byRoll.put(s.getRoll(), seat);
                }
            }
            queues.values().forEach(carryOver::addAll);             // 3. overflow moves to the next hall
            List<List<Seat>> rowsList = new ArrayList<>();
            for (Seat[] row : grid) {
                rowsList.add(java.util.Arrays.asList(row));
            }
            grids.put(room.name(), rowsList);
        }

        List<String> unseated = new ArrayList<>();
        carryOver.forEach(s -> unseated.add(s.getRoll()));
        return new SeatingPlan(examName, grids, byRoll, unseated, countConflicts(grids), capacity);
    }

    /** Department with most remaining students that differs from both neighbours, or null. */
    private String pickDepartment(Map<String, Deque<StudentRecord>> queues, String left, String front) {
        List<Map.Entry<String, Deque<StudentRecord>>> candidates = new ArrayList<>(queues.entrySet());
        candidates.sort(Comparator.comparingInt((Map.Entry<String, Deque<StudentRecord>> e) -> e.getValue().size())
                .reversed()
                .thenComparing(Map.Entry::getKey));
        for (Map.Entry<String, Deque<StudentRecord>> e : candidates) {
            if (!e.getKey().equals(left) && !e.getKey().equals(front)) {
                return e.getKey();
            }
        }
        return null;
    }

    /** Verification: counts horizontally/vertically adjacent seats with the same department (should be 0). */
    static int countConflicts(Map<String, List<List<Seat>>> grids) {
        int conflicts = 0;
        for (List<List<Seat>> grid : grids.values()) {
            for (int r = 0; r < grid.size(); r++) {
                for (int c = 0; c < grid.get(r).size(); c++) {
                    Seat s = grid.get(r).get(c);
                    if (s == null) continue;
                    if (c + 1 < grid.get(r).size() && grid.get(r).get(c + 1) != null
                            && grid.get(r).get(c + 1).department().equals(s.department())) conflicts++;
                    if (r + 1 < grid.size() && grid.get(r + 1).get(c) != null
                            && grid.get(r + 1).get(c).department().equals(s.department())) conflicts++;
                }
            }
        }
        return conflicts;
    }
}
