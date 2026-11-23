package com.university.sms.repository;

import com.university.sms.entity.Mark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarkRepository extends JpaRepository<Mark, Long> {
    List<Mark> findByStudentIdOrderBySubjectCodeAsc(Long studentId);
    List<Mark> findBySubjectIdOrderByStudentRollNumberAsc(Long subjectId);
    Optional<Mark> findByStudentIdAndSubjectId(Long studentId, Long subjectId);
    void deleteByStudentId(Long studentId);
    void deleteBySubjectId(Long subjectId);
}

