package com.cms.dto;

public class ChangePasswordRequest {
    private String otp;
    private String newPassword;
    
    // Default constructor
    public ChangePasswordRequest() {
    }
    
    // Constructor with all fields
    public ChangePasswordRequest(String otp, String newPassword) {
        this.otp = otp;
        this.newPassword = newPassword;
    }
    
    // Getters and setters
    public String getOtp() {
        return otp;
    }
    
    public void setOtp(String otp) {
        this.otp = otp;
    }
    
    public String getNewPassword() {
        return newPassword;
    }
    
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}

