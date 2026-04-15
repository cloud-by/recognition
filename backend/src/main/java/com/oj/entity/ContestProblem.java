package com.oj.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "contest_problem")
public class ContestProblem {

    @EmbeddedId
    private ContestProblemId id;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public ContestProblem() {
    }

    public ContestProblem(Long contestId, Long problemId, Integer sortOrder) {
        this.id = new ContestProblemId(contestId, problemId);
        this.sortOrder = sortOrder;
    }

    public ContestProblemId getId() {
        return id;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }
}