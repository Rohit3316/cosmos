UPDATE sp_base_software_module
    SET module_format = 1
    WHERE module_format IS NULL;