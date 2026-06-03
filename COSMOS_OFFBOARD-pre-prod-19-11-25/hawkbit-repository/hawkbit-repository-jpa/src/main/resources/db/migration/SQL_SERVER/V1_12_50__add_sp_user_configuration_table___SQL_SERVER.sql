CREATE TABLE sp_user_configuration
(
    id NUMERIC(19) IDENTITY NOT NULL,
    created_at NUMERIC(19) NOT NULL,
    created_by VARCHAR(40) NOT NULL,
    last_modified_at NUMERIC(19) NOT NULL,
    last_modified_by VARCHAR(40) NOT NULL,
    optlock_revision INTEGER NULL,
    conf_key VARCHAR(100) NOT NULL,
    conf_value VARCHAR(100) NOT NULL,
    user_id NUMERIC(19) NOT NULL,
    PRIMARY KEY (id)
);

ALTER TABLE sp_user_configuration
ADD CONSTRAINT uk_user_conf_key UNIQUE (user_id, conf_key);

ALTER TABLE sp_user_configuration
ADD CONSTRAINT fk_user_configuration_user
FOREIGN KEY (user_id)
REFERENCES sp_user (id)
ON DELETE CASCADE;