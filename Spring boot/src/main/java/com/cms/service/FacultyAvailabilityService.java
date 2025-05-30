package com.cms.service;

import com.cms.dto.FacultyAvailabilityDTO;
import com.cms.dto.FacultyFilterDTO;
import com.cms.entities.Faculty;
import com.cms.entities.SubstituteRequest;
import com.cms.entities.TimeSlot;
import com.cms.entities.TimetableEntry;
import com.cms.repository.FacultyRepository;
import com.cms.repository.SubstituteRequestRepository;
import com.cms.repository.TimeSlotRepository;
import com.cms.repository.TimetableEntryRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FacultyAvailabilityService {

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;
    

    @Autowired
    private TimetableEntryRepository timetableEntryRepository;
    
 
    @Autowired
    private SubstituteRequestRepository substituteRequestRepository;

    /**
     * Filter faculty based on availability and batch handling
     */
    public List<FacultyAvailabilityDTO> filterFacultyByAvailabilityAndBatch(FacultyFilterDTO filterDTO) {
        // Log the filter criteria for debugging
        System.out.println("DEBUG: Filter criteria - filterByAvailability: " + filterDTO.isFilterByAvailability() +
                ", filterByBatch: " + filterDTO.isFilterByBatch() +
                ", batchId: " + filterDTO.getBatchId());

        List<Faculty> allFaculty = facultyRepository.findAll();

        // Filter out the requesting faculty
        allFaculty = allFaculty.stream()
                .filter(f -> !f.getId().equals(filterDTO.getRequestingFacultyId()))
                .collect(Collectors.toList());

        System.out.println("DEBUG: Total faculty after excluding requester: " + allFaculty.size());

        // Get the day of week for the requested date
        DayOfWeek dayOfWeek = filterDTO.getRequestDate().toLocalDate().getDayOfWeek();

        // Get the time slot for the period
        Optional<TimeSlot> timeSlotOpt = timeSlotRepository.findByDayAndPeriodNumber(dayOfWeek, filterDTO.getPeriodNumber());
        if (!timeSlotOpt.isPresent()) {
            throw new RuntimeException("Time slot not found for the given day and period");
        }

        TimeSlot timeSlot = timeSlotOpt.get();

        List<FacultyAvailabilityDTO> availableFaculty = new ArrayList<>();

        for (Faculty faculty : allFaculty) {
            // Create the DTO for this faculty
            FacultyAvailabilityDTO availabilityDTO = new FacultyAvailabilityDTO();
            availabilityDTO.setFacultyId(faculty.getId());
            availabilityDTO.setName(faculty.getName());
            availabilityDTO.setDepartment(faculty.getDepartment());
            availabilityDTO.setDesignation(faculty.getDesignation());
            availabilityDTO.setEmail(faculty.getUser().getEmail());

            // Check if faculty is available during the requested time slot
            boolean isAvailable = isFacultyAvailable(faculty.getId(), dayOfWeek, filterDTO.getPeriodNumber(),
                    filterDTO.getRequestDate());
            availabilityDTO.setAvailable(isAvailable);

            // Check if faculty handles the requested batch
            boolean handlesBatch = false;
            if (filterDTO.getBatchId() != null) {
                handlesBatch = doesFacultyHandleBatch(faculty.getId(), filterDTO.getBatchId());
            }
            availabilityDTO.setHandlesBatch(handlesBatch);

            // Determine if this faculty should be included based on filter criteria
            boolean includeInResults = true;

            // Apply filters based on which ones are enabled
            if (filterDTO.isFilterByAvailability() && !isAvailable) {
                // Availability filter is enabled and faculty is not available
                includeInResults = false;
                System.out.println("DEBUG: Faculty " + faculty.getName() + " excluded due to availability filter");
            }
            
            if (filterDTO.isFilterByBatch() && !handlesBatch) {
                // Batch filter is enabled and faculty doesn't handle the batch
                includeInResults = false;
                System.out.println("DEBUG: Faculty " + faculty.getName() + " excluded due to batch filter");
            }

            // Add to result list if passes all filters
            if (includeInResults) {
                availableFaculty.add(availabilityDTO);
                System.out.println("DEBUG: Faculty " + faculty.getName() + " included in results with available=" + isAvailable + ", handlesBatch=" + handlesBatch);
            }
        }

        System.out.println("DEBUG: Total faculty in results: " + availableFaculty.size());
        return availableFaculty;
    }

    /**
     * Check if a faculty is available at a specific day, period, and date
     * A faculty is considered unavailable if:
     * 1. They have a regular class scheduled at that time slot
     * 2. They are already assigned as a substitute on that date and period
     */
    private boolean isFacultyAvailable(Long facultyId, DayOfWeek dayOfWeek, int periodNumber, java.sql.Date requestDate) {
        // Convert java.sql.Date to LocalDate
        LocalDate localRequestDate = requestDate.toLocalDate();
        
        // Check if faculty has a regular class at this time slot
        Optional<TimetableEntry> regularClass = timetableEntryRepository.findByFacultyIdAndDayAndPeriod(
            facultyId, dayOfWeek, periodNumber);
        
        // If faculty has a regular class at this time, they are not available
        if (regularClass.isPresent()) {
            System.out.println("DEBUG: Faculty " + facultyId + " has a regular class at " + dayOfWeek + " period " + periodNumber);
            return false;
        }
        
        // Check if faculty is already assigned as a substitute on this date and period
        List<SubstituteRequest> existingSubstitutions = substituteRequestRepository.findBySubstituteIdAndDate(
            facultyId, localRequestDate);
        
        // If there are existing substitutions, check if any are for the same period
        if (existingSubstitutions != null && !existingSubstitutions.isEmpty()) {
            for (SubstituteRequest request : existingSubstitutions) {
                // If the request is approved and for the same period, faculty is not available
                if (request.getStatus() == SubstituteRequest.RequestStatus.APPROVED && 
                    request.getTimetableEntry().getTimeSlot().getPeriodNumber() == periodNumber) {
                    System.out.println("DEBUG: Faculty " + facultyId + " is already assigned as substitute on " + 
                        localRequestDate + " period " + periodNumber);
                    return false;
                }
            }
        }
        
        // If no conflicts found, faculty is available
        return true;
    }


    private boolean doesFacultyHandleBatch(Long facultyId, Long batchId) {
        // Check if there are any timetable entries where this faculty teaches this batch
        List<TimetableEntry> entries = timetableEntryRepository.findByFacultyIdAndBatchId(facultyId, batchId);
        return entries != null && !entries.isEmpty();
    }
}