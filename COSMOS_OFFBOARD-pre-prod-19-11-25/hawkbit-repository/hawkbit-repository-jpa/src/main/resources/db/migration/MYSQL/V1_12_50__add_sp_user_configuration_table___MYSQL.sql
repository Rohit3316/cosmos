create table sp_user_configuration (
    id bigint not null auto_increment,
    created_at bigint,
    created_by varchar(40),
    last_modified_at bigint,
    last_modified_by varchar(40),
    optlock_revision bigint,
    conf_key varchar(100) not null,
    conf_value varchar(100) not null,
    user_id bigint not null,
    primary key (id)
);

alter table sp_user_configuration
add constraint uk_user_conf_key  unique (user_id, conf_key);

alter table sp_user_configuration
add constraint fk_user_configuration_user
foreign key (user_id)
references sp_user (id);