CREATE DATABASE IF NOT EXISTS oj_platform
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

USE oj_platform;

-- =========================
-- 1. 用户表
-- =========================
CREATE TABLE IF NOT EXISTS oj_user (
                                       id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
                                       username VARCHAR(50) NOT NULL COMMENT '用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
    nickname VARCHAR(50) NOT NULL COMMENT '昵称',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';


-- =========================
-- 2. 题目表
-- =========================
CREATE TABLE IF NOT EXISTS problem (
                                       id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '题目ID',
                                       title VARCHAR(200) NOT NULL COMMENT '题目标题',
    description MEDIUMTEXT NOT NULL COMMENT '题目描述',
    input_format TEXT COMMENT '输入格式说明',
    output_format TEXT COMMENT '输出格式说明',
    sample_input TEXT COMMENT '样例输入',
    sample_output TEXT COMMENT '样例输出',
    time_limit_ms INT UNSIGNED NOT NULL DEFAULT 1000 COMMENT '时间限制(ms)',
    memory_limit_mb INT UNSIGNED NOT NULL DEFAULT 256 COMMENT '内存限制(MB)',
    testcase_path VARCHAR(500) NOT NULL COMMENT '测试数据存储路径',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_problem_title (title)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';


-- =========================
-- 3. 提交记录表
-- =========================
CREATE TABLE IF NOT EXISTS submission (
                                          id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '提交ID',
                                          user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
                                          problem_id BIGINT UNSIGNED NOT NULL COMMENT '题目ID',
                                          source_code LONGTEXT NOT NULL COMMENT '提交源码',
                                          language VARCHAR(30) NOT NULL COMMENT '编程语言',
    judge_status ENUM('PENDING', 'JUDGING', 'AC', 'WA', 'TLE', 'MLE', 'RE', 'CE', 'SE')
    NOT NULL DEFAULT 'PENDING' COMMENT '判题状态',
    runtime_ms INT UNSIGNED DEFAULT NULL COMMENT '运行耗时(ms)',
    memory_kb INT UNSIGNED DEFAULT NULL COMMENT '内存占用(KB)',
    submit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',

    CONSTRAINT fk_submission_user
    FOREIGN KEY (user_id) REFERENCES oj_user(id)
    ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_submission_problem
    FOREIGN KEY (problem_id) REFERENCES problem(id)
    ON DELETE CASCADE ON UPDATE CASCADE,

    KEY idx_submission_user (user_id),
    KEY idx_submission_problem (problem_id),
    KEY idx_submission_status (judge_status),
    KEY idx_submission_time (submit_time)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提交记录表';


-- =========================
-- 4. 比赛表
-- =========================
CREATE TABLE IF NOT EXISTS contest (
                                       id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '比赛ID',
                                       title VARCHAR(200) NOT NULL COMMENT '比赛标题',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    contest_type ENUM('ACM', 'OI', 'IOI', 'PRACTICE') NOT NULL DEFAULT 'ACM' COMMENT '比赛类型',
    freeze_board TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否封榜：0否，1是',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    KEY idx_contest_time (start_time, end_time)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛表';


-- =========================
-- 5. 防作弊日志表
-- =========================
CREATE TABLE IF NOT EXISTS anti_cheat_log (
                                              id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
                                              user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
                                              submission_id BIGINT UNSIGNED DEFAULT NULL COMMENT '提交记录ID',
                                              behavior_type VARCHAR(100) NOT NULL COMMENT '行为类型',
    detail_info TEXT COMMENT '详细描述信息',
    occurred_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '行为发生时间',

    CONSTRAINT fk_anti_cheat_user
    FOREIGN KEY (user_id) REFERENCES oj_user(id)
    ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_anti_cheat_submission
    FOREIGN KEY (submission_id) REFERENCES submission(id)
    ON DELETE SET NULL ON UPDATE CASCADE,

    KEY idx_anti_cheat_user (user_id),
    KEY idx_anti_cheat_submission (submission_id),
    KEY idx_anti_cheat_type (behavior_type),
    KEY idx_anti_cheat_time (occurred_time)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='防作弊日志表';


-- =========================
-- 6. 比赛题目关联表
-- =========================
CREATE TABLE IF NOT EXISTS contest_problem (
                                               contest_id BIGINT UNSIGNED NOT NULL COMMENT '比赛ID',
                                               problem_id BIGINT UNSIGNED NOT NULL COMMENT '题目ID',
                                               sort_order INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '题目在比赛中的顺序',

                                               PRIMARY KEY (contest_id, problem_id),

    CONSTRAINT fk_contest_problem_contest
    FOREIGN KEY (contest_id) REFERENCES contest(id)
    ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_contest_problem_problem
    FOREIGN KEY (problem_id) REFERENCES problem(id)
    ON DELETE CASCADE ON UPDATE CASCADE,

    UNIQUE KEY uk_contest_sort (contest_id, sort_order),
    KEY idx_cp_problem (problem_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛题目关联表';


-- =========================
-- 7. 比赛报名/参赛表
-- =========================
CREATE TABLE IF NOT EXISTS contest_participant (
                                                   contest_id BIGINT UNSIGNED NOT NULL COMMENT '比赛ID',
                                                   user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
                                                   register_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
                                                   participate_status ENUM('REGISTERED', 'JOINED', 'FINISHED', 'QUIT')
    NOT NULL DEFAULT 'REGISTERED' COMMENT '参赛状态',

    PRIMARY KEY (contest_id, user_id),

    CONSTRAINT fk_contest_participant_contest
    FOREIGN KEY (contest_id) REFERENCES contest(id)
    ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_contest_participant_user
    FOREIGN KEY (user_id) REFERENCES oj_user(id)
    ON DELETE CASCADE ON UPDATE CASCADE,

    KEY idx_cp_user (user_id),
    KEY idx_cp_status (participate_status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛报名/参赛表';