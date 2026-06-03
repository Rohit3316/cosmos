CREATE TABLE sp_role
(
    id NUMERIC(19) IDENTITY NOT NULL,
    created_at NUMERIC(19) NOT NULL,
    created_by VARCHAR(40) NOT NULL,
    last_modified_at NUMERIC(19) NOT NULL,
    last_modified_by VARCHAR(40) NOT NULL,
    optlock_revision INTEGER NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(512) NOT NULL,
    active VARCHAR(10) default "TRUE" NULL,
    PRIMARY KEY (id)
);

CREATE TABLE sp_permission
(
    id NUMERIC(19) IDENTITY NOT NULL,
    created_at NUMERIC(19) NOT NULL,
    created_by VARCHAR(40) NOT NULL,
    last_modified_at NUMERIC(19) NOT NULL,
    last_modified_by VARCHAR(40) NOT NULL,
    optlock_revision INTEGER NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(512) NOT NULL,
    active VARCHAR(10) default "TRUE" NULL,
    PRIMARY KEY (id)
);

CREATE TABLE sp_role_permission
(
    role_id NUMERIC(19) NOT NULL,
    permission_id NUMERIC(19) NOT NULL,
    PRIMARY KEY (role_id, permission_id)
);

ALTER TABLE sp_role_permission
ADD CONSTRAINT fk_role_permission_role
FOREIGN KEY (role_id)
REFERENCES sp_role (id)
ON DELETE CASCADE;

ALTER TABLE sp_role_permission
ADD CONSTRAINT fk_role_permission_permission
FOREIGN KEY (permission_id)
REFERENCES sp_permission (id)
ON DELETE CASCADE;

ALTER TABLE sp_user DROP COLUMN password;
ALTER TABLE sp_user ADD COLUMN email VARCHAR(100) NOT NULL;
ALTER TABLE sp_user ADD COLUMN active VARCHAR(10);

CREATE TABLE sp_user_role
(
    user_id NUMERIC(19) NOT NULL,
    role_id NUMERIC(19) NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

ALTER TABLE sp_user_role
ADD CONSTRAINT fk_user_role_user
FOREIGN KEY (user_id)
REFERENCES sp_user (id)
ON DELETE CASCADE;

ALTER TABLE sp_user_role
ADD CONSTRAINT fk_user_role_role
FOREIGN KEY (role_id)
REFERENCES sp_role (id)
ON DELETE CASCADE;