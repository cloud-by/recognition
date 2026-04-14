package com.oj.repository;

import com.oj.entity.Problem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
    List<Problem> findByTitleContainingIgnoreCase(String keyword);
}