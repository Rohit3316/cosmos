-- Create the sequence if it doesn't exist (Note: MySQL does not have built-in sequences like PostgreSQL)
-- You can use an auto-incremented column instead
-- For example, you can make the 'id' column auto-incremented:

CREATE TABLE IF NOT EXISTS sp_polling_seq (
    id INT AUTO_INCREMENT PRIMARY KEY
);

CREATE TABLE sp_polling (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    status INT NOT NULL,
    target BIGINT NOT NULL
);

-- Create the index
CREATE INDEX sp_idx_polling_prim_sp_polling
ON sp_polling (tenant, id);

ALTER TABLE sp_polling
ADD PRIMARY KEY (id);

ALTER TABLE sp_polling
ADD CONSTRAINT fk_targ_polling_targ FOREIGN KEY (target)
REFERENCES sp_target (id)
ON UPDATE RESTRICT
ON DELETE CASCADE;