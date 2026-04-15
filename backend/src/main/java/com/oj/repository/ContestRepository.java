package com.oj.repository;

import com.oj.entity.Contest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContestRepository extends JpaRepository<Contest, Long> {
    List<Contest> findAllByOrderByStartTimeDesc();

    List<Contest> findByCreatedByUserIdOrderByStartTimeDesc(Long createdByUserId);
}