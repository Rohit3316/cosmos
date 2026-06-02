
-- Add retry_count column (integer, not null, default 0)
ALTER TABLE sp_rollout ADD COLUMN IF NOT EXISTS retry_count INT NOT NULL DEFAULT 0;

-- Add last_retry_mode column (string/varchar, nullable)
ALTER TABLE sp_rollout ADD COLUMN IF NOT EXISTS last_retry_mode VARCHAR(64);