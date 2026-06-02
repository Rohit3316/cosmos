-- 1. sp_erc_structure
ALTER TABLE sp_erc_structure DROP COLUMN IF EXISTS updated_at;
ALTER TABLE sp_erc_structure DROP COLUMN IF EXISTS updated_by;

ALTER TABLE sp_erc_structure ADD COLUMN IF NOT EXISTS last_modified_at BIGINT;
ALTER TABLE sp_erc_structure ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(64);

-- 2. sp_erc_update_phase
ALTER TABLE sp_erc_update_phase DROP COLUMN IF EXISTS updated_at;
ALTER TABLE sp_erc_update_phase DROP COLUMN IF EXISTS updated_by;

ALTER TABLE sp_erc_update_phase ADD COLUMN IF NOT EXISTS last_modified_at BIGINT;
ALTER TABLE sp_erc_update_phase ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(64);

-- 3. sp_erc_code
ALTER TABLE sp_erc_code DROP COLUMN IF EXISTS updated_at;
ALTER TABLE sp_erc_code DROP COLUMN IF EXISTS updated_by;

ALTER TABLE sp_erc_code ADD COLUMN IF NOT EXISTS last_modified_at BIGINT;
ALTER TABLE sp_erc_code ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(64);
