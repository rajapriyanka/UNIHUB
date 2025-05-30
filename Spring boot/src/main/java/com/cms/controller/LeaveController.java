package com.cms.controller;

import com.cms.dto.LeaveActionDTO;
import com.cms.dto.LeaveRequestDTO;
import com.cms.dto.LeaveResponseDTO;
import com.cms.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @PostMapping("/request/{facultyId}")
    @PreAuthorize("hasRole('FACULTY') ")
    public ResponseEntity<LeaveResponseDTO> requestLeave(
            @PathVariable Long facultyId,
            @RequestBody LeaveRequestDTO leaveRequestDTO) {
        LeaveResponseDTO response = leaveService.requestLeave(facultyId, leaveRequestDTO);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{leaveId}/action/{approverId}")
    @PreAuthorize("hasRole('FACULTY') ")
    public ResponseEntity<LeaveResponseDTO> updateLeaveStatus(
            @PathVariable Long leaveId,
            @PathVariable Long approverId,
            @RequestBody LeaveActionDTO leaveActionDTO) {
        LeaveResponseDTO response = leaveService.updateLeaveStatus(leaveId, approverId, leaveActionDTO);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/faculty/{facultyId}")
    @PreAuthorize("hasRole('FACULTY') ")
    public ResponseEntity<List<LeaveResponseDTO>> getLeaveRequestsByFaculty(@PathVariable Long facultyId) {
        List<LeaveResponseDTO> leaves = leaveService.getLeaveRequestsByFaculty(facultyId);
        return ResponseEntity.ok(leaves);
    }

    @GetMapping("/approver/{approverId}")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<List<LeaveResponseDTO>> getLeaveRequestsForApprover(@PathVariable Long approverId) {
        List<LeaveResponseDTO> leaves = leaveService.getLeaveRequestsForApprover(approverId);
        return ResponseEntity.ok(leaves);
    }

    @GetMapping("/approver/{approverId}/pending")
    @PreAuthorize("hasRole('FACULTY') ")
    public ResponseEntity<List<LeaveResponseDTO>> getPendingLeaveRequestsForApprover(@PathVariable Long approverId) {
        List<LeaveResponseDTO> leaves = leaveService.getPendingLeaveRequestsForApprover(approverId);
        return ResponseEntity.ok(leaves);
    }
}
