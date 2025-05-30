package com.cms.dto;

public class StudentDTO {
    private Long userId;
    private String name;
    private String email;
    private String department;
    private String dno;
    private String batchName;
    private String mobileNumber;
    private Long studentId;
    private String section;

    public StudentDTO() {
    }

    public StudentDTO(Long userId, String name, String email, String department, String dno, 
                     String batchName, String mobileNumber, Long studentId, String section) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.department = department;
        this.dno = dno;
        this.batchName = batchName;
        this.mobileNumber = mobileNumber;
        this.studentId = studentId;
        this.section = section;
    }

    // Getters and setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getDno() {
        return dno;
    }

    public void setDno(String dno) {
        this.dno = dno;
    }

    public String getBatchName() {
        return batchName;
    }

    public void setBatchName(String batchName) {
        this.batchName = batchName;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }
    
    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }
}