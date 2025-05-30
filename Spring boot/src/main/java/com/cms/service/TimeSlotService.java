package com.cms.service;

import com.cms.entities.TimeSlot;
import com.cms.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Service
public class TimeSlotService {

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Transactional
    public void initializeTimeSlots() {
        // Check if time slots are already initialized
        if (timeSlotRepository.count() > 0) {
            return;
        }

        // Create time slots for each day of the week (Monday to Saturday)
        for (DayOfWeek day : new DayOfWeek[]{
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
        }) {
            // 1st period – 9.00am to 9.50am
            timeSlotRepository.save(new TimeSlot(day, 1, LocalTime.of(9, 0), LocalTime.of(9, 50), false));
            
            // 2nd period – 9.50am to 10.40am
            timeSlotRepository.save(new TimeSlot(day, 2, LocalTime.of(9, 50), LocalTime.of(10, 40), false));
            
            // Break – 10.40am to 10.50am
            timeSlotRepository.save(new TimeSlot(day, 0, LocalTime.of(10, 40), LocalTime.of(10, 50), true));
            
            // 3rd period – 10.50am to 11.40am
            timeSlotRepository.save(new TimeSlot(day, 3, LocalTime.of(10, 50), LocalTime.of(11, 40), false));
            
            // 4th period – 11.40am to 12.30pm
            timeSlotRepository.save(new TimeSlot(day, 4, LocalTime.of(11, 40), LocalTime.of(12, 30), false));
            
            // Lunch Break – 12.30pm to 01.10pm
            timeSlotRepository.save(new TimeSlot(day, 0, LocalTime.of(12, 30), LocalTime.of(13, 10), true));
            
            // 5th period – 1.10pm to 2.00pm
            timeSlotRepository.save(new TimeSlot(day, 5, LocalTime.of(13, 10), LocalTime.of(14, 0), false));
            
            // 6th period – 2.00pm to 2.50pm
            timeSlotRepository.save(new TimeSlot(day, 6, LocalTime.of(14, 0), LocalTime.of(14, 50), false));
            
            // Break – 2.50pm to 3.00pm
            timeSlotRepository.save(new TimeSlot(day, 0, LocalTime.of(14, 50), LocalTime.of(15, 0), true));
            
            // 7th period – 3.00pm to 3.50pm
            timeSlotRepository.save(new TimeSlot(day, 7, LocalTime.of(15, 0), LocalTime.of(15, 50), false));
            
            // 8th period – 3.50pm to 4.40pm
            timeSlotRepository.save(new TimeSlot(day, 8, LocalTime.of(15, 50), LocalTime.of(16, 40), false));
        }
    }

    public List<TimeSlot> getAllTimeSlots() {
        return timeSlotRepository.findAll();
    }

    public List<TimeSlot> getTimeSlotsByDay(DayOfWeek day) {
        return timeSlotRepository.findByDayOrderByPeriodNumber(day);
    }

    public List<TimeSlot> getNonBreakTimeSlotsByDay(DayOfWeek day) {
        return timeSlotRepository.findByDayAndIsBreakFalseOrderByPeriodNumber(day);
    }

    public List<TimeSlot> getAllNonBreakTimeSlots() {
        return timeSlotRepository.findAllByIsBreakFalseOrderByDayAscPeriodNumberAsc();
    }
}
