CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    qq_id BIGINT NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    onebot_message_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_chat_message_session_created_at (session_id, created_at)
);

CREATE TABLE IF NOT EXISTS long_term_memory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    qq_id VARCHAR(64) NOT NULL,
    memory_key VARCHAR(128),
    content TEXT NOT NULL,
    source_message_id BIGINT,
    importance INT DEFAULT 5,
    enabled TINYINT DEFAULT 1,
    qdrant_point_id VARCHAR(128),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_qq_enabled (qq_id, enabled)
);

CREATE TABLE IF NOT EXISTS proactive_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    qq_id VARCHAR(64) NOT NULL,
    task_type VARCHAR(32) NOT NULL,
    title VARCHAR(128),
    content TEXT NOT NULL,
    scheduled_at DATETIME NOT NULL,
    next_run_at DATETIME,
    last_run_at DATETIME,
    status VARCHAR(32) DEFAULT 'PENDING',
    enabled TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_qq_status_time (qq_id, status, next_run_at),
    INDEX idx_enabled_time (enabled, next_run_at)
);

CREATE TABLE IF NOT EXISTS chatpush_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    qq_id VARCHAR(64) NOT NULL UNIQUE,
    enabled TINYINT DEFAULT 0,
    min_idle_minutes INT DEFAULT 360,
    cooldown_minutes INT DEFAULT 360,
    quiet_enabled TINYINT DEFAULT 1,
    quiet_start VARCHAR(16) DEFAULT '23:30',
    quiet_end VARCHAR(16) DEFAULT '08:30',
    max_per_day INT DEFAULT 2,
    max_per_day_enabled TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
