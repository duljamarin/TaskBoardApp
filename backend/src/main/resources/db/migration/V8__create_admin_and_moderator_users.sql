-- V8__create_admin_and_moderator_users.sql
-- Ensure a usable admin and moderator exist with known credentials.
--
-- Credentials:
--   admin      / admin123        (ROLE_ADMIN + ROLE_USER)
--   moderator  / moderator123    (ROLE_MODERATOR + ROLE_USER)

-- 1)

INSERT INTO users (username, email, password, full_name, active)
VALUES (
           'admin',
           'admin@taskboard.com',
           '$2a$10$PXSTiXrfg.6ZPbujJinCvOcdH0aQfLhBK3W6leBx95mUG2NjB6atK',
           'Admin User',
           true
       ) ON CONFLICT (username) DO UPDATE
    SET password = EXCLUDED.password;


-- 2) Insert a moderator user (skip if already exists).
--    BCrypt hash of "moderator123"
INSERT INTO users (username, email, password, full_name, active)
VALUES (
    'moderator',
    'moderator@taskboard.com',
    '$2a$10$vCss9bHecVp.zxTDRmycCu.7nkEDTRui6klLoq4GZIfkDLJucpZ0G',
    'Moderator User',
    true
)
ON CONFLICT (username) DO UPDATE
    SET password = EXCLUDED.password;

-- 3) Make sure the admin user has both ROLE_ADMIN and ROLE_USER.
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_USER'
ON CONFLICT DO NOTHING;

-- 4) Assign ROLE_MODERATOR and ROLE_USER to the moderator user.
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'moderator' AND r.name = 'ROLE_MODERATOR'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'moderator' AND r.name = 'ROLE_USER'
ON CONFLICT DO NOTHING;

