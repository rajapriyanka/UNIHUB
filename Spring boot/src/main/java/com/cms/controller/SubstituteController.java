package com.cms.controller;

import com.cms.dto.FacultyAvailabilityDTO;
import com.cms.dto.FacultyFilterDTO;
import com.cms.dto.SubstituteRequestDTO;
import com.cms.entities.SubstituteRequest;
import com.cms.service.FacultyAvailabilityService;
import com.cms.service.SubstituteRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/substitute")
public class SubstituteController {

    @Autowired
    private FacultyAvailabilityService facultyAvailabilityService;
    
    @Autowired
    private SubstituteRequestService substituteRequestService;
    
    @PostMapping("/filter-faculty")
    public ResponseEntity<List<FacultyAvailabilityDTO>> filterFaculty(@RequestBody FacultyFilterDTO filterDTO) {
        List<FacultyAvailabilityDTO> availableFaculty = 
            facultyAvailabilityService.filterFacultyByAvailabilityAndBatch(filterDTO);
        return ResponseEntity.ok(availableFaculty);
    }
    
    /**
     * Create a new substitute request
     */
    @PostMapping("/request")
    public ResponseEntity<SubstituteRequestDTO> createRequest(@RequestBody SubstituteRequestDTO requestDTO) {
        SubstituteRequestDTO createdRequest = substituteRequestService.createSubstituteRequest(requestDTO);
        return new ResponseEntity<>(createdRequest, HttpStatus.CREATED);
    }
    
    /**
     * Update the status of a substitute request
     */
    @PutMapping("/request/{id}/status")
    public ResponseEntity<SubstituteRequestDTO> updateRequestStatus(
            @PathVariable Long id,
            @RequestParam SubstituteRequest.RequestStatus status,
            @RequestParam(required = false) String responseMessage) {
        
        SubstituteRequestDTO updatedRequest = 
            substituteRequestService.updateRequestStatus(id, status, responseMessage);
        return ResponseEntity.ok(updatedRequest);
    }
    
    /**
     * Process a substitute request via email token
     */
    @GetMapping("/process-token")
    public ResponseEntity<SubstituteRequestDTO> processRequestByToken(
            @RequestParam String token,
            @RequestParam boolean approved) {
        
        SubstituteRequestDTO processedRequest = 
            substituteRequestService.processRequestByToken(token, approved);
        return ResponseEntity.ok(processedRequest);
    }
    
    /**
     * Get all substitute requests for a faculty (as requester)
     */
    @GetMapping("/requests/requester/{facultyId}")
    public ResponseEntity<List<SubstituteRequestDTO>> getRequestsByRequester(@PathVariable Long facultyId) {
        List<SubstituteRequestDTO> requests = substituteRequestService.getRequestsByRequester(facultyId);
        return ResponseEntity.ok(requests);
    }
    
    /**
     * Get all substitute requests for a faculty (as substitute)
     */
    @GetMapping("/requests/substitute/{facultyId}")
    public ResponseEntity<List<SubstituteRequestDTO>> getRequestsBySubstitute(@PathVariable Long facultyId) {
        List<SubstituteRequestDTO> requests = substituteRequestService.getRequestsBySubstitute(facultyId);
        return ResponseEntity.ok(requests);
    }
    
    /**
     * Get pending substitute requests for a faculty (as substitute)
     */
    @GetMapping("/requests/substitute/{facultyId}/pending")
    public ResponseEntity<List<SubstituteRequestDTO>> getPendingRequestsBySubstitute(@PathVariable Long facultyId) {
        List<SubstituteRequestDTO> requests = substituteRequestService.getPendingRequestsBySubstitute(facultyId);
        return ResponseEntity.ok(requests);
    }
}