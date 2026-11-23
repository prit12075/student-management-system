package com.university.sms.repository;

import com.university.sms.entity.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacultyRepository extends JpaRepository<Faculty, Long> {
    Optional<Faculty> findByEmployeeId(String employeeId);
    Optional<Faculty> findByUserUsername(String username);
    Optional<Faculty> findByUserId(Long userId);
    List<Faculty> findByDepartmentId(Long departmentId);

    @Query("SELECT f FROM Faculty f WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(f.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(f.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(f.employeeId) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(f.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(f.specialization) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:departmentId IS NULL OR f.department.id = :departmentId) " +
           "ORDER BY f.employeeId ASC")
    List<Faculty> searchFaculty(@Param("keyword") String keyword, @Param("departmentId") Long departmentId);
}

