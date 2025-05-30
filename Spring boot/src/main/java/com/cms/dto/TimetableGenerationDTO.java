package com.cms.dto;

public class TimetableGenerationDTO {
    private Long facultyId;
    private String academicYear;
    private String semester;

    public TimetableGenerationDTO() {}

    public TimetableGenerationDTO(Long facultyId, String academicYear, String semester) {
        this.facultyId = facultyId;
        this.academicYear = academicYear;
        this.semester = semester;
    }

    public Long getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(Long facultyId) {
        this.facultyId = facultyId;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }
}

