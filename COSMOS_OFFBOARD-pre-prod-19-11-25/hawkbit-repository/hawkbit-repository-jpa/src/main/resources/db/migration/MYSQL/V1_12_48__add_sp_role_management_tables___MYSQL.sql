create table sp_role (
    id bigint not null auto_increment,
    created_at bigint,
    created_by varchar(40),
    last_modified_at bigint,
    last_modified_by varchar(40),
    optlock_revision bigint,
    name varchar(100) not null,
    description varchar(512) not null,
    active varchar(10) DEFAULT "TRUE",
    primary key (id)
);

create table sp_permission (
    id bigint not null auto_increment,
    created_at bigint,
    created_by varchar(40),
    last_modified_at bigint,
    last_modified_by varchar(40),
    optlock_revision bigint,
    name varchar(100) not null,
    description varchar(512) not null,
    active varchar(10) DEFAULT "TRUE",
    primary key (id)
);

create table sp_role_permission (
    role_id bigint not null,
    permission_id bigint not null,
    primary key (role_id, permission_id)
);

alter table sp_role_permission
    add constraint fk_role_permission_role
    foreign key (role_id)
    references sp_role (id);

alter table sp_role_permission
    add constraint fk_role_permission_permission
    foreign key (permission_id)
    references sp_permission (id);

ALTER TABLE sp_user DROP COLUMN password;
ALTER TABLE sp_user ADD COLUMN email varchar(100) not null;
ALTER TABLE sp_user ADD COLUMN active varchar(10);

create table sp_user_role (
    user_id bigint not null,
    role_id bigint not null,
    primary key (user_id, role_id)
);

alter table sp_user_role
    add constraint fk_user_role_user
    foreign key (user_id)
    references sp_user (id);

alter table sp_user_role
    add constraint fk_user_role_role
    foreign key (role_id)
    references sp_role (id);