-- Create the sequence if it doesn't exist
CREATE OR REPLACE SEQUENCE _seq
  AS BIGINT
  START WITH 1
  INCREMENT BY 1
  NO MAXVALUE
  NO CYCLE
  CACHE 20;

-- Create the table
CREATE TABLE sp_polling (
    id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    status INTEGER NOT NULL,
    target BIGINT NOT NULL,
    PRIMARY KEY (id)
);
-- Create the index
CREATE INDEX sp_idx_polling_prim_sp_polling
ON sp_polling (tenant, id);

ALTER TABLE sp_polling
ADD CONSTRAINT fk_targ_polling_targ FOREIGN KEY (target)
REFERENCES sp_target (id)
ON DELETE CASCADE;