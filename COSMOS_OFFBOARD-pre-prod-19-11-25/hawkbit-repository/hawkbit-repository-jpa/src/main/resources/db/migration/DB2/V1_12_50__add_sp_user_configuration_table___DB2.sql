CREATE TABLE sp_user_configuration
(
     id                         BIGINT GENERATED always AS IDENTITY NOT NULL,
     created_at       BIGINT NOT NULL,
     created_by       VARCHAR(40) NOT NULL,
     last_modified_at BIGINT NOT NULL,
     last_modified_by VARCHAR(40) NOT NULL,
     optlock_revision INTEGER,
     conf_key                       VARCHAR(100) NOT NULL,
     conf_value                VARCHAR(100) NOT NULL,
     user_id                     BIGINT NOT NULL,
     PRIMARY KEY (id)
);

ALTER TABLE sp_user_configuration
ADD CONSTRAINT uk_user_conf_key UNIQUE (user_id, conf_key);

ALTER TABLE sp_user_configuration
ADD CONSTRAINT fk_user_configuration_user
FOREIGN KEY (user_id)
REFERENCES sp_user (id) ON DELETE CASCADE;