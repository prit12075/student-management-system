package com.university.sms.core.collections;

import com.university.sms.core.grading.Grade;
import com.university.sms.core.model.StudentRecord;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * UNIT 4 - Collections (Arindam).
 * Immutable bundle of everything the analytics engine computed; each field is built with a
 * different collection type (see {@link AcademicAnalyticsEngine}).
 */
public record AnalyticsReport(
        List<StudentRecord> rankList,                 // ArrayList sorted by a Comparator chain
        List<StudentRecord> toppers,                  // PriorityQueue (max-heap) top N
        Map<String, List<StudentRecord>> byDepartment, // TreeMap -> departments in alphabetical order
        Map<String, Double> departmentAverageSgpa,    // TreeMap
        EnumMap<Grade, Integer> gradeDistribution,    // EnumMap keyed by the Grade enum
        Set<String> shortageRollNumbers,              // TreeSet -> sorted & unique
        List<String> promotionQueue,                  // LinkedList used as a FIFO queue
        List<String> detainedAfterIteratorFilter,     // ArrayList filtered with Iterator.remove()
        int distinctDepartments,                      // HashSet size
        String highestSgpa,                           // Collections.max
        String lowestAttendance,                      // Collections.min
        int rankOfSearched,                           // Collections.binarySearch result
        String searchedRoll) {
}
