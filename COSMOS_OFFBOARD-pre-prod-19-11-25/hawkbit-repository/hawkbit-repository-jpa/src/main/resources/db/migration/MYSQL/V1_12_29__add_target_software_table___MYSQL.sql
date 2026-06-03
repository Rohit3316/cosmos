CREATE TABLE sp_target_software (
    target_id BIGINT NOT NULL,
    node VARCHAR(128) NOT NULL,
    component_id VARCHAR(128) NOT NULL,
    version VARCHAR(128) NOT NULL,

    PRIMARY KEY (target_id, node, component_id),
    FOREIGN KEY (target_id)
    REFERENCES sp_target (id)
    ON DELETE CASCADE
);
