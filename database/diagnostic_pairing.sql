-- ============================================================
-- PAIRING DIAGNOSTIC SCRIPT
-- Run this to check if pairing worked in the database
-- ============================================================

USE railway;

-- 1. Check all users
SELECT 
    id, 
    email, 
    full_name, 
    role, 
    pair_code, 
    parent_id,
    created_at
FROM users
ORDER BY created_at DESC;

-- 2. Check all devices
SELECT 
    d.id as device_id,
    d.user_id,
    u.email as user_email,
    u.role as user_role,
    d.device_name,
    d.device_model,
    d.fcm_token IS NOT NULL as has_fcm_token,
    d.is_online,
    d.last_seen
FROM devices d
LEFT JOIN users u ON d.user_id = u.id
ORDER BY d.id DESC;

-- 3. Check parent-child relationships
SELECT 
    p.id as parent_id,
    p.email as parent_email,
    p.pair_code as parent_pair_code,
    c.id as child_id,
    c.email as child_email,
    c.parent_id as child_parent_id,
    d.id as child_device_id,
    d.device_name as child_device_name,
    d.fcm_token IS NOT NULL as child_has_fcm_token
FROM users p
LEFT JOIN users c ON c.parent_id = p.id
LEFT JOIN devices d ON d.user_id = c.id
WHERE p.role = 'PARENT'
ORDER BY p.id DESC;

-- 4. Check if parent has device (CRITICAL!)
SELECT 
    u.id as user_id,
    u.email,
    u.role,
    d.id as device_id,
    d.device_name,
    d.fcm_token IS NOT NULL as has_fcm_token
FROM users u
LEFT JOIN devices d ON d.user_id = u.id
WHERE u.role = 'PARENT'
ORDER BY u.id DESC;

-- 5. Find orphaned children (children without parent device)
SELECT 
    c.id as child_id,
    c.email as child_email,
    c.parent_id,
    p.email as parent_email,
    pd.id as parent_device_id,
    cd.id as child_device_id
FROM users c
LEFT JOIN users p ON c.parent_id = p.id
LEFT JOIN devices pd ON pd.user_id = p.id
LEFT JOIN devices cd ON cd.user_id = c.id
WHERE c.role = 'CHILD' AND c.parent_id IS NOT NULL
ORDER BY c.id DESC;

-- ============================================================
-- MANUAL FIX SCRIPTS (if needed)
-- ============================================================

-- Fix 1: Create missing parent device
-- Replace <PARENT_USER_ID> with actual parent user ID
/*
INSERT INTO devices (user_id, device_name, device_model, is_online, last_seen)
VALUES (<PARENT_USER_ID>, 'Parent Device', 'Web', TRUE, NOW());
*/

-- Fix 2: Update parent device FCM token
-- Replace <PARENT_USER_ID> and <FCM_TOKEN> with actual values
/*
UPDATE devices 
SET fcm_token = '<FCM_TOKEN>', 
    is_online = TRUE, 
    last_seen = NOW()
WHERE user_id = <PARENT_USER_ID>;
*/

-- Fix 3: Verify child is paired with parent
-- Replace <CHILD_EMAIL> and <PARENT_ID> with actual values
/*
UPDATE users 
SET parent_id = <PARENT_ID>
WHERE email = '<CHILD_EMAIL>';
*/

-- Fix 4: Check if pairing worked
-- Replace <PARENT_EMAIL> with actual parent email
/*
SELECT 
    p.id as parent_id,
    p.email as parent_email,
    COUNT(c.id) as children_count,
    GROUP_CONCAT(c.email) as children_emails
FROM users p
LEFT JOIN users c ON c.parent_id = p.id
WHERE p.email = '<PARENT_EMAIL>'
GROUP BY p.id, p.email;
*/
