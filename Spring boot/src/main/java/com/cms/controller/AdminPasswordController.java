package com.cms.controller;

import com.cms.dto.PasswordResetRequest;
import com.cms.dto.UsernameChangeRequest;
import com.cms.dto.PasswordResetOtpRequest;
import com.cms.dto.PasswordResetOtpVerifyRequest;
import com.cms.service.admin.AdminPasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/password")
public class AdminPasswordController {

    private final AdminPasswordService adminPasswordService;

    public AdminPasswordController(AdminPasswordService adminPasswordService) {
        this.adminPasswordService = adminPasswordService;
    }

    @PostMapping("/change-email")
    public ResponseEntity<?> changeEmail(@RequestBody UsernameChangeRequest request) {
        adminPasswordService.changeEmail(request.getEmail(), request.getNewEmail());
        return ResponseEntity.ok(Map.of("message", "Email has been updated successfully"));
    }

    /**
     * Request OTP for password reset
     */
    @PostMapping("/reset-request")
    public ResponseEntity<?> requestPasswordReset(@RequestBody PasswordResetRequest request) {
        adminPasswordService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "If the email exists, a password reset OTP has been sent"));
    }

    /**
     * Verify OTP and reset password
     */
    @PostMapping("/reset-verify")
    public ResponseEntity<?> verifyOtpAndResetPassword(@RequestBody PasswordResetOtpVerifyRequest request) {
        adminPasswordService.verifyOtpAndResetPassword(
            request.getEmail(), 
            request.getOtp(), 
            request.getNewPassword()
        );
        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully"));
    }

    /**
     * Resend OTP for password reset
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody PasswordResetOtpRequest request) {
        adminPasswordService.resendOtp(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "If the email exists, a new OTP has been sent"));
    }
}