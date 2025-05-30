package com.cms.service;

import com.cms.entities.Course;
import com.cms.repository.AttendanceRepository;
import com.cms.repository.CourseRepository;
import com.cms.repository.FacultyCourseRepository;
import com.cms.repository.TimetableEntryRepository;
import com.cms.exception.DuplicateCourseException;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private FacultyCourseRepository facultyCourseRepository;

    @Autowired
    private TimetableEntryRepository timetableEntryRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;
    
    @Autowired
    private ExcelService excelService;

    /**
     * Checks if a course with the same code and type already exists
     * @param course The course to check
     * @return true if a duplicate exists, false otherwise
     */
    private boolean isDuplicateCourse(Course course) {
        if (course.getCode() == null || course.getType() == null) {
            return false;
        }
        
        return courseRepository.findByCodeAndType(course.getCode().trim(), course.getType())
            .isPresent();
    }
    
    /**
     * Checks if updating a course would create a duplicate
     * @param id The ID of the course being updated
     * @param updatedCourse The updated course data
     * @return true if update would create a duplicate, false otherwise
     */
    private boolean isUpdateCreatingDuplicate(Long id, Course updatedCourse, Course existingCourse) {
        // If code or type is not changing, no duplicate can be created
        if ((updatedCourse.getCode() == null || updatedCourse.getCode().trim().equals(existingCourse.getCode())) &&
            (updatedCourse.getType() == null || updatedCourse.getType() == existingCourse.getType())) {
            return false;
        }
        
        // Determine the new code and type after update
        String newCode = updatedCourse.getCode() != null ? updatedCourse.getCode().trim() : existingCourse.getCode();
        Course.CourseType newType = updatedCourse.getType() != null ? updatedCourse.getType() : existingCourse.getType();
        
        // Check if another course with the same code and type exists (excluding this course)
        Optional<Course> conflictingCourse = courseRepository.findByCodeAndType(newCode, newType);
        return conflictingCourse.isPresent() && !conflictingCourse.get().getId().equals(id);
    }

    public Course registerCourse(Course course) {
        try {
            // Trim input values
            if (course.getCode() != null) {
                course.setCode(course.getCode().trim());
            }
            if (course.getTitle() != null) {
                course.setTitle(course.getTitle().trim());
            }
            if (course.getDepartment() != null) {
                course.setDepartment(course.getDepartment().trim());
            }
            
            // Check for duplicates before attempting to save
            if (isDuplicateCourse(course)) {
                throw new DuplicateCourseException(
                    "A course with the same code and type already exists. " +
                    "Courses with the same code must have different types."
                );
            }
            
            return courseRepository.save(course);
        } catch (DataIntegrityViolationException e) {
            // Check if the error is due to the unique constraint on code and type
            if (e.getMessage().contains("UK") || e.getMessage().contains("constraint")) {
                throw new DuplicateCourseException(
                    "A course with the same code and type already exists. " +
                    "Courses with the same code must have different types."
                );
            }
            throw e;
        }
    }

    public Course updateCourse(Long id, Course updatedCourse) {
        Course existingCourse = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        try {
            // Check for duplicates before attempting to update
            if (isUpdateCreatingDuplicate(id, updatedCourse, existingCourse)) {
                throw new DuplicateCourseException(
                    "Another course with the same code and type already exists. " +
                    "Courses with the same code must have different types."
                );
            }
            
            // Update the course fields
            if (updatedCourse.getTitle() != null && !updatedCourse.getTitle().trim().isEmpty()) {
                existingCourse.setTitle(updatedCourse.getTitle().trim());
            }
            if (updatedCourse.getCode() != null && !updatedCourse.getCode().trim().isEmpty()) {
                existingCourse.setCode(updatedCourse.getCode().trim());
            }
            if (updatedCourse.getContactPeriods() != null && updatedCourse.getContactPeriods() > 0) {
                existingCourse.setContactPeriods(updatedCourse.getContactPeriods());
            }
            if (updatedCourse.getSemesterNo() != null && updatedCourse.getSemesterNo() > 0) {
                existingCourse.setSemesterNo(updatedCourse.getSemesterNo());
            }
            if (updatedCourse.getDepartment() != null && !updatedCourse.getDepartment().trim().isEmpty()) {
                existingCourse.setDepartment(updatedCourse.getDepartment().trim());
            }
            if (updatedCourse.getType() != null) {
                existingCourse.setType(updatedCourse.getType());
            }

            return courseRepository.save(existingCourse);
        } catch (DataIntegrityViolationException e) {
            // Check if the error is due to the unique constraint on code and type
            if (e.getMessage().contains("UK") || e.getMessage().contains("constraint")) {
                throw new DuplicateCourseException(
                    "Another course with the same code and type already exists. " +
                    "Courses with the same code must have different types."
                );
            }
            throw e;
        }
    }

    @Transactional
    public void deleteCourse(Long courseId) {
    	facultyCourseRepository.deleteByCourseId(courseId);
        attendanceRepository.deleteByCourseId(courseId);
        timetableEntryRepository.deleteByCourseId(courseId);
        courseRepository.deleteById(courseId);
    }
    
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public List<Course> searchCourses(String query) {
        return courseRepository.findByTitleContainingOrCodeContaining(query, query);
    }
    
    public List<Course> uploadCoursesFromExcel(MultipartFile file) throws IOException {
        List<Course> courses = excelService.extractCoursesFromExcel(file);
        List<Course> savedCourses = new ArrayList<>();
        List<Course> updatedCourses = new ArrayList<>();
        List<Course> skippedCourses = new ArrayList<>();
        
        for (Course course : courses) {
            // Skip courses with missing required fields
            if (course.getTitle() == null || course.getTitle().isEmpty() ||
                course.getCode() == null || course.getCode().isEmpty() ||
                course.getContactPeriods() == null || course.getSemesterNo() == null ||
                course.getDepartment() == null || course.getDepartment().isEmpty() ||
                course.getType() == null) {
                skippedCourses.add(course);
                continue;
            }
            
            try {
                // Trim input values
                course.setCode(course.getCode().trim());
                course.setTitle(course.getTitle().trim());
                course.setDepartment(course.getDepartment().trim());
                
                // Check if a course with the same code and type already exists
                Optional<Course> existingCourse = courseRepository.findByCodeAndType(
                    course.getCode(), course.getType());
                
                if (existingCourse.isPresent()) {
                    // Update the existing course
                    Course existing = existingCourse.get();
                    existing.setTitle(course.getTitle());
                    existing.setContactPeriods(course.getContactPeriods());
                    existing.setSemesterNo(course.getSemesterNo());
                    existing.setDepartment(course.getDepartment());
                    
                    updatedCourses.add(courseRepository.save(existing));
                    System.out.println("Updated existing course: " + course.getCode() + " - " + course.getTitle());
                } else {
                    // Save as a new course
                    savedCourses.add(courseRepository.save(course));
                }
            } catch (DataIntegrityViolationException e) {
                // Log the error and continue with the next course
                System.err.println("Skipping duplicate course: " + course.getCode() + " - " + 
                                  course.getTitle() + " - " + course.getType() + 
                                  ". Error: " + e.getMessage());
                skippedCourses.add(course);
            } catch (Exception e) {
                // Log the error and continue with the next course
                System.err.println("Error saving course: " + course.getCode() + " - " + e.getMessage());
                skippedCourses.add(course);
            }
        }
        
        // Log summary
        System.out.println("Excel upload summary: " +
                          savedCourses.size() + " courses added, " +
                          updatedCourses.size() + " courses updated, " +
                          skippedCourses.size() + " courses skipped.");
        
        // Combine saved and updated courses for the response
        List<Course> result = new ArrayList<>(savedCourses);
        result.addAll(updatedCourses);
        return result;
    }
}
