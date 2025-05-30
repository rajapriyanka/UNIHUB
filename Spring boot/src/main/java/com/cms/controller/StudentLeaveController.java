package com.cms.controller;

import com.cms.dto.LeaveActionDTO;
import com.cms.dto.StudentLeaveRequestDTO;
import com.cms.dto.StudentLeaveResponseDTO;
import com.cms.service.StudentLeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student-leave")
public class StudentLeaveController {

    @Autowired
    private StudentLeaveService studentLeaveService;

    @PostMapping("/request/{studentId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentLeaveResponseDTO> requestLeave(
            @PathVariable Long studentId,
            @RequestBody StudentLeaveRequestDTO leaveRequestDTO) {
        StudentLeaveResponseDTO response = studentLeaveService.requestLeave(studentId, leaveRequestDTO);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<StudentLeaveResponseDTO>> getLeaveRequestsByStudent(@PathVariable Long studentId) {
        List<StudentLeaveResponseDTO> leaves = studentLeaveService.getLeaveRequestsByStudent(studentId);
        return ResponseEntity.ok(leaves);
    }

    @PutMapping("/{leaveId}/action/{facultyId}")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<StudentLeaveResponseDTO> updateLeaveStatus(
            @PathVariable Long leaveId,
            @PathVariable Long facultyId,
            @RequestBody LeaveActionDTO leaveActionDTO) {
        StudentLeaveResponseDTO response = studentLeaveService.updateLeaveStatus(leaveId, facultyId, leaveActionDTO);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/faculty/{facultyId}")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<List<StudentLeaveResponseDTO>> getLeaveRequestsForFaculty(@PathVariable Long facultyId) {
        List<StudentLeaveResponseDTO> leaves = studentLeaveService.getLeaveRequestsForFaculty(facultyId);
        return ResponseEntity.ok(leaves);
    }

    @GetMapping("/faculty/{facultyId}/pending")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<List<StudentLeaveResponseDTO>> getPendingLeaveRequestsForFaculty(@PathVariable Long facultyId) {
        List<StudentLeaveResponseDTO> leaves = studentLeaveService.getPendingLeaveRequestsForFaculty(facultyId);
        return ResponseEntity.ok(leaves);
    }
}

