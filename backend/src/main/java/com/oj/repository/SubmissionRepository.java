package com.oj.repository;

import com.oj.entity.Submission;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByUserIdOrderBySubmitTimeDesc(Long userId);

    Optional<Submission> findTopByUserIdOrderBySubmitTimeDesc(Long userId);

    List<Submission> findByProblemIdInAndSubmitTimeBetweenOrderBySubmitTimeDesc(
            List<Long> problemIds,
            LocalDateTime startTime,
            LocalDateTime endTime
    );
}