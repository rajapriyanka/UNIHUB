package com.cms.dto;

public class FacultyRegistrationResponse {
    private String id;
    private String name;
    private String email;
    private String department;
    private String designation;
    private String mobileNo;
    private String userRole;

    // Default constructor
    public FacultyRegistrationResponse() {
    }

    // Parameterized constructor
    public FacultyRegistrationResponse(String id, String name, String email, String department, String designation, String mobileNo, String userRole) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.department = department;
        this.designation = designation;
        this.mobileNo = mobileNo;
        this.userRole = userRole;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    @Override
    public String toString() {
        return "FacultyRegistrationResponse{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", department='" + department + '\'' +
                ", designation='" + designation + '\'' +
                ", mobileNo='" + mobileNo + '\'' +
                ", userRole='" + userRole + '\'' +
                '}';
    }
}
