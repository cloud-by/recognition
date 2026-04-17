package com.oj.repository;

import com.oj.entity.Problem;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
    List<Problem> findByTitleContainingIgnoreCase(String keyword);

    @Modifying
    @Query(value = "UPDATE problem SET id = id - 1 WHERE id > :deletedId ORDER BY id ASC", nativeQuery = true)
    int compactIdsAfterDelete(@Param("deletedId") Long deletedId);
}