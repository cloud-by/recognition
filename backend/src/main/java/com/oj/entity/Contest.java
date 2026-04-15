package com.oj.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "contest")
public class Contest {

    public enum ContestType { ACM, OI, IOI, PRACTICE }
    public enum RankingPolicy { FORMAL, CLASSROOM }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "contest_type", nullable = false, length = 20)
    private ContestType contestType = ContestType.ACM;

    @Enumerated(EnumType.STRING)
    @Column(name = "ranking_policy", nullable = false, length = 20)
    private RankingPolicy rankingPolicy = RankingPolicy.FORMAL;

    @Column(name = "freeze_board", nullable = false)
    private Boolean freezeBoard = false;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public ContestType getContestType() { return contestType; }
    public void setContestType(ContestType contestType) { this.contestType = contestType; }
    public RankingPolicy getRankingPolicy() { return rankingPolicy; }
    public void setRankingPolicy(RankingPolicy rankingPolicy) { this.rankingPolicy = rankingPolicy; }
    public Boolean getFreezeBoard() { return freezeBoard; }
    public void setFreezeBoard(Boolean freezeBoard) { this.freezeBoard = freezeBoard; }
    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }
}