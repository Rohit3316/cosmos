ALTER TABLE public.sp_vehicle_model
ADD COLUMN erc_type VARCHAR(10),
ADD CONSTRAINT fk_sp_vehicle_model_erc_type
    FOREIGN KEY (erc_type)
    REFERENCES public.sp_erc_structure(erc_type)
    ON UPDATE RESTRICT;