-- Reset all devices to offline state
-- Run this to fix stuck "online" devices

USE railway;

-- Set all devices to offline
UPDATE devices 
SET is_online = FALSE, 
    connection_type = 'OFFLINE'
WHERE is_online = TRUE;

-- Show results
SELECT id, user_id, device_name, is_online, connection_type, last_seen 
FROM devices;
