ALTER TABLE sp_software_versions DROP COLUMN name RESTRICT;
ALTER TABLE sp_software_versions DROP COLUMN number RESTRICT;
ALTER TABLE sp_software_versions ADD COLUMN name VARCHAR(100) NOT NULL;
ALTER TABLE sp_software_versions ADD COLUMN number INTEGER NOT NULL;
ALTER TABLE sp_software_versions ADD CONSTRAINT unique_constraint_name_sid UNIQUE (name, software_module_id);
ALTER TABLE sp_software_versions ADD CONSTRAINT unique_constraint_number_sid UNIQUE (number, software_module_id);