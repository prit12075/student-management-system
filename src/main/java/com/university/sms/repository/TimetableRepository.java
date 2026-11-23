package com.university.sms.repository;

import com.university.sms.entity.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimetableRepository extends JpaRepository<Timetable, Long> {
    List<Timetable> findAllByOrderByDayOfWeekAscStartTimeAsc();
    List<Timetable> findByDepartmentIdAndSemesterOrderByDayOfWeekAscStartTimeAsc(Long departmentId, Integer semester);
    List<Timetable> findBySubjectAssignedFacultyIdOrderByDayOfWeekAscStartTimeAsc(Long facultyId);
    void deleteBySubjectId(Long subjectId);
}

