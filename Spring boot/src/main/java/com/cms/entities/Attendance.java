package com.cms.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @Column(nullable = false)
    private String batchName;

    @Column(nullable = false)
    private Integer semesterNo;

    @Column(nullable = false)
    private Integer totalPeriods;

    @Column(nullable = false)
    private Integer periodsAttended;

    @Column(nullable = false)
    private Double attendancePercentage;

    @Column(nullable = false)
    private String date;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Faculty getFaculty() {
        return faculty;
    }

    public void setFaculty(Faculty faculty) {
        this.faculty = faculty;
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

    // Calculate attendance percentage
    public void calculateAttendancePercentage() {
        if (totalPeriods > 0) {
            this.attendancePercentage = ((double) periodsAttended / totalPeriods) * 100;
        } else {
            this.attendancePercentage = 0.0;
        }
    }
}