package com.cms.controller;

import com.cms.dto.ChangePasswordRequest;
import com.cms.dto.StudentDTO;
import com.cms.dto.StudentProfileUpdateRequest;
import com.cms.entities.Student;
import com.cms.service.EmailService;
import com.cms.service.OtpService;
import com.cms.service.StudentService;
import com.cms.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/student/profile")
public class StudentProfileController {

    @Autowired
    private StudentService studentService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private OtpService otpService;
    
    @Autowired
    private EmailService emailService;
    
    /**
     * Get the profile of the currently logged-in student
     */
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping
    public ResponseEntity<StudentDTO> getStudentProfile() {
        String email = getCurrentUserEmail();
        Student student = studentService.getStudentByEmail(email);
        return ResponseEntity.ok(convertToDTO(student));
    }
    
    /**
     * Update the profile of the currently logged-in student
     */
    @PreAuthorize("hasRole('STUDENT')")
    @PutMapping
    public ResponseEntity<StudentDTO> updateStudentProfile(@RequestBody StudentProfileUpdateRequest request) {
        String email = getCurrentUserEmail();
        Student student = studentService.updateStudentProfile(email, request);
        return ResponseEntity.ok(convertToDTO(student));
    }
    
    /**
     * Request OTP for password change
     */
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/request-password-change")
    public ResponseEntity<String> requestPasswordChange() {
        String email = getCurrentUserEmail();
        String otp = otpService.generateOtp(email);
        emailService.sendOtpEmail(email, otp);
        return ResponseEntity.ok("OTP sent to your email address");
    }
    
    /**
     * Change password with OTP verification
     */
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordRequest request) {
        String email = getCurrentUserEmail();
        
        // Verify OTP
        if (!otpService.validateOtp(email, request.getOtp())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid OTP");
        }
        
        // Change password
        userService.changePassword(email, request.getNewPassword());
        
        // Invalidate OTP after successful password change
        otpService.clearOtp(email);
        
        return ResponseEntity.ok("Password changed successfully");
    }
    
    /**
     * Helper method to get the email of the currently authenticated user
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return authentication.getName(); // In Spring Security, getName() typically returns the username, which is the email in this case
    }
    
    /**
     * Convert Student entity to StudentDTO
     */
    private StudentDTO convertToDTO(Student student) {
        return new StudentDTO(
            student.getUser().getId(),
            student.getName(),
            student.getUser().getEmail(),
            student.getDepartment(),
            student.getDno(),
            student.getBatchName(),
            student.getMobileNumber(),
            student.getId(),
            student.getSection()
        );
    }
}