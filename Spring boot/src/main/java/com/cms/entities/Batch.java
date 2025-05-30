package com.cms.entities;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "batches")
public class Batch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String batchName;

    @Column(nullable = false)
    private String department;

    private String section;
    
    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<FacultyCourse> facultyCourses = new HashSet<>();

    // Getters and setters...

    public void addFacultyCourse(FacultyCourse facultyCourse) {
        facultyCourses.add(facultyCourse);
        facultyCourse.setBatch(this);
    }

    public void removeFacultyCourse(FacultyCourse facultyCourse) {
        facultyCourses.remove(facultyCourse);
        facultyCourse.setBatch(null);
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBatchName() {
        return batchName;
    }

    public void setBatchName(String batchName) {
        this.batchName = batchName;
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
		// TODO Auto-generated method stub
		return null;
	}
}
