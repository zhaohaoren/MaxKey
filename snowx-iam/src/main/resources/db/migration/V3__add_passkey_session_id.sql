ALTER TABLE mxk_passkey_challenges
    ADD COLUMN IF NOT EXISTS session_id VARCHAR(255);
