package com.university.sms.repository;

import com.university.sms.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByRollNumber(String rollNumber);
    Optional<Student> findByUserUsername(String username);
    Optional<Student> findByUserId(Long userId);
    List<Student> findByDepartmentIdOrderByRollNumberAsc(Long departmentId);
    List<Student> findByDepartmentIdAndSemesterOrderByRollNumberAsc(Long departmentId, Integer semester);
    List<Student> findByCourseIdAndSemesterOrderByRollNumberAsc(Long courseId, Integer semester);

    @Query("SELECT s FROM Student s WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(s.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.rollNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:departmentId IS NULL OR s.department.id = :departmentId) " +
           "AND (:semester IS NULL OR s.semester = :semester) " +
           "ORDER BY s.rollNumber ASC")
    List<Student> searchStudents(@Param("keyword") String keyword,
                                 @Param("departmentId") Long departmentId,
                                 @Param("semester") Integer semester);
}

