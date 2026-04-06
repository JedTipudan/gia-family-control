-- Migration: fix app_controls unique constraint and add HIDDEN/VISIBLE control types
-- Run this on your Railway MySQL database

-- Step 1: Drop the foreign key that's blocking the index drop
ALTER TABLE app_controls DROP FOREIGN KEY fk_ctrl_device;

-- Step 2: Drop the old unique index (one row per package — too restrictive)
ALTER TABLE app_controls DROP INDEX uq_ctrl;

-- Step 3: Add new unique index (one row per device+package+type)
ALTER TABLE app_controls ADD UNIQUE KEY uq_ctrl_type (device_id, package_name, control_type);

-- Step 4: Recreate the foreign key
ALTER TABLE app_controls
  ADD CONSTRAINT fk_ctrl_device
  FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE;

-- Step 5: Add HIDDEN and VISIBLE to control_type enum
ALTER TABLE app_controls
  MODIFY COLUMN control_type ENUM('BLOCKED','ALLOWED','SCHEDULED','HIDDEN','VISIBLE') NOT NULL DEFAULT 'BLOCKED';

-- Step 6: Add missing command types to commands table
ALTER TABLE commands
  MODIFY COLUMN command_type ENUM(
    'LOCK','UNLOCK','BLOCK_APP','UNBLOCK_APP','SOS','EMERGENCY',
    'GRANT_TEMP_ACCESS','REVOKE_TEMP_ACCESS',
    'ENABLE_LAUNCHER','DISABLE_LAUNCHER',
    'SET_PIN','HIDE_APP','UNHIDE_APP'
  ) NOT NULL;
