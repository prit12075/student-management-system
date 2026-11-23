package com.university.sms.repository;

import com.university.sms.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByStudentIdOrderByAttendanceDateDesc(Long studentId);
    List<Attendance> findByStudentIdAndSubjectIdOrderByAttendanceDateDesc(Long studentId, Long subjectId);
    List<Attendance> findBySubjectIdOrderByAttendanceDateDesc(Long subjectId);
    List<Attendance> findBySubjectIdAndAttendanceDate(Long subjectId, LocalDate attendanceDate);
    Optional<Attendance> findByStudentIdAndSubjectIdAndAttendanceDate(Long studentId, Long subjectId, LocalDate attendanceDate);
    void deleteByStudentId(Long studentId);
    void deleteBySubjectId(Long subjectId);
}

