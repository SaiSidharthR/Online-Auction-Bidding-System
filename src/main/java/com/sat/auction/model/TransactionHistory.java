package com.sat.auction.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_history")
public class TransactionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "reference_id", nullable = false)
    private String referenceId;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status")
    private Status oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status")
    private Status newStatus;

    @Column(name = "event_at", nullable = false)
    private LocalDateTime eventAt;

    public TransactionHistory() {
    }

    public TransactionHistory(String referenceId, Long actorId, Status oldStatus, Status newStatus, LocalDateTime eventAt) {
        this.referenceId = referenceId;
        this.actorId = actorId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.eventAt = eventAt;
    }

    public Long getHistoryId() {
        return historyId;
    }

    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public Status getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(Status oldStatus) {
        this.oldStatus = oldStatus;
    }

    public Status getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(Status newStatus) {
        this.newStatus = newStatus;
    }

    public LocalDateTime getEventAt() {
        return eventAt;
    }

    public void setEventAt(LocalDateTime eventAt) {
        this.eventAt = eventAt;
    }
}
