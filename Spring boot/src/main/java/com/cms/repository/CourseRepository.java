package com.cms.repository;

import com.cms.entities.Course;
import com.cms.entities.Course.CourseType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByTitleContainingOrCodeContaining(String title, String code);
    List<Course> findByDepartment(String department);
    Optional<Course> findByCode(String code);
 // Add this method to find courses by code, title, and type
    boolean existsByCodeAndTitleAndType(String code, String title, CourseType type);
    
    // Find courses by code and title
    List<Course> findByCodeAndTitle(String code, String title);
    
    // Find a course by code, title, and type
    Optional<Course> findByCodeAndTitleAndType(String code, String title, CourseType type);

    List<Course> findBySemesterNo(Integer semesterNo);
    
    // Add these methods to the CourseRepository interface
    boolean existsByCodeAndType(String code, CourseType type);
    Optional<Course> findByCodeAndType(String code, CourseType type);
    
}

