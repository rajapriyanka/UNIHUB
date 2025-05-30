package com.cms.service.admin;

import com.cms.entities.User;
import com.cms.enums.UserRole;
import com.cms.repository.UserRepository;
import com.cms.service.EmailService;
import com.cms.service.OtpService;

import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class AdminPasswordService {

    private static final Logger logger = LoggerFactory.getLogger(AdminPasswordService.class);
    
    // Email validation regex pattern
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");
    
    // Password strength regex pattern (min 8 chars, at least one letter, one number, one special char)
    private static final Pattern PASSWORD_STRENGTH_PATTERN = 
        Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpService otpService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * Change email for admin user
     */
    @Transactional
    public void changeEmail(String oldEmail, String newEmail) {
        logger.info("Email change requested for email: {}", oldEmail);

        // Validate new email format
        if (newEmail == null || !EMAIL_PATTERN.matcher(newEmail).matches()) {
            logger.error("Invalid email format provided: {}", newEmail);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email format");
        }

        // Find user by email
        User user = userRepository.findFirstByEmail(oldEmail)
                .orElseThrow(() -> {
                    logger.error("User not found for email: {}", oldEmail);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                });

        // Check if user is an admin
        if (user.getUserRole() != UserRole.ADMIN) {
            logger.error("Email change attempted for non-admin user: {}", oldEmail);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin users can use this endpoint");
        }

        // Check if new email is already taken
        if (userRepository.findFirstByEmail(newEmail).isPresent()) {
            logger.error("Email already taken: {}", newEmail);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already taken");
        }

        // Update email
        user.setEmail(newEmail);
        
        // Increment credential version to invalidate existing tokens
        user.incrementCredentialVersion();
        
        userRepository.save(user);

        // Send email change confirmation email to both old and new addresses
        sendEmailChangeConfirmationEmail(user, oldEmail);
        sendEmailChangeNotificationToOldEmail(user.getName(), oldEmail, newEmail);

        logger.info("Email change successful from: {} to: {}", oldEmail, newEmail);
    }

    /**
     * Send email change confirmation email to new email
     */
    private void sendEmailChangeConfirmationEmail(User user, String oldEmail) {
        try {
            // Create HTML email for better user experience
            String subject = "Email Changed Successfully - College Management System";

            String htmlBody = "<p>Dear " + user.getName() + ",</p>"
                    + "<p>Your email for the College Management System has been changed successfully from "
                    + oldEmail + " to " + user.getEmail() + ".</p>"
                    + "<p>If you did not make this change, please contact the system administrator immediately.</p>"
                    + "<p>Thank you.</p>"
                    + "<p>With Regards,</p>"
                    + "<p>College Management System</p>";

            // Send HTML email
            MimeMessage message = emailService.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            emailService.sendEmail(message);
            logger.info("Email change confirmation sent to new email: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send email change confirmation email to: {}", user.getEmail(), e);
            // Don't throw exception here as email has already been changed
        }
    }
    
    /**
     * Send notification to old email about the change
     */
    private void sendEmailChangeNotificationToOldEmail(String userName, String oldEmail, String newEmail) {
        try {
            String subject = "Your Email Has Been Changed - College Management System";

            String htmlBody = "<p>Dear " + userName + ",</p>"
                    + "<p>Your email for the College Management System has been changed from "
                    + oldEmail + " to " + newEmail + ".</p>"
                    + "<p>If you did not make this change, please contact the system administrator immediately.</p>"
                    + "<p>Thank you.</p>"
                    + "<p>With Regards,</p>"
                    + "<p>College Management System</p>";

            MimeMessage message = emailService.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(oldEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            emailService.sendEmail(message);
            logger.info("Email change notification sent to old email: {}", oldEmail);
        } catch (Exception e) {
            logger.error("Failed to send email change notification to old email: {}", oldEmail, e);
        }
    }
    
    /**
     * Request password reset for admin user
     */
    public void requestPasswordReset(String email) {
        logger.info("Password reset requested for email: {}", email);
        
        // Validate email format
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            logger.error("Invalid email format in password reset request: {}", email);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email format");
        }
        
        // Find user by email
        Optional<User> userOptional = userRepository.findFirstByEmail(email);
        
        // For security reasons, don't reveal if the email exists or not
        if (userOptional.isEmpty()) {
            logger.warn("Password reset requested for non-existent email: {}", email);
            return;
        }
        
        User user = userOptional.get();
        
        // Check if user is an admin
        if (user.getUserRole() != UserRole.ADMIN) {
            logger.warn("Password reset requested for non-admin user: {}", email);
            return;
        }
        
        // Generate OTP
        String otp = otpService.generateOtp(email);
        
        // Send OTP via email
        sendPasswordResetOtpEmail(user, otp);
        
        logger.info("Password reset OTP sent to: {}", email);
    }
    
    /**
     * Verify OTP and reset password
     */
    @Transactional
    public void verifyOtpAndResetPassword(String email, String otp, String newPassword) {
        logger.info("Verifying OTP for password reset: {}", email);
        
        // Validate email format
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            logger.error("Invalid email format in OTP verification: {}", email);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email format");
        }
        
        // Validate password strength
        if (newPassword == null || !PASSWORD_STRENGTH_PATTERN.matcher(newPassword).matches()) {
            logger.error("Weak password provided for reset: {}", email);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Password must be at least 8 characters long and include at least one letter, one number, and one special character");
        }
        
        // Validate OTP
        if (!otpService.validateOtp(email, otp)) {
            logger.warn("Invalid or expired OTP for email: {}", email);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP");
        }
        
        // Find user by email
        User user = userRepository.findFirstByEmail(email)
                .orElseThrow(() -> {
                    logger.error("User not found for email: {}", email);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                });
        
        // Check if user is an admin
        if (user.getUserRole() != UserRole.ADMIN) {
            logger.error("Password reset attempted for non-admin user: {}", email);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin users can use this endpoint");
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        
        // Increment credential version to invalidate existing tokens
        user.incrementCredentialVersion();
        
        userRepository.save(user);
        
        // Clear OTP
        otpService.clearOtp(email);
        
        // Send password change confirmation email
        sendPasswordChangeConfirmationEmail(user);
        
        logger.info("Password reset successful for: {}", email);
    }
    
    /**
     * Resend OTP for password reset
     */
    public void resendOtp(String email) {
        logger.info("OTP resend requested for email: {}", email);
        
        // Validate email format
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            logger.error("Invalid email format in OTP resend request: {}", email);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email format");
        }
        
        // Find user by email
        Optional<User> userOptional = userRepository.findFirstByEmail(email);
        
        // For security reasons, don't reveal if the email exists or not
        if (userOptional.isEmpty()) {
            logger.warn("OTP resend requested for non-existent email: {}", email);
            return;
        }
        
        User user = userOptional.get();
        
        // Check if user is an admin
        if (user.getUserRole() != UserRole.ADMIN) {
            logger.warn("OTP resend requested for non-admin user: {}", email);
            return;
        }
        
        // Generate new OTP
        String otp = otpService.generateOtp(email);
        
        // Send OTP via email
        sendPasswordResetOtpEmail(user, otp);
        
        logger.info("Password reset OTP resent to: {}", email);
    }
    
    /**
     * Send password reset OTP email
     */
    private void sendPasswordResetOtpEmail(User user, String otp) {
        try {
            // Create HTML email for better user experience
            String subject = "Password Reset OTP - College Management System";
            
            String htmlBody = "<p>Dear " + user.getName() + ",</p>"
                    + "<p>You have requested to reset your password for the College Management System.</p>"
                    + "<p>Your One-Time Password (OTP) for password reset is: <strong>" + otp + "</strong></p>"
                    + "<p>This OTP will expire in 3 minutes.</p>"
                    + "<p>If you did not request this password reset, please ignore this email or contact the system administrator.</p>"
                    + "<p>Thank you.</p>"
                    + "<p>With Regards,</p>"
                    + "<p>College Management System</p>";
            
            // Send HTML email
            MimeMessage message = emailService.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            
            emailService.sendEmail(message);
            logger.info("Password reset OTP email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send password reset OTP email to: {}", user.getEmail(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send OTP email");
        }
    }
    
    /**
     * Send password change confirmation email
     */
    private void sendPasswordChangeConfirmationEmail(User user) {
        try {
            // Create HTML email for better user experience
            String subject = "Password Changed Successfully - College Management System";
            
            String htmlBody = "<p>Dear " + user.getName() + ",</p>"
                    + "<p>Your password for the College Management System has been changed successfully.</p>"
                    + "<p>If you did not make this change, please contact the system administrator immediately.</p>"
                    + "<p>Thank you.</p>"
                    + "<p>With Regards,</p>"
                    + "<p>College Management System</p>";
            
            // Send HTML email
            MimeMessage message = emailService.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            
            emailService.sendEmail(message);
            logger.info("Password change confirmation email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send password change confirmation email to: {}", user.getEmail(), e);
            // Don't throw exception here as password has already been changed
        }
    }
    
    /**
     * Force reset password for any user (admin only operation)
     */
    @Transactional
    public void forceResetPassword(String adminEmail, String targetEmail, String newPassword) {
        logger.info("Force password reset requested by admin: {} for user: {}", adminEmail, targetEmail);
        
        // Validate admin email
        User adminUser = userRepository.findFirstByEmail(adminEmail)
                .orElseThrow(() -> {
                    logger.error("Admin user not found: {}", adminEmail);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found");
                });
        
        // Verify admin role
        if (adminUser.getUserRole() != UserRole.ADMIN) {
            logger.error("Force password reset attempted by non-admin user: {}", adminEmail);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin users can perform this operation");
        }
        
        // Validate password strength
        if (newPassword == null || !PASSWORD_STRENGTH_PATTERN.matcher(newPassword).matches()) {
            logger.error("Weak password provided for force reset: {}", targetEmail);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Password must be at least 8 characters long and include at least one letter, one number, and one special character");
        }
        
        // Find target user
        User targetUser = userRepository.findFirstByEmail(targetEmail)
                .orElseThrow(() -> {
                    logger.error("Target user not found: {}", targetEmail);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found");
                });
        
        // Update password
        targetUser.setPassword(passwordEncoder.encode(newPassword));
        
        // Increment credential version to invalidate existing tokens
        targetUser.incrementCredentialVersion();
        
        userRepository.save(targetUser);
        
        // Send notification emails
        sendForcedPasswordResetNotification(targetUser);
        sendAdminPasswordResetConfirmation(adminUser, targetUser);
        
        logger.info("Force password reset successful for user: {}", targetEmail);
    }
    
    /**
     * Send notification to user about forced password reset
     */
    private void sendForcedPasswordResetNotification(User user) {
        try {
            String subject = "Your Password Has Been Reset - College Management System";
            
            String htmlBody = "<p>Dear " + user.getName() + ",</p>"
                    + "<p>Your password for the College Management System has been reset by an administrator.</p>"
                    + "<p>Please contact the system administrator for your new temporary password.</p>"
                    + "<p>We recommend changing your password after logging in.</p>"
                    + "<p>Thank you.</p>"
                    + "<p>With Regards,</p>"
                    + "<p>College Management System</p>";
            
            MimeMessage message = emailService.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            
            emailService.sendEmail(message);
            logger.info("Forced password reset notification sent to user: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send forced password reset notification to: {}", user.getEmail(), e);
        }
    }
    
    /**
     * Send confirmation to admin about forced password reset
     */
    private void sendAdminPasswordResetConfirmation(User admin, User targetUser) {
        try {
            String subject = "Password Reset Confirmation - College Management System";
            
            String htmlBody = "<p>Dear " + admin.getName() + ",</p>"
                    + "<p>You have successfully reset the password for user: " + targetUser.getEmail() + ".</p>"
                    + "<p>The user has been notified about this change.</p>"
                    + "<p>Thank you.</p>"
                    + "<p>With Regards,</p>"
                    + "<p>College Management System</p>";
            
            MimeMessage message = emailService.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(admin.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            
            emailService.sendEmail(message);
            logger.info("Admin password reset confirmation sent to: {}", admin.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send admin password reset confirmation to: {}", admin.getEmail(), e);
        }
    }
    
    /**
     * Check if password needs to be reset (e.g., temporary password, password expiry)
     */
    public boolean isPasswordResetRequired(String email) {
        logger.info("Checking if password reset is required for: {}", email);
        
        Optional<User> userOptional = userRepository.findFirstByEmail(email);
        
        if (userOptional.isEmpty()) {
            logger.warn("User not found for password reset check: {}", email);
            return false;
        }
        
        User user = userOptional.get();
        
        // Implement your password reset requirement logic here
        // For example, check if password is temporary or expired
        
        // This is a placeholder implementation
        // You might want to add a field to the User entity to track this
        boolean resetRequired = false;
        
        logger.info("Password reset required for {}: {}", email, resetRequired);
        return resetRequired;
    }
    
    /**
     * Debug method to check password encoding (for development use only)
     */
    public boolean debugPasswordMatch(String email, String rawPassword) {
        logger.info("Debug password match for: {}", email);
        
        Optional<User> userOptional = userRepository.findFirstByEmail(email);
        
        if (userOptional.isEmpty()) {
            logger.warn("User not found for debug password match: {}", email);
            return false;
        }
        
        User user = userOptional.get();
        
        boolean matches = passwordEncoder.matches(rawPassword, user.getPassword());
        logger.info("Debug password match result: {}", matches);
        
        return matches;
    }
}