package com.cms.dto;

public class FacultyDTO {
    private Long id;
    private String name;
    private String email;
    private String department;
    private String designation;
    private String mobileNo;
    private Long facultyId;  // New field to store faculty-specific ID

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public Long getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(Long facultyId) {
        this.facultyId = facultyId;
    }

    // Updated Constructor
    public FacultyDTO(Long id, String name, String email, String department, String designation, String mobileNo, Long facultyId) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.department = department;
        this.designation = designation;
        this.mobileNo = mobileNo;
        this.facultyId = facultyId;
    }
}
