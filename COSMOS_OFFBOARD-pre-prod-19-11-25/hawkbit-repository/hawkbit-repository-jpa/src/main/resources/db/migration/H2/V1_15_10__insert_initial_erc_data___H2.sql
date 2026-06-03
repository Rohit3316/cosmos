-- 1. Insert into sp_erc_structure
INSERT INTO sp_erc_structure (erc_type, erc_format, created_at, created_by)
SELECT 'STLAB', '$UpdatePhase$_$ErrorCode$_$AdditionalInformation$',
       DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'
WHERE NOT EXISTS (
    SELECT 1 FROM sp_erc_structure WHERE erc_type = 'STLAB'
);

-- 2. Insert into sp_erc_update_phase
INSERT INTO sp_erc_update_phase (erc_type, code, device_state_offboard, message_text, created_at, created_by)
VALUES
    ('STLAB', 'N', 'DD_SENT, DD_ACCEPTED', 'Pre-Download Phase', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', 'D', 'DD_ACCEPTED, DOWNLOAD_STARTED, DOWNLOAD_IN_PROGRESS, DOWNLOAD_COMPLETED', 'Download Phase', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', 'P', 'DOWNLOAD_COMPLETED', 'Pre-Install Phase', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', 'I', 'DOWNLOAD_COMPLETED', 'Install Phase', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', 'A', 'DOWNLOAD_COMPLETED', 'Scheduled Install Phase', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin');


-- 3. Insert into sp_erc_code
INSERT INTO sp_erc_code (erc_type, code, device_state_offboard, message_text, created_at, created_by)
VALUES
    ('STLAB', '0001', 'All States', 'Power Cut event', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0002', 'All States', 'Unexpected Reset', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0003', 'All States', 'Vehicle Shutdown', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0004', 'All States', 'Loss of Network', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0005', 'DOWNLOAD_COMPLETED', 'Installation Timeout', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0006', 'DOWNLOAD_COMPLETED', 'Installation GSTM Timeout', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0007', 'DOWNLOAD_COMPLETED', 'Rollout Cancel', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0008', 'DD_SENT, DD_ACCEPTED, DOWNLOAD_STARTED, DOWNLOAD_IN_PROGRESS, DOWNLOAD_COMPLETED', 'Rollout Expiration', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0010', 'DOWNLOAD_STARTED, DOWNLOAD_IN_PROGRESS, DOWNLOAD_COMPLETED', 'Package validation failure', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0011', 'DOWNLOAD_COMPLETED', 'Hardware Version mismatch in Baseline Inventory', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0013', 'DOWNLOAD_COMPLETED', 'Hardware Serial Number mismatch in Baseline Inventory', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0014', 'DOWNLOAD_COMPLETED', 'Software mismatch in Baseline Inventory', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0015', 'DOWNLOAD_COMPLETED', 'Software Version mismatch in Baseline Inventory', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0018', 'DOWNLOAD_COMPLETED', 'Invalid Baseline Inventory format', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0019', 'DOWNLOAD_COMPLETED', 'Missing Baseline Inventory file', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0020', 'DOWNLOAD_COMPLETED', 'Installation Aborted due Deactivating DTC failure', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0021', 'DOWNLOAD_COMPLETED', 'Installation Aborted due to DTC check failure', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0022', 'DOWNLOAD_COMPLETED', 'Installation Aborted due Activating DTC failure', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0023', 'DOWNLOAD_COMPLETED', 'Post Installation DTC Check failure', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0024', 'DOWNLOAD_COMPLETED', 'Post Installation Commit failure', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', 'C_01', 'DD_ACCEPTED, DOWNLOAD_STARTED, DOWNLOAD_IN_PROGRESS', 'Download Connection failure in RTCU', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', 'C_02', 'DD_ACCEPTED, DOWNLOAD_STARTED, DOWNLOAD_IN_PROGRESS', 'Download Connection failure in HPC_IVI_R2_MAX', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', 'C_03', 'DD_ACCEPTED, DOWNLOAD_STARTED, DOWNLOAD_IN_PROGRESS', 'Download Connection failure in HPC_MAIN', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0026', 'DOWNLOAD_COMPLETED', 'Inventory collection failure', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0028', 'DD_ACCEPTED, DOWNLOAD_STARTED, DOWNLOAD_IN_PROGRESS', 'Download failure due to Rollout expiration', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0032', 'DOWNLOAD_STARTED, DOWNLOAD_IN_PROGRESS, DOWNLOAD_COMPLETED', 'Package Transfer to Big Target ECU failed', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0033', 'DOWNLOAD_STARTED, DOWNLOAD_IN_PROGRESS, DOWNLOAD_COMPLETED', 'Package Transfer to Big Target ECU timed out', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0036', 'DOWNLOAD_COMPLETED', 'ADA License could not be loaded', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0040', 'DOWNLOAD_COMPLETED', 'Negative Response Code received from one of the ECU during Install', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0041', 'DOWNLOAD_COMPLETED', 'Software Version check failure post installation', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0042', 'DOWNLOAD_COMPLETED', 'Negative Response Code receieved during Rollback', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0043', 'DOWNLOAD_COMPLETED', 'No response receieved for Rollback', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0044', 'DOWNLOAD_COMPLETED', 'Failure to enable Connectivity', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0045', 'DOWNLOAD_COMPLETED', 'Failure to disable Connectivity', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0046', 'DOWNLOAD_COMPLETED', 'EVCU power down timeout failure', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0050', 'DOWNLOAD_COMPLETED', 'Invalid or missing ECU Node Address in Baseline Inventory', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin'),
    ('STLAB', '0051', 'DOWNLOAD_COMPLETED', 'Invalid or missing VIN in Baseline Inventory', DATEDIFF('SECOND', DATE '1970-01-01', CURRENT_TIMESTAMP()), 'admin');
