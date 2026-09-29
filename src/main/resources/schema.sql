-- ============================================================
--  新媒体小组管理系统 · SQLite 建表脚本（幂等，可重复执行）
-- ============================================================

-- 用户表：账号只能由管理员创建
CREATE TABLE IF NOT EXISTS sys_user (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    username       TEXT    NOT NULL UNIQUE,
    nickname       TEXT    NOT NULL,
    password_hash  TEXT    NOT NULL,
    role           TEXT    NOT NULL DEFAULT 'EMPLOYEE',   -- ADMIN / EMPLOYEE
    email          TEXT,
    group_id       INTEGER,
    must_change    INTEGER NOT NULL DEFAULT 1,            -- 1=登录后必须先改密码
    status         TEXT    NOT NULL DEFAULT 'ACTIVE',     -- ACTIVE / DISABLED
    last_login_at  TEXT,
    created_at     TEXT    NOT NULL,
    updated_at     TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_user_group ON sys_user(group_id);
CREATE INDEX IF NOT EXISTS idx_user_role  ON sys_user(role);

-- 小组表：组长可以是管理员，也可以是员工
CREATE TABLE IF NOT EXISTS sys_group (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL UNIQUE,
    leader_id   INTEGER,
    description TEXT,
    created_at  TEXT NOT NULL
);

-- 任务表
CREATE TABLE IF NOT EXISTS task (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    title          TEXT    NOT NULL,
    content        TEXT,
    creator_id     INTEGER NOT NULL,
    claim_deadline TEXT    NOT NULL,          -- 接取时限，超时自动分配
    ddl            TEXT    NOT NULL,          -- 完成截止时间
    claim_score    INTEGER NOT NULL DEFAULT 1,
    complete_score INTEGER NOT NULL DEFAULT 1,
    review_score   INTEGER NOT NULL DEFAULT 1,
    assignee_id    INTEGER,
    assign_type    TEXT,                      -- CLAIM(抢单) / AUTO(超时自动) / MANUAL(管理员指派)
    status         TEXT    NOT NULL DEFAULT 'OPEN',  -- OPEN / IN_PROGRESS / PENDING_REVIEW / COMPLETED / CANCELLED
    claimed_at     TEXT,
    submitted_at   TEXT,
    preview_url    TEXT,                      -- 秀米预览链接
    submit_note    TEXT,
    attach_name    TEXT,                      -- 任务压缩包原始文件名
    attach_path    TEXT,                      -- 服务器上的存储相对路径（任务归档后删除）
    attach_size    INTEGER,                   -- 压缩包大小（字节）
    reviewer_id    INTEGER,                   -- 轮询分配到的审核管理员
    review_comment TEXT,
    reviewed_at    TEXT,
    created_at     TEXT    NOT NULL,
    updated_at     TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_task_status   ON task(status);
CREATE INDEX IF NOT EXISTS idx_task_assignee ON task(assignee_id);

-- 任务流转日志（详情页时间线）
CREATE TABLE IF NOT EXISTS task_log (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id    INTEGER NOT NULL,
    actor_id   INTEGER,
    action     TEXT    NOT NULL,
    detail     TEXT,
    created_at TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_tasklog_task ON task_log(task_id);

-- 积分流水
CREATE TABLE IF NOT EXISTS score_record (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL,
    task_id     INTEGER,
    type        TEXT    NOT NULL,             -- CLAIM / COMPLETE / REVIEW / MANUAL
    score       INTEGER NOT NULL,
    remark      TEXT,
    operator_id INTEGER,
    created_at  TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_score_user ON score_record(user_id);
CREATE INDEX IF NOT EXISTS idx_score_time ON score_record(created_at);

-- 站内消息
CREATE TABLE IF NOT EXISTS notification (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER NOT NULL,
    type       TEXT    NOT NULL,
    title      TEXT    NOT NULL,
    content    TEXT,
    task_id    INTEGER,
    is_read    INTEGER NOT NULL DEFAULT 0,
    created_at TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_notice_user ON notification(user_id, is_read);

-- 系统键值配置
CREATE TABLE IF NOT EXISTS sys_meta (
    k TEXT PRIMARY KEY,
    v TEXT
);
