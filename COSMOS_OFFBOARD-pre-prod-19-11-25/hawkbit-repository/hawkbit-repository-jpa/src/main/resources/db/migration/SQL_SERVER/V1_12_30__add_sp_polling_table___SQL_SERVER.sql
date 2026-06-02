-- Create the sequence if it doesn't exist
IF NOT EXISTS (SELECT * FROM sys.sequences WHERE name = 'sp_polling_seq')
BEGIN
    CREATE SEQUENCE sp_polling_seq
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;
END;

-- Create the table
CREATE TABLE sp_polling (
    id BIGINT DEFAULT NEXT VALUE FOR sp_polling_seq PRIMARY KEY,
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    status INT NOT NULL,
    target BIGINT NOT NULL
)

-- Create the index
CREATE INDEX sp_idx_polling_prim_sp_polling
ON sp_polling (tenant, id);

ALTER TABLE sp_polling
ADD CONSTRAINT fk_targ_polling_targ FOREIGN KEY (target)
REFERENCES sp_target (id)
ON DELETE CASCADE;