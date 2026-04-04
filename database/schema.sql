-- ============================================================
-- GIA FAMILY CONTROL — DATABASE SCHEMA
-- MySQL 8.0+
-- ============================================================

CREATE DATABASE IF NOT EXISTS railway CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE railway;

-- USERS
CREATE TABLE users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    full_name   VARCHAR(100) NOT NULL,
    role        ENUM('PARENT','CHILD') NOT NULL,
    pair_code   VARCHAR(10) UNIQUE,
    parent_id   BIGINT NULL,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_parent FOREIGN KEY (parent_id) REFERENCES users(id) ON DELETE SET NULL
);

-- DEVICES
CREATE TABLE devices (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL UNIQUE,
    device_name     VARCHAR(100),
    device_model    VARCHAR(100),
    android_version VARCHAR(20),
    fcm_token       TEXT,
    battery_level   INT DEFAULT 0,
    is_charging     BOOLEAN DEFAULT FALSE,
    is_wifi_connected BOOLEAN DEFAULT FALSE,
    is_online       BOOLEAN DEFAULT FALSE,
    is_locked       BOOLEAN DEFAULT FALSE,
    last_seen       DATETIME,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_device_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- LOCATIONS
CREATE TABLE locations (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id   BIGINT NOT NULL,
    latitude    DECIMAL(10, 8) NOT NULL,
    longitude   DECIMAL(11, 8) NOT NULL,
    accuracy    FLOAT,
    speed       FLOAT,
    recorded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_device_time (device_id, recorded_at),
    CONSTRAINT fk_loc_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

-- COMMANDS
CREATE TABLE commands (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id        BIGINT NOT NULL,
    target_device_id BIGINT NOT NULL,
    command_type     ENUM('LOCK','UNLOCK','BLOCK_APP','UNBLOCK_APP','SOS','EMERGENCY') NOT NULL,
    payload          JSON,
    status           ENUM('PENDING','DELIVERED','EXECUTED','FAILED') DEFAULT 'PENDING',
    sent_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    executed_at      DATETIME NULL,
    CONSTRAINT fk_cmd_sender FOREIGN KEY (sender_id) REFERENCES users(id),
    CONSTRAINT fk_cmd_device FOREIGN KEY (target_device_id) REFERENCES devices(id)
);

-- INSTALLED APPS
CREATE TABLE apps (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id    BIGINT NOT NULL,
    package_name VARCHAR(255) NOT NULL,
    app_name     VARCHAR(255),
    app_icon     MEDIUMTEXT,
    is_system    BOOLEAN DEFAULT FALSE,
    synced_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_device_pkg (device_id, package_name),
    CONSTRAINT fk_app_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

-- APP CONTROLS
CREATE TABLE app_controls (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id       BIGINT NOT NULL,
    device_id       BIGINT NOT NULL,
    package_name    VARCHAR(255) NOT NULL,
    control_type    ENUM('BLOCKED','ALLOWED','SCHEDULED') NOT NULL DEFAULT 'BLOCKED',
    schedule_start  TIME NULL,
    schedule_end    TIME NULL,
    schedule_days   VARCHAR(20) NULL COMMENT 'e.g. MON,TUE,WED',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_ctrl (device_id, package_name),
    CONSTRAINT fk_ctrl_parent FOREIGN KEY (parent_id) REFERENCES users(id),
    CONSTRAINT fk_ctrl_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

-- GEOFENCES
CREATE TABLE geofences (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id   BIGINT NOT NULL,
    device_id   BIGINT NOT NULL,
    name        VARCHAR(100) NOT NULL,
    latitude    DECIMAL(10, 8) NOT NULL,
    longitude   DECIMAL(11, 8) NOT NULL,
    radius_m    INT NOT NULL DEFAULT 200,
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_geo_parent FOREIGN KEY (parent_id) REFERENCES users(id),
    CONSTRAINT fk_geo_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);
