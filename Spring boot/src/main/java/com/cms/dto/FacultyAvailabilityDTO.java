package com.cms.dto;

public class FacultyAvailabilityDTO {
    private Long facultyId;
    private String name;
    private String department;
    private String designation;
    private String email;
    private boolean isAvailable;
    private boolean handlesBatch;
    
    // Getters and setters
    public Long getFacultyId() {
        return facultyId;
    }
    
    public void setFacultyId(Long facultyId) {
        this.facultyId = facultyId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDepartment() {
        return department;
    }
    
    public void setDepartment(String department) {
        this.department = department;
    }
    
    public String getDesignation() {
        return designation;
    }
    
    public void setDesignation(String designation) {
        this.designation = designation;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public boolean isAvailable() {
        return isAvailable;
    }
    
    public void setAvailable(boolean available) {
        isAvailable = available;
    }
    
    public boolean isHandlesBatch() {
        return handlesBatch;
    }
    
    public void setHandlesBatch(boolean handlesBatch) {
        this.handlesBatch = handlesBatch;
    }
}