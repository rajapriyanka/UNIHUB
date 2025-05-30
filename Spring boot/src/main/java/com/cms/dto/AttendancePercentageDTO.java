package com.cms.dto;

public class AttendancePercentageDTO {
    private Long studentId;
    private String studentName;
    private String studentDno;
    private Long courseId;
    private String courseCode;
    private String courseTitle;
    private Long facultyId;
    private String facultyName;
    private Integer semesterNo;
    private Double attendancePercentage;
    private Boolean isBelowThreshold;
    
    public AttendancePercentageDTO() {}

    // Getters and setters
    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentDno() {
        return studentDno;
    }

    public void setStudentDno(String studentDno) {
        this.studentDno = studentDno;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public Long getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(Long facultyId) {
        this.facultyId = facultyId;
    }

    public String getFacultyName() {
        return facultyName;
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }

    public Integer getSemesterNo() {
        return semesterNo;
    }

    public void setSemesterNo(Integer semesterNo) {
        this.semesterNo = semesterNo;
    }

    public Double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(Double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }

    public Boolean getIsBelowThreshold() {
        return isBelowThreshold;
    }

    public void setIsBelowThreshold(Boolean isBelowThreshold) {
        this.isBelowThreshold = isBelowThreshold;
    }
}