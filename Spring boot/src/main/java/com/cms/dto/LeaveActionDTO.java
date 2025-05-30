package com.cms.dto;

import com.cms.entities.Leave;

public class LeaveActionDTO {
    private Leave.LeaveStatus status;
    private String comments;

    // Getters and setters
    public Leave.LeaveStatus getStatus() {
        return status;
    }

    public void setStatus(Leave.LeaveStatus status) {
        this.status = status;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}

