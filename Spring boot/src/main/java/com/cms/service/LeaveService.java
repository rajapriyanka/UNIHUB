package com.cms.service;

import com.cms.dto.LeaveActionDTO;
import com.cms.dto.LeaveRequestDTO;
import com.cms.dto.LeaveResponseDTO;
import com.cms.entities.Faculty;
import com.cms.entities.Leave;
import com.cms.entities.User;
import com.cms.repository.FacultyRepository;
import com.cms.repository.LeaveRepository;
import com.cms.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LeaveService {
    private static final Logger logger = LoggerFactory.getLogger(LeaveService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    // Map of department names to approver email addresses
    private static final Map<String, String> DEPARTMENT_APPROVER_EMAILS = new HashMap<>();
    
    // Initialize the department to approver email mapping
    static {
        // Add department to approver email mappings
        DEPARTMENT_APPROVER_EMAILS.put("Computer Science and Engineering", "kanmanik135@gmail.com");
        DEPARTMENT_APPROVER_EMAILS.put("Electrical Engineering", "ee.hod@university.edu");
        DEPARTMENT_APPROVER_EMAILS.put("Mechanical Engineering", "me.hod@university.edu");
        DEPARTMENT_APPROVER_EMAILS.put("Civil Engineering", "ce.hod@university.edu");
        DEPARTMENT_APPROVER_EMAILS.put("Electronics and Communication", "ece.hod@university.edu");
        DEPARTMENT_APPROVER_EMAILS.put("Information Technology", "it.hod@university.edu");
        DEPARTMENT_APPROVER_EMAILS.put("Aeronautical Engineering", "aero.hod@university.edu");
        // Add more departments as needed
    }

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private FacultyRepository facultyRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    /**
     * Find the appropriate approver for a faculty based on department
     * @param faculty The faculty requesting leave
     * @return The appropriate approver faculty
     */
    private Faculty findApproverByDepartment(Faculty faculty) {
        String department = faculty.getDepartment();
        logger.info("Finding approver for faculty in department: {}", department);
        
        // Get the approver email for this department
        String approverEmail = DEPARTMENT_APPROVER_EMAILS.get(department);
        
        // Check if the requesting faculty is the department approver
        if (approverEmail != null && faculty.getUser() != null && 
            approverEmail.equals(faculty.getUser().getEmail())) {
            logger.info("Faculty is the department approver, finding alternative approver");
            return findAlternativeApprover(faculty);
        }
        
        if (approverEmail != null) {
            logger.info("Found configured approver email for department {}: {}", department, approverEmail);
            
            // Find the faculty with this email
            Optional<User> approverUser = userRepository.findFirstByEmail(approverEmail);
            
            if (approverUser.isPresent()) {
                Optional<Faculty> approverFaculty = facultyRepository.findByUser(approverUser.get());
                
                if (approverFaculty.isPresent()) {
                    Faculty approver = approverFaculty.get();
                    logger.info("Found department approver: {} (Faculty ID: {})", approver.getName(), approver.getId());
                    return approver;
                }
            }
            
            logger.warn("Configured approver email {} not found in the system", approverEmail);
        } else {
            logger.warn("No configured approver email for department: {}", department);
        }
        
        return findAlternativeApprover(faculty);
    }

    // Add a new method to find an alternative approver when the faculty is the department approver
    private Faculty findAlternativeApprover(Faculty faculty) {
        logger.info("Finding alternative approver for faculty ID: {}", faculty.getId());
        
        // First try to find a default fallback approver (could be a dean or higher admin)
        // You can add a static configuration for this
        String fallbackApproverEmail = "rajapriyanka1101@gmail.com"; // Replace with your default fallback approver
        Optional<User> fallbackUser = userRepository.findFirstByEmail(fallbackApproverEmail);
        
        if (fallbackUser.isPresent()) {
            Optional<Faculty> fallbackFaculty = facultyRepository.findByUser(fallbackUser.get());
            if (fallbackFaculty.isPresent() && !fallbackFaculty.get().getId().equals(faculty.getId())) {
                Faculty approver = fallbackFaculty.get();
                logger.info("Found fallback approver: {} (Faculty ID: {})", approver.getName(), approver.getId());
                return approver;
            }
        }
        
        // If no specific fallback approver is configured or found, find any faculty in the same department
        List<Faculty> departmentFaculty = facultyRepository.findByDepartment(faculty.getDepartment());
        
        // Filter out the requesting faculty
        departmentFaculty = departmentFaculty.stream()
                .filter(f -> !f.getId().equals(faculty.getId()))
                .collect(Collectors.toList());
        
        if (!departmentFaculty.isEmpty()) {
            // For simplicity, just take the first one
            Faculty seniorFaculty = departmentFaculty.get(0);
            logger.info("Using another faculty from same department as approver: {}", 
                    seniorFaculty.getName());
            return seniorFaculty;
        }
        
        // If no faculty in the same department, find a default approver from any department
        List<Faculty> allFaculty = facultyRepository.findAll();
        Optional<Faculty> anyOtherFaculty = allFaculty.stream()
                .filter(f -> !f.getId().equals(faculty.getId()))
                .findFirst();
        
        if (anyOtherFaculty.isPresent()) {
            logger.info("Using any available faculty as approver: {}", anyOtherFaculty.get().getName());
            return anyOtherFaculty.get();
        }
        
        // If we get here, there's no other faculty in the system
        logger.error("No suitable approver found for faculty ID: {}", faculty.getId());
        throw new RuntimeException("No suitable approver found in the system");
    }

    @Transactional
    public LeaveResponseDTO requestLeave(Long facultyId, LeaveRequestDTO leaveRequestDTO) {
        logger.info("Processing leave request for faculty ID: {}", facultyId);
        
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
        
        // Automatically determine the approver based on department
        Faculty approver;
        
        // If approverId is provided in the request, use it (for backward compatibility)
        if (leaveRequestDTO.getApproverId() != null && leaveRequestDTO.getApproverId() > 0) {
            User approverUser = userRepository.findById(leaveRequestDTO.getApproverId())
                    .orElseThrow(() -> new RuntimeException("Approver user not found"));
            
            approver = facultyRepository.findByUser(approverUser)
                    .orElseThrow(() -> new RuntimeException("Approver faculty not found for user ID: " + leaveRequestDTO.getApproverId()));
            
            logger.info("Using provided approver: {} (Faculty ID: {})", approver.getName(), approver.getId());
        } else {
            // Find the appropriate approver based on department
            approver = findApproverByDepartment(faculty);
            logger.info("Automatically selected approver: {} (Faculty ID: {})", approver.getName(), approver.getId());
        }

        Leave leave = new Leave();
        leave.setFaculty(faculty);
        leave.setApprover(approver);
        leave.setSubject(leaveRequestDTO.getSubject());
        leave.setReason(leaveRequestDTO.getReason());
        leave.setFromDate(leaveRequestDTO.getFromDate());
        leave.setToDate(leaveRequestDTO.getToDate());
        leave.setRequestedAt(LocalDateTime.now());
        leave.setStatus(Leave.LeaveStatus.PENDING);
        
        Leave savedLeave = leaveRepository.save(leave);
        logger.info("Leave request saved with ID: {}", savedLeave.getId());
        
        try {
            String fromDateStr = leave.getFromDate().format(DATE_FORMATTER);
            String toDateStr = leave.getToDate().format(DATE_FORMATTER);
            
            emailService.sendHtmlLeaveRequestEmail(
                approver.getUser().getEmail(),
                faculty.getUser().getEmail(),
                "Leave Request: " + leave.getSubject(),
                leave.getReason(),
                savedLeave.getId(),
                approver.getId(),
                faculty.getName(),
                approver.getName(),
                fromDateStr,
                toDateStr
            );
        } catch (Exception e) {
            logger.error("Error sending email notification for leave request ID {}: {}", savedLeave.getId(), e.getMessage());
        }
        
        return convertToDTO(savedLeave);
    }

    @Transactional
    public LeaveResponseDTO updateLeaveStatus(Long leaveId, Long approverId, LeaveActionDTO leaveActionDTO) {
        logger.info("Updating leave status for leave ID: {} by approver ID: {}", leaveId, approverId);
        
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));
        
        if (leave.getApprover() == null) {
            throw new RuntimeException("This leave request has no assigned approver");
        }
        
        if (!leave.getApprover().getId().equals(approverId)) {
            throw new RuntimeException("You are not authorized to update this leave request");
        }

        leave.setStatus(leaveActionDTO.getStatus());
        leave.setComments(leaveActionDTO.getComments());
        leave.setRespondedAt(LocalDateTime.now());
        
        Leave updatedLeave = leaveRepository.save(leave);
        logger.info("Leave status updated to {} for leave ID: {}", updatedLeave.getStatus(), updatedLeave.getId());
        
        try {
            String fromDateStr = leave.getFromDate().format(DATE_FORMATTER);
            String toDateStr = leave.getToDate().format(DATE_FORMATTER);
            
            emailService.sendLeaveStatusUpdateEmail(
                leave.getFaculty().getUser().getEmail(),
                leave.getApprover().getUser().getEmail(),
                "Leave Request " + leave.getStatus() + ": " + leave.getSubject(),
                leave.getStatus().toString(),
                leave.getApprover().getName(),
                leave.getFaculty().getName(),
                fromDateStr,
                toDateStr
            );
        } catch (Exception e) {
            logger.error("Error sending email notification for leave status update: {}", e.getMessage());
        }
        
        return convertToDTO(updatedLeave);
    }

    public List<LeaveResponseDTO> getLeaveRequestsByFaculty(Long facultyId) {
        logger.info("Fetching leave requests for faculty ID: {}", facultyId);
        
        // Verify faculty exists
        facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
        
        List<Leave> leaves = leaveRepository.findByFacultyId(facultyId);
        logger.info("Found {} leave requests for faculty ID: {}", leaves.size(), facultyId);
        
        return leaves.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<LeaveResponseDTO> getLeaveRequestsForApprover(Long approverId) {
        logger.info("Fetching leave requests for approver ID: {}", approverId);
        
        // Verify approver exists
        facultyRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("Approver faculty not found"));
        
        List<Leave> leaves = leaveRepository.findByApproverId(approverId);
        logger.info("Found {} leave requests for approver ID: {}", leaves.size(), approverId);
        
        return leaves.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<LeaveResponseDTO> getPendingLeaveRequestsForApprover(Long approverId) {
        logger.info("Fetching pending leave requests for approver ID: {}", approverId);
        
        // Verify approver exists
        facultyRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("Approver faculty not found"));
        
        List<Leave> leaves = leaveRepository.findByApproverIdAndStatus(
                approverId, Leave.LeaveStatus.PENDING);
        logger.info("Found {} pending leave requests for approver ID: {}", leaves.size(), approverId);
        
        return leaves.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private LeaveResponseDTO convertToDTO(Leave leave) {
        LeaveResponseDTO dto = new LeaveResponseDTO();
        dto.setId(leave.getId());
        
        // Handle faculty information
        if (leave.getFaculty() != null) {
            dto.setFacultyId(leave.getFaculty().getId());
            dto.setFacultyName(leave.getFaculty().getName());
            
            if (leave.getFaculty().getUser() != null) {
                dto.setFacultyEmail(leave.getFaculty().getUser().getEmail());
            } else {
                dto.setFacultyEmail("N/A");
            }
        } else {
            dto.setFacultyId(null);
            dto.setFacultyName("Unknown");
            dto.setFacultyEmail("N/A");
        }
        
        // Handle approver information - check for null approver
        if (leave.getApprover() != null) {
            dto.setApproverId(leave.getApprover().getId());
            dto.setApproverName(leave.getApprover().getName());
            
            if (leave.getApprover().getUser() != null) {
                dto.setApproverEmail(leave.getApprover().getUser().getEmail());
            } else {
                dto.setApproverEmail("N/A");
            }
        } else {
            // Handle null approver
            dto.setApproverId(null);
            dto.setApproverName("Unassigned");
            dto.setApproverEmail("N/A");
        }
        
        dto.setSubject(leave.getSubject());
        dto.setReason(leave.getReason());
        dto.setFromDate(leave.getFromDate());
        dto.setToDate(leave.getToDate());
        dto.setRequestedAt(leave.getRequestedAt());
        dto.setRespondedAt(leave.getRespondedAt());
        dto.setStatus(leave.getStatus());
        dto.setComments(leave.getComments());
        
        return dto;
    }
    
    /**
     * Fix existing leave records with null approvers by assigning appropriate approvers
     * This method can be called from an admin endpoint or scheduled task
     */
    @Transactional
    public void fixLeavesWithNullApprovers() {
        logger.info("Starting to fix leave records with null approvers");
        
        List<Leave> leavesWithNullApprovers = leaveRepository.findByApproverIsNull();
        logger.info("Found {} leave records with null approvers", leavesWithNullApprovers.size());
        
        for (Leave leave : leavesWithNullApprovers) {
            try {
                Faculty faculty = leave.getFaculty();
                if (faculty != null) {
                    Faculty approver = findApproverByDepartment(faculty);
                    leave.setApprover(approver);
                    leaveRepository.save(leave);
                    logger.info("Fixed leave ID {} by assigning approver ID {}", leave.getId(), approver.getId());
                } else {
                    logger.warn("Cannot fix leave ID {} because faculty is null", leave.getId());
                }
            } catch (Exception e) {
                logger.error("Error fixing leave ID {}: {}", leave.getId(), e.getMessage());
            }
        }
        
        logger.info("Completed fixing leave records with null approvers");
    }
}
