package com.cms.service.admin;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import com.cms.dto.TimetableEntryDTO;
import com.cms.dto.TimetableGenerationDTO;
import com.cms.entities.*;
import com.cms.repository.*;
import com.cms.service.TimeSlotService;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "cms.service.enabled", havingValue = "true", matchIfMissing = true)
public class TimetableService {
    
    @Autowired
    private TimetableEntryRepository timetableEntryRepository;

    @Autowired
    private FacultyCourseRepository facultyCourseRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private TimeSlotService timeSlotService;

    private static final int POPULATION_SIZE = 50;
    private static final int GENERATIONS = 100;
    private static final double MUTATION_RATE = 0.1;

    @Transactional
    public List<TimetableEntryDTO> generateTimetable(TimetableGenerationDTO dto) {
        timeSlotService.initializeTimeSlots();
        Faculty faculty = facultyRepository.findById(dto.getFacultyId())
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
        List<FacultyCourse> facultyCourses = facultyCourseRepository.findByFacultyId(dto.getFacultyId());
        if (facultyCourses.isEmpty()) {
            throw new RuntimeException("No courses assigned to faculty");
        }
        
        List<TimetableEntry> bestSolution = runGeneticAlgorithm(faculty, facultyCourses, dto.getAcademicYear(), dto.getSemester());
        timetableEntryRepository.deleteAll(timetableEntryRepository.findByFacultyId(dto.getFacultyId()));
        timetableEntryRepository.saveAll(bestSolution);
        
        return convertToDTO(bestSolution);
    }
    
    private List<TimetableEntry> runGeneticAlgorithm(Faculty faculty, List<FacultyCourse> facultyCourses, String academicYear, String semester) {
        List<List<TimetableEntry>> population = initializePopulation(faculty, facultyCourses, academicYear, semester);
        for (int gen = 0; gen < GENERATIONS; gen++) {
            population = evolvePopulation(population);
        }
        return population.get(0);
    }
    private List<TimetableEntry> generateRandomTimetable(Faculty faculty, List<FacultyCourse> facultyCourses, String academicYear, String semester) {
        List<TimetableEntry> entries = new ArrayList<>();
        List<TimeSlot> allTimeSlots = null;
		try {
			allTimeSlots = timeSlotService.getAllNonBreakTimeSlots();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        Collections.shuffle(allTimeSlots);
        for (FacultyCourse course : facultyCourses) {
            int periods = course.getCourse().getContactPeriods();
            for (int i = 0; i < periods; i++) {
                TimeSlot slot = allTimeSlots.get(i % allTimeSlots.size());
                entries.add(new TimetableEntry(faculty, course.getCourse(), course.getBatch(), slot, academicYear, semester));
            }
        }
        return entries;
    }

    private List<List<TimetableEntry>> initializePopulation(Faculty faculty, List<FacultyCourse> facultyCourses, String academicYear, String semester) {
        List<List<TimetableEntry>> population = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            List<TimetableEntry> entries = generateRandomTimetable(faculty, facultyCourses, academicYear, semester);
            population.add(entries);
        }
        return population;
    }

     private List<TimetableEntry> crossover(List<TimetableEntry> parent1, List<TimetableEntry> parent2) {
        List<TimetableEntry> child = new ArrayList<>();
        for (int i = 0; i < parent1.size(); i++) {
            child.add(Math.random() < 0.5 ? parent1.get(i) : parent2.get(i));
        }
        return child;
    }
     
     
     private void mutate(List<TimetableEntry> timetable) {
         List<TimeSlot> allTimeSlots = timeSlotService.getAllNonBreakTimeSlots();
         TimetableEntry entry = timetable.get(new Random().nextInt(timetable.size()));
         entry.setTimeSlot(allTimeSlots.get(new Random().nextInt(allTimeSlots.size())));
     }
         
     private int calculateFitness(List<TimetableEntry> timetable) {
         int score = 0;
         Set<String> occupiedSlots = new HashSet<>();
         for (TimetableEntry entry : timetable) {
             String key = entry.getFaculty().getId() + "-" + entry.getTimeSlot().getDay() + "-" + entry.getTimeSlot().getPeriodNumber();
             if (!occupiedSlots.add(key)) {
                 score += 10;
             }
         }
         return score;
     }
    
    
    private List<List<TimetableEntry>> evolvePopulation(List<List<TimetableEntry>> population) {
        population.sort(Comparator.comparingInt(this::calculateFitness));
        List<List<TimetableEntry>> newPopulation = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE / 2; i++) {
            List<TimetableEntry> parent1 = population.get(i);
            List<TimetableEntry> parent2 = population.get(i + 1);
            List<TimetableEntry> child = crossover(parent1, parent2);
            if (Math.random() < MUTATION_RATE) {
                mutate(child);
            }
            newPopulation.add(child);
        }
        newPopulation.addAll(population.subList(0, POPULATION_SIZE / 2));
        return newPopulation;
    }
    

    
    private List<TimetableEntryDTO> convertToDTO(List<TimetableEntry> entries) {
        return entries.stream().map(entry -> {
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
            return dto;
        }).collect(Collectors.toList());
    }
}