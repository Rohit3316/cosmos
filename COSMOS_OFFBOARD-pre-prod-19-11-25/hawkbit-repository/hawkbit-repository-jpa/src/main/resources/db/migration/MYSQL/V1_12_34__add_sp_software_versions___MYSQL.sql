-- Create the table
CREATE TABLE sp_software_versions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    description VARCHAR(100),
    number INT NOT NULL,
    created_at BIGINT,
    created_by VARCHAR(64),
    last_updated_at BIGINT,
    last_created_by VARCHAR(64),
    optlock_revision BIGINT,
    software_module_id INT NOT NULL,
    FOREIGN KEY (software_module_id) REFERENCES sp_base_software_module (id) ON DELETE CASCADE ON UPDATE RESTRICT
);