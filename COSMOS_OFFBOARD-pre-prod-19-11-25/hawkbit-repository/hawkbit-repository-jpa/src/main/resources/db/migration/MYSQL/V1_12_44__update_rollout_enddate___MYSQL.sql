ALTER TABLE sp_rollout ADD COLUMN end_at bigint;

CREATE INDEX sp_idx_rollout_end_at ON sp_rollout (end_at);
