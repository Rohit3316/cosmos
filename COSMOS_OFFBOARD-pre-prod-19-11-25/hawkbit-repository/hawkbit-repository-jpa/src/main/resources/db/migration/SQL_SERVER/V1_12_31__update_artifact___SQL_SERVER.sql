ALTER TABLE sp_artifact ADD source_version VARCHAR(128);
ALTER TABLE sp_artifact ADD target_version VARCHAR(128);
ALTER TABLE sp_artifact ADD downgrade BIT DEFAULT 0;
