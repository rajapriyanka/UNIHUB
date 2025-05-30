package com.cms.repository;

import com.cms.entities.Faculty;
import com.cms.entities.Leave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, Long> {
    List<Leave> findByFacultyId(Long facultyId);
    List<Leave> findByApproverId(Long approverId);
    List<Leave> findByFacultyIdAndStatus(Long facultyId, Leave.LeaveStatus status);
 // Explicitly define the query to ensure it works correctly
    @Query("SELECT l FROM Leave l WHERE l.approver.id = :approverId AND l.status = :status")
    List<Leave> findByApproverIdAndStatus(@Param("approverId") Long approverId, @Param("status") Leave.LeaveStatus status);
    
    @Modifying
    @Query("UPDATE Leave l SET l.approver = NULL WHERE l.approver.id = :facultyId")
    void removeFacultyAsApprover(@Param("facultyId") Long facultyId);

    @Modifying
    @Query("DELETE FROM Leave l WHERE l.faculty = :faculty")
    void deleteByFaculty(@Param("faculty") Faculty faculty);
    

    // Add this method to find leaves with null approvers
    List<Leave> findByApproverIsNull();
    


}

