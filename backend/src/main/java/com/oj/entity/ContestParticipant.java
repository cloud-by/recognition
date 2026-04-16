package com.oj.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "contest_participant")
public class ContestParticipant {

    public enum ParticipateStatus { REGISTERED, JOINED, FINISHED, QUIT }

    @EmbeddedId
    private ContestParticipantId id;

    @Column(name = "register_time", nullable = false)
    private LocalDateTime registerTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "participate_status", nullable = false, length = 20)
    private ParticipateStatus participateStatus = ParticipateStatus.REGISTERED;

    @Column(name = "last_access_ip", length = 64)
    private String lastAccessIp;

    public ContestParticipant() {
    }

    public ContestParticipant(Long contestId, Long userId) {
        this.id = new ContestParticipantId(contestId, userId);
    }

    @PrePersist
    public void prePersist() {
        if (this.registerTime == null) {
            this.registerTime = LocalDateTime.now();
        }
    }

    public ContestParticipantId getId() {
        return id;
    }

    public LocalDateTime getRegisterTime() {
        return registerTime;
    }

    public ParticipateStatus getParticipateStatus() {
        return participateStatus;
    }

    public String getLastAccessIp() {
        return lastAccessIp;
    }

    public void setLastAccessIp(String lastAccessIp) {
        this.lastAccessIp = lastAccessIp;
    }
}