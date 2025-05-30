package com.cms.controller;

import com.cms.dto.ChangePasswordRequest;
import com.cms.dto.FacultyDTO;
import com.cms.dto.FacultyProfileUpdateRequest;
import com.cms.entities.Faculty;
import com.cms.service.EmailService;
import com.cms.service.FacultyService;
import com.cms.service.OtpService;
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
@RequestMapping("/api/faculty/profile")
public class FacultyProfileController {

    @Autowired
    private FacultyService facultyService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private OtpService otpService;
    
    @Autowired
    private EmailService emailService;
    
    /**
     * Get the profile of the currently logged-in faculty
     */
    @PreAuthorize("hasRole('FACULTY')")
    @GetMapping
    public ResponseEntity<FacultyDTO> getFacultyProfile() {
        String email = getCurrentUserEmail();
        Faculty faculty = facultyService.getFacultyByEmail(email);
        return ResponseEntity.ok(convertToDTO(faculty));
    }
    
    /**
     * Update the profile of the currently logged-in faculty
     */
    @PreAuthorize("hasRole('FACULTY')")
    @PutMapping
    public ResponseEntity<FacultyDTO> updateFacultyProfile(@RequestBody FacultyProfileUpdateRequest request) {
        String email = getCurrentUserEmail();
        Faculty faculty = facultyService.updateFacultyProfile(email, request);
        return ResponseEntity.ok(convertToDTO(faculty));
    }
    
    /**
     * Request OTP for password change
     */
    @PreAuthorize("hasRole('FACULTY')")
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
    @PreAuthorize("hasRole('FACULTY')")
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
     * Convert Faculty entity to FacultyDTO
     */
    private FacultyDTO convertToDTO(Faculty faculty) {
        return new FacultyDTO(
            faculty.getUser().getId(),
            faculty.getName(),
            faculty.getUser().getEmail(),
            faculty.getDepartment(),
            faculty.getDesignation(),
            faculty.getMobileNo(),
            faculty.getId()
        );
    }
}

