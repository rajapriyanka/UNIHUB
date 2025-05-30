package com.cms.service;

import com.cms.dto.SubstituteRequestDTO;
import com.cms.entities.*;
import com.cms.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SubstituteRequestService {
    private static final Logger logger = LoggerFactory.getLogger(SubstituteRequestService.class);

    @Autowired
    private SubstituteRequestRepository substituteRequestRepository;
    
    @Autowired
    private FacultyRepository facultyRepository;
    
    @Autowired
    private TimetableEntryRepository timetableEntryRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private EmailTokenService emailTokenService;

    /**
     * Create a new substitute request
     */
    @Transactional
    public SubstituteRequestDTO createSubstituteRequest(SubstituteRequestDTO requestDTO) {
        System.out.println("DEBUG: Creating substitute request: " + requestDTO);
        logger.info("Creating substitute request: {}", requestDTO);
        
        // Validate inputs
        Faculty requester = facultyRepository.findById(requestDTO.getRequesterId())
            .orElseThrow(() -> new RuntimeException("Requester faculty not found"));
        
        Faculty substitute = facultyRepository.findById(requestDTO.getSubstituteId())
            .orElseThrow(() -> new RuntimeException("Substitute faculty not found"));
        
        TimetableEntry timetableEntry = timetableEntryRepository.findById(requestDTO.getTimetableEntryId())
            .orElseThrow(() -> new RuntimeException("Timetable entry not found"));
        
        System.out.println("DEBUG: Found requester: " + requester.getName() + 
                          ", substitute: " + substitute.getName() + 
                          ", timetable entry: " + timetableEntry.getId());
        logger.info("Found requester: {}, substitute: {}, timetable entry: {}", 
                   requester.getName(), substitute.getName(), timetableEntry.getId());
        
        // Create the substitute request
        SubstituteRequest request = new SubstituteRequest(
            requester,
            substitute,
            timetableEntry,
            requestDTO.getRequestDate(),
            requestDTO.getReason()
        );
        
        // If substituteDate is provided in the DTO, use it; otherwise it defaults to requestDate in the constructor
        if (requestDTO.getSubstituteDate() != null) {
            request.setSubstituteDate(requestDTO.getSubstituteDate());
        }
        
        // Save the request
        System.out.println("DEBUG: Saving substitute request");
        logger.info("Saving substitute request");
        SubstituteRequest savedRequest = substituteRequestRepository.save(request);
        System.out.println("DEBUG: Substitute request saved with ID: " + savedRequest.getId());
        logger.info("Substitute request saved with ID: {}", savedRequest.getId());
        
        // Try to send email notification to the substitute faculty, but don't fail if email sending fails
        try {
            System.out.println("DEBUG: Attempting to send email notification for substitute request ID: " + savedRequest.getId());
            logger.info("Attempting to send email notification for substitute request ID: {}", savedRequest.getId());
            
            // Check if emails are available
            String requesterEmail = requester.getUser() != null ? requester.getUser().getEmail() : null;
            String substituteEmail = substitute.getUser() != null ? substitute.getUser().getEmail() : null;
            
            System.out.println("DEBUG: Requester email: " + requesterEmail + ", Substitute email: " + substituteEmail);
            logger.info("Requester email: {}, Substitute email: {}", requesterEmail, substituteEmail);
            
            if (requesterEmail == null || substituteEmail == null || requesterEmail.isEmpty() || substituteEmail.isEmpty()) {
                System.out.println("DEBUG: Email addresses missing or empty for requester or substitute");
                logger.error("Email addresses missing or empty for requester or substitute");
                throw new RuntimeException("Email addresses missing for requester or substitute");
            }
            
            boolean emailSent = sendSubstituteRequestEmail(savedRequest);
            System.out.println("DEBUG: Email notification sent: " + emailSent);
            logger.info("Email notification sent: {}", emailSent);
        } catch (Exception e) {
            System.out.println("DEBUG: Failed to send substitute request email: " + e.getMessage());
            e.printStackTrace();
            logger.error("Failed to send substitute request email: {}", e.getMessage(), e);
            // Continue processing - don't throw the exception
        }
        
        // Convert to DTO and return
        SubstituteRequestDTO result = convertToDTO(savedRequest);
        System.out.println("DEBUG: Returning substitute request DTO: " + result);
        logger.info("Returning substitute request DTO: {}", result);
        return result;
    }
    
    /**
     * Update the status of a substitute request
     */
    @Transactional
    public SubstituteRequestDTO updateRequestStatus(Long requestId, SubstituteRequest.RequestStatus status, String responseMessage) {
        System.out.println("DEBUG: Updating request status for ID: " + requestId + ", status: " + status + ", message: " + responseMessage);
        logger.info("Updating request status for ID: {}, status: {}, message: {}", requestId, status, responseMessage);
        
        SubstituteRequest request = substituteRequestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Substitute request not found"));
        
        request.setStatus(status);
        request.setResponseTime(LocalDateTime.now());
        request.setResponseMessage(responseMessage);
        
        System.out.println("DEBUG: Saving updated request");
        logger.info("Saving updated request");
        SubstituteRequest updatedRequest = substituteRequestRepository.save(request);
        System.out.println("DEBUG: Request updated with status: " + updatedRequest.getStatus());
        logger.info("Request updated with status: {}", updatedRequest.getStatus());
        
        // Try to send email notification about the status update, but don't fail if email sending fails
        try {
            System.out.println("DEBUG: Attempting to send status update email for request ID: " + updatedRequest.getId());
            logger.info("Attempting to send status update email for request ID: {}", updatedRequest.getId());
            boolean emailSent = sendStatusUpdateEmail(updatedRequest);
            System.out.println("DEBUG: Status update email sent: " + emailSent);
            logger.info("Status update email sent: {}", emailSent);
        } catch (Exception e) {
            System.out.println("DEBUG: Failed to send status update email: " + e.getMessage());
            e.printStackTrace();
            logger.error("Failed to send status update email: {}", e.getMessage(), e);
            // Continue processing - don't throw the exception
        }
        
        return convertToDTO(updatedRequest);
    }
    
    /**
     * Process a substitute request via email token
     */
    @Transactional
    public SubstituteRequestDTO processRequestByToken(String token, boolean approved) {
        // Validate token and get request ID
        Long requestId = emailTokenService.validateSubstituteToken(token);
        if (requestId == null) {
            throw new RuntimeException("Invalid or expired token");
        }
        
        // Update request status
        SubstituteRequest.RequestStatus status = approved ? 
            SubstituteRequest.RequestStatus.APPROVED : SubstituteRequest.RequestStatus.REJECTED;
        
        String responseMessage = approved ? 
            "Request approved via email link" : "Request rejected via email link";
        
        return updateRequestStatus(requestId, status, responseMessage);
    }
    
    /**
     * Get all substitute requests for a faculty (as requester)
     */
    public List<SubstituteRequestDTO> getRequestsByRequester(Long facultyId) {
        List<SubstituteRequest> requests = substituteRequestRepository.findByRequesterId(facultyId);
        return requests.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all substitute requests for a faculty (as substitute)
     */
    public List<SubstituteRequestDTO> getRequestsBySubstitute(Long facultyId) {
        List<SubstituteRequest> requests = substituteRequestRepository.findBySubstituteId(facultyId);
        return requests.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Get pending substitute requests for a faculty (as substitute)
     */
    public List<SubstituteRequestDTO> getPendingRequestsBySubstitute(Long facultyId) {
        List<SubstituteRequest> requests = substituteRequestRepository.findBySubstituteIdAndStatus(
            facultyId, SubstituteRequest.RequestStatus.PENDING);
        return requests.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Send email notification for a new substitute request
     */
    private boolean sendSubstituteRequestEmail(SubstituteRequest request) {
        System.out.println("DEBUG: Preparing to send substitute request email for request ID: " + request.getId());
        logger.info("Preparing to send substitute request email for request ID: {}", request.getId());
        
        Faculty requester = request.getRequester();
        Faculty substitute = request.getSubstitute();
        TimetableEntry entry = request.getTimetableEntry();
        
        // Check if user emails are available
        if (requester.getUser() == null || substitute.getUser() == null) {
            System.out.println("DEBUG: User information missing for requester or substitute");
            logger.error("User information missing for requester or substitute");
            return false;
        }
        
        String requesterEmail = requester.getUser().getEmail();
        String substituteEmail = substitute.getUser().getEmail();
        
        if (requesterEmail == null || substituteEmail == null) {
            System.out.println("DEBUG: Email addresses missing for requester or substitute");
            logger.error("Email addresses missing for requester or substitute");
            return false;
        }
        
        System.out.println("DEBUG: Requester email: " + requesterEmail + ", Substitute email: " + substituteEmail);
        logger.info("Requester email: {}, Substitute email: {}", requesterEmail, substituteEmail);
        
        // Generate approval and rejection tokens
        try {
            String approveToken = emailTokenService.generateSubstituteToken(request.getId(), true);
            String rejectToken = emailTokenService.generateSubstituteToken(request.getId(), false);
            
            if (approveToken == null || rejectToken == null) {
                System.out.println("DEBUG: Failed to generate tokens for substitute request");
                logger.error("Failed to generate tokens for substitute request");
                return false;
            }
            
            System.out.println("DEBUG: Generated tokens - Approve: " + approveToken + ", Reject: " + rejectToken);
            logger.info("Generated tokens - Approve: {}, Reject: {}", approveToken, rejectToken);
            
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
            String formattedDate = request.getSubstituteDate().format(dateFormatter);
            
            String subject = "Substitute Request: " + requester.getName() + " for " + 
                            entry.getCourse().getTitle() + " on " + formattedDate;
            
            String periodInfo = "Period " + entry.getTimeSlot().getPeriodNumber() + 
                              " (" + entry.getTimeSlot().getStartTime() + " - " + entry.getTimeSlot().getEndTime() + ")";
            
            String batchInfo = entry.getBatch().getBatchName() + " " + 
                             (entry.getBatch().getSection() != null ? entry.getBatch().getSection() : "");
            
            String body = "Substitute request details:\n" +
                        "Course: " + entry.getCourse().getCode() + " - " + entry.getCourse().getTitle() + "\n" +
                        "Batch: " + batchInfo + "\n" +
                        "Date: " + formattedDate + "\n" +
                        "Time: " + periodInfo + "\n" +
                        "Reason: " + request.getReason();
            
            System.out.println("DEBUG: Sending email with subject: " + subject);
            logger.info("Sending email with subject: {}", subject);
            
            // Send email with approval/rejection links
            boolean emailSent = emailService.sendHtmlSubstituteRequestEmail(
                substituteEmail,
                requesterEmail,
                subject,
                body,
                request.getId(),
                substitute.getId(),
                requester.getName(),
                substitute.getName(),
                formattedDate,
                periodInfo,
                approveToken,
                rejectToken
            );
            
            if (!emailSent) {
                System.out.println("DEBUG: Email notification could not be sent for substitute request ID: " + request.getId());
                logger.warn("Email notification could not be sent for substitute request ID: {}", request.getId());
            } else {
                System.out.println("DEBUG: Email notification sent successfully for substitute request ID: " + request.getId());
                logger.info("Email notification sent successfully for substitute request ID: {}", request.getId());
            }
            
            return emailSent;
        } catch (Exception e) {
            System.out.println("DEBUG: Exception while preparing or sending substitute request email: " + e.getMessage());
            e.printStackTrace();
            logger.error("Exception while preparing or sending substitute request email: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Send email notification about status update
     */
    private boolean sendStatusUpdateEmail(SubstituteRequest request) {
        System.out.println("DEBUG: Preparing to send status update email for request ID: " + request.getId());
        logger.info("Preparing to send status update email for request ID: {}", request.getId());
        
        Faculty requester = request.getRequester();
        Faculty substitute = request.getSubstitute();
        TimetableEntry entry = request.getTimetableEntry();
        
        // Check if user emails are available
        if (requester.getUser() == null || substitute.getUser() == null) {
            System.out.println("DEBUG: User information missing for requester or substitute");
            logger.error("User information missing for requester or substitute");
            return false;
        }
        
        String requesterEmail = requester.getUser().getEmail();
        String substituteEmail = substitute.getUser().getEmail();
        
        if (requesterEmail == null || substituteEmail == null) {
            System.out.println("DEBUG: Email addresses missing for requester or substitute");
            logger.error("Email addresses missing for requester or substitute");
            return false;
        }
        
        System.out.println("DEBUG: Requester email: " + requesterEmail + ", Substitute email: " + substituteEmail);
        logger.info("Requester email: {}, Substitute email: {}", requesterEmail, substituteEmail);
        
        try {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
            String formattedDate = request.getSubstituteDate().format(dateFormatter);
            
            String subject = "Substitute Request " + request.getStatus().toString() + ": " + 
                            entry.getCourse().getTitle() + " on " + formattedDate;
            
            String periodInfo = "Period " + entry.getTimeSlot().getPeriodNumber() + 
                              " (" + entry.getTimeSlot().getStartTime() + " - " + entry.getTimeSlot().getEndTime() + ")";
            
            String statusInfo = request.getStatus().toString() + 
                              (request.getResponseMessage() != null ? ": " + request.getResponseMessage() : "");
            
            System.out.println("DEBUG: Sending status update email with subject: " + subject);
            logger.info("Sending status update email with subject: {}", subject);
            
            // Send email to requester about the status update
            boolean emailSent = emailService.sendSubstituteStatusUpdateEmail(
                requesterEmail,
                substituteEmail,
                subject,
                statusInfo,
                substitute.getName(),
                requester.getName(),
                formattedDate,
                periodInfo
            );
            
            if (!emailSent) {
                System.out.println("DEBUG: Status update email could not be sent for substitute request ID: " + request.getId());
                logger.warn("Status update email could not be sent for substitute request ID: {}", request.getId());
            } else {
                System.out.println("DEBUG: Status update email sent successfully for substitute request ID: " + request.getId());
                logger.info("Status update email sent successfully for substitute request ID: {}", request.getId());
            }
            
            return emailSent;
        } catch (Exception e) {
            System.out.println("DEBUG: Exception while preparing or sending status update email: " + e.getMessage());
            e.printStackTrace();
            logger.error("Exception while preparing or sending status update email: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Convert entity to DTO
     */
    private SubstituteRequestDTO convertToDTO(SubstituteRequest request) {
        SubstituteRequestDTO dto = new SubstituteRequestDTO();
        dto.setId(request.getId());
        dto.setRequesterId(request.getRequester().getId());
        dto.setRequesterName(request.getRequester().getName());
        dto.setSubstituteId(request.getSubstitute().getId());
        dto.setSubstituteName(request.getSubstitute().getName());
        dto.setTimetableEntryId(request.getTimetableEntry().getId());
        
        TimetableEntry entry = request.getTimetableEntry();
        dto.setCourseCode(entry.getCourse().getCode());
        dto.setCourseTitle(entry.getCourse().getTitle());
        dto.setBatchName(entry.getBatch().getBatchName());
        dto.setSection(entry.getBatch().getSection());
        dto.setDay(entry.getTimeSlot().getDay());
        dto.setPeriodNumber(entry.getTimeSlot().getPeriodNumber());
        dto.setStartTime(entry.getTimeSlot().getStartTime());
        dto.setEndTime(entry.getTimeSlot().getEndTime());
        
        dto.setRequestDate(request.getRequestDate());
        dto.setSubstituteDate(request.getSubstituteDate());
        dto.setReason(request.getReason());
        dto.setStatus(request.getStatus());
        dto.setResponseTime(request.getResponseTime());
        dto.setResponseMessage(request.getResponseMessage());
        
        return dto;
    }
}
