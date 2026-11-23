package com.university.sms.repository;

import com.university.sms.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findAllByOrderByDueDateAsc();
    List<Assignment> findBySubjectIdOrderByDueDateAsc(Long subjectId);
    List<Assignment> findByCreatedByIdOrderByDueDateAsc(Long facultyId);
    List<Assignment> findBySubjectDepartmentIdAndSubjectSemesterOrderByDueDateAsc(Long departmentId, Integer semester);
    void deleteBySubjectId(Long subjectId);
}

