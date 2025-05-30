package com.cms.repository;

import com.cms.entities.Faculty;
import com.cms.entities.StudentLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentLeaveRepository extends JpaRepository<StudentLeave, Long> {
    List<StudentLeave> findByStudentId(Long studentId);
    List<StudentLeave> findByFacultyId(Long facultyId);
    List<StudentLeave> findByFacultyIdAndStatus(Long facultyId, StudentLeave.LeaveStatus status);
    
    void deleteByFaculty(Faculty faculty);
    void deleteByStudentId(Long studentId);
}

