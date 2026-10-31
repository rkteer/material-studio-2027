CREATE TABLE app_user (
    id VARCHAR(36) PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    avatar_color CHAR(7) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE template (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    category VARCHAR(60) NOT NULL,
    cover_url VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    required_image_count INT NOT NULL,
    accepted_mime_types VARCHAR(255) NOT NULL,
    max_file_size_bytes BIGINT NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE asset (
    id CHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    storage_name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_asset_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    INDEX idx_asset_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE creation_task (
    id CHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    template_id VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    stage VARCHAR(20) NULL,
    failed_stage VARCHAR(20) NULL,
    failure_reason VARCHAR(1000) NULL,
    result_storage_name VARCHAR(255) NULL,
    mock_delay_ms BIGINT NOT NULL,
    mock_fail_stage VARCHAR(20) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_task_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT fk_task_template FOREIGN KEY (template_id) REFERENCES template(id),
    INDEX idx_task_user_created (user_id, created_at),
    INDEX idx_task_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE task_asset (
    task_id CHAR(36) NOT NULL,
    asset_id CHAR(36) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (task_id, asset_id),
    CONSTRAINT fk_task_asset_task FOREIGN KEY (task_id) REFERENCES creation_task(id),
    CONSTRAINT fk_task_asset_asset FOREIGN KEY (asset_id) REFERENCES asset(id),
    INDEX idx_task_asset_asset (asset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
