ALTER TABLE sp_artifact_audit DROP COLUMN downgrade;
ALTER TABLE sp_artifact_audit ADD COLUMN file_type VARCHAR(10) default 'DELTA';
ALTER TABLE sp_artifact_audit DROP COLUMN source_version;
ALTER TABLE sp_artifact_audit DROP COLUMN target_version;
ALTER TABLE sp_artifact_audit ADD COLUMN source_version BIGINT;
ALTER TABLE sp_artifact_audit ADD COLUMN target_version BIGINT;

ALTER TABLE sp_artifact ADD CONSTRAINT fk_version_relation_source FOREIGN KEY (source_version) REFERENCES sp_software_versions (id) ON DELETE SET NULL;
ALTER TABLE sp_artifact ADD CONSTRAINT fk_version_relation_target FOREIGN KEY (target_version) REFERENCES sp_software_versions (id) ON DELETE SET NULL;