package com.cms.repository;

import com.cms.entities.Faculty;
import com.cms.entities.SubstituteRequest;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubstituteRequestRepository extends JpaRepository<SubstituteRequest, Long> {
    
    List<SubstituteRequest> findByRequesterId(Long requesterId);
    
    List<SubstituteRequest> findBySubstituteId(Long substituteId);
    
    List<SubstituteRequest> findByRequesterIdAndStatus(Long requesterId, SubstituteRequest.RequestStatus status);
    
    List<SubstituteRequest> findBySubstituteIdAndStatus(Long substituteId, SubstituteRequest.RequestStatus status);
    
    @Query("SELECT sr FROM SubstituteRequest sr WHERE sr.substitute.id = :facultyId AND sr.requestDate = :date")
    List<SubstituteRequest> findBySubstituteIdAndDate(@Param("facultyId") Long facultyId, @Param("date") LocalDate date);
    
    @Query("SELECT sr FROM SubstituteRequest sr WHERE sr.requester.id = :facultyId AND sr.requestDate = :date")
    List<SubstituteRequest> findByRequesterIdAndDate(@Param("facultyId") Long facultyId, @Param("date") LocalDate date);

    void deleteByRequesterOrSubstitute(Faculty requester, Faculty substitute);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM SubstituteRequest sr WHERE sr.timetableEntry.faculty.id = :facultyId AND sr.timetableEntry.course.id = :courseId AND sr.timetableEntry.batch.id = :batchId")
    void deleteByTimetableEntryFacultyCourseBatch(@Param("facultyId") Long facultyId, @Param("courseId") Long courseId, @Param("batchId") Long batchId);

    @Modifying
    @Transactional
    @Query("DELETE FROM SubstituteRequest sr WHERE sr.timetableEntry.id = :timetableEntryId")
    void deleteByTimetableEntryId(@Param("timetableEntryId") Long timetableEntryId);
    



}