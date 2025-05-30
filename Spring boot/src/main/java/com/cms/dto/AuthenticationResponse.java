package com.cms.dto;

import com.cms.enums.UserRole;

public class AuthenticationResponse {
    private final String jwt;
    private final UserRole userRole;
    private final Long facultyId; // Add this field

    public AuthenticationResponse(String jwt, UserRole userRole, Long facultyId) {
        this.jwt = jwt;
        this.userRole = userRole;
        this.facultyId = facultyId; // Assign it
    }

    public String getJwt() {
        return jwt;
    }

    public UserRole getUserRole() {
        return userRole;
    }

    public Long getFacultyId() {
        return facultyId;
    }
}

