-- Migration: Add connection_type column to devices table
-- Run this on existing databases

USE railway;

ALTER TABLE devices 
ADD COLUMN connection_type VARCHAR(20) DEFAULT 'OFFLINE' COMMENT 'WIFI, MOBILE_DATA, OFFLINE'
AFTER is_online;
