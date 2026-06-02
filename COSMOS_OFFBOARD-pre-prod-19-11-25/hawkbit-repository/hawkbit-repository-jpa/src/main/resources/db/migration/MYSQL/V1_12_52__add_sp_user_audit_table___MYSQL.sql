create table sp_user_audit (
    id bigint not null auto_increment,
    created_at bigint,
    created_by varchar(40),
    last_modified_at bigint,
    last_modified_by varchar(40),
    optlock_revision bigint,
    deleted_at bigint,
    user_id bigint not null,
    role_id bigint not null,
    tenant_id bigint not null,
    primary key (id)
);

alter table sp_user_audit
add constraint uk_user_role_tenant unique (user_id, role_id, tenant_id);

alter table sp_user_audit
    add constraint fk_user_audit_user
    foreign key (user_id)
    references sp_user (id);

alter table sp_user_audit
    add constraint fk_user_audit_role
    foreign key (role_id)
    references sp_role (id);

alter table sp_user_audit
    add constraint fk_user_audit_tenant
    foreign key (tenant_id)
    references sp_tenant (id);