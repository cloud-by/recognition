package com.oj.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "submission_test_point",
        indexes = {
                @Index(name = "idx_submission_test_point_submission", columnList = "submission_id"),
                @Index(name = "idx_submission_test_point_status", columnList = "judge_status"),
                @Index(name = "idx_submission_test_point_test_point", columnList = "test_point_id")
        })
public class SubmissionTestPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false,
            foreignKey = @jakarta.persistence.ForeignKey(name = "fk_submission_test_point_submission"))
    private Submission submission;

    @Setter
    @Column(name = "test_point_id", nullable = false)
    private Integer testPointId;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "judge_status", nullable = false, length = 20)
    private Submission.JudgeStatus judgeStatus;

    @Setter
    @Column(name = "runtime_ms")
    private Integer runtimeMs;

    @Setter
    @Column(name = "memory_kb")
    private Integer memoryKb;

    @Setter
    @Column(name = "stdout", columnDefinition = "TEXT")
    private String stdout;

    @Setter
    @Column(name = "expected_output", columnDefinition = "TEXT")
    private String expectedOutput;

    @Setter
    @Column(name = "stderr", columnDefinition = "TEXT")
    private String stderr;

    @Setter
    @Column(name = "compile_output", columnDefinition = "TEXT")
    private String compileOutput;

    @Setter
    @Column(name = "judge0_token", length = 100)
    private String judge0Token;

    @Setter
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Getters
    public Long getId() { return id; }
    public Submission getSubmission() { return submission; }
    public Integer getTestPointId() { return testPointId; }
    public Submission.JudgeStatus getJudgeStatus() { return judgeStatus; }
    public Integer getRuntimeMs() { return runtimeMs; }
    public Integer getMemoryKb() { return memoryKb; }
    public String getStdout() { return stdout; }
    public String getExpectedOutput() { return expectedOutput; }
    public String getStderr() { return stderr; }
    public String getCompileOutput() { return compileOutput; }
    public String getJudge0Token() { return judge0Token; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}