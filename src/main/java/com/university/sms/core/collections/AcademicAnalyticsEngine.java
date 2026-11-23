package com.university.sms.core.collections;

import com.university.sms.core.grading.Grade;
import com.university.sms.core.model.StudentRecord;
import com.university.sms.core.model.UniversityInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * UNIT 4 - Collections Framework (Arindam).
 *
 * <p>Builds the University Analytics report using the right collection for each job:</p>
 * <table>
 *   <tr><td>{@link ArrayList}</td><td>rank list, sorted with a {@link Comparator} chain</td></tr>
 *   <tr><td>{@link LinkedList}</td><td>promotion queue (FIFO via the {@link Queue} interface)</td></tr>
 *   <tr><td>{@link PriorityQueue}</td><td>top-N toppers (heap ordered by SGPA)</td></tr>
 *   <tr><td>{@link HashSet}</td><td>distinct departments (unique, unordered)</td></tr>
 *   <tr><td>{@link TreeSet}</td><td>attendance-shortage roll numbers (unique, sorted)</td></tr>
 *   <tr><td>{@link TreeMap}</td><td>students grouped per department, keys sorted</td></tr>
 *   <tr><td>{@link EnumMap}</td><td>grade distribution keyed by the {@link Grade} enum</td></tr>
 *   <tr><td>{@link Iterator}</td><td>safe removal while iterating</td></tr>
 *   <tr><td>{@link Collections}</td><td>sort, max, min, binarySearch, unmodifiableList</td></tr>
 * </table>
 */
public class AcademicAnalyticsEngine {

    /** Comparator chain: SGPA high→low, then attendance high→low, then roll number A→Z. */
    public static final Comparator<StudentRecord> MERIT_ORDER =
            Comparator.comparingDouble(StudentRecord::getSgpa).reversed()
                    .thenComparing(Comparator.comparingDouble(StudentRecord::getAttendancePercentage).reversed())
                    .thenComparing(StudentRecord::getRoll);

    public AnalyticsReport analyse(List<StudentRecord> students, int topN, String searchRoll) {
        // 1. ArrayList + Collections.sort with the comparator chain
        List<StudentRecord> rankList = new ArrayList<>(students);
        Collections.sort(rankList, MERIT_ORDER);

        // 2. PriorityQueue as a max-heap on SGPA; poll() always returns the current best
        PriorityQueue<StudentRecord> heap = new PriorityQueue<>(MERIT_ORDER);
        heap.addAll(students);
        List<StudentRecord> toppers = new ArrayList<>();
        while (!heap.isEmpty() && toppers.size() < topN) {
            toppers.add(heap.poll());
        }

        // 3. TreeMap<String, List<...>>: grouping with computeIfAbsent, keys kept sorted
        Map<String, List<StudentRecord>> byDept = new TreeMap<>();
        for (StudentRecord s : students) {
            byDept.computeIfAbsent(s.getDepartmentCode(), k -> new ArrayList<>()).add(s);
        }

        // 4. TreeMap of averages computed by iterating the entrySet
        Map<String, Double> deptAvg = new TreeMap<>();
        for (Map.Entry<String, List<StudentRecord>> e : byDept.entrySet()) {
            double sum = 0;
            for (StudentRecord s : e.getValue()) {
                sum += s.getSgpa();
            }
            deptAvg.put(e.getKey(), Math.round(sum / e.getValue().size() * 100.0) / 100.0);
        }

        // 5. EnumMap: one counter per Grade constant, in enum order
        EnumMap<Grade, Integer> distribution = new EnumMap<>(Grade.class);
        for (Grade g : Grade.values()) {
            distribution.put(g, 0);
        }
        for (StudentRecord s : students) {
            for (double score : s.getSubjectScores()) {
                distribution.merge(Grade.fromPercentage(score), 1, Integer::sum);
            }
        }

        // 6. TreeSet: unique + sorted roll numbers of students below 75%
        Set<String> shortage = new TreeSet<>();
        for (StudentRecord s : students) {
            if (s.getAttendancePercentage() < UniversityInfo.MIN_ATTENDANCE_PERCENT) {
                shortage.add(s.getRoll());
            }
        }

        // 7. LinkedList used through the Queue interface: offer() at the tail, poll() from the head
        Queue<String> promotion = new LinkedList<>();
        for (StudentRecord s : rankList) {
            if (s.getBacklogs() == 0 && !shortage.contains(s.getRoll())) {
                promotion.offer(s.getRoll());
            }
        }
        List<String> promotionOrder = new ArrayList<>();
        while (!promotion.isEmpty()) {
            promotionOrder.add(promotion.poll());
        }

        // 8. Iterator.remove(): start with everyone, remove the eligible ones while iterating
        List<StudentRecord> detained = new ArrayList<>(students);
        Iterator<StudentRecord> it = detained.iterator();
        while (it.hasNext()) {
            StudentRecord s = it.next();
            if (s.getBacklogs() == 0 && !s.isAttendanceShort()) {
                it.remove();               // calling list.remove() here would throw ConcurrentModificationException
            }
        }
        List<String> detainedRolls = new ArrayList<>();
        for (StudentRecord s : detained) {
            detainedRolls.add(s.getRoll());
        }

        // 9. HashSet: duplicates are ignored automatically
        Set<String> departments = new HashSet<>();
        for (StudentRecord s : students) {
            departments.add(s.getDepartmentCode());
        }

        // 10. Collections utility methods
        String best = students.isEmpty() ? "-" :
                Collections.max(students, Comparator.comparingDouble(StudentRecord::getSgpa)).describe();
        String lowAtt = students.isEmpty() ? "-" :
                Collections.min(students, Comparator.comparingDouble(StudentRecord::getAttendancePercentage)).getRoll();

        List<String> sortedRolls = new ArrayList<>();
        for (StudentRecord s : students) {
            sortedRolls.add(s.getRoll());
        }
        Collections.sort(sortedRolls);                              // binarySearch needs a sorted list
        String key = searchRoll == null ? "" : searchRoll.trim().toUpperCase();
        int position = key.isEmpty() ? -1 : Collections.binarySearch(sortedRolls, key);

        return new AnalyticsReport(
                Collections.unmodifiableList(rankList),
                Collections.unmodifiableList(toppers),
                Collections.unmodifiableMap(byDept),
                Collections.unmodifiableMap(deptAvg),
                distribution,
                Collections.unmodifiableSet(shortage),
                promotionOrder,
                detainedRolls,
                departments.size(),
                best,
                lowAtt,
                position,
                key);
    }
}
