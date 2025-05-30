package com.cms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.cms.entities.Leave;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private EmailTokenService emailTokenService;
    
    @Value("${spring.mail.enabled:true}")
    private boolean mailEnabled;
    
    @Value("${spring.mail.username:}")
    private String defaultFromEmail;
    
    @Value("${app.base-url:http://localhost:3000}") 
    private String baseUrl;

    
    public boolean sendHtmlLeaveRequestEmail(String to, String from, String subject, String body, 
                                            Long leaveId, Long approverId, String senderName, 
                                            String approverName, String fromDate, String toDate) {
        try {
            if (!mailEnabled) {
                logger.warn("Email sending is disabled.");
                return false;
            }

            // Generate approval and rejection tokens
            String approveToken = emailTokenService.generateToken(leaveId, approverId, Leave.LeaveStatus.APPROVED);
            String rejectToken = emailTokenService.generateToken(leaveId, approverId, Leave.LeaveStatus.REJECTED);
            
            // Frontend URLs for approval/rejection
            String approveUrl = baseUrl ;
            String rejectUrl = baseUrl;
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setFrom(defaultFromEmail);
            if (from != null && !from.isEmpty()) {
                helper.setReplyTo(from);
            }
            helper.setSubject(subject);
            
            String htmlBody = "<p>Dear " + approverName + ",</p>"
                    + "<p>A leave request has been submitted by " + senderName + " with the following details:</p>"
                    + "<p><strong>Subject:</strong> " + subject + "</p>"
                    + "<p><strong>Reason:</strong> " + body + "</p>"
                    + "<p><strong>Leave Period:</strong> " + fromDate + " to " + toDate + "</p>"
                    + "<p><a href='" + approveUrl + "' style='padding: 10px; background: green; color: white; text-decoration: none;'>Approve</a>"
                    + " &nbsp; "
                    + "<a href='" + rejectUrl + "' style='padding: 10px; background: red; color: white; text-decoration: none;'>Reject</a></p>"
                    + "<p>Thank you.</p>"
                    +"<p>With Regards,</p>"
                    +"<p>CMS.</p>";
            
            helper.setText(htmlBody, true);
            mailSender.send(message);
            logger.info("Leave approval email sent successfully to: {}", to);
            return true;
        } catch (MailException | MessagingException e) {
            logger.error("Failed to send email to: {}. Error: {}", to, e.getMessage(), e);
            return false;
        }
    }
    
    public boolean sendLeaveStatusUpdateEmail(String to, String from, String subject, String body, 
                                             String approverName, String senderName, 
                                             String fromDate, String toDate) {
        try {
            if (!mailEnabled) {
                logger.warn("Email sending is disabled.");
                return false;
            }
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setFrom(defaultFromEmail);
            if (from != null && !from.isEmpty()) {
                helper.setReplyTo(from);
            }
            helper.setSubject(subject);
            
            String htmlBody = "<p>Dear " + senderName + ",</p>"
                    + "<p>Your leave request for the period <strong>" + fromDate + " to " + toDate + "</strong> has been updated by " + approverName + ":</p>"
                    + "<p><strong>Status:</strong> " + body + "</p>"
                    + "<p>If you have any questions, please contact " + approverName + ".</p>"
                    + "<p>Thank you.</p>"
                    +"<p>With Regards,</p>"
                    +"<p>CMS.</p>";
            
            helper.setText(htmlBody, true);
            mailSender.send(message);
            logger.info("Leave status update email sent successfully to: {}", to);
            return true;
        } catch (MailException | MessagingException e) {
            logger.error("Failed to send leave status update email to: {}. Error: {}", to, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Send OTP email for password change
     */
    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Password Change OTP");
        message.setText("Your OTP for password change is: " + otp + 
                        "\nThis OTP will expire in 10 minutes.");
        
        mailSender.send(message);
    }
    
    /**
     * Create a new MimeMessage
     */
    public MimeMessage createMimeMessage() {
        return mailSender.createMimeMessage();
    }
    
    /**
     * Send an email message
     */
    public void sendEmail(MimeMessage message) {
        if (!mailEnabled) {
            logger.warn("Email sending is disabled.");
            return;
        }
        
        mailSender.send(message);
    }

    /**
     * Send HTML email for substitute request
     */
    public boolean sendHtmlSubstituteRequestEmail(String to, String from, String subject, String body, 
                                                Long requestId, Long substituteId, String senderName, 
                                                String substituteName, String requestDate, String periodInfo,
                                                String approveToken, String rejectToken) {
        try {
            System.out.println("DEBUG: EmailService.sendHtmlSubstituteRequestEmail called");
            System.out.println("DEBUG: Mail enabled: " + mailEnabled);
            System.out.println("DEBUG: To: " + to + ", From: " + from + ", Subject: " + subject);
            System.out.println("DEBUG: Request ID: " + requestId + ", Substitute ID: " + substituteId);
            System.out.println("DEBUG: Approve Token: " + approveToken + ", Reject Token: " + rejectToken);
            
            if (!mailEnabled) {
                System.out.println("DEBUG: Email sending is disabled.");
                logger.warn("Email sending is disabled.");
                return false;
            }
            
            // Validate input parameters
            if (to == null || to.isEmpty()) {
                System.out.println("DEBUG: Recipient email is null or empty");
                logger.error("Recipient email is null or empty");
                return false;
            }
            
            if (approveToken == null || rejectToken == null) {
                System.out.println("DEBUG: Approval or rejection token is null");
                logger.error("Approval or rejection token is null");
                return false;
            }
            
            // Log detailed information for debugging
            logger.info("Attempting to send substitute request email to: {}", to);
            logger.info("From: {}, Subject: {}", from, subject);
            logger.info("Request ID: {}, Substitute ID: {}", requestId, substituteId);
            logger.info("Approve Token: {}, Reject Token: {}", approveToken, rejectToken);
            
            // Frontend URLs for approval/rejection
            String approveUrl = baseUrl ;
            String rejectUrl = baseUrl ;
            
            System.out.println("DEBUG: Approve URL: " + approveUrl);
            System.out.println("DEBUG: Reject URL: " + rejectUrl);
            logger.info("Approve URL: {}", approveUrl);
            logger.info("Reject URL: {}", approveUrl);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            
            // Ensure defaultFromEmail is not empty
            if (defaultFromEmail == null || defaultFromEmail.isEmpty()) {
                defaultFromEmail = "noreply@example.com";
                System.out.println("DEBUG: Default from email is empty, using fallback: " + defaultFromEmail);
                logger.warn("Default from email is empty, using fallback: {}", defaultFromEmail);
            }
            
            helper.setFrom(defaultFromEmail);
            if (from != null && !from.isEmpty()) {
                helper.setReplyTo(from);
            }
            helper.setSubject(subject);
            
            String htmlBody = "<p>Dear " + substituteName + ",</p>"
                    + "<p>A substitute request has been submitted by " + senderName + " with the following details:</p>"
                    + "<p><strong>Subject:</strong> " + subject + "</p>"
                    + "<p><strong>Details:</strong> " + body.replace("\n", "<br/>") + "</p>"
                    + "<p><strong>Date:</strong> " + requestDate + "</p>"
                    + "<p><strong>Time:</strong> " + periodInfo + "</p>"
                    + "<p>Can you substitute for this class?</p>"
                    + "<p><a href='" + approveUrl + "' style='padding: 10px; background: green; color: white; text-decoration: none;'>Accept</a>"
                    + " &nbsp; "
                    + "<a href='" + rejectUrl + "' style='padding: 10px; background: red; color: white; text-decoration: none;'>Decline</a></p>"
                    + "<p>Thank you.</p>"
                    + "<p>With Regards,</p>"
                    + "<p>CMS.</p>";
            
            helper.setText(htmlBody, true);
            
            // Log before sending
            System.out.println("DEBUG: About to send email to: " + to);
            logger.info("About to send email to: {}", to);
            
            try {
                mailSender.send(message);
                System.out.println("DEBUG: Substitute request email sent successfully to: " + to);
                logger.info("Substitute request email sent successfully to: {}", to);
                return true;
            } catch (MailException e) {
                System.out.println("DEBUG: MailException while sending email: " + e.getMessage());
                e.printStackTrace();
                logger.error("MailException while sending email: {}", e.getMessage(), e);
                return false;
            }
        } catch (MailException e) {
            System.out.println("DEBUG: MailException while preparing email: " + e.getMessage());
            e.printStackTrace();
            logger.error("MailException while preparing email: {}", e.getMessage(), e);
            return false;
        } catch (MessagingException e) {
            System.out.println("DEBUG: MessagingException while preparing email: " + e.getMessage());
            e.printStackTrace();
            logger.error("MessagingException while preparing email: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            System.out.println("DEBUG: Unexpected exception while sending email: " + e.getMessage());
            e.printStackTrace();
            logger.error("Unexpected exception while sending email: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Send email for substitute request status update
     */
    public boolean sendSubstituteStatusUpdateEmail(String to, String from, String subject, String statusInfo, 
                                                 String substituteName, String requesterName, 
                                                 String requestDate, String periodInfo) {
        try {
            System.out.println("DEBUG: EmailService.sendSubstituteStatusUpdateEmail called");
            System.out.println("DEBUG: Mail enabled: " + mailEnabled);
            System.out.println("DEBUG: To: " + to + ", From: " + from + ", Subject: " + subject);
            
            if (!mailEnabled) {
                System.out.println("DEBUG: Email sending is disabled.");
                logger.warn("Email sending is disabled.");
                return false;
            }
            
            // Log detailed information for debugging
            logger.info("Attempting to send substitute status update email to: {}", to);
            logger.info("From: {}, Subject: {}", from, subject);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            
            // Ensure defaultFromEmail is not empty
            if (defaultFromEmail == null || defaultFromEmail.isEmpty()) {
                defaultFromEmail = "noreply@example.com";
                System.out.println("DEBUG: Default from email is empty, using fallback: " + defaultFromEmail);
                logger.warn("Default from email is empty, using fallback: {}", defaultFromEmail);
            }
            
            helper.setFrom(defaultFromEmail);
            if (from != null && !from.isEmpty()) {
                helper.setReplyTo(from);
            }
            helper.setSubject(subject);
            
            String htmlBody = "<p>Dear " + requesterName + ",</p>"
                    + "<p>Your substitute request for the class on <strong>" + requestDate + "</strong> at <strong>" + periodInfo + "</strong> has been updated:</p>"
                    + "<p><strong>Status:</strong> " + statusInfo + "</p>"
                    + "<p>Response from: " + substituteName + "</p>"
                    + "<p>Thank you.</p>"
                    + "<p>With Regards,</p>"
                    + "<p>CMS.</p>";
            
            helper.setText(htmlBody, true);
            
            // Log before sending
            System.out.println("DEBUG: About to send status update email to: " + to);
            logger.info("About to send status update email to: {}", to);
            
            try {
                mailSender.send(message);
                System.out.println("DEBUG: Substitute status update email sent successfully to: " + to);
                logger.info("Substitute status update email sent successfully to: {}", to);
                return true;
            } catch (MailException e) {
                System.out.println("DEBUG: MailException while sending status update email: " + e.getMessage());
                e.printStackTrace();
                logger.error("MailException while sending status update email: {}", e.getMessage(), e);
                return false;
            }
        } catch (MailException e) {
            System.out.println("DEBUG: MailException while preparing status update email: " + e.getMessage());
            e.printStackTrace();
            logger.error("MailException while preparing status update email: {}", e.getMessage(), e);
            return false;
        } catch (MessagingException e) {
            System.out.println("DEBUG: MessagingException while preparing status update email: " + e.getMessage());
            e.printStackTrace();
            logger.error("MessagingException while preparing email: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            System.out.println("DEBUG: Unexpected exception while sending status update email: " + e.getMessage());
            e.printStackTrace();
            logger.error("Failed to send substitute status update email to: {}. Error: {}", to, e.getMessage(), e);
            return false;
        }
    }
    /**
     * Send HTML OTP email for password reset
     */
    public boolean sendHtmlOtpEmail(String to, String subject, String otp, String userName) {
        try {
            if (!mailEnabled) {
                logger.warn("Email sending is disabled.");
                return false;
            }
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setFrom(defaultFromEmail);
            helper.setSubject(subject);
            
            String htmlBody = "<p>Dear " + userName + ",</p>"
                    + "<p>You have requested to reset your password for the College Management System.</p>"
                    + "<p>Your One-Time Password (OTP) for password reset is: <strong>" + otp + "</strong></p>"
                    + "<p>This OTP will expire in 10 minutes.</p>"
                    + "<p>If you did not request this password reset, please ignore this email or contact the system administrator.</p>"
                    + "<p>Thank you.</p>"
                    + "<p>With Regards,</p>"
                    + "<p>College Management System</p>";
            
            helper.setText(htmlBody, true);
            mailSender.send(message);
            logger.info("OTP email sent successfully to: {}", to);
            return true;
        } catch (MailException | MessagingException e) {
            logger.error("Failed to send OTP email to: {}. Error: {}", to, e.getMessage(), e);
            return false;
        }
    }
}
