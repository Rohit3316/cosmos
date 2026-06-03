CREATE TABLE sp_user_tenant (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    optlock_revision BIGINT,
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    PRIMARY KEY (id)
);

ALTER TABLE sp_user_tenant
ADD CONSTRAINT fk_user_tenant_tenant FOREIGN KEY (tenant_id)
REFERENCES public.sp_tenant (id) ON DELETE CASCADE;

ALTER TABLE sp_user_tenant
ADD CONSTRAINT fk_user_tenant_user FOREIGN KEY (user_id)
REFERENCES public.sp_user (id) ON DELETE CASCADE;

alter table sp_user_tenant
   add constraint uk_user_tenant_association  unique (user_id, tenant_id);