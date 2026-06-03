ALTER TABLE sp_base_software_module
    ADD COLUMN module_format INTEGER;



    CREATE TABLE sp_software_module_format
        (
           id NUMERIC(19) IDENTITY NOT NULL,
           created_at         BIGINT NOT NULL,
           created_by         VARCHAR(40) NOT NULL,
           last_modified_at   BIGINT NOT NULL,
           last_modified_by   VARCHAR(40) NOT NULL,
           optlock_revision   INTEGER,
           tenant             VARCHAR(40) NOT NULL,
           description        VARCHAR(512),
           name               VARCHAR(64) NOT NULL,
           deleted            BIT default 0 NULL,
           type_key           VARCHAR(64) NOT NULL,

           PRIMARY KEY (id)
        );



-- Inserimento del primo record solo se non esiste già un record con type_key 'qnx'
INSERT INTO sp_software_module_format (created_at, created_by, last_modified_at, last_modified_by, optlock_revision, tenant, description, name, deleted, type_key)
SELECT CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 'admin', 1, 'DEFAULT', 'QNX Format value', 'QNX', false, 'qnx'
WHERE NOT EXISTS (
    SELECT 1 FROM sp_software_module_format WHERE type_key = 'qnx'
);

-- Inserimento del secondo record solo se non esiste già un record con type_key 'android'
INSERT INTO sp_software_module_format (created_at, created_by, last_modified_at, last_modified_by, optlock_revision, tenant, description, name, deleted, type_key)
SELECT CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 'admin', 1, 'DEFAULT', 'Android format value', 'Android', false, 'android'
WHERE NOT EXISTS (
    SELECT 1 FROM sp_software_module_format WHERE type_key = 'android'
);

-- Inserimento del terzo record solo se non esiste già un record con type_key 'Linux'
INSERT INTO sp_software_module_format (created_at, created_by, last_modified_at, last_modified_by, optlock_revision, tenant, description, name, deleted, type_key)
SELECT CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 'admin', 1, 'DEFAULT', 'Linux format value', 'Linux', false, 'linux'
WHERE NOT EXISTS (
    SELECT 1 FROM sp_software_module_format WHERE type_key = 'linux'
);

ALTER TABLE sp_base_software_module
ADD CONSTRAINT fk_module_format
FOREIGN KEY (module_format)
REFERENCES sp_software_module_format (id)
ON UPDATE RESTRICT
ON DELETE RESTRICT;