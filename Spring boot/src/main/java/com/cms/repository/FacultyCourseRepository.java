package com.cms.repository;

import com.cms.entities.FacultyCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FacultyCourseRepository extends JpaRepository<FacultyCourse, Long> {

    Optional<FacultyCourse> findByFacultyIdAndCourseIdAndBatchId(Long facultyId, Long courseId, Long batchId);

    List<FacultyCourse> findByFacultyId(Long facultyId);
    
 // In FacultyCourseRepository.java:

 // Add this method:
 List<FacultyCourse> findAllByFacultyIdAndCourseId(Long facultyId, Long courseId);
    
    // Update to return the count of deleted entries
    @Modifying
    @Query("DELETE FROM FacultyCourse fc WHERE fc.faculty.id = :facultyId")
    int deleteByFacultyId(@Param("facultyId") Long facultyId);

	Optional<FacultyCourse> findByFacultyIdAndCourseId(Long facultyId, Long courseId);
	
	@Modifying
	@Query("DELETE FROM FacultyCourse fc WHERE fc.course.id = :courseId")
	void deleteByCourseId(@Param("courseId") Long courseId);
	
	@Modifying
	@Query("DELETE FROM FacultyCourse fc WHERE fc.batch.id = :batchId")
	void deleteByBatchId(@Param("batchId") Long batchId);
	@Query("SELECT COUNT(fc) FROM FacultyCourse fc WHERE fc.faculty.id = :facultyId AND fc.course.id = :courseId AND fc.batch.id = :batchId")
	long countByFacultyCourseBatch(@Param("facultyId") Long facultyId, @Param("courseId") Long courseId, @Param("batchId") Long batchId);


	// Add this new method to check if any other faculty has the same course-batch pair
    @Query("SELECT COUNT(fc) FROM FacultyCourse fc WHERE fc.course.id = :courseId AND fc.batch.id = :batchId AND fc.faculty.id != :facultyId")
    long countByCourseBatchWithDifferentFaculty(@Param("courseId") Long courseId, @Param("batchId") Long batchId, @Param("facultyId") Long facultyId);

    // Add this method to find which faculty has the course-batch pair
    @Query("SELECT fc.faculty.name FROM FacultyCourse fc WHERE fc.course.id = :courseId AND fc.batch.id = :batchId")
    List<String> findFacultyNamesByCourseBatch(@Param("courseId") Long courseId, @Param("batchId") Long batchId);
}

