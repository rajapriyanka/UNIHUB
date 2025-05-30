package com.cms.service;

import com.cms.dto.TimetableEntryDTO;
import com.cms.dto.TimetableGenerationDTO;
import com.cms.entities.*;
import com.cms.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TimetableService {
    private static final Logger logger = LoggerFactory.getLogger(TimetableService.class);

    @Autowired
    private TimetableEntryRepository timetableEntryRepository;

    @Autowired
    private FacultyCourseRepository facultyCourseRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private TimeSlotService timeSlotService;

    // Maximum number of labs allowed per batch per day
    private static final int MAX_LABS_PER_DAY = 2;
    // Maximum number of attempts for timetable generation
    private static final int MAX_GENERATION_ATTEMPTS = 10;
    // Maximum recursion depth for faculty schedule adjustments
    private static final int MAX_RECURSION_DEPTH = 5;

    @Transactional
    public List<TimetableEntryDTO> generateTimetable(TimetableGenerationDTO dto) {
        // Initialize time slots if not already done
        timeSlotService.initializeTimeSlots();

        // Get faculty
        Faculty faculty = facultyRepository.findById(dto.getFacultyId())
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        // Get faculty courses
        List<FacultyCourse> facultyCourses = facultyCourseRepository.findByFacultyId(dto.getFacultyId());
        if (facultyCourses.isEmpty()) {
            throw new RuntimeException("No courses assigned to faculty");
        }

        // Clear existing timetable entries for this faculty
        List<TimetableEntry> existingEntries = timetableEntryRepository.findByFacultyId(dto.getFacultyId());
        timetableEntryRepository.deleteAll(existingEntries);

        // Generate new timetable with constraint handling
        List<TimetableEntry> generatedEntries = generateTimetableWithConstraints(faculty, facultyCourses, dto.getAcademicYear(), dto.getSemester());
        
        // Save the generated entries
        if (!generatedEntries.isEmpty()) {
            timetableEntryRepository.saveAll(generatedEntries);
        } else {
            throw new RuntimeException("Failed to generate a valid timetable after multiple attempts");
        }

        // Convert to DTOs
        return convertToDTO(generatedEntries);
    }

    private List<TimetableEntry> generateTimetableWithConstraints(Faculty faculty, List<FacultyCourse> facultyCourses, String academicYear, String semester) {
        List<TimetableEntry> bestEntries = new ArrayList<>();
        int bestViolationCount = Integer.MAX_VALUE;
        
        // Try multiple times to generate a valid timetable
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            logger.info("Timetable generation attempt {}/{}", attempt + 1, MAX_GENERATION_ATTEMPTS);
            
            // Generate a timetable
            List<TimetableEntry> entries = new ArrayList<>();
            Map<Long, Map<DayOfWeek, Set<Long>>> batchLabsByDay = new HashMap<>();
            
            try {
                // First, allocate lab courses (they have more constraints)
                allocateLabCourses(faculty, facultyCourses, entries, academicYear, semester, batchLabsByDay);
                
                // Then allocate non-academic courses
                allocateNonAcademicCourses(faculty, facultyCourses, entries, academicYear, semester);
                
                // Finally allocate theory courses
                allocateTheoryCourses(faculty, facultyCourses, entries, academicYear, semester);
                
                // Check for constraint violations
                List<String> violations = validateTimetable(entries);
                
                if (violations.isEmpty()) {
                    // If no violations, we have a valid timetable
                    logger.info("Valid timetable generated on attempt {}", attempt + 1);
                    return entries;
                } else {
                    // If this attempt has fewer violations than previous best, keep it
                    if (violations.size() < bestViolationCount) {
                        bestViolationCount = violations.size();
                        bestEntries = new ArrayList<>(entries);
                        
                        logger.info("Found better timetable with {} violations", bestViolationCount);
                        for (String violation : violations) {
                            logger.info("Violation: {}", violation);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error during timetable generation attempt {}: {}", attempt + 1, e.getMessage());
            }
        }
        
        // If we couldn't generate a completely valid timetable, try to fix the best one we found
        if (!bestEntries.isEmpty()) {
            logger.info("Attempting to fix timetable with {} violations", bestViolationCount);
            List<TimetableEntry> fixedEntries = fixTimetableViolations(bestEntries, faculty, facultyCourses, academicYear, semester);
            
            // Check if fixing was successful
            List<String> remainingViolations = validateTimetable(fixedEntries);
            if (remainingViolations.isEmpty()) {
                logger.info("Successfully fixed all violations");
                return fixedEntries;
            } else {
                logger.info("Fixed some violations, {} remaining", remainingViolations.size());
                for (String violation : remainingViolations) {
                    logger.info("Remaining violation: {}", violation);
                }
                
                // Try recursive adjustment of other faculty schedules
                logger.info("Attempting recursive adjustment of faculty schedules");
                List<TimetableEntry> recursivelyFixedEntries = recursivelyAdjustFacultySchedules(fixedEntries, faculty, facultyCourses, academicYear, semester, 0);
                
                // Final validation
                List<String> finalViolations = validateTimetable(recursivelyFixedEntries);
                if (finalViolations.isEmpty()) {
                    logger.info("Successfully fixed all violations through recursive adjustment");
                    return recursivelyFixedEntries;
                } else {
                    logger.info("After recursive adjustment, {} violations remain", finalViolations.size());
                    for (String violation : finalViolations) {
                        logger.info("Final violation: {}", violation);
                    }
                    return recursivelyFixedEntries; // Return the best we could achieve
                }
            }
        }
        
        // If all attempts failed, return an empty list
        logger.error("Failed to generate a valid timetable after {} attempts", MAX_GENERATION_ATTEMPTS);
        return new ArrayList<>();
    }
    
    private List<TimetableEntry> recursivelyAdjustFacultySchedules(List<TimetableEntry> entries, Faculty primaryFaculty, 
                                                                 List<FacultyCourse> facultyCourses, String academicYear, 
                                                                 String semester, int recursionDepth) {
        // Base case: if we've reached maximum recursion depth, return current entries
        if (recursionDepth >= MAX_RECURSION_DEPTH) {
            logger.info("Reached maximum recursion depth ({}), returning current best solution", MAX_RECURSION_DEPTH);
            return entries;
        }
        
        // Get all violations
        List<String> violations = validateTimetable(entries);
        if (violations.isEmpty()) {
            return entries; // No violations to fix
        }
        
        logger.info("Recursive adjustment at depth {}, violations: {}", recursionDepth, violations.size());
        
        // Create a working copy of entries
        List<TimetableEntry> workingEntries = new ArrayList<>(entries);
        
        // Process each violation type
        List<String> facultyConflicts = violations.stream()
            .filter(v -> v.contains("Faculty") && v.contains("scheduled twice"))
            .collect(Collectors.toList());
        
        List<String> batchConflicts = violations.stream()
            .filter(v -> v.contains("Batch") && v.contains("scheduled twice"))
            .collect(Collectors.toList());
        
        List<String> labConflicts = violations.stream()
            .filter(v -> v.contains("Batch") && v.contains("labs on"))
            .collect(Collectors.toList());
        
        // First try to fix faculty conflicts by adjusting other faculty schedules
        if (!facultyConflicts.isEmpty()) {
            workingEntries = fixFacultyConflicts(workingEntries, primaryFaculty, academicYear, semester, recursionDepth);
        }
        
        // Then fix batch conflicts
        if (!batchConflicts.isEmpty()) {
            workingEntries = fixBatchConflicts(workingEntries, primaryFaculty, facultyCourses, academicYear, semester);
        }
        
        // Finally fix lab conflicts
        if (!labConflicts.isEmpty()) {
            workingEntries = fixLabConflicts(workingEntries, primaryFaculty, facultyCourses, academicYear, semester);
        }
        
        // Check if we've made progress
        List<String> remainingViolations = validateTimetable(workingEntries);
        if (remainingViolations.size() < violations.size()) {
            // We've made progress, continue recursively
            logger.info("Made progress: reduced violations from {} to {}", violations.size(), remainingViolations.size());
            return recursivelyAdjustFacultySchedules(workingEntries, primaryFaculty, facultyCourses, academicYear, semester, recursionDepth + 1);
        } else {
            // No progress made, return current best
            logger.info("No further progress possible at recursion depth {}", recursionDepth);
            return workingEntries;
        }
    }
    
    private List<TimetableEntry> fixFacultyConflicts(List<TimetableEntry> entries, Faculty primaryFaculty, 
                                                   String academicYear, String semester, int recursionDepth) {
        logger.info("Fixing faculty conflicts at recursion depth {}", recursionDepth);
    
        // Create a working copy
        List<TimetableEntry> workingEntries = new ArrayList<>(entries);
    
        // Find all faculty conflicts
        Map<String, List<TimetableEntry>> facultySlotMap = new HashMap<>();
    
        // Build a map of faculty-slot to entries
        for (TimetableEntry entry : workingEntries) {
            String key = entry.getFaculty().getId() + "-" + entry.getTimeSlot().getDay() + "-" + entry.getTimeSlot().getPeriodNumber();
        
            facultySlotMap.computeIfAbsent(key, k -> new ArrayList<>());
            facultySlotMap.get(key).add(entry);
        }
    
        // Find conflicts (more than one entry per faculty-slot)
        List<Map.Entry<String, List<TimetableEntry>>> conflicts = facultySlotMap.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .collect(Collectors.toList());
    
        for (Map.Entry<String, List<TimetableEntry>> conflict : conflicts) {
            List<TimetableEntry> conflictingEntries = conflict.getValue();
        
            // Group conflicting entries by course ID to identify same subject across batches
            Map<Long, List<TimetableEntry>> courseGroups = conflictingEntries.stream()
                .collect(Collectors.groupingBy(e -> e.getCourse().getId()));
        
            // Handle each course group separately
            for (Map.Entry<Long, List<TimetableEntry>> courseGroup : courseGroups.entrySet()) {
                List<TimetableEntry> sameCourseBatchEntries = courseGroup.getValue();
            
                // If this is the same course taught to multiple batches
                if (sameCourseBatchEntries.size() > 1) {
                    logger.info("Found conflict with same course {} taught to {} batches",
                              sameCourseBatchEntries.get(0).getCourse().getCode(),
                              sameCourseBatchEntries.size());
                
                    // Sort entries so that primary faculty's entries are preserved if possible
                    sameCourseBatchEntries.sort((e1, e2) -> {
                        if (e1.getFaculty().getId().equals(primaryFaculty.getId())) return -1;
                        if (e2.getFaculty().getId().equals(primaryFaculty.getId())) return 1;
                        return 0;
                    });
                
                    // Keep the first entry (primary faculty if present) and move others
                    TimetableEntry keptEntry = sameCourseBatchEntries.get(0);
                
                    for (int i = 1; i < sameCourseBatchEntries.size(); i++) {
                        TimetableEntry entryToMove = sameCourseBatchEntries.get(i);
                    
                        // Remove this entry from the working set
                        workingEntries.remove(entryToMove);
                    
                        // Try to find an alternative slot for this entry
                        TimetableEntry movedEntry = findAlternativeSlot(entryToMove, workingEntries, academicYear, semester);
                    
                        if (movedEntry != null) {
                            workingEntries.add(movedEntry);
                            logger.info("Moved {} course {} for batch {} from {} period {} to {} period {}",
                                      entryToMove.getFaculty().getName(),
                                      entryToMove.getCourse().getCode(),
                                      entryToMove.getBatch().getBatchName(),
                                      entryToMove.getTimeSlot().getDay(),
                                      entryToMove.getTimeSlot().getPeriodNumber(),
                                      movedEntry.getTimeSlot().getDay(),
                                      movedEntry.getTimeSlot().getPeriodNumber());
                        } else {
                            // If we couldn't find an alternative slot, try more aggressively
                            movedEntry = findAlternativeSlotAggressively(entryToMove, workingEntries, academicYear, semester);
                        
                            if (movedEntry != null) {
                                workingEntries.add(movedEntry);
                                logger.info("Aggressively moved {} course {} for batch {} from {} period {} to {} period {}",
                                          entryToMove.getFaculty().getName(),
                                          entryToMove.getCourse().getCode(),
                                          entryToMove.getBatch().getBatchName(),
                                          entryToMove.getTimeSlot().getDay(),
                                          entryToMove.getTimeSlot().getPeriodNumber(),
                                          movedEntry.getTimeSlot().getDay(),
                                          movedEntry.getTimeSlot().getPeriodNumber());
                            } else {
                                logger.warn("Could not find alternative slot for {} course {} batch {}",
                                          entryToMove.getFaculty().getName(),
                                          entryToMove.getCourse().getCode(),
                                          entryToMove.getBatch().getBatchName());
                            }
                        }
                    }
                }
            }
        }
    
        return workingEntries;
    }
    
    private List<TimetableEntry> generateTimetableForFaculty(Faculty faculty, List<FacultyCourse> facultyCourses, 
                                                           List<TimetableEntry> existingEntries, 
                                                           String academicYear, String semester) {
        List<TimetableEntry> newEntries = new ArrayList<>();
        Map<Long, Map<DayOfWeek, Set<Long>>> batchLabsByDay = buildBatchLabsMap(existingEntries);
        
        try {
            // First, allocate lab courses (they have more constraints)
            allocateLabCoursesWithExisting(faculty, facultyCourses, newEntries, existingEntries, academicYear, semester, batchLabsByDay);
            
            // Then allocate non-academic courses
            allocateNonAcademicCoursesWithExisting(faculty, facultyCourses, newEntries, existingEntries, academicYear, semester);
            
            // Finally allocate theory courses
            allocateTheoryCoursesWithExisting(faculty, facultyCourses, newEntries, existingEntries, academicYear, semester);
        } catch (Exception e) {
            logger.error("Error during faculty timetable generation: {}", e.getMessage());
        }
        
        return newEntries;
    }
    
    private Map<Long, Map<DayOfWeek, Set<Long>>> buildBatchLabsMap(List<TimetableEntry> entries) {
        Map<Long, Map<DayOfWeek, Set<Long>>> batchLabsByDay = new HashMap<>();
        
        for (TimetableEntry entry : entries) {
            if (entry.getCourse().getType() == Course.CourseType.LAB) {
                Long batchId = entry.getBatch().getId();
                DayOfWeek day = entry.getTimeSlot().getDay();
                Long courseId = entry.getCourse().getId();
                
                batchLabsByDay.computeIfAbsent(batchId, k -> new HashMap<>());
                batchLabsByDay.get(batchId).computeIfAbsent(day, k -> new HashSet<>());
                batchLabsByDay.get(batchId).get(day).add(courseId);
            }
        }
        
        return batchLabsByDay;
    }
    
    private TimetableEntry findAlternativeSlot(TimetableEntry entry, List<TimetableEntry> existingEntries, 
                                             String academicYear, String semester) {
        Faculty faculty = entry.getFaculty();
        Course course = entry.getCourse();
        Batch batch = entry.getBatch();
        
        // Get all time slots
        List<TimeSlot> allTimeSlots = timeSlotService.getAllNonBreakTimeSlots();
        Collections.shuffle(allTimeSlots); // Randomize to avoid patterns
        
        // For lab courses, we need to find consecutive slots
        if (course.getType() == Course.CourseType.LAB) {
            // Find how many consecutive slots we need
            int consecutiveSlotsNeeded = existingEntries.stream()
                .filter(e -> e.getCourse().getId().equals(course.getId()) && 
                         e.getBatch().getId().equals(batch.getId()) && 
                         e.getTimeSlot().getDay() == entry.getTimeSlot().getDay())
                .collect(Collectors.toList())
                .size();
            
            // If no existing entries found, use the contact periods from the course
            if (consecutiveSlotsNeeded == 0) {
                consecutiveSlotsNeeded = course.getContactPeriods();
            }
            
            // Try to find consecutive slots on any day
            for (DayOfWeek day : Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                                             DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)) {
                
                List<TimeSlot> daySlots = allTimeSlots.stream()
                    .filter(ts -> ts.getDay() == day && ts.getPeriodNumber() > 1) // Not in first period
                    .sorted(Comparator.comparing(TimeSlot::getPeriodNumber))
                    .collect(Collectors.toList());
                
                // Find consecutive slots
                for (int i = 0; i <= daySlots.size() - consecutiveSlotsNeeded; i++) {
                    List<TimeSlot> consecutiveSlots = daySlots.subList(i, i + consecutiveSlotsNeeded);
                    
                    // Check if these slots are consecutive
                    boolean areConsecutive = true;
                    for (int j = 0; j < consecutiveSlots.size() - 1; j++) {
                        if (consecutiveSlots.get(j + 1).getPeriodNumber() != consecutiveSlots.get(j).getPeriodNumber() + 1) {
                            areConsecutive = false;
                            break;
                        }
                    }
                    
                    if (areConsecutive && canAllocateSlots(faculty, batch, consecutiveSlots, existingEntries)) {
                        // Return the first slot (others will be allocated later)
                        return new TimetableEntry(faculty, course, batch, consecutiveSlots.get(0), academicYear, semester);
                    }
                }
            }
        } else {
            // For non-lab courses, just find any available slot
            for (TimeSlot slot : allTimeSlots) {
                if (canAllocateSlot(faculty, batch, slot, existingEntries)) {
                    // For theory courses, check if this would create consecutive periods for the same course-batch pair
                    if (course.getType() == Course.CourseType.ACADEMIC) {
                        if (isCourseBatchContinuous(slot, batch.getId(), course.getId(), existingEntries)) {
                            continue; // Skip this slot to avoid consecutive periods for same course-batch
                        }
                    }
                    
                    return new TimetableEntry(faculty, course, batch, slot, academicYear, semester);
                }
            }
        }
        
        return null; // No alternative slot found
    }
    
    private List<TimetableEntry> fixBatchConflicts(List<TimetableEntry> entries, Faculty primaryFaculty, 
                                                 List<FacultyCourse> facultyCourses, String academicYear, String semester) {
        logger.info("Fixing batch conflicts");
        
        // Create a working copy
        List<TimetableEntry> workingEntries = new ArrayList<>(entries);
        
        // Find all batch conflicts
        Map<String, List<TimetableEntry>> batchSlotMap = new HashMap<>();
        
        // Build a map of batch-slot to entries
        for (TimetableEntry entry : workingEntries) {
            String key = entry.getBatch().getId() + "-" + entry.getTimeSlot().getDay() + "-" + entry.getTimeSlot().getPeriodNumber();
            
            batchSlotMap.computeIfAbsent(key, k -> new ArrayList<>());
            batchSlotMap.get(key).add(entry);
        }
        
        // Find conflicts (more than one entry per batch-slot)
        List<Map.Entry<String, List<TimetableEntry>>> conflicts = batchSlotMap.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .collect(Collectors.toList());
        
        for (Map.Entry<String, List<TimetableEntry>> conflict : conflicts) {
            List<TimetableEntry> conflictingEntries = conflict.getValue();
            
            // Sort entries so that primary faculty's entries are preserved if possible
            conflictingEntries.sort((e1, e2) -> {
                if (e1.getFaculty().getId().equals(primaryFaculty.getId())) return -1;
                if (e2.getFaculty().getId().equals(primaryFaculty.getId())) return 1;
                return 0;
            });
            
            // Keep the first entry (primary faculty if present) and move others
            TimetableEntry keptEntry = conflictingEntries.get(0);
            
            for (int i = 1; i < conflictingEntries.size(); i++) {
                TimetableEntry entryToMove = conflictingEntries.get(i);
                
                // Remove this entry from the working set
                workingEntries.remove(entryToMove);
                
                // Try to find an alternative slot for this entry
                TimetableEntry movedEntry = findAlternativeSlot(entryToMove, workingEntries, academicYear, semester);
                
                if (movedEntry != null) {
                    workingEntries.add(movedEntry);
                    logger.info("Moved {} course {} for batch {} from {} period {} to {} period {}", 
                              entryToMove.getFaculty().getName(),
                              entryToMove.getCourse().getCode(),
                              entryToMove.getBatch().getBatchName(),
                              entryToMove.getTimeSlot().getDay(),
                              entryToMove.getTimeSlot().getPeriodNumber(),
                              movedEntry.getTimeSlot().getDay(),
                              movedEntry.getTimeSlot().getPeriodNumber());
                } else {
                    logger.warn("Could not find alternative slot for {} course {} batch {}", 
                              entryToMove.getFaculty().getName(),
                              entryToMove.getCourse().getCode(),
                              entryToMove.getBatch().getBatchName());
                }
            }
        }
        
        return workingEntries;
    }
    
    private List<TimetableEntry> fixLabConflicts(List<TimetableEntry> entries, Faculty primaryFaculty, 
                                               List<FacultyCourse> facultyCourses, String academicYear, String semester) {
        logger.info("Fixing lab conflicts");
        
        // Create a working copy
        List<TimetableEntry> workingEntries = new ArrayList<>(entries);
        
        // Build the batch labs map
        Map<Long, Map<DayOfWeek, Set<Long>>> batchLabsByDay = buildBatchLabsMap(workingEntries);
        
        // Find batches with more than MAX_LABS_PER_DAY labs on any day
        List<Map.Entry<Long, Map<DayOfWeek, Set<Long>>>> violatingBatches = batchLabsByDay.entrySet().stream()
            .filter(entry -> entry.getValue().entrySet().stream()
                .anyMatch(dayEntry -> dayEntry.getValue().size() > MAX_LABS_PER_DAY))
            .collect(Collectors.toList());
        
        // For each violating batch, try to move labs to other days
        for (Map.Entry<Long, Map<DayOfWeek, Set<Long>>> batchEntry : violatingBatches) {
            Long batchId = batchEntry.getKey();
            
            // Find days with too many labs
            List<Map.Entry<DayOfWeek, Set<Long>>> violatingDays = batchEntry.getValue().entrySet().stream()
                .filter(dayEntry -> dayEntry.getValue().size() > MAX_LABS_PER_DAY)
                .collect(Collectors.toList());
            
            for (Map.Entry<DayOfWeek, Set<Long>> dayEntry : violatingDays) {
                DayOfWeek violatingDay = dayEntry.getKey();
                Set<Long> labCourseIds = dayEntry.getValue();
                
                // We need to move some labs to other days
                int labsToMove = labCourseIds.size() - MAX_LABS_PER_DAY;
                
                // Get the lab courses to move
                List<Long> coursesToMove = new ArrayList<>(labCourseIds);
                
                // Prioritize moving non-primary faculty labs
                coursesToMove.sort((id1, id2) -> {
                    boolean isPrimaryFaculty1 = workingEntries.stream()
                        .anyMatch(e -> e.getCourse().getId().equals(id1) && 
                                 e.getFaculty().getId().equals(primaryFaculty.getId()));
                    
                    boolean isPrimaryFaculty2 = workingEntries.stream()
                        .anyMatch(e -> e.getCourse().getId().equals(id2) && 
                                 e.getFaculty().getId().equals(primaryFaculty.getId()));
                    
                    if (isPrimaryFaculty1 && !isPrimaryFaculty2) return 1;
                    if (!isPrimaryFaculty1 && isPrimaryFaculty2) return -1;
                    return 0;
                });
                
                coursesToMove = coursesToMove.subList(0, labsToMove);
                
                // For each course to move, find all its entries on this day
                for (Long courseId : coursesToMove) {
                    // Find all entries for this course, batch, and day
                    List<TimetableEntry> entriesToMove = workingEntries.stream()
                        .filter(e -> e.getCourse().getId().equals(courseId) &&
                                e.getBatch().getId().equals(batchId) &&
                                e.getTimeSlot().getDay() == violatingDay)
                        .collect(Collectors.toList());
                    
                    if (!entriesToMove.isEmpty()) {
                        // Remove these entries
                        workingEntries.removeAll(entriesToMove);
                        
                        // Try to reallocate this lab to another day
                        reallocateLabToAnotherDay(entriesToMove.get(0).getFaculty(), entriesToMove, workingEntries, academicYear, semester, batchLabsByDay);
                    }
                }
            }
        }
        
        return workingEntries;
    }
    
    private List<TimetableEntry> fixTimetableViolations(List<TimetableEntry> entries, Faculty faculty, 
                                                      List<FacultyCourse> facultyCourses, String academicYear, String semester) {
        // Create a copy of entries to work with
        List<TimetableEntry> fixedEntries = new ArrayList<>(entries);
        
        // Find lab constraint violations (more than 2 labs per batch per day)
        Map<Long, Map<DayOfWeek, Set<Long>>> batchLabsByDay = new HashMap<>();
        
        // Build the current lab allocation map
        for (TimetableEntry entry : fixedEntries) {
            if (entry.getCourse().getType() == Course.CourseType.LAB) {
                Long batchId = entry.getBatch().getId();
                DayOfWeek day = entry.getTimeSlot().getDay();
                Long courseId = entry.getCourse().getId();
                
                batchLabsByDay.computeIfAbsent(batchId, k -> new HashMap<>());
                batchLabsByDay.get(batchId).computeIfAbsent(day, k -> new HashSet<>());
                batchLabsByDay.get(batchId).get(day).add(courseId);
            }
        }
        
        // Find batches with more than MAX_LABS_PER_DAY labs on any day
        List<Map.Entry<Long, Map<DayOfWeek, Set<Long>>>> violatingBatches = batchLabsByDay.entrySet().stream()
            .filter(entry -> entry.getValue().entrySet().stream()
                .anyMatch(dayEntry -> dayEntry.getValue().size() > MAX_LABS_PER_DAY))
            .collect(Collectors.toList());
        
        // For each violating batch, try to move labs to other days
        for (Map.Entry<Long, Map<DayOfWeek, Set<Long>>> batchEntry : violatingBatches) {
            Long batchId = batchEntry.getKey();
            
            
            // Find days with too many labs
            List<Map.Entry<DayOfWeek, Set<Long>>> violatingDays = batchEntry.getValue().entrySet().stream()
                .filter(dayEntry -> dayEntry.getValue().size() > MAX_LABS_PER_DAY)
                .collect(Collectors.toList());
            
            for (Map.Entry<DayOfWeek, Set<Long>> dayEntry : violatingDays) {
                DayOfWeek violatingDay = dayEntry.getKey();
                Set<Long> labCourseIds = dayEntry.getValue();
                
                // We need to move some labs to other days
                int labsToMove = labCourseIds.size() - MAX_LABS_PER_DAY;
                
                // Get the lab courses to move (take the last ones)
                List<Long> coursesToMove = new ArrayList<>(labCourseIds);
                Collections.shuffle(coursesToMove); // Randomize which labs to move
                coursesToMove = coursesToMove.subList(0, labsToMove);
                
                // For each course to move, find all its entries on this day
                for (Long courseId : coursesToMove) {
                    // Find all entries for this course, batch, and day
                    List<TimetableEntry> entriesToMove = fixedEntries.stream()
                        .filter(e -> e.getCourse().getId().equals(courseId) &&
                                e.getBatch().getId().equals(batchId) &&
                                e.getTimeSlot().getDay() == violatingDay)
                        .collect(Collectors.toList());
                    
                    if (!entriesToMove.isEmpty()) {
                        // Remove these entries
                        fixedEntries.removeAll(entriesToMove);
                        
                        // Try to reallocate this lab to another day
                        reallocateLabToAnotherDay(faculty, entriesToMove, fixedEntries, academicYear, semester, batchLabsByDay);
                    }
                }
            }
        }
        
        // Now fix faculty and batch conflicts
        fixedEntries = fixFacultyConflicts(fixedEntries, faculty, academicYear, semester, 0);
        fixedEntries = fixBatchConflicts(fixedEntries, faculty, facultyCourses, academicYear, semester);
        
        return fixedEntries;
    }
    
    private void reallocateLabToAnotherDay(Faculty faculty, List<TimetableEntry> entriesToMove, 
                                         List<TimetableEntry> allEntries, String academicYear, String semester,
                                         Map<Long, Map<DayOfWeek, Set<Long>>> batchLabsByDay) {
        if (entriesToMove.isEmpty()) {
            return;
        }
        
        // Get details from the first entry
        TimetableEntry firstEntry = entriesToMove.get(0);
        Course course = firstEntry.getCourse();
        Batch batch = firstEntry.getBatch();
        Long batchId = batch.getId();
        Long courseId = course.getId();
        
        // Get all time slots
        List<TimeSlot> allTimeSlots = timeSlotService.getAllNonBreakTimeSlots();
        
        // Find days where this batch has fewer than MAX_LABS_PER_DAY labs
        List<DayOfWeek> availableDays = Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                                                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);
        
        // Filter to days with fewer than MAX_LABS_PER_DAY labs
        availableDays = availableDays.stream()
            .filter(day -> {
                if (!batchLabsByDay.containsKey(batchId)) {
                    return true;
                }
                if (!batchLabsByDay.get(batchId).containsKey(day)) {
                    return true;
                }
                return batchLabsByDay.get(batchId).get(day).size() < MAX_LABS_PER_DAY;
            })
            .collect(Collectors.toList());
        
        // Shuffle to try different days
        Collections.shuffle(availableDays);
        
        // Number of consecutive periods needed
        int periodsNeeded = entriesToMove.size();
        
        // Try each available day
        for (DayOfWeek day : availableDays) {
            // Get slots for this day (not in first period)
            List<TimeSlot> daySlots = allTimeSlots.stream()
                .filter(ts -> ts.getDay() == day && ts.getPeriodNumber() > 1) // Not in first period
                .sorted(Comparator.comparing(TimeSlot::getPeriodNumber))
                .collect(Collectors.toList());
            
            // Find consecutive slots
            for (int i = 0; i <= daySlots.size() - periodsNeeded; i++) {
                List<TimeSlot> consecutiveSlots = daySlots.subList(i, i + periodsNeeded);
                
                // Check if these slots are consecutive
                boolean areConsecutive = true;
                for (int j = 0; j < consecutiveSlots.size() - 1; j++) {
                    if (consecutiveSlots.get(j + 1).getPeriodNumber() != consecutiveSlots.get(j).getPeriodNumber() + 1) {
                        areConsecutive = false;
                        break;
                    }
                }
                
                // Check if slots are available
                boolean slotsAvailable = true;
                for (TimeSlot slot : consecutiveSlots) {
                    // Check if faculty or batch is already allocated in this slot
                    boolean slotBusy = allEntries.stream()
                        .anyMatch(e -> (e.getFaculty().getId().equals(faculty.getId()) || 
                                     e.getBatch().getId().equals(batchId)) && 
                                     e.getTimeSlot().getDay() == slot.getDay() && 
                                     e.getTimeSlot().getPeriodNumber() == slot.getPeriodNumber());
                    
                    if (slotBusy) {
                        slotsAvailable = false;
                        break;
                    }
                }
                
                if (areConsecutive && slotsAvailable) {
                    // We found suitable slots, create new entries
                    for (int j = 0; j < consecutiveSlots.size(); j++) {
                        TimeSlot slot = consecutiveSlots.get(j);
                        
                        TimetableEntry newEntry = new TimetableEntry(
                            faculty, 
                            course, 
                            batch, 
                            slot, 
                            academicYear, 
                            semester
                        );
                        allEntries.add(newEntry);
                    }
                    
                    // Update the batch labs tracking
                    batchLabsByDay.computeIfAbsent(batchId, k -> new HashMap<>());
                    batchLabsByDay.get(batchId).computeIfAbsent(day, k -> new HashSet<>());
                    batchLabsByDay.get(batchId).get(day).add(courseId);
                    
                    logger.info("Successfully reallocated lab {} for batch {} from {} to {}", 
                              course.getCode(), batch.getBatchName(), entriesToMove.get(0).getTimeSlot().getDay(), day);
                    
                    return; // Successfully reallocated
                }
            }
        }
        
        // If we couldn't find suitable slots, try to force reallocation by clearing conflicts
        if (!availableDays.isEmpty()) {
            DayOfWeek forcedDay = availableDays.get(0);
            
            // Get slots for this day
            List<TimeSlot> daySlots = allTimeSlots.stream()
                .filter(ts -> ts.getDay() == forcedDay && ts.getPeriodNumber() > 1) // Not in first period
                .sorted(Comparator.comparing(TimeSlot::getPeriodNumber))
                .collect(Collectors.toList());
            
            // Find consecutive slots
            if (daySlots.size() >= periodsNeeded) {
                List<TimeSlot> consecutiveSlots = daySlots.subList(0, periodsNeeded);
                
                // Remove any conflicting entries
                for (TimeSlot slot : consecutiveSlots) {
                    allEntries.removeIf(e -> (e.getFaculty().getId().equals(faculty.getId()) || 
                                           e.getBatch().getId().equals(batchId)) && 
                                           e.getTimeSlot().getDay() == slot.getDay() && 
                                           e.getTimeSlot().getPeriodNumber() == slot.getPeriodNumber());
                }
                
                // Create new entries
                for (int j = 0; j < consecutiveSlots.size(); j++) {
                    TimeSlot slot = consecutiveSlots.get(j);
                    
                    TimetableEntry newEntry = new TimetableEntry(
                        faculty, 
                        course, 
                        batch, 
                        slot, 
                        academicYear, 
                        semester
                    );
                    allEntries.add(newEntry);
                }
                
                // Update the batch labs tracking
                batchLabsByDay.computeIfAbsent(batchId, k -> new HashMap<>());
                batchLabsByDay.get(batchId).computeIfAbsent(forcedDay, k -> new HashSet<>());
                batchLabsByDay.get(batchId).get(forcedDay).add(courseId);
                
                logger.info("Forced reallocation of lab {} for batch {} from {} to {}", 
                          course.getCode(), batch.getBatchName(), entriesToMove.get(0).getTimeSlot().getDay(), forcedDay);
            } else {
                logger.warn("Failed to reallocate lab {} for batch {}: not enough slots available", 
                          course.getCode(), batch.getBatchName());
            }
        } else {
            logger.warn("Failed to reallocate lab {} for batch {}: no available days", 
                      course.getCode(), batch.getBatchName());
        }
    }
    
    private void allocateLabCoursesWithExisting(Faculty faculty, List<FacultyCourse> facultyCourses, 
                                              List<TimetableEntry> newEntries, List<TimetableEntry> existingEntries,
                                              String academicYear, String semester, 
                                              Map<Long, Map<DayOfWeek, Set<Long>>> batchLabsByDay) {
        // Get lab courses
        List<FacultyCourse> labCourses = facultyCourses.stream()
            .filter(fc -> fc.getCourse().getType() == Course.CourseType.LAB)
            .collect(Collectors.toList());
        
        // Shuffle to randomize allocation
        Collections.shuffle(labCourses);
        
        // Get all time slots
        List<TimeSlot> allTimeSlots = timeSlotService.getAllNonBreakTimeSlots();
        
        // Allocate each lab course
        for (FacultyCourse labCourse : labCourses) {
            allocateLabCourseWithExisting(faculty, labCourse, allTimeSlots, newEntries, existingEntries, academicYear, semester, batchLabsByDay);
        }
    }
    
    private void allocateLabCourseWithExisting(Faculty faculty, FacultyCourse labCourse, List<TimeSlot> allTimeSlots,
                                             List<TimetableEntry> newEntries, List<TimetableEntry> existingEntries,
                                             String academicYear, String semester,
                                             Map<Long, Map<DayOfWeek, Set<Long>>> batchLabsByDay) {
        // Labs need consecutive periods and should not be in first period
        int totalPeriodsNeeded = labCourse.getCourse().getContactPeriods();
        
        // Keep all lab periods together regardless of the total number
        int periodsPerDay = totalPeriodsNeeded;
        int daysNeeded = 1; // Always allocate on a single day
        
        // Keep track of days already allocated for this lab
        Set<DayOfWeek> allocatedDays = new HashSet<>();
        Long batchId = labCourse.getBatch().getId();
        Long courseId = labCourse.getCourse().getId();
        
        // Initialize lab count for this batch if not already done
        if (!batchLabsByDay.containsKey(batchId)) {
            batchLabsByDay.put(batchId, new HashMap<>());
        }
        
        // Allocate lab periods across different days
        for (int day = 0; day < daysNeeded; day++) {
            // Find suitable consecutive slots (not in first period)
            for (DayOfWeek dayOfWeek : Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
                                                  DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)) {
                
                // Skip days already allocated for this lab
                if (allocatedDays.contains(dayOfWeek)) {
                    continue;
                }

                // Check if batch already has MAX_LABS_PER_DAY labs on this day
                Set<Long> existingLabsOnDay = batchLabsByDay.get(batchId).getOrDefault(dayOfWeek, new HashSet<>());
                if (existingLabsOnDay.size() >= MAX_LABS_PER_DAY) {
                    continue; // Skip this day if batch already has maximum labs
                }
                
                List<TimeSlot> daySlots = allTimeSlots.stream()
                    .filter(ts -> ts.getDay() == dayOfWeek && ts.getPeriodNumber() > 1) // Not in first period
                    .sorted(Comparator.comparing(TimeSlot::getPeriodNumber))
                    .collect(Collectors.toList());
                
                // Find consecutive slots
                for (int i = 0; i <= daySlots.size() - periodsPerDay; i++) {
                    List<TimeSlot> consecutiveSlots = daySlots.subList(i, i + periodsPerDay);
                    
                    // Check if these slots are consecutive
                    boolean areConsecutive = true;
                    for (int j = 0; j < consecutiveSlots.size() - 1; j++) {
                        if (consecutiveSlots.get(j + 1).getPeriodNumber() != consecutiveSlots.get(j).getPeriodNumber() + 1) {
                            areConsecutive = false;
                            break;
                        }
                    }
                    
                    // Combine existing and new entries for checking
                    List<TimetableEntry> combinedEntries = new ArrayList<>(existingEntries);
                    combinedEntries.addAll(newEntries);
                    
                    if (areConsecutive && canAllocateSlots(faculty, labCourse.getBatch(), consecutiveSlots, combinedEntries)) {
                        // Allocate all consecutive slots for this lab
                        for (TimeSlot slot : consecutiveSlots) {
                            TimetableEntry entry = new TimetableEntry(
                                faculty, 
                                labCourse.getCourse(), 
                                labCourse.getBatch(), 
                                slot, 
                                academicYear, 
                                semester
                            );
                            newEntries.add(entry);
                        }
                        
                        // Mark this day as allocated
                        allocatedDays.add(dayOfWeek);
                        
                        // Track this lab allocation
                        batchLabsByDay.get(batchId).computeIfAbsent(dayOfWeek, k -> new HashSet<>());
                        batchLabsByDay.get(batchId).get(dayOfWeek).add(courseId);
                        
                        break; // Successfully allocated for this day
                    }
                }
                
                // If we've allocated for this day, break the day loop
                if (allocatedDays.size() > day) {
                    break;
                }
            }
        }
    }
    
    private void allocateNonAcademicCoursesWithExisting(Faculty faculty, List<FacultyCourse> facultyCourses, 
                                                      List<TimetableEntry> newEntries, List<TimetableEntry> existingEntries,
                                                      String academicYear, String semester) {
        // Get non-academic courses
        List<FacultyCourse> nonAcademicCourses = facultyCourses.stream()
            .filter(fc -> fc.getCourse().getType() == Course.CourseType.NON_ACADEMIC)
            .collect(Collectors.toList());
        
        // Shuffle to randomize allocation
        Collections.shuffle(nonAcademicCourses);
        
        // Get all time slots
        List<TimeSlot> allTimeSlots = timeSlotService.getAllNonBreakTimeSlots();
        
        // Allocate each non-academic course
        for (FacultyCourse nonAcademicCourse : nonAcademicCourses) {
            allocateNonAcademicCourseWithExisting(faculty, nonAcademicCourse, allTimeSlots, newEntries, existingEntries, academicYear, semester);
        }
    }
    
    private void allocateNonAcademicCourseWithExisting(Faculty faculty, FacultyCourse nonAcademicCourse, List<TimeSlot> allTimeSlots, 
                                                     List<TimetableEntry> newEntries, List<TimetableEntry> existingEntries,
                                                     String academicYear, String semester) {
        int periodsToAllocate = nonAcademicCourse.getCourse().getContactPeriods();
        int periodsAllocated = 0;
        
        // Keep track of days already allocated for this non-academic course
        Set<DayOfWeek> allocatedDays = new HashSet<>();
        
        // Combine existing and new entries for checking
        List<TimetableEntry> combinedEntries = new ArrayList<>(existingEntries);
        combinedEntries.addAll(newEntries);
        
        // Distribute non-academic periods throughout the week (one per day)
        while (periodsAllocated < periodsToAllocate) {
            // Get available days that haven't been allocated yet for this course
            List<DayOfWeek> availableDays = Arrays.asList(DayOfWeek.values()).stream()
                .filter(day -> day != DayOfWeek.SUNDAY && !allocatedDays.contains(day))
                .collect(Collectors.toList());
            
            if (availableDays.isEmpty()) {
                // If we've used all days, we might need to allocate more than one period per day
                // This is a fallback if we can't satisfy the constraint
                availableDays = Arrays.asList(DayOfWeek.values()).stream()
                    .filter(day -> day != DayOfWeek.SUNDAY)
                    .collect(Collectors.toList());
            }
            
            Collections.shuffle(availableDays);
            
            boolean allocated = false;
            for (DayOfWeek day : availableDays) {
                // Get slots for this day (excluding first and last periods)
                List<TimeSlot> daySlots = allTimeSlots.stream()
                    .filter(ts -> ts.getDay() == day && 
                           ts.getPeriodNumber() > 1 && // Not first period
                           ts.getPeriodNumber() < getLastPeriodNumber(allTimeSlots, day)) // Not last period
                    .collect(Collectors.toList());
                
                Collections.shuffle(daySlots);
                
                for (TimeSlot slot : daySlots) {
                    if (canAllocateSlot(faculty, nonAcademicCourse.getBatch(), slot, combinedEntries)) {
                        TimetableEntry entry = new TimetableEntry(
                            faculty, 
                            nonAcademicCourse.getCourse(), 
                            nonAcademicCourse.getBatch(), 
                            slot, 
                            academicYear, 
                            semester
                        );
                        newEntries.add(entry);
                        combinedEntries.add(entry); // Update combined entries
                        periodsAllocated++;
                        allocated = true;
                        allocatedDays.add(day);
                        break;
                    }
                }
                
                if (allocated) {
                    break;
                }
            }
            
            if (!allocated) {
                // If we can't allocate more periods, break to avoid infinite loop
                break;
            }
            
            if (periodsAllocated >= periodsToAllocate) {
                break;
            }
        }
    }
    
    private void allocateTheoryCoursesWithExisting(Faculty faculty, List<FacultyCourse> facultyCourses, 
                                                 List<TimetableEntry> newEntries, List<TimetableEntry> existingEntries,
                                                 String academicYear, String semester) {
        // Get theory courses
        List<FacultyCourse> theoryCourses = facultyCourses.stream()
            .filter(fc -> fc.getCourse().getType() == Course.CourseType.ACADEMIC)
            .collect(Collectors.toList());
        
        // Group theory courses by course ID to handle same subject across batches
        Map<Long, List<FacultyCourse>> courseGroups = theoryCourses.stream()
            .collect(Collectors.groupingBy(fc -> fc.getCourse().getId()));
        
        // Sort groups by size (descending) to allocate courses taught to multiple batches first
        List<Map.Entry<Long, List<FacultyCourse>>> sortedGroups = courseGroups.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
            .collect(Collectors.toList());
        
        // Get all time slots
        List<TimeSlot> allTimeSlots = timeSlotService.getAllNonBreakTimeSlots();
        
        // Allocate each course group
        for (Map.Entry<Long, List<FacultyCourse>> group : sortedGroups) {
            // For courses taught to multiple batches, ensure different time slots
            if (group.getValue().size() > 1) {
                allocateMultiBatchTheoryCourseWithExisting(faculty, group.getValue(), allTimeSlots, newEntries, existingEntries, academicYear, semester);
            } else {
                // For single batch courses, use the original method
                allocateTheoryCourseWithExisting(faculty, group.getValue().get(0), allTimeSlots, newEntries, existingEntries, academicYear, semester);
            }
        }
    }
    
    private void allocateMultiBatchTheoryCourseWithExisting(Faculty faculty, List<FacultyCourse> courseBatches,
                                                          List<TimeSlot> allTimeSlots, List<TimetableEntry> newEntries, 
                                                          List<TimetableEntry> existingEntries, String academicYear, String semester) {
        logger.info("Allocating course {} for {} batches",
                  courseBatches.get(0).getCourse().getCode(),
                  courseBatches.size());
        
        // Get the course details from the first entry
        Course course = courseBatches.get(0).getCourse();
        int periodsPerBatch = course.getContactPeriods();
        
        // Track allocated slots for each batch to avoid conflicts
        Map<Long, Set<String>> batchAllocatedSlots = new HashMap<>();
        
        // Combine existing and new entries for checking
        List<TimetableEntry> combinedEntries = new ArrayList<>(existingEntries);
        combinedEntries.addAll(newEntries);
        
        // For each batch
        for (FacultyCourse facultyCourse : courseBatches) {
            Batch batch = facultyCourse.getBatch();
            int periodsAllocated = 0;
            
            // Keep track of allocated slots for this course-batch to avoid continuous scheduling
            Map<DayOfWeek, Set<Integer>> allocatedPeriodsByDay = new HashMap<>();
            
            // Distribute theory periods throughout the week
            while (periodsAllocated < periodsPerBatch) {
                // Shuffle time slots to randomize allocation
                List<TimeSlot> availableSlots = new ArrayList<>(allTimeSlots);
                Collections.shuffle(availableSlots);
                
                boolean allocated = false;
                for (TimeSlot slot : availableSlots) {
                    // Create a unique key for this time slot
                    String slotKey = slot.getDay() + "-" + slot.getPeriodNumber();
                    
                    // Check if this slot would create continuous theory classes for this course-batch pair
                    if (isCourseBatchContinuous(slot, batch.getId(), course.getId(), combinedEntries)) {
                        continue; // Skip this slot to avoid consecutive periods for same course-batch
                    }
                    
                    // Check if faculty is already allocated this slot for another batch of the same course
                    boolean slotUsedForOtherBatch = false;
                    for (Map.Entry<Long, Set<String>> entry : batchAllocatedSlots.entrySet()) {
                        if (entry.getValue().contains(slotKey)) {
                            slotUsedForOtherBatch = true;
                            break;
                        }
                    }
                    
                    if (slotUsedForOtherBatch) {
                        continue; // Skip this slot as it's used for another batch
                    }
                    
                    if (canAllocateSlot(faculty, batch, slot, combinedEntries)) {
                        TimetableEntry entry = new TimetableEntry(
                            faculty,
                            course,
                            batch,
                            slot,
                            academicYear,
                            semester
                        );
                        newEntries.add(entry);
                        combinedEntries.add(entry); // Update combined entries
                        
                        // Track this allocation to avoid continuous classes
                        allocatedPeriodsByDay
                            .computeIfAbsent(slot.getDay(), k -> new HashSet<>())
                            .add(slot.getPeriodNumber());
                        
                        // Track this allocation for this batch
                        batchAllocatedSlots
                            .computeIfAbsent(batch.getId(), k -> new HashSet<>())
                            .add(slotKey);
                        
                        periodsAllocated++;
                        allocated = true;
                        break;
                    }
                }
                
                if (!allocated) {
                    // If we can't allocate more periods, break to avoid infinite loop
                    logger.warn("Could not allocate all periods for course {} batch {}",
                              course.getCode(), batch.getBatchName());
                    break;
                }
                
                if (periodsAllocated >= periodsPerBatch) {
                    break;
                }
            }
        }
    }
    
    private void allocateTheoryCourseWithExisting(Faculty faculty, FacultyCourse theoryCourse, List<TimeSlot> allTimeSlots, 
                                                List<TimetableEntry> newEntries, List<TimetableEntry> existingEntries,
                                                String academicYear, String semester) {
        int periodsToAllocate = theoryCourse.getCourse().getContactPeriods();
        int periodsAllocated = 0;
        
        // Keep track of allocated slots for this course-batch to avoid continuous scheduling
        Map<DayOfWeek, Set<Integer>> allocatedPeriodsByDay = new HashMap<>();
        
        // Combine existing and new entries for checking
        List<TimetableEntry> combinedEntries = new ArrayList<>(existingEntries);
        combinedEntries.addAll(newEntries);
        
        // Distribute theory periods throughout the week
        while (periodsAllocated < periodsToAllocate) {
            // Shuffle time slots to randomize allocation
            List<TimeSlot> availableSlots = new ArrayList<>(allTimeSlots);
            Collections.shuffle(availableSlots);
            
            boolean allocated = false;
            for (TimeSlot slot : availableSlots) {
                // Check if this slot would create continuous theory classes for this course-batch pair
                if (isCourseBatchContinuous(slot, theoryCourse.getBatch().getId(), theoryCourse.getCourse().getId(), combinedEntries)) {
                    continue; // Skip this slot to avoid consecutive periods for same course-batch
                }
            
                if (canAllocateSlot(faculty, theoryCourse.getBatch(), slot, combinedEntries)) {
                    TimetableEntry entry = new TimetableEntry(
                        faculty, 
                        theoryCourse.getCourse(), 
                        theoryCourse.getBatch(), 
                        slot, 
                        academicYear, 
                        semester
                    );
                    newEntries.add(entry);
                    combinedEntries.add(entry); // Update combined entries
                
                    // Track this allocation to avoid continuous classes
                    allocatedPeriodsByDay
                        .computeIfAbsent(slot.getDay(), k -> new HashSet<>())
                        .add(slot.getPeriodNumber());
                
                    periodsAllocated++;
                    allocated = true;
                    break;
                }
            }
            
            if (!allocated) {
                // If we can't allocate more periods, break to avoid infinite loop
                break;
            }
            
            if (periodsAllocated >= periodsToAllocate) {
                break;
            }
        }
    }
    
    private List<String> validateTimetable(List<TimetableEntry> entries) {
        List<String> violations = new ArrayList<>();
        
        // Check lab constraints (max 2 labs per batch per day)
        Map<Long, Map<DayOfWeek, Set<Long>>> batchLabsByDay = new HashMap<>();
        
        // Build the lab allocation map
        for (TimetableEntry entry : entries) {
            if (entry.getCourse().getType() == Course.CourseType.LAB) {
                Long batchId = entry.getBatch().getId();
                DayOfWeek day = entry.getTimeSlot().getDay();
                Long courseId = entry.getCourse().getId();
                
                batchLabsByDay.computeIfAbsent(batchId, k -> new HashMap<>());
                batchLabsByDay.get(batchId).computeIfAbsent(day, k -> new HashSet<>());
                batchLabsByDay.get(batchId).get(day).add(courseId);
            }
        }
        
        // Check for violations
        for (Map.Entry<Long, Map<DayOfWeek, Set<Long>>> batchEntry : batchLabsByDay.entrySet()) {
            Long batchId = batchEntry.getKey();
            String batchName = entries.stream()
                .filter(e -> e.getBatch().getId().equals(batchId))
                .map(e -> e.getBatch().getBatchName())
                .findFirst()
                .orElse("Unknown");
            
            for (Map.Entry<DayOfWeek, Set<Long>> dayEntry : batchEntry.getValue().entrySet()) {
                DayOfWeek day = dayEntry.getKey();
                Set<Long> labCourseIds = dayEntry.getValue();
                
                if (labCourseIds.size() > MAX_LABS_PER_DAY) {
                    violations.add(String.format("Batch %s has %d labs on %s (max allowed: %d)", 
                                              batchName, labCourseIds.size(), day, MAX_LABS_PER_DAY));
                }
            }
        }
        
        // Check for faculty conflicts (same faculty in two places at once)
        Map<String, List<TimetableEntry>> facultySlotMap = new HashMap<>();
        
        for (TimetableEntry entry : entries) {
            String key = entry.getFaculty().getId() + "-" + entry.getTimeSlot().getDay() + "-" + entry.getTimeSlot().getPeriodNumber();
            
            facultySlotMap.computeIfAbsent(key, k -> new ArrayList<>());
            facultySlotMap.get(key).add(entry);
            
            if (facultySlotMap.get(key).size() > 1) {
                violations.add(String.format("Faculty %s is scheduled twice on %s period %d", 
                                          entry.getFaculty().getName(), 
                                          entry.getTimeSlot().getDay(), 
                                          entry.getTimeSlot().getPeriodNumber()));
            }
        }
        
        // Check for batch conflicts (same batch in two places at once)
        Map<String, List<TimetableEntry>> batchSlotMap = new HashMap<>();
        
        for (TimetableEntry entry : entries) {
            String key = entry.getBatch().getId() + "-" + entry.getTimeSlot().getDay() + "-" + entry.getTimeSlot().getPeriodNumber();
            
            batchSlotMap.computeIfAbsent(key, k -> new ArrayList<>());
            batchSlotMap.get(key).add(entry);
            
            if (batchSlotMap.get(key).size() > 1) {
                violations.add(String.format("Batch %s is scheduled twice on %s period %d", 
                                          entry.getBatch().getBatchName(), 
                                          entry.getTimeSlot().getDay(), 
                                          entry.getTimeSlot().getPeriodNumber()));
            }
        }
        
        // Check for consecutive periods for the same course-batch pair
        Map<String, List<TimetableEntry>> courseBatchDayMap = new HashMap<>();
        
        for (TimetableEntry entry : entries) {
            if (entry.getCourse().getType() == Course.CourseType.ACADEMIC) {
                String key = entry.getCourse().getId() + "-" + entry.getBatch().getId() + "-" + entry.getTimeSlot().getDay();
                
                courseBatchDayMap.computeIfAbsent(key, k -> new ArrayList<>());
                courseBatchDayMap.get(key).add(entry);
            }
        }
        
        // Check each course-batch-day group for consecutive periods
        for (Map.Entry<String, List<TimetableEntry>> entry : courseBatchDayMap.entrySet()) {
            List<TimetableEntry> dayEntries = entry.getValue();
            
            if (dayEntries.size() > 1) {
                // Sort by period number
                dayEntries.sort(Comparator.comparing(e -> e.getTimeSlot().getPeriodNumber()));
                
                // Check for consecutive periods
                for (int i = 0; i < dayEntries.size() - 1; i++) {
                    TimetableEntry current = dayEntries.get(i);
                    TimetableEntry next = dayEntries.get(i + 1);
                    
                    if (next.getTimeSlot().getPeriodNumber() == current.getTimeSlot().getPeriodNumber() + 1) {
                        violations.add(String.format("Course %s for batch %s has consecutive periods on %s (%d and %d)",
                                                  current.getCourse().getCode(),
                                                  current.getBatch().getBatchName(),
                                                  current.getTimeSlot().getDay(),
                                                  current.getTimeSlot().getPeriodNumber(),
                                                  next.getTimeSlot().getPeriodNumber()));
                    }
                }
            }
        }
        
        return violations;
    }
    
    private void allocateLabCourses(Faculty faculty, List<FacultyCourse> facultyCourses, List<TimetableEntry> entries, 
                                  String academicYear, String semester, Map<Long, Map<DayOfWeek, Set<Long>>> batchLabsByDay) {
        // Get lab courses
        List<FacultyCourse> labCourses = facultyCourses.stream()
            .filter(fc -> fc.getCourse().getType() == Course.CourseType.LAB)
            .collect(Collectors.toList());
        
        // Shuffle to randomize allocation
        Collections.shuffle(labCourses);
        
        // Get all time slots
        List<TimeSlot> allTimeSlots = timeSlotService.getAllNonBreakTimeSlots();
        
        // Allocate each lab course
        for (FacultyCourse labCourse : labCourses) {
            allocateLabCourse(faculty, labCourse, allTimeSlots, entries, academicYear, semester, batchLabsByDay);
        }
    }
    
    private void allocateNonAcademicCourses(Faculty faculty, List<FacultyCourse> facultyCourses, List<TimetableEntry> entries, 
                                          String academicYear, String semester) {
        // Get non-academic courses
        List<FacultyCourse> nonAcademicCourses = facultyCourses.stream()
            .filter(fc -> fc.getCourse().getType() == Course.CourseType.NON_ACADEMIC)
            .collect(Collectors.toList());
        
        // Shuffle to randomize allocation
        Collections.shuffle(nonAcademicCourses);
        
        // Get all time slots
        List<TimeSlot> allTimeSlots = timeSlotService.getAllNonBreakTimeSlots();
        
        // Allocate each non-academic course
        for (FacultyCourse nonAcademicCourse : nonAcademicCourses) {
            allocateNonAcademicCourse(faculty, nonAcademicCourse, allTimeSlots, entries, academicYear, semester);
        }
    }
    
    private void allocateTheoryCourses(Faculty faculty, List<FacultyCourse> facultyCourses, List<TimetableEntry> entries, 
                                 String academicYear, String semester) {
        // Get theory courses
        List<FacultyCourse> theoryCourses = facultyCourses.stream()
            .filter(fc -> fc.getCourse().getType() == Course.CourseType.ACADEMIC)
            .collect(Collectors.toList());
    
        // Group theory courses by course ID to handle same subject across batches
        Map<Long, List<FacultyCourse>> courseGroups = theoryCourses.stream()
            .collect(Collectors.groupingBy(fc -> fc.getCourse().getId()));
    
        // Sort groups by size (descending) to allocate courses taught to multiple batches first
        List<Map.Entry<Long, List<FacultyCourse>>> sortedGroups = courseGroups.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
            .collect(Collectors.toList());
    
        // Get all time slots
        List<TimeSlot> allTimeSlots = timeSlotService.getAllNonBreakTimeSlots();
    
        // Allocate each course group
        for (Map.Entry<Long, List<FacultyCourse>> group : sortedGroups) {
            // For courses taught to multiple batches, ensure different time slots
            if (group.getValue().size() > 1) {
                allocateMultiBatchTheoryCourse(faculty, group.getValue(), allTimeSlots, entries, academicYear, semester);
            } else {
                // For single batch courses, use the original method
                allocateTheoryCourse(faculty, group.getValue().get(0), allTimeSlots, entries, academicYear, semester);
            }
        }
    }

    // Add a new method to handle courses taught to multiple batches
    private void allocateMultiBatchTheoryCourse(Faculty faculty, List<FacultyCourse> courseBatches,
                                              List<TimeSlot> allTimeSlots, List<TimetableEntry> entries,
                                              String academicYear, String semester) {
        logger.info("Allocating course {} for {} batches",
                  courseBatches.get(0).getCourse().getCode(),
                  courseBatches.size());
    
        // Get the course details from the first entry
        Course course = courseBatches.get(0).getCourse();
        int periodsPerBatch = course.getContactPeriods();
    
        // Track allocated slots for each batch to avoid conflicts
        Map<Long, Set<String>> batchAllocatedSlots = new HashMap<>();
    
        // For each batch
        for (FacultyCourse facultyCourse : courseBatches) {
            Batch batch = facultyCourse.getBatch();
            int periodsAllocated = 0;
        
            // Keep track of allocated slots for this course-batch to avoid continuous scheduling
            Map<DayOfWeek, Set<Integer>> allocatedPeriodsByDay = new HashMap<>();
        
            // Distribute theory periods throughout the week
            while (periodsAllocated < periodsPerBatch) {
                // Shuffle time slots to randomize allocation
                List<TimeSlot> availableSlots = new ArrayList<>(allTimeSlots);
                Collections.shuffle(availableSlots);
            
                boolean allocated = false;
                for (TimeSlot slot : availableSlots) {
                    // Create a unique key for this time slot
                    String slotKey = slot.getDay() + "-" + slot.getPeriodNumber();
                
                    // Check if this slot would create consecutive periods for this course-batch pair
                    if (isCourseBatchContinuous(slot, batch.getId(), course.getId(), entries)) {
                        continue; // Skip this slot to avoid consecutive periods for same course-batch
                    }
                
                    // Check if faculty is already allocated this slot for another batch of the same course
                    boolean slotUsedForOtherBatch = false;
                    for (Map.Entry<Long, Set<String>> entry : batchAllocatedSlots.entrySet()) {
                        if (entry.getValue().contains(slotKey)) {
                            slotUsedForOtherBatch = true;
                            break;
                        }
                    }
                
                    if (slotUsedForOtherBatch) {
                        continue; // Skip this slot as it's used for another batch
                    }
                
                    if (canAllocateSlot(faculty, batch, slot, entries)) {
                        TimetableEntry entry = new TimetableEntry(
                            faculty,
                            course,
                            batch,
                            slot,
                            academicYear,
                            semester
                        );
                        entries.add(entry);
                
                        // Track this allocation to avoid continuous classes
                        allocatedPeriodsByDay
                            .computeIfAbsent(slot.getDay(), k -> new HashSet<>())
                            .add(slot.getPeriodNumber());
                    
                        // Track this allocation for this batch
                        batchAllocatedSlots
                            .computeIfAbsent(batch.getId(), k -> new HashSet<>())
                            .add(slotKey);
                
                        periodsAllocated++;
                        allocated = true;
                        break;
                    }
                }
            
                if (!allocated) {
                    // If we can't allocate more periods, break to avoid infinite loop
                    logger.warn("Could not allocate all periods for course {} batch {}",
                              course.getCode(), batch.getBatchName());
                    break;
                }
            
                if (periodsAllocated >= periodsPerBatch) {
                    break;
                }
            }
        }
    }
    
    private void allocateLabCourse(Faculty faculty, FacultyCourse labCourse, List<TimeSlot> allTimeSlots, 
                                  List<TimetableEntry> entries, String academicYear, String semester,
                                  Map<Long, Map<DayOfWeek, Set<Long>>> batchLabsByDay) {
        // Labs need consecutive periods and should not be in first period
        int totalPeriodsNeeded = labCourse.getCourse().getContactPeriods();
        
        // Keep all lab periods together regardless of the total number
        int periodsPerDay = totalPeriodsNeeded;
        int daysNeeded = 1; // Always allocate on a single day
        
        // Keep track of days already allocated for this lab
        Set<DayOfWeek> allocatedDays = new HashSet<>();
        Long batchId = labCourse.getBatch().getId();
        Long courseId = labCourse.getCourse().getId();
        
        // Initialize lab count for this batch if not already done
        if (!batchLabsByDay.containsKey(batchId)) {
            batchLabsByDay.put(batchId, new HashMap<>());
        }
          {
            batchLabsByDay.put(batchId, new HashMap<>());
        }
        
        // Allocate lab periods across different days
        for (int day = 0; day < daysNeeded; day++) {
            // Find suitable consecutive slots (not in first period)
            for (DayOfWeek dayOfWeek : Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
                                                  DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)) {
                
                // Skip days already allocated for this lab
                if (allocatedDays.contains(dayOfWeek)) {
                    continue;
                }

                // Check if batch already has MAX_LABS_PER_DAY labs on this day
                Set<Long> existingLabsOnDay = batchLabsByDay.get(batchId).getOrDefault(dayOfWeek, new HashSet<>());
                if (existingLabsOnDay.size() >= MAX_LABS_PER_DAY) {
                    continue; // Skip this day if batch already has maximum labs
                }
                
                List<TimeSlot> daySlots = allTimeSlots.stream()
                    .filter(ts -> ts.getDay() == dayOfWeek && ts.getPeriodNumber() > 1) // Not in first period
                    .sorted(Comparator.comparing(TimeSlot::getPeriodNumber))
                    .collect(Collectors.toList());
                
                // Find consecutive slots
                for (int i = 0; i <= daySlots.size() - periodsPerDay; i++) {
                    List<TimeSlot> consecutiveSlots = daySlots.subList(i, i + periodsPerDay);
                    
                    // Check if these slots are consecutive
                    boolean areConsecutive = true;
                    for (int j = 0; j < consecutiveSlots.size() - 1; j++) {
                        if (consecutiveSlots.get(j + 1).getPeriodNumber() != consecutiveSlots.get(j).getPeriodNumber() + 1) {
                            areConsecutive = false;
                            break;
                        }
                    }
                    
                    if (areConsecutive && canAllocateSlots(faculty, labCourse.getBatch(), consecutiveSlots, entries)) {
                        // Allocate all consecutive slots for this lab
                        for (TimeSlot slot : consecutiveSlots) {
                            TimetableEntry entry = new TimetableEntry(
                                faculty, 
                                labCourse.getCourse(), 
                                labCourse.getBatch(), 
                                slot, 
                                academicYear, 
                                semester
                            );
                            entries.add(entry);
                        }
                        
                        // Mark this day as allocated
                        allocatedDays.add(dayOfWeek);
                        
                        // Track this lab allocation
                        batchLabsByDay.get(batchId).computeIfAbsent(dayOfWeek, k -> new HashSet<>());
                        batchLabsByDay.get(batchId).get(dayOfWeek).add(courseId);
                        
                        break; // Successfully allocated for this day
                    }
                }
                
                // If we've allocated for this day, break the day loop
                if (allocatedDays.size() > day) {
                    break;
                }
            }
        }
    }

    private int countLabsForBatchOnDay(Batch batch, DayOfWeek day, List<TimetableEntry> entries) {
        // Count unique lab courses for this batch on this day
        return (int) entries.stream()
            .filter(e -> e.getBatch().getId().equals(batch.getId()) && 
                    e.getTimeSlot().getDay() == day && 
                    e.getCourse().getType() == Course.CourseType.LAB)
            .map(e -> e.getCourse().getId()) // Group by course ID
            .distinct() // Count each lab course only once
            .count();
    }
    
    private void allocateNonAcademicCourse(Faculty faculty, FacultyCourse nonAcademicCourse, List<TimeSlot> allTimeSlots, 
                                         List<TimetableEntry> entries, String academicYear, String semester) {
        int periodsToAllocate = nonAcademicCourse.getCourse().getContactPeriods();
        int periodsAllocated = 0;
        
        // Keep track of days already allocated for this non-academic course
        Set<DayOfWeek> allocatedDays = new HashSet<>();
        
        // Distribute non-academic periods throughout the week (one per day)
        while (periodsAllocated < periodsToAllocate) {
            // Get available days that haven't been allocated yet for this course
            List<DayOfWeek> availableDays = Arrays.asList(DayOfWeek.values()).stream()
                .filter(day -> day != DayOfWeek.SUNDAY && !allocatedDays.contains(day))
                .collect(Collectors.toList());
            
            if (availableDays.isEmpty()) {
                // If we've used all days, we might need to allocate more than one period per day
                // This is a fallback if we can't satisfy the constraint
                availableDays = Arrays.asList(DayOfWeek.values()).stream()
                    .filter(day -> day != DayOfWeek.SUNDAY)
                    .collect(Collectors.toList());
            }
            
            Collections.shuffle(availableDays);
            
            boolean allocated = false;
            for (DayOfWeek day : availableDays) {
                // Get slots for this day (excluding first and last periods)
                List<TimeSlot> daySlots = allTimeSlots.stream()
                    .filter(ts -> ts.getDay() == day && 
                           ts.getPeriodNumber() > 1 && // Not first period
                           ts.getPeriodNumber() < getLastPeriodNumber(allTimeSlots, day)) // Not last period
                    .collect(Collectors.toList());
                
                Collections.shuffle(daySlots);
                
                for (TimeSlot slot : daySlots) {
                    if (canAllocateSlot(faculty, nonAcademicCourse.getBatch(), slot, entries)) {
                        TimetableEntry entry = new TimetableEntry(
                            faculty, 
                            nonAcademicCourse.getCourse(), 
                            nonAcademicCourse.getBatch(), 
                            slot, 
                            academicYear, 
                            semester
                        );
                        entries.add(entry);
                        periodsAllocated++;
                        allocated = true;
                        allocatedDays.add(day);
                        break;
                    }
                }
                
                if (allocated) {
                    break;
                }
            }
            
            if (!allocated) {
                // If we can't allocate more periods, break to avoid infinite loop
                break;
            }
            
            if (periodsAllocated >= periodsToAllocate) {
                break;
            }
        }
    }
    
    private int getLastPeriodNumber(List<TimeSlot> allTimeSlots, DayOfWeek day) {
        return allTimeSlots.stream()
            .filter(ts -> ts.getDay() == day)
            .mapToInt(TimeSlot::getPeriodNumber)
            .max()
            .orElse(8); // Default to 8 if no periods found
    }
    
    private void allocateTheoryCourse(Faculty faculty, FacultyCourse theoryCourse, List<TimeSlot> allTimeSlots, 
                                     List<TimetableEntry> entries, String academicYear, String semester) {
        int periodsToAllocate = theoryCourse.getCourse().getContactPeriods();
        int periodsAllocated = 0;
        
        // Keep track of allocated slots for this course-batch to avoid continuous scheduling
        Map<DayOfWeek, Set<Integer>> allocatedPeriodsByDay = new HashMap<>();
        
        // Distribute theory periods throughout the week
        while (periodsAllocated < periodsToAllocate) {
            // Shuffle time slots to randomize allocation
            List<TimeSlot> availableSlots = new ArrayList<>(allTimeSlots);
            Collections.shuffle(availableSlots);
            
            boolean allocated = false;
            for (TimeSlot slot : availableSlots) {
                // Check if this slot would create consecutive periods for this course-batch pair
                if (isCourseBatchContinuous(slot, theoryCourse.getBatch().getId(), theoryCourse.getCourse().getId(), entries)) {
                    continue; // Skip this slot to avoid consecutive periods for same course-batch
                }
            
                if (canAllocateSlot(faculty, theoryCourse.getBatch(), slot, entries)) {
                    TimetableEntry entry = new TimetableEntry(
                        faculty, 
                        theoryCourse.getCourse(), 
                        theoryCourse.getBatch(), 
                        slot, 
                        academicYear, 
                        semester
                    );
                    entries.add(entry);
                
                    // Track this allocation to avoid continuous classes
                    allocatedPeriodsByDay
                        .computeIfAbsent(slot.getDay(), k -> new HashSet<>())
                        .add(slot.getPeriodNumber());
                
                    periodsAllocated++;
                    allocated = true;
                    break;
                }
            }
            
            if (!allocated) {
                // If we can't allocate more periods, break to avoid infinite loop
                break;
            }
            
            if (periodsAllocated >= periodsToAllocate) {
                break;
            }
        }
    }

    // Modified to check for consecutive periods for the same course-batch pair
    private boolean isCourseBatchContinuous(TimeSlot slot, Long batchId, Long courseId, List<TimetableEntry> entries) {
        DayOfWeek day = slot.getDay();
        int period = slot.getPeriodNumber();
    
        // Check if the previous or next period is already allocated for this course-batch on this day
        boolean hasPreviousPeriod = entries.stream()
            .anyMatch(e -> e.getCourse().getId().equals(courseId) && 
                     e.getBatch().getId().equals(batchId) &&
                     e.getTimeSlot().getDay() == day && 
                     e.getTimeSlot().getPeriodNumber() == period - 1);
                 
        boolean hasNextPeriod = entries.stream()
            .anyMatch(e -> e.getCourse().getId().equals(courseId) && 
                     e.getBatch().getId().equals(batchId) &&
                     e.getTimeSlot().getDay() == day && 
                     e.getTimeSlot().getPeriodNumber() == period + 1);
    
        return hasPreviousPeriod || hasNextPeriod;
    }
    
    private boolean canAllocateSlot(Faculty faculty, Batch batch, TimeSlot slot, List<TimetableEntry> entries) {
        // Check if faculty is already allocated in this time slot
        boolean facultyBusy = entries.stream()
            .anyMatch(e -> e.getFaculty().getId().equals(faculty.getId()) && 
                     e.getTimeSlot().getDay() == slot.getDay() && 
                     e.getTimeSlot().getPeriodNumber() == slot.getPeriodNumber());
        
        if (facultyBusy) {
            return false;
        }
        
        // Check if batch is already allocated in this time slot
        boolean batchBusy = entries.stream()
            .anyMatch(e -> e.getBatch().getId().equals(batch.getId()) && 
                     e.getTimeSlot().getDay() == slot.getDay() && 
                     e.getTimeSlot().getPeriodNumber() == slot.getPeriodNumber());
        
        if (batchBusy) {
            return false;
        }
        
        // Check existing timetable entries in database
        Optional<TimetableEntry> facultyEntry = timetableEntryRepository.findByFacultyIdAndDayAndPeriod(
            faculty.getId(), slot.getDay(), slot.getPeriodNumber());
        
        if (facultyEntry.isPresent()) {
            return false;
        }
        
        Optional<TimetableEntry> batchEntry = timetableEntryRepository.findByBatchIdAndDayAndPeriod(
            batch.getId(), slot.getDay(), slot.getPeriodNumber());
        
        return !batchEntry.isPresent();
    }
    
    private boolean canAllocateSlots(Faculty faculty, Batch batch, List<TimeSlot> slots, List<TimetableEntry> entries) {
        for (TimeSlot slot : slots) {
            if (!canAllocateSlot(faculty, batch, slot, entries)) {
                return false;
            }
        }
        return true;
    }

    public List<TimetableEntryDTO> getFacultyTimetable(Long facultyId) {
        List<TimetableEntry> entries = timetableEntryRepository.findByFacultyId(facultyId);
        return convertToDTO(entries);
    }

    public List<TimetableEntryDTO> getBatchTimetable(Long batchId, String academicYear, String semester) {
        List<TimetableEntry> entries = timetableEntryRepository.findByBatchIdAndAcademicYearAndSemester(batchId, academicYear, semester);
        return convertToDTO(entries);
    }

    private List<TimetableEntryDTO> convertToDTO(List<TimetableEntry> entries) {
        List<TimetableEntryDTO> dtos = new ArrayList<>();
        
        for (TimetableEntry entry : entries) {
            TimetableEntryDTO dto = new TimetableEntryDTO();
            dto.setId(entry.getId());
            dto.setFacultyName(entry.getFaculty().getName());
            dto.setCourseName(entry.getCourse().getTitle());
            dto.setCourseCode(entry.getCourse().getCode());
            dto.setBatchName(entry.getBatch().getBatchName());
            dto.setDepartment(entry.getBatch().getDepartment());
            dto.setSection(entry.getBatch().getSection());
            dto.setDay(entry.getTimeSlot().getDay());
            dto.setPeriodNumber(entry.getTimeSlot().getPeriodNumber());
            dto.setStartTime(entry.getTimeSlot().getStartTime());
            dto.setEndTime(entry.getTimeSlot().getEndTime());
            dto.setCourseType(entry.getCourse().getType().toString());
            dtos.add(dto);
        }
        
        return dtos;
    }

    // Add a more aggressive slot finding method for multi-batch scenarios
    private TimetableEntry findAlternativeSlotAggressively(TimetableEntry entry, List<TimetableEntry> existingEntries,
                                                         String academicYear, String semester) {
        Faculty faculty = entry.getFaculty();
        Course course = entry.getCourse();
        Batch batch = entry.getBatch();
    
        // Get all time slots
        List<TimeSlot> allTimeSlots = timeSlotService.getAllNonBreakTimeSlots();
        Collections.shuffle(allTimeSlots); // Randomize to avoid patterns
    
        // For each time slot
        for (TimeSlot slot : allTimeSlots) {
            // Check if faculty is already allocated in this time slot
            boolean facultyBusy = existingEntries.stream()
                .anyMatch(e -> e.getFaculty().getId().equals(faculty.getId()) &&
                         e.getTimeSlot().getDay() == slot.getDay() &&
                         e.getTimeSlot().getPeriodNumber() == slot.getPeriodNumber());
        
            if (facultyBusy) {
                continue;
            }
        
            // Check if batch is already allocated in this time slot
            boolean batchBusy = existingEntries.stream()
                .anyMatch(e -> e.getBatch().getId().equals(batch.getId()) &&
                         e.getTimeSlot().getDay() == slot.getDay() &&
                         e.getTimeSlot().getPeriodNumber() == slot.getPeriodNumber());
        
            if (batchBusy) {
                continue;
            }
        
            // If we get here, the slot is available for both faculty and batch
            return new TimetableEntry(faculty, course, batch, slot, academicYear, semester);
        }
    
        return null; // No alternative slot found
    }
}
