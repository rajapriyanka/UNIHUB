package com.cms.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.cms.dto.LeaveActionDTO;
import com.cms.entities.Leave;
import com.cms.service.EmailTokenService;
import com.cms.service.LeaveService;
import com.cms.service.EmailTokenService.TokenData;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/email-actions")
public class EmailActionController {

    private static final Logger logger = LoggerFactory.getLogger(EmailActionController.class);

    @Autowired
    private EmailTokenService emailTokenService;

    @Autowired
    private LeaveService leaveService;

    @GetMapping("/leave/{token}/api")
    public ResponseEntity<Map<String, Object>> handleLeaveActionApi(
        @PathVariable String token, @RequestParam(required = false) String comment) {

        logger.info("Received leave action request for token: {}", token);
        
        Map<String, Object> response = new HashMap<>();
        try {
            TokenData tokenData = emailTokenService.validateToken(token);
            if (tokenData == null) {
                logger.warn("Invalid or expired token: {}", token);
                response.put("success", false);
                response.put("message", "This link is invalid or has expired.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            LeaveActionDTO actionDTO = new LeaveActionDTO();
            actionDTO.setStatus(tokenData.getAction());
            actionDTO.setComments(comment != null ? comment :
                (tokenData.getAction() == Leave.LeaveStatus.APPROVED ? "Approved via email" : "Rejected via email"));

            leaveService.updateLeaveStatus(tokenData.getLeaveId(), tokenData.getApproverId(), actionDTO);

            // Invalidate the token after use
            emailTokenService.invalidateToken(token);

            String actionText = tokenData.getAction() == Leave.LeaveStatus.APPROVED ? "approved" : "rejected";
            response.put("success", true);
            response.put("message", "Leave request has been successfully " + actionText + ".");

            logger.info("Leave request processed successfully. Action: {}", actionText);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error processing email action: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "An error occurred while processing your request.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}