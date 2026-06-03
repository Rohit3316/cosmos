CREATE TABLE sp_target_software (
    target_id BIGINT NOT NULL,
    node VARCHAR(128) NOT NULL,
    component_id VARCHAR(128) NOT NULL,
    version VARCHAR(128) NOT NULL
);

ALTER TABLE sp_target_software
ADD CONSTRAINT pk_sp_target_software PRIMARY KEY (target_id, node, component_id);

ALTER TABLE sp_target_software
ADD CONSTRAINT fk_software_target FOREIGN KEY (target_id)
REFERENCES sp_target (id)
ON DELETE CASCADE;
