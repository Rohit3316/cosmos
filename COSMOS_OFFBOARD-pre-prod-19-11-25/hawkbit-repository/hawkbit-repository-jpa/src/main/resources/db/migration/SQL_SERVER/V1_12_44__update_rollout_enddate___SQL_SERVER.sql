ALTER TABLE sp_rollout ADD end_at NUMERIC(19) NULL;

CREATE INDEX sp_idx_rollout_end_at ON sp_rollout (end_at);