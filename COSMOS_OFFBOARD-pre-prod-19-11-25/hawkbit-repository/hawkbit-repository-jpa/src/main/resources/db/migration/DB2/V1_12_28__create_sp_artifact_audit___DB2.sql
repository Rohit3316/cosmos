CREATE TABLE sp_artifact_audit
  (
     id                 BIGINT GENERATED always AS IDENTITY NOT NULL,
     tenant             VARCHAR(40) NOT NULL,
     created_at         BIGINT NOT NULL,
     created_by         VARCHAR(64) NOT NULL,
     provided_file_name VARCHAR(256),
     last_modified_at   BIGINT NOT NULL,
     last_modified_by   VARCHAR(64) NOT NULL,
     deleted_at         BIGINT,
     deleted_by         VARCHAR(64),
     md5_hash           VARCHAR(32),
     optlock_revision   BIGINT,
     sha1_hash          VARCHAR(40) NOT NULL,
     sha256_hash        VARCHAR(64),
     version            VARCHAR(64) NOT NULL,
     source_version     VARCHAR(128),
     target_version     VARCHAR(128),
     downgrade          SMALLINT DEFAULT 0,
     file_size          BIGINT,
     software_module    BIGINT NOT NULL,
     PRIMARY KEY (id)
  );

ALTER TABLE sp_artifact
    ADD COLUMN version varchar(64) NOT NULL DEFAULT '1';