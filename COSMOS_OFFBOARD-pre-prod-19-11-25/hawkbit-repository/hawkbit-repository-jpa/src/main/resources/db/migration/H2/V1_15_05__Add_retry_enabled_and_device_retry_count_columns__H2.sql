-- Add retry_count column (integer)
ALTER TABLE sp_rollouttargetgroup ADD COLUMN IF NOT EXISTS retry_enabled BOOLEAN DEFAULT FALSE;

-- Add last_retry_mode column (string/varchar, nullable)
ALTER TABLE sp_rollouttargetgroup ADD COLUMN IF NOT EXISTS device_retry_count INTEGER DEFAULT 0;
