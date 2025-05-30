package com.cms.dto;

public class FacultyProfileUpdateRequest {
    private String name;
    private String department;
    private String designation;
    private String mobileNo;
    
    // Default constructor
    public FacultyProfileUpdateRequest() {
    }
    
    // Constructor with all fields
    public FacultyProfileUpdateRequest(String name, String department, String designation, String mobileNo) {
        this.name = name;
        this.department = department;
        this.designation = designation;
        this.mobileNo = mobileNo;
    }
    
    // Getters and setters
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
    
    public String getMobileNo() {
        return mobileNo;
    }
    
    public void setMobileNo(String mobileNo) {
        this.mobileNo = mobileNo;
    }
}

