package com.cms.controller;

import com.cms.dto.FacultyDTO;
import com.cms.dto.FacultyRegistrationRequest;
import com.cms.dto.FacultyUpdateRequest;
import com.cms.entities.Faculty;
import com.cms.repository.UserRepository;
import com.cms.service.ExcelService;
import com.cms.service.FacultyService;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/faculty")
public class FacultyController {

    @Autowired
    private FacultyService facultyService;


    @Autowired
    private ExcelService excelService;
    
    @Autowired
    private UserRepository userRepository;
    
   
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> uploadFaculty(@RequestParam("file") MultipartFile file) {
        try {
            List<FacultyRegistrationRequest> facultyRequests = excelService.extractFacultyFromExcel(file);
            int updated = 0;
            int created = 0;
            List<String> errors = new ArrayList<>();
            
            for (FacultyRegistrationRequest request : facultyRequests) {
                try {
                    // Check if faculty with this email already exists
                    boolean exists = userRepository.findFirstByEmail(request.getEmail()).isPresent();
                    Faculty faculty = facultyService.createOrUpdateFaculty(request);
                    
                    if (exists) {
                        updated++;
                    } else {
                        created++;
                    }
                } catch (Exception e) {
                    errors.add("Error processing faculty with email " + request.getEmail() + ": " + e.getMessage());
                }
            }
            
            StringBuilder resultMessage = new StringBuilder("Faculty upload completed: ");
            resultMessage.append(created).append(" created, ").append(updated).append(" updated");
            
            if (!errors.isEmpty()) {
                resultMessage.append(". Errors: ").append(String.join("; ", errors));
            }
            
            return ResponseEntity.ok(resultMessage.toString());
        } catch (InvalidFormatException e) {
            return ResponseEntity.badRequest().body("Invalid Excel file format: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error uploading faculty members: " + e.getMessage());
        }
    }



    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN') or hasRole ('STUDENT')")
    @GetMapping
    public ResponseEntity<List<FacultyDTO>> getAllFaculty() {
        List<Faculty> faculties = facultyService.getAllFaculty();
        List<FacultyDTO> facultyDTOs = faculties.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(facultyDTOs);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<List<FacultyDTO>> searchFacultyByName(@RequestParam String name) {
        List<Faculty> faculties = facultyService.searchFacultyByName(name);
        List<FacultyDTO> facultyDTOs = faculties.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(facultyDTOs);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<FacultyDTO> registerFaculty(@RequestBody FacultyRegistrationRequest request) {
        Faculty faculty = facultyService.createOrUpdateFaculty(request);
        return ResponseEntity.ok(convertToDTO(faculty));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/user/{userId}")
    public ResponseEntity<FacultyDTO> updateFacultyByUserId(@PathVariable Long userId, @RequestBody FacultyUpdateRequest request) {
        Faculty faculty = facultyService.updateFacultyByUserId(userId, request);
        return ResponseEntity.ok(convertToDTO(faculty));
    }


    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<String> deleteFacultyByUserId(@PathVariable Long userId) {
        try {
            facultyService.deleteFacultyByUserId(userId);
            return ResponseEntity.ok("Faculty deleted successfully.");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }


    private FacultyDTO convertToDTO(Faculty faculty) {
        return new FacultyDTO(
            faculty.getUser().getId(), // User ID
            faculty.getName(),
            faculty.getUser().getEmail(),
            faculty.getDepartment(),
            faculty.getDesignation(),
            faculty.getMobileNo(),
            faculty.getId() // Faculty-specific ID
        );
    }

}
