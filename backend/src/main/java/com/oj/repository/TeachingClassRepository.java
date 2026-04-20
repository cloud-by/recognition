package com.oj.repository;

import com.oj.entity.TeachingClass;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeachingClassRepository extends JpaRepository<TeachingClass, Long> {
    List<TeachingClass> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    List<TeachingClass> findByNameContainingIgnoreCaseOrderByCreatedAtDesc(String keyword);

    List<TeachingClass> findByTeacherIdAndNameContainingIgnoreCaseOrderByCreatedAtDesc(Long teacherId, String keyword);

    List<TeachingClass> findAllByOrderByCreatedAtDesc();
}