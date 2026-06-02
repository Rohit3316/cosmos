-- Step 1: Add new column
ALTER TABLE sp_vehicle_model
ADD COLUMN erc_type VARCHAR(10);

-- Step 2: Add foreign key constraint
ALTER TABLE sp_vehicle_model
ADD CONSTRAINT fk_sp_vehicle_model_erc_type
    FOREIGN KEY (erc_type)
    REFERENCES sp_erc_structure(erc_type)
    ON UPDATE RESTRICT;