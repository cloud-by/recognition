package com.oj.repository;

import com.oj.entity.OjUser;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OjUserRepository extends JpaRepository<OjUser, Long> {
    Optional<OjUser> findByUsername(String username);

    List<OjUser> findByTeachingClassIdOrderByIdAsc(Long teachingClassId);

    List<OjUser> findByUsernameIn(Collection<String> usernames);
}