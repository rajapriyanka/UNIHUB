package com.cms.controller;

import com.cms.dto.TimetableEntryDTO;
import com.cms.dto.TimetableGenerationDTO;
import com.cms.service.TimeSlotService;
import com.cms.service.TimetableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.List;

@RestController
@RequestMapping("/api/timetable")
public class TimetableController {

    @Autowired
    private TimetableService timetableService;
    
    @Autowired
    private TimeSlotService timeSlotService;

    @PostMapping("/generate")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<List<TimetableEntryDTO>> generateTimetable(@RequestBody TimetableGenerationDTO dto) {
        List<TimetableEntryDTO> timetable = timetableService.generateTimetable(dto);
        return ResponseEntity.ok(timetable);
    }

    @GetMapping("/faculty/{facultyId}")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<List<TimetableEntryDTO>> getFacultyTimetable(@PathVariable Long facultyId) {
        List<TimetableEntryDTO> timetable = timetableService.getFacultyTimetable(facultyId);
        return ResponseEntity.ok(timetable);
    }

    @GetMapping("/batch/{batchId}")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<List<TimetableEntryDTO>> getBatchTimetable(
            @PathVariable Long batchId,
            @RequestParam String academicYear,
            @RequestParam String semester) {
        List<TimetableEntryDTO> timetable = timetableService.getBatchTimetable(batchId, academicYear, semester);
        return ResponseEntity.ok(timetable);
    }

    @GetMapping("/initialize-slots")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<String> initializeTimeSlots() {
        timeSlotService.initializeTimeSlots();
        return ResponseEntity.ok("Time slots initialized successfully");
    }
}

