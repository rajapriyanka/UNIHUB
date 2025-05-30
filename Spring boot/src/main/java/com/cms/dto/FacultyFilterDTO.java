package com.cms.dto;

import java.sql.Date;
import java.time.LocalDate;

public class FacultyFilterDTO {
    private Long requestingFacultyId;
    private Date requestDate;
    private Integer periodNumber;
    private Long batchId;
    private boolean filterByAvailability;
    private boolean filterByBatch;
    
    // Getters and setters
    public Long getRequestingFacultyId() {
        return requestingFacultyId;
    }
    
    public void setRequestingFacultyId(Long requestingFacultyId) {
        this.requestingFacultyId = requestingFacultyId;
    }
    
    public Date getRequestDate() {
        return requestDate;
    }
    
    public void setRequestDate(Date requestDate) {
        this.requestDate = requestDate;
    }
    
    public Integer getPeriodNumber() {
        return periodNumber;
    }
    
    public void setPeriodNumber(Integer periodNumber) {
        this.periodNumber = periodNumber;
    }
    
    public Long getBatchId() {
        return batchId;
    }
    
    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }
    
    public boolean isFilterByAvailability() {
        return filterByAvailability;
    }
    
    public void setFilterByAvailability(boolean filterByAvailability) {
        this.filterByAvailability = filterByAvailability;
    }
    
    public boolean isFilterByBatch() {
        return filterByBatch;
    }
    
    public void setFilterByBatch(boolean filterByBatch) {
        this.filterByBatch = filterByBatch;
    }
}