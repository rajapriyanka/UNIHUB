package com.cms.repository;

import com.cms.entities.Attendance;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    
    List<Attendance> findByFacultyId(Long facultyId);
    
    List<Attendance> findByStudentId(Long studentId);
    
    List<Attendance> findByCourseId(Long courseId);
    
    List<Attendance> findByBatchName(String batchName);
    
    List<Attendance> findByFacultyIdAndCourseIdAndBatchName(Long facultyId, Long courseId, String batchName);
    
    List<Attendance> findByStudentIdAndCourseId(Long studentId, Long courseId);
    

    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.course.id = :courseId")
    void deleteByCourseId(@Param("courseId") Long courseId);
    
    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.batchName = :batchName")
    void deleteByBatchName(@Param("batchName") String batchName);

    @Modifying
    @Transactional
    @Query("DELETE FROM Attendance a WHERE a.faculty.id = :facultyId")
    void deleteByFacultyId(@Param("facultyId") Long facultyId);


    
    List<Attendance> findByStudentIdAndCourseIdOrderByDateDesc(Long studentId, Long courseId);
    
    void deleteByStudentId(Long studentId);
}