package com.oj.repository;

import com.oj.entity.ProblemTag;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemTagRepository extends JpaRepository<ProblemTag, Long> {
    Optional<ProblemTag> findByName(String name);
}