package com.cms.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class OtpService {

    // Store OTPs with expiration time (in a real application, this should be in a database or cache)
    private final Map<String, OtpData> otpMap = new HashMap<>();
    
    // OTP expiration time in minutes
    private static final int OTP_EXPIRY_MINUTES = 3;
    
    /**
     * Generate a new OTP for the given email
     */
    public String generateOtp(String email) {
        String otp = generateRandomOtp();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        otpMap.put(email, new OtpData(otp, expiryTime));
        return otp;
    }
    
    /**
     * Validate the OTP for the given email
     */
    public boolean validateOtp(String email, String otp) {
        OtpData otpData = otpMap.get(email);
        if (otpData == null) {
            return false;
        }
        
        // Check if OTP is expired
        if (LocalDateTime.now().isAfter(otpData.expiryTime)) {
            otpMap.remove(email);
            return false;
        }
        
        return otpData.otp.equals(otp);
    }
    
    /**
     * Clear the OTP for the given email
     */
    public void clearOtp(String email) {
        otpMap.remove(email);
    }
    
    /**
     * Generate a random 6-digit OTP
     */
    private String generateRandomOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(800000); // 6-digit OTP
        return String.valueOf(otp);
    }
    
    /**
     * Inner class to store OTP data with expiration time
     */
    private static class OtpData {
        private final String otp;
        private final LocalDateTime expiryTime;
        
        public OtpData(String otp, LocalDateTime expiryTime) {
            this.otp = otp;
            this.expiryTime = expiryTime;
        }
    }
    /**
     * Generate a new OTP specifically for admin password reset
     */
    public String generateAdminPasswordResetOtp(String email) {
        String otp = generateRandomOtp();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        
        // Use a specific key format for admin password resets
        String key = "admin_pwd_reset:" + email;
        otpMap.put(key, new OtpData(otp, expiryTime));
        
        return otp;
    }

    /**
     * Validate the OTP specifically for admin password reset
     */
    public boolean validateAdminPasswordResetOtp(String email, String otp) {
        String key = "admin_pwd_reset:" + email;
        OtpData otpData = otpMap.get(key);
        
        if (otpData == null) {
            return false;
        }
        
        // Check if OTP is expired
        if (LocalDateTime.now().isAfter(otpData.expiryTime)) {
            otpMap.remove(key);
            return false;
        }
        
        // Check if OTP matches
        boolean isValid = otpData.otp.equals(otp);
        
        // If valid, remove the OTP to prevent reuse
        if (isValid) {
            otpMap.remove(key);
        }
        
        return isValid;
    }
}

