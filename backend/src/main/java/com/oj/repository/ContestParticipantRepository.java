package com.oj.repository;

import com.oj.entity.ContestParticipant;
import com.oj.entity.ContestParticipantId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContestParticipantRepository extends JpaRepository<ContestParticipant, ContestParticipantId> {
    List<ContestParticipant> findByIdContestId(Long contestId);

    List<ContestParticipant> findByIdUserId(Long userId);

    boolean existsByIdContestIdAndIdUserId(Long contestId, Long userId);
}