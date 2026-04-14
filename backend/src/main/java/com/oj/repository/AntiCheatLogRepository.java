package com.oj.repository;

import com.oj.entity.AntiCheatLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AntiCheatLogRepository extends JpaRepository<AntiCheatLog, Long>, JpaSpecificationExecutor<AntiCheatLog> {
}