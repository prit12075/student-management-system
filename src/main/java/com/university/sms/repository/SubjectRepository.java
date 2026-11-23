package com.university.sms.repository;

import com.university.sms.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Optional<Subject> findByCode(String code);
    List<Subject> findByDepartmentIdOrderByCodeAsc(Long departmentId);
    List<Subject> findByDepartmentIdAndSemesterOrderByCodeAsc(Long departmentId, Integer semester);
    List<Subject> findByCourseIdAndSemesterOrderByCodeAsc(Long courseId, Integer semester);
    List<Subject> findByAssignedFacultyIdOrderByCodeAsc(Long facultyId);
}

