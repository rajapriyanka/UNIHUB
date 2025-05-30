package com.cms.dto;

public class AttendanceDTO {
    
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentDno;
    private String department;
    private String section;
    private Long courseId;
    private String courseCode;
    private String courseTitle;
    private Long facultyId;
    private String facultyName;
    private String batchName;
    private Integer semesterNo;
    private Integer totalPeriods;
    private Integer periodsAttended;
    private Double attendancePercentage;
    private String date;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
    
    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
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

    public String getBatchName() {
        return batchName;
    }

    public void setBatchName(String batchName) {
        this.batchName = batchName;
    }

    public Integer getSemesterNo() {
        return semesterNo;
    }

    public void setSemesterNo(Integer semesterNo) {
        this.semesterNo = semesterNo;
    }

    public Integer getTotalPeriods() {
        return totalPeriods;
    }

    public void setTotalPeriods(Integer totalPeriods) {
        this.totalPeriods = totalPeriods;
    }

    public Integer getPeriodsAttended() {
        return periodsAttended;
    }

    public void setPeriodsAttended(Integer periodsAttended) {
        this.periodsAttended = periodsAttended;
    }

    public Double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(Double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}