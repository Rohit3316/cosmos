CREATE TABLE sp_artifact_audit
(
    id NUMERIC(19) IDENTITY NOT NULL,
    tenant VARCHAR(40) NOT NULL,
    created_at NUMERIC(19) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    provided_file_name VARCHAR(256) NULL,
    last_modified_at NUMERIC(19) NOT NULL,
    last_modified_by VARCHAR(64) NOT NULL,
    deleted_at NUMERIC(19) NULL,
    deleted_by VARCHAR(64) NULL,
    md5_hash VARCHAR(32) NULL,
    optlock_revision INTEGER NULL,
    sha1_hash VARCHAR(40) NOT NULL,
    sha256_hash VARCHAR(64) NULL,
    version VARCHAR(64) NOT NULL,
    source_version  VARCHAR(128) NULL,
    target_version  VARCHAR(128) NULL,
    downgrade BIT DEFAULT 0 NULL,
    file_size NUMERIC(19) NULL,
    software_module NUMERIC(19) NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX sp_idx_artifact_audit_01 ON sp_artifact_audit (tenant, software_module);
CREATE INDEX sp_idx_artifact_audit_02 ON sp_artifact_audit (tenant, sha1_hash);
CREATE INDEX sp_idx_artifact_audit_prim ON sp_artifact_audit (tenant, id);

ALTER TABLE sp_artifact
    ADD COLUMN version VARCHAR(64) NOT NULL DEFAULT '1';