package com.oj.repository;

import com.oj.entity.SubmissionTestPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubmissionTestPointRepository extends JpaRepository<SubmissionTestPoint, Long> {

}