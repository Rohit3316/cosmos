-- 1. Table: sp_erc_structure
CREATE TABLE sp_erc_structure
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    erc_type         character varying(10)  NOT NULL,
    erc_format       character varying(256) NOT NULL,
    created_at       bigint,
    created_by       character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    CONSTRAINT uq_sp_erc_structure_type UNIQUE (erc_type)
);

-- 2. Table: sp_erc_update_phase
CREATE TABLE sp_erc_update_phase
(
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    erc_type             character varying(10)  NOT NULL,
    code                 character varying(10)  NOT NULL,
    device_state_offboard character varying(256),
    message_text         character varying(500),
    created_at           bigint,
    created_by           character varying(64),
    last_modified_at     bigint,
    last_modified_by     character varying(64),
    optlock_revision     bigint,
    CONSTRAINT fk_sp_erc_update_phase_erc_type
        FOREIGN KEY (erc_type)
            REFERENCES sp_erc_structure (erc_type)
            ON DELETE CASCADE,
    CONSTRAINT uq_sp_erc_update_phase_type_code UNIQUE (erc_type, code)
);

-- 3. Table: sp_erc_code
CREATE TABLE sp_erc_code
(
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    erc_type             character varying(10)  NOT NULL,
    code                 character varying(10)  NOT NULL,
    device_state_offboard character varying(256),
    message_text         character varying(500),
    created_at           bigint,
    created_by           character varying(64),
    last_modified_at     bigint,
    last_modified_by     character varying(64),
    optlock_revision     bigint,
    CONSTRAINT fk_sp_erc_code_erc_type
        FOREIGN KEY (erc_type)
            REFERENCES sp_erc_structure (erc_type)
            ON DELETE CASCADE,
    CONSTRAINT uq_sp_erc_code_type_code UNIQUE (erc_type, code)
);
