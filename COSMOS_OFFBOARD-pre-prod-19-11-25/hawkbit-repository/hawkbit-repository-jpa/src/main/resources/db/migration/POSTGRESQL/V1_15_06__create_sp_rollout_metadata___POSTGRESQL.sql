CREATE TABLE IF NOT EXISTS sp_rollout_metadata
(
    id
    BIGINT
    GENERATED
    BY
    DEFAULT AS
    IDENTITY
    PRIMARY
    KEY,
    rollout_id
    BIGINT
    NOT
    NULL,
    tenant
    character
    varying
(
    40
) NOT NULL,
    created_at bigint,
    created_by character varying
(
    64
),
    last_modified_at bigint,
    last_modified_by character varying
(
    64
),
    optlock_revision bigint,
    metadata_key VARCHAR
(
    255
) NOT NULL,
    metadata_value VARCHAR
(
    255
) NOT NULL,
    CONSTRAINT fk_rollout
    FOREIGN KEY
(
    rollout_id
)
    REFERENCES sp_rollout
(
    id
)
    ON DELETE CASCADE
    );
