package com.cms.controller;

import com.cms.dto.AttendanceDTO;
import com.cms.dto.AttendancePercentageDTO;
import com.cms.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    /**
     * Generate attendance template for faculty to fill
     */
    @GetMapping("/template")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<InputStreamResource> generateAttendanceTemplate(
            @RequestParam Long facultyId,
            @RequestParam Long courseId,
            @RequestParam String batchName,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String section) throws IOException {
        
        ByteArrayInputStream template = attendanceService.generateAttendanceTemplate(
            facultyId, courseId, batchName, department, section);
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=attendance_template.xlsx");
        
        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(new InputStreamResource(template));
    }

    /**
     * Upload filled attendance Excel file
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<List<AttendanceDTO>> uploadAttendance(
            @RequestParam Long facultyId,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String section,
            @RequestParam("file") MultipartFile file) throws IOException {
        
        List<AttendanceDTO> processedAttendance = attendanceService.processAttendanceFromExcel(
            facultyId, department, section, file);
        return ResponseEntity.ok(processedAttendance);
    }

    /**
     * Get attendance records for a faculty's course and batch
     */
 // Only updating the getAttendanceByFacultyCourseAndBatch method, the rest of the file remains the same

    /**
     * Get attendance records for a faculty's course and batch
     */
    @GetMapping("/faculty/{facultyId}/course/{courseId}/batch/{batchName}")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<List<AttendanceDTO>> getAttendanceByFacultyCourseAndBatch(
            @PathVariable Long facultyId,
            @PathVariable Long courseId,
            @PathVariable String batchName,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String section) {
        
        List<AttendanceDTO> attendance = attendanceService.getAttendanceByFacultyCourseAndBatch(
                facultyId, courseId, batchName, department, section);
        return ResponseEntity.ok(attendance);
    }
    /**
     * Get attendance records for a student in a specific course
     */
    @GetMapping("/student/{studentId}/course/{courseId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('FACULTY')")
    public ResponseEntity<List<AttendanceDTO>> getStudentAttendanceForCourse(
            @PathVariable Long studentId,
            @PathVariable Long courseId) {
        
        List<AttendanceDTO> attendance = attendanceService.getStudentAttendanceForCourse(studentId, courseId);
        return ResponseEntity.ok(attendance);
    }

    /**
     * Get attendance percentage for a student across all courses
     */
    @GetMapping("/student/{studentId}/percentage")
    @PreAuthorize("hasRole('STUDENT') or hasRole('FACULTY')")
    public ResponseEntity<List<AttendancePercentageDTO>> getStudentAttendancePercentage(
            @PathVariable Long studentId,
            @RequestParam(required = false) Integer semesterNo) {
        
        List<AttendancePercentageDTO> attendancePercentages = 
            attendanceService.getStudentAttendancePercentageByCourses(studentId, semesterNo);
        return ResponseEntity.ok(attendancePercentages);
    }

    /**
     * Generate attendance report Excel for a course and batch
     */
    @GetMapping("/report")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<InputStreamResource> generateAttendanceReport(
            @RequestParam Long facultyId,
            @RequestParam Long courseId,
            @RequestParam String batchName,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String section) throws IOException {
        
        ByteArrayInputStream report = attendanceService.generateAttendanceReport(
            facultyId, courseId, batchName, department, section);
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=attendance_report.xlsx");
        
        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(new InputStreamResource(report));
    }
    
    /**
     * Check if any student's attendance is below threshold and send notifications
     */
    @PostMapping("/check-and-notify")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> checkAttendanceAndNotify() {
        Map<String, Object> result = attendanceService.checkAttendanceBelowThresholdAndNotify();
        return ResponseEntity.ok(result);
    }
}