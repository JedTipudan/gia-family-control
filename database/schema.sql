-- ============================================================
-- GIA FAMILY CONTROL — PostgreSQL SCHEMA
-- ============================================================

-- USERS
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    full_name   VARCHAR(100) NOT NULL,
    role        VARCHAR(10) NOT NULL CHECK (role IN ('PARENT','CHILD')),
    pair_code   VARCHAR(10) UNIQUE,
    parent_id   BIGINT NULL REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- DEVICES
CREATE TABLE IF NOT EXISTS devices (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    device_name     VARCHAR(100),
    device_model    VARCHAR(100),
    android_version VARCHAR(20),
    fcm_token       TEXT,
    battery_level   INT DEFAULT 0,
    is_online       BOOLEAN DEFAULT FALSE,
    connection_type VARCHAR(20) DEFAULT 'OFFLINE',
    is_locked       BOOLEAN DEFAULT FALSE,
    last_seen       TIMESTAMP,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- LOCATIONS
CREATE TABLE IF NOT EXISTS locations (
    id          BIGSERIAL PRIMARY KEY,
    device_id   BIGINT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    latitude    DECIMAL(10, 8) NOT NULL,
    longitude   DECIMAL(11, 8) NOT NULL,
    accuracy    FLOAT,
    speed       FLOAT,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_device_time ON locations(device_id, recorded_at);

-- COMMANDS
CREATE TABLE IF NOT EXISTS commands (
    id               BIGSERIAL PRIMARY KEY,
    sender_id        BIGINT NOT NULL REFERENCES users(id),
    target_device_id BIGINT NOT NULL REFERENCES devices(id),
    command_type     VARCHAR(30) NOT NULL,
    payload          TEXT,
    status           VARCHAR(10) DEFAULT 'PENDING',
    sent_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    executed_at      TIMESTAMP NULL
);

-- INSTALLED APPS
CREATE TABLE IF NOT EXISTS apps (
    id           BIGSERIAL PRIMARY KEY,
    device_id    BIGINT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    package_name VARCHAR(255) NOT NULL,
    app_name     VARCHAR(255),
    app_icon     TEXT,
    is_system    BOOLEAN DEFAULT FALSE,
    synced_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (device_id, package_name)
);

-- APP CONTROLS
CREATE TABLE IF NOT EXISTS app_controls (
    id              BIGSERIAL PRIMARY KEY,
    parent_id       BIGINT NOT NULL REFERENCES users(id),
    device_id       BIGINT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    package_name    VARCHAR(255) NOT NULL,
    control_type    VARCHAR(10) NOT NULL DEFAULT 'BLOCKED',
    schedule_start  TIME NULL,
    schedule_end    TIME NULL,
    schedule_days   VARCHAR(20) NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (device_id, package_name, control_type)
);

-- GEOFENCES
CREATE TABLE IF NOT EXISTS geofences (
    id          BIGSERIAL PRIMARY KEY,
    parent_id   BIGINT NOT NULL REFERENCES users(id),
    device_id   BIGINT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    latitude    DECIMAL(10, 8) NOT NULL,
    longitude   DECIMAL(11, 8) NOT NULL,
    radius_m    INT NOT NULL DEFAULT 200,
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
