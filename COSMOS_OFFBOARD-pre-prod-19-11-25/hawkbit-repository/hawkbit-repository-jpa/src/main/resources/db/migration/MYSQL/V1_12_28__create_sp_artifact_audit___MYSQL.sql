create table sp_artifact_audit (
    id bigint not null auto_increment,
    created_at bigint,
    created_by varchar(64),
    last_modified_at bigint,
    last_modified_by varchar(64),
    deleted_at bigint,
    deleted_by varchar(64),
    optlock_revision bigint,
    tenant varchar(40) not null,
    md5_hash varchar(32),
    sha1_hash varchar(40) not null,
    sha256_hash varchar(64),
    version varchar(64) not null,
    source_version  varchar(128),
    target_version  varchar(128),
    downgrade bit DEFAULT false,
    file_size bigint,
    provided_file_name varchar(256),
    gridfs_file_name varchar(40),
    software_module bigint not null,
    primary key (id)
);

ALTER TABLE sp_artifact
    ADD COLUMN version varchar(64) NOT NULL DEFAULT '1';