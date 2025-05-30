package com.cms.repository;

import com.cms.entities.Faculty;
import com.cms.entities.TimetableEntry;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {
    List<TimetableEntry> findByFacultyId(Long facultyId);
    
    void deleteByFaculty(Faculty faculty);
    List<TimetableEntry> findByFacultyIdAndCourseIdAndBatchId(Long facultyId, Long courseId, Long batchId);

    
    List<TimetableEntry> findByBatchIdAndAcademicYearAndSemester(Long batchId, String academicYear, String semester);
    
    @Query("SELECT te FROM TimetableEntry te JOIN te.timeSlot ts WHERE te.faculty.id = :facultyId AND ts.day = :day AND ts.periodNumber = :periodNumber AND ts.isBreak = false")
    Optional<TimetableEntry> findByFacultyIdAndDayAndPeriod(@Param("facultyId") Long facultyId, @Param("day") DayOfWeek day, @Param("periodNumber") Integer periodNumber);
    
    @Query("SELECT te FROM TimetableEntry te JOIN te.timeSlot ts WHERE te.batch.id = :batchId AND ts.day = :day AND ts.periodNumber = :periodNumber AND ts.isBreak = false")
    Optional<TimetableEntry> findByBatchIdAndDayAndPeriod(@Param("batchId") Long batchId, @Param("day") DayOfWeek day, @Param("periodNumber") Integer periodNumber);
    
    @Query("SELECT COUNT(te) FROM TimetableEntry te JOIN te.timeSlot ts WHERE te.faculty.id = :facultyId AND ts.periodNumber = 1 AND ts.isBreak = false")
    Long countFirstPeriodsByFacultyId(@Param("facultyId") Long facultyId);
    
    @Query("SELECT COUNT(te) FROM TimetableEntry te JOIN te.timeSlot ts WHERE te.faculty.id = :facultyId AND ts.periodNumber = 2 AND ts.isBreak = false")
    Long countSecondPeriodsByFacultyId(@Param("facultyId") Long facultyId);
    
    @Query("SELECT COUNT(te) FROM TimetableEntry te JOIN te.timeSlot ts WHERE te.faculty.id = :facultyId AND ts.periodNumber = 8 AND ts.isBreak = false")
    Long countLastPeriodsByFacultyId(@Param("facultyId") Long facultyId);
    
  
    List<TimetableEntry> findByFacultyIdAndBatchId(Long facultyId, Long batchId);
    @Modifying
    @Transactional
    @Query("DELETE FROM TimetableEntry te WHERE te.faculty.id = :facultyId")
    int deleteByFacultyId(@Param("facultyId") Long facultyId);
    
    // Delete timetable entries based on course ID
    @Modifying
    @Transactional
    @Query("DELETE FROM TimetableEntry te WHERE te.course.id = :courseId")
    int deleteByCourseId(@Param("courseId") Long courseId);
    
    // Delete timetable entries based on batch ID
    @Modifying
    @Transactional
    @Query("DELETE FROM TimetableEntry te WHERE te.batch.id = :batchId")
    int deleteByBatchId(@Param("batchId") Long batchId);

    // Delete timetable entries based on faculty and course
    @Modifying
    @Transactional
    @Query("DELETE FROM TimetableEntry te WHERE te.faculty.id = :facultyId AND te.course.id = :courseId AND te.batch.id = :batchId")
    int deleteByFacultyIdAndCourseIdAndBatchId(@Param("facultyId") Long facultyId, @Param("courseId") Long courseId, @Param("batchId") Long batchId);
    
    // Fixed query to properly join with timeSlot to access the day property
    @Query("SELECT te FROM TimetableEntry te JOIN te.timeSlot ts WHERE te.batch.id = :batchId AND ts.day = :day")
    List<TimetableEntry> findByBatchIdAndDay(@Param("batchId") Long batchId, @Param("day") DayOfWeek day);
    
    // Find all entries for a faculty on a specific day
    @Query("SELECT te FROM TimetableEntry te JOIN te.timeSlot ts WHERE te.faculty.id = :facultyId AND ts.day = :day")
    List<TimetableEntry> findByFacultyIdAndDay(@Param("facultyId") Long facultyId, @Param("day") DayOfWeek day);
    
    // Find all entries for a course on a specific day
    @Query("SELECT te FROM TimetableEntry te JOIN te.timeSlot ts WHERE te.course.id = :courseId AND ts.day = :day")
    List<TimetableEntry> findByCourseIdAndDay(@Param("courseId") Long courseId, @Param("day") DayOfWeek day);
    
    // Find all lab entries for a batch
    @Query("SELECT te FROM TimetableEntry te WHERE te.batch.id = :batchId AND te.course.type = 'LAB'")
    List<TimetableEntry> findLabEntriesByBatchId(@Param("batchId") Long batchId);
    
    // Find all lab entries for a batch on a specific day
    @Query("SELECT te FROM TimetableEntry te JOIN te.timeSlot ts WHERE te.batch.id = :batchId AND te.course.type = 'LAB' AND ts.day = :day")
    List<TimetableEntry> findLabEntriesByBatchIdAndDay(@Param("batchId") Long batchId, @Param("day") DayOfWeek day);
    
    // Find all entries for a faculty in a specific academic year and semester
    List<TimetableEntry> findByFacultyIdAndAcademicYearAndSemester(Long facultyId, String academicYear, String semester);
    
    // Find all entries for a specific course
    List<TimetableEntry> findByCourseId(Long courseId);
    
    // Find all entries with faculty conflicts (same faculty, day, period)
    @Query("SELECT te FROM TimetableEntry te JOIN te.timeSlot ts WHERE te.faculty.id = :facultyId AND ts.day = :day AND ts.periodNumber = :periodNumber GROUP BY ts.id HAVING COUNT(te) > 1")
    List<TimetableEntry> findFacultyConflicts(@Param("facultyId") Long facultyId, @Param("day") DayOfWeek day, @Param("periodNumber") Integer periodNumber);
    
    // Find all entries with batch conflicts (same batch, day, period)
    @Query("SELECT te FROM TimetableEntry te JOIN te.timeSlot ts WHERE te.batch.id = :batchId AND ts.day = :day AND ts.periodNumber = :periodNumber GROUP BY ts.id HAVING COUNT(te) > 1")
    List<TimetableEntry> findBatchConflicts(@Param("batchId") Long batchId, @Param("day") DayOfWeek day, @Param("periodNumber") Integer periodNumber);
    
    // Count lab courses for a batch on a specific day
    @Query("SELECT COUNT(DISTINCT te.course.id) FROM TimetableEntry te JOIN te.timeSlot ts WHERE te.batch.id = :batchId AND ts.day = :day AND te.course.type = 'LAB'")
    Integer countLabCoursesForBatchOnDay(@Param("batchId") Long batchId, @Param("day") DayOfWeek day);
    
    // Find all consecutive theory classes for a course
    @Query("SELECT te1 FROM TimetableEntry te1 JOIN te1.timeSlot ts1, TimetableEntry te2 JOIN te2.timeSlot ts2 " +
           "WHERE te1.course.id = :courseId AND te2.course.id = :courseId " +
           "AND te1.batch.id = te2.batch.id AND ts1.day = ts2.day " +
           "AND (ts1.periodNumber = ts2.periodNumber - 1 OR ts1.periodNumber = ts2.periodNumber + 1)")
    List<TimetableEntry> findConsecutiveTheoryClasses(@Param("courseId") Long courseId);
}
