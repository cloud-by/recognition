package com.oj.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ContestParticipantId implements Serializable {

    @Column(name = "contest_id")
    private Long contestId;

    @Column(name = "user_id")
    private Long userId;

    public ContestParticipantId() {
    }

    public ContestParticipantId(Long contestId, Long userId) {
        this.contestId = contestId;
        this.userId = userId;
    }

    public Long getContestId() {
        return contestId;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ContestParticipantId that)) {
            return false;
        }
        return Objects.equals(contestId, that.contestId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contestId, userId);
    }
}