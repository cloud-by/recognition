package com.oj.repository;

import com.oj.entity.ContestProblem;
import com.oj.entity.ContestProblemId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContestProblemRepository extends JpaRepository<ContestProblem, ContestProblemId> {
    List<ContestProblem> findByIdContestIdOrderBySortOrderAsc(Long contestId);

    void deleteByIdContestId(Long contestId);

    List<ContestProblem> findByIdProblemId(Long problemId);

    List<ContestProblem> findByIdContestIdIn(List<Long> contestIds);

    Optional<ContestProblem> findByIdContestIdAndIdProblemId(Long contestId, Long problemId);
}