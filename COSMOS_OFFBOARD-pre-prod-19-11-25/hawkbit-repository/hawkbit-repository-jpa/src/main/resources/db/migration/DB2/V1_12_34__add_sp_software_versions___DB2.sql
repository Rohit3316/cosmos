-- Create the sequence
CREATE SEQUENCE sp_software_versions_seq
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO CYCLE
    CACHE 20;

-- Create the table
CREATE TABLE sp_software_versions (
    id BIGINT NOT NULL DEFAULT NEXTVAL FOR sp_software_versions_seq,
    name VARCHAR(100),
    description VARCHAR(100),
    number INTEGER NOT NULL,
    created_at BIGINT,
    created_by VARCHAR(64),
    last_updated_at BIGINT,
    last_created_by VARCHAR(64),
    optlock_revision BIGINT,
    software_module_id INTEGER NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (software_module_id) REFERENCES sp_base_software_module (id) ON DELETE CASCADE ON UPDATE RESTRICT
);