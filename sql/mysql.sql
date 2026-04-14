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

USE oj_platform;

-- 为了方便重复执行，这里先按外键依赖顺序清空测试数据
DELETE FROM anti_cheat_log;
DELETE FROM contest_participant;
DELETE FROM contest_problem;
DELETE FROM submission;
DELETE FROM contest;
DELETE FROM problem;
DELETE FROM oj_user;

-- =========================
-- 1. 用户测试数据
-- =========================
INSERT INTO oj_user (id, username, password_hash, nickname, created_at, updated_at) VALUES
                                                                                        (1, 'admin',   '$2a$10$admin_demo_hash',   '管理员', '2026-04-01 09:00:00', '2026-04-01 09:00:00'),
                                                                                        (2, 'alice',   '$2a$10$alice_demo_hash',   'Alice', '2026-04-01 09:10:00', '2026-04-01 09:10:00'),
                                                                                        (3, 'bob',     '$2a$10$bob_demo_hash',     'Bob',   '2026-04-01 09:20:00', '2026-04-01 09:20:00'),
                                                                                        (4, 'charlie', '$2a$10$charlie_demo_hash', '小查',  '2026-04-01 09:30:00', '2026-04-01 09:30:00'),
                                                                                        (5, 'diana',   '$2a$10$diana_demo_hash',   'Diana', '2026-04-01 09:40:00', '2026-04-01 09:40:00');

-- =========================
-- 2. 题目测试数据
-- =========================
INSERT INTO problem (
    id, title, description, input_format, output_format,
    sample_input, sample_output, time_limit_ms, memory_limit_mb,
    testcase_path, created_at, updated_at
) VALUES
      (
          1,
          'A + B Problem',
          '输入两个整数 A 和 B，输出它们的和。',
          '输入一行，包含两个整数 A 和 B。',
          '输出一个整数，表示 A + B 的结果。',
          '1 2',
          '3',
          1000,
          128,
          '/data/testcases/problem_1/',
          '2026-04-01 10:00:00',
          '2026-04-01 10:00:00'
      ),
      (
          2,
          '最大值',
          '给定 n 个整数，输出其中的最大值。',
          '第一行输入整数 n。第二行输入 n 个整数。',
          '输出一个整数，表示最大值。',
          '5\n1 9 3 7 2',
          '9',
          1000,
          128,
          '/data/testcases/problem_2/',
          '2026-04-01 10:10:00',
          '2026-04-01 10:10:00'
      ),
      (
          3,
          '回文串判断',
          '输入一个字符串，判断其是否为回文串，是则输出 Yes，否则输出 No。',
          '输入一行字符串 s。',
          '输出 Yes 或 No。',
          'level',
          'Yes',
          1000,
          128,
          '/data/testcases/problem_3/',
          '2026-04-01 10:20:00',
          '2026-04-01 10:20:00'
      ),
      (
          4,
          '斐波那契数列',
          '输入 n，输出第 n 项斐波那契数列的值（从 0 开始计数）。',
          '输入一个整数 n。',
          '输出一个整数。',
          '6',
          '8',
          2000,
          256,
          '/data/testcases/problem_4/',
          '2026-04-01 10:30:00',
          '2026-04-01 10:30:00'
      );

-- =========================
-- 3. 比赛测试数据
-- =========================
INSERT INTO contest (
    id, title, start_time, end_time, contest_type, freeze_board, created_at, updated_at
) VALUES
      (
          1,
          '2026 春季新生选拔赛',
          '2026-04-20 19:00:00',
          '2026-04-20 21:00:00',
          'ACM',
          1,
          '2026-04-05 12:00:00',
          '2026-04-05 12:00:00'
      ),
      (
          2,
          '每周练习赛 Week 1',
          '2026-04-18 14:00:00',
          '2026-04-18 16:00:00',
          'PRACTICE',
          0,
          '2026-04-05 12:30:00',
          '2026-04-05 12:30:00'
      );

-- =========================
-- 4. 比赛题目关联测试数据
-- =========================
INSERT INTO contest_problem (contest_id, problem_id, sort_order) VALUES
                                                                     (1, 1, 1),
                                                                     (1, 2, 2),
                                                                     (1, 3, 3),
                                                                     (2, 2, 1),
                                                                     (2, 4, 2);

-- =========================
-- 5. 比赛报名/参赛测试数据
-- =========================
INSERT INTO contest_participant (contest_id, user_id, register_time, participate_status) VALUES
                                                                                             (1, 2, '2026-04-10 08:00:00', 'REGISTERED'),
                                                                                             (1, 3, '2026-04-10 08:10:00', 'JOINED'),
                                                                                             (1, 4, '2026-04-10 08:20:00', 'FINISHED'),
                                                                                             (2, 2, '2026-04-11 10:00:00', 'JOINED'),
                                                                                             (2, 5, '2026-04-11 10:05:00', 'REGISTERED');

-- =========================
-- 6. 提交记录测试数据
-- =========================
INSERT INTO submission (
    id, user_id, problem_id, source_code, language, judge_status,
    runtime_ms, memory_kb, submit_time
) VALUES
      (
          1,
          2,
          1,
          '#include <iostream>\nusing namespace std;\nint main(){int a,b;cin>>a>>b;cout<<a+b;return 0;}',
          'cpp',
          'AC',
          12,
          1024,
          '2026-04-12 10:00:00'
      ),
      (
          2,
          2,
          2,
          '#include <bits/stdc++.h>\nusing namespace std;\nint main(){int n;cin>>n;vector<int>a(n);for(int i=0;i<n;i++)cin>>a[i];cout<<*max_element(a.begin(),a.end());}',
          'cpp',
          'AC',
          18,
          1536,
          '2026-04-12 10:05:00'
      ),
      (
          3,
          3,
          1,
          'print(sum(map(int,input().split())))',
          'python',
          'AC',
          35,
          8192,
          '2026-04-12 10:10:00'
      ),
      (
          4,
          3,
          3,
          's=input()\nprint("Yes" if s==s[::-1] else "Noo")',
          'python',
          'WA',
          27,
          7168,
          '2026-04-12 10:15:00'
      ),
      (
          5,
          4,
          4,
          '#include <iostream>\nusing namespace std;\nint f(int n){if(n<=1)return n;return f(n-1)+f(n-2);}int main(){int n;cin>>n;cout<<f(n);}',
          'cpp',
          'TLE',
          2001,
          2048,
          '2026-04-12 10:20:00'
      ),
      (
          6,
          5,
          2,
          'public class Main { public static void main(String[] args) { System.out.println(0); } }',
          'java',
          'CE',
          NULL,
          NULL,
          '2026-04-12 10:25:00'
      ),
      (
          7,
          4,
          1,
          '#include <iostream>\nusing namespace std;\nint main(){int a,b;cin>>a>>b;cout<<a-b;return 0;}',
          'cpp',
          'WA',
          11,
          1024,
          '2026-04-12 10:30:00'
      ),
      (
          8,
          2,
          4,
          'def fib(n):\n    a,b=0,1\n    for _ in range(n):\n        a,b=b,a+b\n    print(a)\nfib(int(input()))',
          'python',
          'AC',
          22,
          9216,
          '2026-04-12 10:40:00'
      );

-- =========================
-- 7. 防作弊日志测试数据
-- =========================
INSERT INTO anti_cheat_log (
    id, user_id, submission_id, behavior_type, detail_info, occurred_time
) VALUES
      (
          1,
          3,
          4,
          'SWITCH_WINDOW',
          '比赛过程中检测到用户频繁切换到非考试页面，累计 5 次。',
          '2026-04-12 10:16:00'
      ),
      (
          2,
          4,
          5,
          'OPEN_UNAUTHORIZED_SITE',
          '检测到访问未授权网站：example-search.com。',
          '2026-04-12 10:21:00'
      ),
      (
          3,
          4,
          7,
          'COPY_PASTE',
          '检测到粘贴行为，粘贴字符数约 320。',
          '2026-04-12 10:31:00'
      );

-- 可选：重置自增起点，便于后续继续插入
ALTER TABLE oj_user AUTO_INCREMENT = 6;
ALTER TABLE problem AUTO_INCREMENT = 5;
ALTER TABLE contest AUTO_INCREMENT = 3;
ALTER TABLE submission AUTO_INCREMENT = 9;
ALTER TABLE anti_cheat_log AUTO_INCREMENT = 4;

-- 检查数据是否插入成功
SELECT 'oj_user' AS table_name, COUNT(*) AS row_count FROM oj_user
UNION ALL
SELECT 'problem', COUNT(*) FROM problem
UNION ALL
SELECT 'contest', COUNT(*) FROM contest
UNION ALL
SELECT 'contest_problem', COUNT(*) FROM contest_problem
UNION ALL
SELECT 'contest_participant', COUNT(*) FROM contest_participant
UNION ALL
SELECT 'submission', COUNT(*) FROM submission
UNION ALL
SELECT 'anti_cheat_log', COUNT(*) FROM anti_cheat_log;