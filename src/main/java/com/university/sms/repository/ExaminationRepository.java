package com.university.sms.repository;

import com.university.sms.entity.Examination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExaminationRepository extends JpaRepository<Examination, Long> {
    List<Examination> findAllByOrderByExamDateAscStartTimeAsc();
    List<Examination> findBySubjectDepartmentIdAndSemesterOrderByExamDateAsc(Long departmentId, Integer semester);
    List<Examination> findBySubjectIdOrderByExamDateAsc(Long subjectId);
}

