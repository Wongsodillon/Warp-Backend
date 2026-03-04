INSERT INTO users (clerk_user_id, role, created_date)
VALUES
    ('dev-user-1', 'USER', NOW()),
    ('dev-admin-1', 'ADMIN', NOW())
    ON CONFLICT (clerk_user_id)
DO UPDATE SET role = EXCLUDED.role;

INSERT INTO urls (
    short_url,
    destination_url,
    user_id,
    password,
    is_protected,
    disabled,
    created_date,
    expiry_date
)
SELECT
    'dev' || lpad(gs::text, 4, '0') AS short_url,
    'https://example.com/page/' || gs AS destination_url,
    u.id,
    NULL,
    FALSE,
    FALSE,
    NOW() - (gs || ' minutes')::interval,
    NULL
FROM generate_series(1, 50) gs
JOIN users u ON u.clerk_user_id = 'dev-user-1'
ON CONFLICT (short_url) DO NOTHING;