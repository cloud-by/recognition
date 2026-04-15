package com.oj.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "submission")
public class Submission {

    public enum JudgeStatus { PENDING, JUDGING, AC, WA, TLE, MLE, RE, CE, SE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private OjUser user;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Setter
    @Column(name = "source_code", nullable = false, columnDefinition = "LONGTEXT")
    private String sourceCode;

    @Setter
    @Column(nullable = false, length = 30)
    private String language;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "judge_status", nullable = false, length = 20)
    private JudgeStatus judgeStatus = JudgeStatus.PENDING;

    @Setter
    @Column(name = "runtime_ms")
    private Integer runtimeMs;

    @Setter
    @Column(name = "memory_kb")
    private Integer memoryKb;

    @Setter
    @Column(name = "submit_time", nullable = false)
    private LocalDateTime submitTime;

    @Setter
    @Column(name = "submit_ip", length = 64)
    private String submitIp;

    @Setter
    @Column(name = "judge_token", length = 100)
    private String judgeToken;

    @Setter
    @Column(name = "judge_detail", columnDefinition = "TEXT")
    private String judgeDetail;

    public Long getId() { return id; }
    public OjUser getUser() { return user; }

    public Problem getProblem() { return problem; }

    public String getSourceCode() { return sourceCode; }

    public String getLanguage() { return language; }

    public JudgeStatus getJudgeStatus() { return judgeStatus; }

    public Integer getRuntimeMs() { return runtimeMs; }

    public Integer getMemoryKb() { return memoryKb; }

    public LocalDateTime getSubmitTime() { return submitTime; }

    public String getSubmitIp() { return submitIp; }

    public String getJudgeToken() { return judgeToken; }

    public String getJudgeDetail() { return judgeDetail; }
}