package com.cms.repository;

import com.cms.entities.Faculty;
import com.cms.entities.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByFacultyId(Long facultyId);
    
    void deleteByFaculty(Faculty faculty);
}

