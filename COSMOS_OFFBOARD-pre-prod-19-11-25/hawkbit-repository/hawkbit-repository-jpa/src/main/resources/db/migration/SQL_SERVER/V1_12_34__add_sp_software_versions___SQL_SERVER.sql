-- Create the sequence
CREATE SEQUENCE sp_software_versions_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO CYCLE;

-- Create the table
CREATE TABLE sp_software_versions (
    id BIGINT NOT NULL DEFAULT NEXT VALUE FOR sp_software_versions_seq,
    name NVARCHAR(100),
    description NVARCHAR(100),
    number INT NOT NULL,
    created_at BIGINT,
    created_by NVARCHAR(64),
    last_updated_at BIGINT,
    last_created_by NVARCHAR(64),
    optlock_revision BIGINT,
    software_module_id INT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (software_module_id) REFERENCES sp_base_software_module (id) ON DELETE CASCADE ON UPDATE RESTRICT
);