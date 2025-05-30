package com.cms.repository;

import com.cms.entities.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findByDayOrderByPeriodNumber(DayOfWeek day);
    List<TimeSlot> findByDayAndIsBreakFalseOrderByPeriodNumber(DayOfWeek day);
    List<TimeSlot> findByPeriodNumberAndIsBreakFalse(Integer periodNumber);
    List<TimeSlot> findAllByIsBreakFalseOrderByDayAscPeriodNumberAsc();
    List<TimeSlot> findByDay(DayOfWeek day);
    
    List<TimeSlot> findByDayAndIsBreak(DayOfWeek day, Boolean isBreak);
    
    Optional<TimeSlot> findByDayAndPeriodNumber(DayOfWeek day, Integer periodNumber);
    
    List<TimeSlot> findByIsBreak(Boolean isBreak);
    
    // Find consecutive time slots for a specific day
    @Query("SELECT ts FROM TimeSlot ts WHERE ts.day = :day AND ts.isBreak = false AND ts.periodNumber BETWEEN :startPeriod AND :endPeriod ORDER BY ts.periodNumber")
    List<TimeSlot> findConsecutiveSlots(@Param("day") DayOfWeek day, @Param("startPeriod") Integer startPeriod, @Param("endPeriod") Integer endPeriod);
    
    // Find time slots excluding first period
    @Query("SELECT ts FROM TimeSlot ts WHERE ts.day = :day AND ts.isBreak = false AND ts.periodNumber > 1 ORDER BY ts.periodNumber")
    List<TimeSlot> findSlotsExcludingFirstPeriod(@Param("day") DayOfWeek day);
    
    // Find time slots excluding last period
    @Query("SELECT ts FROM TimeSlot ts WHERE ts.day = :day AND ts.isBreak = false AND ts.periodNumber < (SELECT MAX(t.periodNumber) FROM TimeSlot t WHERE t.day = :day AND t.isBreak = false) ORDER BY ts.periodNumber")
    List<TimeSlot> findSlotsExcludingLastPeriod(@Param("day") DayOfWeek day);
    
    // Find time slots excluding first and last periods
    @Query("SELECT ts FROM TimeSlot ts WHERE ts.day = :day AND ts.isBreak = false AND ts.periodNumber > 1 AND ts.periodNumber < (SELECT MAX(t.periodNumber) FROM TimeSlot t WHERE t.day = :day AND t.isBreak = false) ORDER BY ts.periodNumber")
    List<TimeSlot> findSlotsExcludingFirstAndLastPeriods(@Param("day") DayOfWeek day);
    
    // Find the maximum period number for a day
    @Query("SELECT MAX(ts.periodNumber) FROM TimeSlot ts WHERE ts.day = :day AND ts.isBreak = false")
    Integer findMaxPeriodNumberForDay(@Param("day") DayOfWeek day);
    
    // Find time slots for lab allocation (not in first period)
    @Query("SELECT ts FROM TimeSlot ts WHERE ts.day = :day AND ts.isBreak = false AND ts.periodNumber > 1 ORDER BY ts.periodNumber")
    List<TimeSlot> findSlotsForLabAllocation(@Param("day") DayOfWeek day);
    
    // Find time slots for theory allocation (any period)
    @Query("SELECT ts FROM TimeSlot ts WHERE ts.day = :day AND ts.isBreak = false ORDER BY ts.periodNumber")
    List<TimeSlot> findSlotsForTheoryAllocation(@Param("day") DayOfWeek day);
    
    // Find time slots for non-academic allocation (middle periods)
    @Query("SELECT ts FROM TimeSlot ts WHERE ts.day = :day AND ts.isBreak = false AND ts.periodNumber > 1 AND ts.periodNumber < (SELECT MAX(t.periodNumber) FROM TimeSlot t WHERE t.day = :day AND t.isBreak = false) ORDER BY ts.periodNumber")
    List<TimeSlot> findSlotsForNonAcademicAllocation(@Param("day") DayOfWeek day);
}
