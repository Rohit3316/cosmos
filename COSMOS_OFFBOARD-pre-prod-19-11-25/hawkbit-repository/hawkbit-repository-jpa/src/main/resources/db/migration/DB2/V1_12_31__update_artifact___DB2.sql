ALTER TABLE sp_artifact ADD COLUMN source_version VARCHAR(128);
ALTER TABLE sp_artifact ADD COLUMN target_version VARCHAR(128);
ALTER TABLE sp_artifact ADD COLUMN downgrade SMALLINT DEFAULT 0;
