-- Ensure one user per username (fixes duplicate-username login issues)
ALTER TABLE identity.app_users ADD CONSTRAINT uk_app_users_username UNIQUE (username);
