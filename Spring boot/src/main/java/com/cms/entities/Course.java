package com.cms.entities;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "courses", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"code", "type"})
       })
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String code;
   
    @Column(nullable = false)
    private Integer contactPeriods;

    @Column(nullable = false)
    private Integer semesterNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseType type;

    @Column(nullable = false)
    private String department;
    
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<FacultyCourse> facultyCourses = new HashSet<>();

    public void addFacultyCourse(FacultyCourse facultyCourse) {
        facultyCourses.add(facultyCourse);
        facultyCourse.setCourse(this);
    }

    public void removeFacultyCourse(FacultyCourse facultyCourse) {
        facultyCourses.remove(facultyCourse);
        facultyCourse.setCourse(null);
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getContactPeriods() {
        return contactPeriods;
    }

    public void setContactPeriods(Integer contactPeriods) {
        this.contactPeriods = contactPeriods;
    }

    public Integer getSemesterNo() {
        return semesterNo;
    }

    public void setSemesterNo(Integer semesterNo) {
        this.semesterNo = semesterNo;
    }

    public CourseType getType() {
        return type;
    }

    public void setType(CourseType type) {
        this.type = type;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public enum CourseType {
        ACADEMIC,
        NON_ACADEMIC,
        LAB
    }
}
