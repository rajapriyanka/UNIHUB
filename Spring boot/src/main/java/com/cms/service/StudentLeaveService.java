package com.cms.service;

import com.cms.dto.LeaveActionDTO;
import com.cms.dto.StudentLeaveRequestDTO;
import com.cms.dto.StudentLeaveResponseDTO;
import com.cms.entities.Faculty;
import com.cms.entities.Student;
import com.cms.entities.StudentLeave;
import com.cms.repository.FacultyRepository;
import com.cms.repository.StudentLeaveRepository;
import com.cms.repository.StudentRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentLeaveService {
    private static final Logger logger = LoggerFactory.getLogger(StudentLeaveService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @Autowired
    private StudentLeaveRepository studentLeaveRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private EmailService emailService;

    @Transactional
    public StudentLeaveResponseDTO requestLeave(Long studentId, StudentLeaveRequestDTO leaveRequestDTO) {
        logger.info("Processing leave request for student ID: {}", studentId);
        
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        
        // Find faculty by userId instead of facultyId
        Faculty faculty = facultyRepository.findByUserId(leaveRequestDTO.getFacultyId())
                .orElseThrow(() -> new RuntimeException("Faculty not found with user ID: " + leaveRequestDTO.getFacultyId()));

        logger.info("Faculty found: {} (Faculty ID: {}, User ID: {})", 
                faculty.getName(), faculty.getId(), faculty.getUser().getId());

        StudentLeave leave = new StudentLeave();
        leave.setStudent(student);
        leave.setFaculty(faculty);
        leave.setSubject(leaveRequestDTO.getSubject());
        leave.setReason(leaveRequestDTO.getReason());
        leave.setFromDate(leaveRequestDTO.getFromDate());
        leave.setToDate(leaveRequestDTO.getToDate());
        leave.setRequestedAt(LocalDateTime.now());
        leave.setStatus(StudentLeave.LeaveStatus.PENDING);
        
        StudentLeave savedLeave = studentLeaveRepository.save(leave);
        logger.info("Leave request saved with ID: {}", savedLeave.getId());
        
        try {
            String fromDateStr = leave.getFromDate().format(DATE_FORMATTER);
            String toDateStr = leave.getToDate().format(DATE_FORMATTER);
            
            emailService.sendHtmlLeaveRequestEmail(
                faculty.getUser().getEmail(),
                student.getUser().getEmail(),
                "Student Leave Request: " + leave.getSubject(),
                leave.getReason(),
                savedLeave.getId(),
                faculty.getId(),
                student.getName(),
                faculty.getName(),
                fromDateStr,
                toDateStr
            );
        } catch (Exception e) {
            logger.error("Error sending email notification for student leave request ID {}: {}", savedLeave.getId(), e.getMessage());
        }
        
        return convertToDTO(savedLeave);
    }

    public List<StudentLeaveResponseDTO> getLeaveRequestsByStudent(Long studentId) {
        logger.info("Fetching leave requests for student ID: {}", studentId);
        
        // Verify student exists
        studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        
        List<StudentLeave> leaves = studentLeaveRepository.findByStudentId(studentId);
        logger.info("Found {} leave requests for student ID: {}", leaves.size(), studentId);
        
        return leaves.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public StudentLeaveResponseDTO updateLeaveStatus(Long leaveId, Long facultyId, LeaveActionDTO leaveActionDTO) {
        logger.info("Updating student leave status for leave ID: {} by faculty ID: {}", leaveId, facultyId);
        
        StudentLeave leave = studentLeaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Student leave request not found"));
        
        if (!leave.getFaculty().getId().equals(facultyId)) {
            throw new RuntimeException("You are not authorized to update this leave request");
        }

        leave.setStatus(StudentLeave.LeaveStatus.valueOf(leaveActionDTO.getStatus().name()));
        leave.setComments(leaveActionDTO.getComments());
        leave.setRespondedAt(LocalDateTime.now());
        
        StudentLeave updatedLeave = studentLeaveRepository.save(leave);
        logger.info("Student leave status updated to {} for leave ID: {}", updatedLeave.getStatus(), updatedLeave.getId());
        
        try {
            String fromDateStr = leave.getFromDate().format(DATE_FORMATTER);
            String toDateStr = leave.getToDate().format(DATE_FORMATTER);
            
            emailService.sendLeaveStatusUpdateEmail(
                leave.getStudent().getUser().getEmail(),
                leave.getFaculty().getUser().getEmail(),
                "Student Leave Request " + leave.getStatus() + ": " + leave.getSubject(),
                leave.getStatus().toString(),
                leave.getFaculty().getName(),
                leave.getStudent().getName(),
                fromDateStr,
                toDateStr
            );
        } catch (Exception e) {
            logger.error("Error sending email notification for student leave status update: {}", e.getMessage());
        }
        
        return convertToDTO(updatedLeave);
    }

    public List<StudentLeaveResponseDTO> getLeaveRequestsForFaculty(Long facultyId) {
        logger.info("Fetching student leave requests for faculty ID: {}", facultyId);
        
        // Verify faculty exists
        facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
        
        List<StudentLeave> leaves = studentLeaveRepository.findByFacultyId(facultyId);
        logger.info("Found {} student leave requests for faculty ID: {}", leaves.size(), facultyId);
        
        return leaves.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<StudentLeaveResponseDTO> getPendingLeaveRequestsForFaculty(Long facultyId) {
        logger.info("Fetching pending student leave requests for faculty ID: {}", facultyId);
        
        // Verify faculty exists
        facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
        
        List<StudentLeave> leaves = studentLeaveRepository.findByFacultyIdAndStatus(
                facultyId, StudentLeave.LeaveStatus.PENDING);
        logger.info("Found {} pending student leave requests for faculty ID: {}", leaves.size(), facultyId);
        
        return leaves.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private StudentLeaveResponseDTO convertToDTO(StudentLeave leave) {
        StudentLeaveResponseDTO dto = new StudentLeaveResponseDTO();
        dto.setId(leave.getId());
        dto.setStudentId(leave.getStudent().getId());
        dto.setStudentName(leave.getStudent().getName());
        dto.setStudentEmail(leave.getStudent().getUser().getEmail());
        dto.setFacultyId(leave.getFaculty().getId());
        dto.setFacultyName(leave.getFaculty().getName());
        dto.setFacultyEmail(leave.getFaculty().getUser().getEmail());
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
}

