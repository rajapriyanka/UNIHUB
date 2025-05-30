package com.cms.dto;

import com.cms.entities.Course;
import com.cms.entities.Course.CourseType;

public class CourseDTO {
    private Long id;
    private String title;
    private String code;
    private int contactPeriods;
    private int semesterNo;
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
	public int getContactPeriods() {
		return contactPeriods;
	}
	public void setContactPeriods(int contactPeriods) {
		this.contactPeriods = contactPeriods;
	}
	public int getSemesterNo() {
		return semesterNo;
	}
	public void setSemesterNo(int semesterNo) {
		this.semesterNo = semesterNo;
	}
	public Course.CourseType getType() {
		return type;
	}
	public void setType(Course.CourseType type) {
		this.type = type;
	}
	private Course.CourseType type;
	public CourseDTO(Long id, String title, String code, int contactPeriods, int semesterNo, CourseType type) {
		super();
		this.id = id;
		this.title = title;
		this.code = code;
		this.contactPeriods = contactPeriods;
		this.semesterNo = semesterNo;
		this.type = type;
	}

    // Constructor, getters, and setters
    // ...
}

