package com.oj.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "anti_cheat_log")
public class AntiCheatLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private OjUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id")
    private Submission submission;

    @Column(name = "behavior_type", nullable = false, length = 100)
    private String behaviorType;

    @Column(name = "detail_info", columnDefinition = "TEXT")
    private String detailInfo;

    @Column(name = "occurred_time", nullable = false)
    private LocalDateTime occurredTime;

    @PrePersist
    public void prePersist() {
        if (occurredTime == null) {
            occurredTime = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public OjUser getUser() { return user; }
    public void setUser(OjUser user) { this.user = user; }
    public Submission getSubmission() { return submission; }
    public void setSubmission(Submission submission) { this.submission = submission; }
    public String getBehaviorType() { return behaviorType; }
    public void setBehaviorType(String behaviorType) { this.behaviorType = behaviorType; }
    public String getDetailInfo() { return detailInfo; }
    public void setDetailInfo(String detailInfo) { this.detailInfo = detailInfo; }
    public LocalDateTime getOccurredTime() { return occurredTime; }
    public void setOccurredTime(LocalDateTime occurredTime) { this.occurredTime = occurredTime; }
}