package com.cms.dto;

public class FacultyCourseDTO {
    private String title;
    private String code;
    private Integer contactPeriods;
    private String batchName;
    private String department;
    private String section;
    private Long courseId;
    private Long batchId;
    public Long getCourseId() {
		return courseId;
	}

	public void setCourseId(Long courseId) {
		this.courseId = courseId;
	}

	public Long getBatchId() {
		return batchId;
	}

	public void setBatchId(Long batchId) {
		this.batchId = batchId;
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
}
