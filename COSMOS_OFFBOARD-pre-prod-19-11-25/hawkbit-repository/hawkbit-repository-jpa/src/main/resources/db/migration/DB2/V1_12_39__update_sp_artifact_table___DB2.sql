ALTER TABLE sp_artifact DROP COLUMN downgrade;

ALTER TABLE sp_artifact ADD COLUMN file_type VARCHAR(10) default 'DELTA';

ALTER TABLE sp_artifact DROP COLUMN source_version;
ALTER TABLE sp_artifact DROP COLUMN target_version;

ALTER TABLE sp_artifact ADD COLUMN source_version BIGINT;
ALTER TABLE sp_artifact ADD COLUMN target_version BIGINT;