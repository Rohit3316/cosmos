CREATE TABLE sp_role
(
     id                         BIGINT GENERATED always AS IDENTITY NOT NULL,
     created_at       BIGINT NOT NULL,
     created_by       VARCHAR(40) NOT NULL,
     last_modified_at BIGINT NOT NULL,
     last_modified_by VARCHAR(40) NOT NULL,
     optlock_revision INTEGER,
     name                       VARCHAR(100) NOT NULL,
     description                VARCHAR(512) NOT NULL,
     active                     VARCHAR(10) DEFAULT "TRUE",
     PRIMARY KEY (id)
);

CREATE TABLE sp_permission
(
    id                          BIGINT GENERATED always AS IDENTITY NOT NULL,
    created_at       BIGINT NOT NULL,
    created_by       VARCHAR(40) NOT NULL,
    last_modified_at BIGINT NOT NULL,
    last_modified_by VARCHAR(40) NOT NULL,
    optlock_revision INTEGER,
    name                        VARCHAR(100) NOT NULL,
    description                 VARCHAR(512) NOT NULL,
    active                      VARCHAR(10) DEFAULT "TRUE",
    PRIMARY KEY (id)
);

CREATE TABLE sp_role_permission
(
    role_id                     BIGINT NOT NULL,
    permission_id               BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id)
);

ALTER TABLE sp_role_permission
ADD CONSTRAINT fk_role_permission_role
FOREIGN KEY (role_id)
REFERENCES sp_role (id) ON DELETE CASCADE;

ALTER TABLE sp_role_permission
ADD CONSTRAINT fk_role_permission_permission
FOREIGN KEY (permission_id)
REFERENCES sp_permission (id) ON DELETE CASCADE;

ALTER TABLE sp_user DROP COLUMN password;
ALTER TABLE sp_user ADD COLUMN email varchar(100) not null;
ALTER TABLE sp_user ADD COLUMN active varchar(10);

CREATE TABLE sp_user_role
(
    user_id                     BIGINT NOT NULL,
    role_id                     BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

ALTER TABLE sp_user_role
ADD CONSTRAINT fk_user_role_user
FOREIGN KEY (user_id)
REFERENCES sp_user (id) ON DELETE CASCADE;

ALTER TABLE sp_user_role
ADD CONSTRAINT fk_user_role_role
FOREIGN KEY (role_id)
REFERENCES sp_role (id) ON DELETE CASCADE;