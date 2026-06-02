-- Add retry_count column (integer)
ALTER TABLE sp_rollout ADD COLUMN IF NOT EXISTS retry_count INTEGER DEFAULT 0;

-- Add last_retry_mode column (string/varchar, nullable)
ALTER TABLE sp_rollout ADD COLUMN IF NOT EXISTS  last_retry_mode VARCHAR(64);
