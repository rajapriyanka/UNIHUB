package com.cms.entities;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

@Entity
@Table(name = "faculty")
public class Faculty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String department;
    
    @OneToMany(mappedBy = "faculty", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<TimetableEntry> timetableEntries;
    
    @OneToMany(mappedBy = "faculty", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<FacultyCourse> facultyCourses = new HashSet<>();

    public void addFacultyCourse(FacultyCourse facultyCourse) {
        facultyCourses.add(facultyCourse);
        facultyCourse.setFaculty(this);
    }

    public void removeFacultyCourse(FacultyCourse facultyCourse) {
        facultyCourses.remove(facultyCourse);
        facultyCourse.setFaculty(null);
    }
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

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	private String designation;
    private String mobileNo;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonManagedReference
    private User user;


    // Getters and setters
    // ...
}

