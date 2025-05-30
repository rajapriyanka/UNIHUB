package com.cms.service;

import com.cms.dto.FacultyCourseDTO;
import com.cms.entities.*;
import com.cms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FacultyCourseService {

    @Autowired
    private FacultyCourseRepository facultyCourseRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private BatchRepository batchRepository;
    
    @Autowired
    private TimetableEntryRepository timetableEntryRepository;
    
    @Autowired
    private SubstituteRequestRepository substituteRequestRepository;

    /**
     * Fetches all available courses.
     */
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    /**
     * Fetches all available batches.
     */
    public List<Batch> getAllBatches() {
        return batchRepository.findAll();
    }


    /**
     * Adds a course to a batch for a faculty, checking for duplicates
     * @return A message indicating the result of the operation
     */
    @Transactional
    public String addCourseToBatch(Long facultyId, Long courseId, Long batchId) {
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found"));

        // Check if the faculty-course-batch entry already exists
        Optional<FacultyCourse> existingEntry = facultyCourseRepository
                .findByFacultyIdAndCourseIdAndBatchId(facultyId, courseId, batchId);

        if (existingEntry.isPresent()) {
            // Entry already exists, return a message
            return "Course '" + course.getTitle() + "' is already assigned to this faculty for batch " + 
               batch.getBatchName() + " " + batch.getSection();
        } 
        
        // Check if any other faculty has the same course-batch pair
        long otherFacultyCount = facultyCourseRepository.countByCourseBatchWithDifferentFaculty(courseId, batchId, facultyId);
        if (otherFacultyCount > 0) {
            List<String> facultyNames = facultyCourseRepository.findFacultyNamesByCourseBatch(courseId, batchId);
            String assignedFaculty = facultyNames.isEmpty() ? "another faculty" : facultyNames.get(0);
            return "Course '" + course.getTitle() + "' for batch " + batch.getBatchName() + " " + batch.getSection() + 
               " is already assigned to " + assignedFaculty + ". A course-batch pair can only be assigned to one faculty.";
        } else {
            // Create a new faculty-course entry
            FacultyCourse facultyCourse = new FacultyCourse();
            facultyCourse.setFaculty(faculty);
            facultyCourse.setCourse(course);
            facultyCourse.setBatch(batch);
            facultyCourseRepository.save(facultyCourse);
        
            return "Course '" + course.getTitle() + "' successfully assigned to faculty for batch " + 
               batch.getBatchName() + " " + batch.getSection();
        }
    }

    @Transactional
    public FacultyCourse addCourse(Long facultyId, FacultyCourseDTO dto) {
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        // Ensure course exists before adding
        Course course = courseRepository.findByCode(dto.getCode())
                .orElseThrow(() -> new RuntimeException("Course not found. Please select an existing course."));

        // Ensure batch exists before adding
        Batch batch = batchRepository.findByBatchNameAndDepartmentAndSection(
            dto.getBatchName(), dto.getDepartment(), dto.getSection())
                .orElseThrow(() -> new RuntimeException("Batch not found. Please select an existing batch."));

        // Check if the faculty-course-batch entry already exists
        Optional<FacultyCourse> existingFacultyCourse = facultyCourseRepository.findByFacultyIdAndCourseIdAndBatchId(
            facultyId, course.getId(), batch.getId());

        if (existingFacultyCourse.isPresent()) {
            // Entry already exists, just return the existing entry
            return existingFacultyCourse.get();
        } else {
            // Check if any other faculty has the same course-batch pair
            long otherFacultyCount = facultyCourseRepository.countByCourseBatchWithDifferentFaculty(
                course.getId(), batch.getId(), facultyId);
        
            if (otherFacultyCount > 0) {
                throw new RuntimeException("This course-batch pair is already assigned to another faculty. " +
                    "A course-batch pair can only be assigned to one faculty.");
            }
        
            // Create a new faculty-course entry
            FacultyCourse facultyCourse = new FacultyCourse(faculty, course, batch);
            return facultyCourseRepository.save(facultyCourse);
        }
    }

    @Transactional
    public void removeCourse(Long facultyId, Long courseId, Long batchId) {
        FacultyCourse facultyCourse = facultyCourseRepository.findByFacultyIdAndCourseIdAndBatchId(facultyId, courseId, batchId)
                .orElseThrow(() -> new RuntimeException("Faculty course not found"));

        // Step 1: Find all timetable entries for this faculty-course-batch
        List<TimetableEntry> timetableEntries = timetableEntryRepository
                .findByFacultyIdAndCourseIdAndBatchId(facultyId, courseId, batchId);

        // Step 2: Delete all substitute requests linked to these timetable entries
        for (TimetableEntry entry : timetableEntries) {
            substituteRequestRepository.deleteByTimetableEntryId(entry.getId());
        }

        // Step 3: Delete timetable entries
        timetableEntryRepository.deleteAll(timetableEntries);

        // Step 4: Delete the faculty-course mapping
        facultyCourseRepository.delete(facultyCourse);
    }


    /**
     * Get assigned courses for a faculty.
     */
    @Transactional(readOnly = true)
    public List<FacultyCourseDTO> getAssignedCourses(Long facultyId) {
        List<FacultyCourse> facultyCourses = facultyCourseRepository.findByFacultyId(facultyId);
        return facultyCourses.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private FacultyCourseDTO convertToDTO(FacultyCourse facultyCourse) {
        FacultyCourseDTO dto = new FacultyCourseDTO();
        dto.setCourseId(facultyCourse.getCourse().getId());
        dto.setCode(facultyCourse.getCourse().getCode());
        dto.setTitle(facultyCourse.getCourse().getTitle());
        dto.setBatchId(facultyCourse.getBatch().getId());
        dto.setBatchName(facultyCourse.getBatch().getBatchName());
        dto.setDepartment(facultyCourse.getBatch().getDepartment());
        dto.setSection(facultyCourse.getBatch().getSection());
        return dto;
    }

    /**
     * Checks if a faculty-course-batch combination already exists
     * @return true if the combination exists, false otherwise
     */
    public boolean isDuplicateFacultyCourse(Long facultyId, Long courseId, Long batchId) {
        return facultyCourseRepository.countByFacultyCourseBatch(facultyId, courseId, batchId) > 0;
    }

    // Other existing methods remain unchanged...
}

