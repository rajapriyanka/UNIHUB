package com.cms.entities;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "substitute_requests")
public class SubstituteRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "requester_id", nullable = false)
    private Faculty requester;

    @ManyToOne
    @JoinColumn(name = "substitute_id", nullable = false)
    private Faculty substitute;

    @ManyToOne
    @JoinColumn(name = "timetable_entry_id", nullable = false)
    private TimetableEntry timetableEntry;

    @Column(nullable = false)
    private LocalDate requestDate;

    @Column(nullable = false)
    private LocalDate substituteDate;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    private LocalDateTime responseTime;
    private String responseMessage;

    public enum RequestStatus {
        PENDING, APPROVED, REJECTED
    }

    public SubstituteRequest() {}

    public SubstituteRequest(Faculty requester, Faculty substitute, TimetableEntry timetableEntry, 
                            LocalDate requestDate, String reason) {
        this.requester = requester;
        this.substitute = substitute;
        this.timetableEntry = timetableEntry;
        this.requestDate = requestDate;
        this.substituteDate = requestDate; // Initialize substituteDate with the same value as requestDate
        this.reason = reason;
        this.status = RequestStatus.PENDING;
        this.requestedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Faculty getRequester() {
        return requester;
    }

    public void setRequester(Faculty requester) {
        this.requester = requester;
    }

    public Faculty getSubstitute() {
        return substitute;
    }

    public void setSubstitute(Faculty substitute) {
        this.substitute = substitute;
    }

    public TimetableEntry getTimetableEntry() {
        return timetableEntry;
    }

    public void setTimetableEntry(TimetableEntry timetableEntry) {
        this.timetableEntry = timetableEntry;
    }

    public LocalDate getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDate requestDate) {
        this.requestDate = requestDate;
    }

    public LocalDate getSubstituteDate() {
        return substituteDate;
    }

    public void setSubstituteDate(LocalDate substituteDate) {
        this.substituteDate = substituteDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(LocalDateTime responseTime) {
        this.responseTime = responseTime;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }
}

