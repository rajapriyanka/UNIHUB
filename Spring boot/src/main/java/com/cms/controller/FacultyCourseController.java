package com.cms.controller;

import com.cms.dto.FacultyCourseDTO;
import com.cms.entities.Batch;
import com.cms.entities.Course;
import com.cms.entities.FacultyCourse;
import com.cms.service.FacultyCourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faculties/dashboard")
public class FacultyCourseController {

    @Autowired
    private FacultyCourseService facultyCourseService;
    

    /**
     * Fetch assigned courses for a faculty.
     */
    @GetMapping("/faculty/{facultyId}/assigned-courses")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<List<FacultyCourseDTO>> getAssignedCourses(@PathVariable Long facultyId) {
        List<FacultyCourseDTO> assignedCourses = facultyCourseService.getAssignedCourses(facultyId);
        return ResponseEntity.ok(assignedCourses);
    }

    @PostMapping("/faculty/{facultyId}/courses/{courseId}/batches/{batchId}")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<String> addCourseToBatch(@PathVariable Long facultyId,
            @PathVariable Long courseId, 
            @PathVariable Long batchId) {
        
        String result = facultyCourseService.addCourseToBatch(facultyId, courseId, batchId);
        
        // Check if the result contains an error message about already assigned courses
        if (result.contains("already assigned")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }
        
        return ResponseEntity.ok(result);
    }

    
    @GetMapping("/courses")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<List<Course>> getAllCourses() {
        List<Course> courses = facultyCourseService.getAllCourses();
        return ResponseEntity.ok(courses);
    }

    /**
     * Fetch all available batches.
     */
    @GetMapping("/batches")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<List<Batch>> getAllBatches() {
        List<Batch> batches = facultyCourseService.getAllBatches();
        return ResponseEntity.ok(batches);
    }

    /**
     * Assign a faculty member to a course and batch.
     */
    @PostMapping("/{facultyId}/courses")
    @PreAuthorize("hasRole('FACULTY') and @userSecurity.checkFacultyId(authentication, #facultyId)")
    public ResponseEntity<FacultyCourse> addCourse(@PathVariable Long facultyId, @RequestBody FacultyCourseDTO dto) {
        FacultyCourse addedCourse = facultyCourseService.addCourse(facultyId, dto);
        return ResponseEntity.ok(addedCourse);
    }

    /**
     * Remove a faculty member from a course and batch.
     */
    @DeleteMapping("/{facultyId}/courses/{courseId}/batch/{batchId}")
    @PreAuthorize("hasRole('FACULTY') ")
    public ResponseEntity<Void> removeCourse(@PathVariable Long facultyId, @PathVariable Long courseId, @PathVariable Long batchId) {
        facultyCourseService.removeCourse(facultyId, courseId, batchId);
        return ResponseEntity.ok().build();
    }
}
