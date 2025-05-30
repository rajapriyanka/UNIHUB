package com.cms.controller;

import com.cms.dto.AuthenticationRequest;
import com.cms.dto.AuthenticationResponse;
import com.cms.dto.FacultyRegistrationRequest;
import com.cms.dto.AdminRegistrationRequest;
import com.cms.entities.User;
import com.cms.service.admin.AdminServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AdminServiceImpl adminService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthenticationRequest request) {
        logger.info("Login attempt for user: {}", request.getEmail());
        try {
            AuthenticationResponse response = adminService.login(request);
            logger.info("Login successful for user: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Login failed for user: {}", request.getEmail(), e);
            return ResponseEntity.badRequest().body("Authentication failed: " + e.getMessage());
        }
    }

    @PostMapping("/faculty/login")
    public ResponseEntity<?> facultyLogin(@RequestBody AuthenticationRequest request) {
        logger.info("Faculty login attempt for user: {}", request.getEmail());
        try {
            AuthenticationResponse response = adminService.facultyLogin(request);
            logger.info("Faculty login successful for user: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Faculty login failed for user: {}", request.getEmail(), e);
            return ResponseEntity.badRequest().body("Authentication failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/student/login")
    public ResponseEntity<?> studentLogin(@RequestBody AuthenticationRequest request) {
        logger.info("Student login attempt for user: {}", request.getEmail());
        try {
            AuthenticationResponse response = adminService.studentLogin(request);
            logger.info("Student login successful for user: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Student login failed for user: {}", request.getEmail(), e);
            return ResponseEntity.badRequest().body("Authentication failed: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/register/faculty")
    public ResponseEntity<?> registerFaculty(@RequestBody FacultyRegistrationRequest request) {
        logger.info("Attempting to register faculty: {}", request.getEmail());
        try {
            User faculty = adminService.registerFaculty(request);
            logger.info("Faculty registered successfully with ID: {} and email: {}", faculty.getId(), faculty.getEmail());
            return ResponseEntity.ok("Faculty registered successfully with ID: " + faculty.getId());
        } catch (Exception e) {
            logger.error("Error registering faculty: {}", request.getEmail(), e);
            return ResponseEntity.badRequest().body("Error registering faculty: " + e.getMessage());
        }
    }

    @GetMapping("/admin/default-exists")
    public ResponseEntity<?> checkDefaultAdminExists() {
        boolean exists = adminService.defaultAdminExists();
        return ResponseEntity.ok(exists);
    }

    @PostMapping("/admin/register")
    public ResponseEntity<?> registerAdmin(@RequestBody AdminRegistrationRequest request) {
        logger.info("Attempting to register admin: {}", request.getEmail());
        try {
            User admin = adminService.registerAdmin(request);
            logger.info("Admin registered successfully with ID: {} and email: {}", admin.getId(), admin.getEmail());
            return ResponseEntity.ok("Admin registered successfully with ID: " + admin.getId());
        } catch (Exception e) {
            logger.error("Error registering admin: {}", request.getEmail(), e);
            return ResponseEntity.badRequest().body("Error registering admin: " + e.getMessage());
        }
    }
}

