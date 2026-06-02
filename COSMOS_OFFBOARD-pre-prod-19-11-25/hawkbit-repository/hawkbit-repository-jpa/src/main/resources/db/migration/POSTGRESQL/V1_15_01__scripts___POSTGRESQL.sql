
CREATE SCHEMA IF NOT EXISTS public;

--
-- TOC entry 215 (class 1259 OID 131209)
-- Name: sp_action_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_action_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 230 (class 1259 OID 131224)
-- Name: sp_action; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_action (
                                                id bigint DEFAULT nextval('public.sp_action_seq'::regclass) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    tenant character varying(40) NOT NULL,
    active boolean,
    forced_time bigint,
    status integer NOT NULL,
    distribution_set bigint NOT NULL,
    target bigint NOT NULL,
    rollout bigint,
    rolloutgroup bigint,
    user_acceptance_required integer NOT NULL,
    maintenance_cron_schedule character varying(40),
    maintenance_duration character varying(40),
    maintenance_time_zone character varying(40),
    external_ref character varying(512),
    weight integer,
    initiated_by character varying(64) NOT NULL,
    last_action_status_code integer
    );



--
-- TOC entry 216 (class 1259 OID 131210)
-- Name: sp_action_status_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_action_status_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- TOC entry 231 (class 1259 OID 131230)
-- Name: sp_action_status; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_action_status (
                                                       id bigint DEFAULT nextval('public.sp_action_status_seq'::regclass) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    tenant character varying(40) NOT NULL,
    target_occurred_at bigint NOT NULL,
    status integer NOT NULL,
    action bigint NOT NULL,
    code integer,
    error_code character varying(1000),
    useracceptancemessagejob1 character varying(255),
    download_progress character varying(256)
    );




--
-- TOC entry 232 (class 1259 OID 131234)
-- Name: sp_action_status_messages; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_action_status_messages (
                                                                action_status_id bigint NOT NULL,
                                                                detail_message character varying(512) NOT NULL
    );


--
-- TOC entry 310 (class 1259 OID 3170413)
-- Name: sp_action_status_user_acceptance_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_action_status_user_acceptance_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



--
-- TOC entry 311 (class 1259 OID 3170414)
-- Name: sp_action_status_user_acceptance; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_action_status_user_acceptance (
                                                                       id bigint DEFAULT nextval('public.sp_action_status_user_acceptance_seq'::regclass) NOT NULL,
    time_stamp_of_prompt bigint,
    user_response integer,
    prompt character varying(64) NOT NULL,
    vin character varying(17) NOT NULL,
    ota_master_serial_number character varying(50) NOT NULL,
    ecu_hmi_serial_number character varying(50) NOT NULL,
    scheduled_time bigint,
    action_status_id bigint NOT NULL
    );




--
-- TOC entry 217 (class 1259 OID 131211)
-- Name: sp_artifact_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_artifact_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 233 (class 1259 OID 131239)
-- Name: sp_artifact; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_artifact (
                                                  id bigint DEFAULT nextval('public.sp_artifact_seq'::regclass) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    tenant character varying(40) NOT NULL,
    md5_hash character varying(32),
    file_size bigint,
    provided_file_name character varying(256),
    sha1_hash character varying(40) NOT NULL,
    software_module bigint,
    sha256_hash character(64),
    version character varying(64) DEFAULT '1'::character varying NOT NULL,
    file_type character varying(10) DEFAULT 'DELTA'::character varying,
    source_version bigint,
    target_version bigint
    );




--
-- TOC entry 262 (class 1259 OID 131657)
-- Name: sp_artifact_audit_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_artifact_audit_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



--
-- TOC entry 263 (class 1259 OID 131658)
-- Name: sp_artifact_audit; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_artifact_audit (
                                                        id bigint DEFAULT nextval('public.sp_artifact_audit_seq'::regclass) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    deleted_at bigint,
    deleted_by character varying(64),
    optlock_revision bigint,
    tenant character varying(40) NOT NULL,
    md5_hash character varying(32),
    file_size bigint,
    provided_file_name character varying(256),
    version character varying(64) NOT NULL,
    sha1_hash character varying(40) NOT NULL,
    software_module bigint NOT NULL,
    sha256_hash character(64),
    file_type character varying(10) DEFAULT 'DELTA'::character varying,
    source_version bigint,
    target_version bigint
    );




--
-- TOC entry 299 (class 1259 OID 765348)
-- Name: sp_artifact_module_link_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_artifact_module_link_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



--
-- TOC entry 318 (class 1259 OID 3346683)
-- Name: sp_artifact_module_link; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_artifact_module_link (
                                                              id bigint DEFAULT nextval('public.sp_artifact_module_link_seq'::regclass) NOT NULL,
    artifact_id bigint NOT NULL,
    software_module_id bigint NOT NULL,
    source_version bigint NOT NULL,
    target_version bigint NOT NULL
    );



--
-- TOC entry 300 (class 1259 OID 765349)
-- Name: sp_artifact_software_module; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_artifact_software_module (
                                                                  id bigint DEFAULT nextval('public.sp_artifact_module_link_seq'::regclass) NOT NULL,
    artifact_id bigint NOT NULL,
    software_module_id bigint NOT NULL,
    source_version bigint,
    target_version bigint NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint
    );




--
-- TOC entry 291 (class 1259 OID 532877)
-- Name: sp_artifacts_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_artifacts_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



--
-- TOC entry 292 (class 1259 OID 532878)
-- Name: sp_artifacts; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_artifacts (
                                                   id bigint DEFAULT nextval('public.sp_artifacts_seq'::regclass) NOT NULL,
    file_name character varying(300) NOT NULL,
    file_type character varying(10) NOT NULL,
    description character varying(300) NOT NULL,
    expiry_date bigint NOT NULL,
    sha256_hash character varying,
    file_size bigint,
    tenant character varying(40) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    optlock_revision bigint,
    last_modified_at bigint,
    last_modified_by character varying(64),
    md5_hash character varying(32),
    file_status character varying(50),
    status character varying(50) DEFAULT 'ACTIVE'::character varying NOT NULL
    );



--
-- TOC entry 218 (class 1259 OID 131212)
-- Name: sp_base_software_module_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_base_software_module_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



--
-- TOC entry 234 (class 1259 OID 131245)
-- Name: sp_base_software_module; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_base_software_module (
                                                              id bigint DEFAULT nextval('public.sp_base_software_module_seq'::regclass) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    tenant character varying(40) NOT NULL,
    description character varying(512),
    name character varying(128),
    version character varying(64) NOT NULL,
    deleted boolean,
    vendor character varying(256),
    module_type bigint NOT NULL,
    encrypted boolean,
    module_format bigint,
    software_installer_type bigint NOT NULL
    );




--
-- TOC entry 293 (class 1259 OID 623918)
-- Name: sp_deployment_log_sec; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_deployment_log_sec
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 294 (class 1259 OID 623919)
-- Name: sp_deployment_log; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_deployment_log (
                                                        id bigint DEFAULT nextval('public.sp_deployment_log_sec'::regclass) NOT NULL,
    action bigint NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    tenant character varying(40) NOT NULL,
    file_original_name character varying(256),
    file_name character varying(256),
    sequence integer,
    file_size bigint,
    byte_size bigint,
    byte_range bigint,
    is_last_chunk boolean,
    is_last_file boolean NOT NULL,
    sha256_hash character(64),
    file_path character varying
    );




--
-- TOC entry 219 (class 1259 OID 131213)
-- Name: sp_distribution_set_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_distribution_set_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 235 (class 1259 OID 131251)
-- Name: sp_distribution_set; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_distribution_set (
                                                          id bigint DEFAULT nextval('public.sp_distribution_set_seq'::regclass) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    tenant character varying(40) NOT NULL,
    description character varying(512),
    name character varying(128),
    version character varying(64),
    complete boolean,
    deleted boolean,
    required_migration_step boolean,
    ds_id bigint NOT NULL,
    valid boolean,
    software_downgrade_enabled boolean DEFAULT false NOT NULL
    );




--
-- TOC entry 220 (class 1259 OID 131214)
-- Name: sp_distribution_set_type_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_distribution_set_type_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 236 (class 1259 OID 131257)
-- Name: sp_distribution_set_type; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_distribution_set_type (
                                                               id bigint DEFAULT nextval('public.sp_distribution_set_type_seq'::regclass) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    tenant character varying(40) NOT NULL,
    description character varying(512),
    name character varying(128),
    colour character varying(16),
    deleted boolean,
    type_key character varying(64) NOT NULL
    );




--
-- TOC entry 221 (class 1259 OID 131215)
-- Name: sp_distributionset_tag_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_distributionset_tag_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 237 (class 1259 OID 131263)
-- Name: sp_distributionset_tag; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_distributionset_tag (
                                                             id bigint DEFAULT nextval('public.sp_distributionset_tag_seq'::regclass) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    tenant character varying(40) NOT NULL,
    description character varying(512),
    name character varying(128),
    colour character varying(16)
    );




--
-- TOC entry 238 (class 1259 OID 131269)
-- Name: sp_ds_dstag; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_ds_dstag (
                                                  ds bigint NOT NULL,
                                                  tag bigint NOT NULL
);




--
-- TOC entry 239 (class 1259 OID 131272)
-- Name: sp_ds_metadata; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_ds_metadata (
                                                     meta_key character varying(128) NOT NULL,
    meta_value character varying(4000),
    ds_id bigint NOT NULL
    );




--
-- TOC entry 240 (class 1259 OID 131277)
-- Name: sp_ds_module; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_ds_module (
                                                   ds_id bigint NOT NULL,
                                                   module_id bigint NOT NULL,
                                                   software_version_id bigint NOT NULL
);




--
-- TOC entry 316 (class 1259 OID 3346652)
-- Name: sp_ds_module_basic_table; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_ds_module_basic_table (
                                                               ds_id bigint NOT NULL,
                                                               module_id bigint NOT NULL
);




--
-- TOC entry 241 (class 1259 OID 131280)
-- Name: sp_ds_type_element; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_ds_type_element (
                                                         mandatory boolean,
                                                         distribution_set_type bigint NOT NULL,
                                                         software_module_type bigint NOT NULL
);




--
-- TOC entry 285 (class 1259 OID 270010)
-- Name: sp_ecu_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_ecu_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 286 (class 1259 OID 270011)
-- Name: sp_ecu_model; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_ecu_model (
                                                   id bigint DEFAULT nextval('public.sp_ecu_id_seq'::regclass) NOT NULL,
    ecu_model_name character varying(25) NOT NULL,
    ecu_node_id character varying(25) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    ecu_model_type bigint NOT NULL
    );




--
-- TOC entry 297 (class 1259 OID 638336)
-- Name: sp_ecu_model_type_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_ecu_model_type_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 298 (class 1259 OID 638337)
-- Name: sp_ecu_model_type; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_ecu_model_type (
                                                        id bigint DEFAULT nextval('public.sp_ecu_model_type_seq'::regclass) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    name character varying(50) NOT NULL,
    description character varying(512),
    deleted boolean
    );


--
-- TOC entry 301 (class 1259 OID 1546514)
-- Name: sp_esp_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_esp_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 302 (class 1259 OID 1546515)
-- Name: sp_esp; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_esp (
                                             id bigint DEFAULT nextval('public.sp_esp_id_seq'::regclass) NOT NULL,
                                             file_name character varying(50) NOT NULL,
    file_type integer NOT NULL,
    file_url text,
    sha_256 character varying(64) NOT NULL,
    md5 character varying(32),
    file_version character varying NOT NULL,
    file_content_description character varying,
    file_info_url text,
    file_metadata character varying,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    tenant character varying(40) NOT NULL,
    file_status character varying(50),
    file_size bigint
    );


--
-- TOC entry 305 (class 1259 OID 1546532)
-- Name: sp_esp_ecu_rollout_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_esp_ecu_rollout_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 306 (class 1259 OID 1546533)
-- Name: sp_esp_ecu_rollout; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_esp_ecu_rollout (
                                                         id bigint DEFAULT nextval('public.sp_esp_ecu_rollout_id_seq'::regclass) NOT NULL,
                                                         package_id bigint NOT NULL,
                                                         controller_id character varying NOT NULL,
                                                         rollout_id bigint NOT NULL,
                                                         ecu_node_addr character varying,
                                                         created_at bigint,
                                                         created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    tenant character varying(40) NOT NULL
    );


--
-- TOC entry 3976 (class 0 OID 0)
-- Dependencies: 305
-- Name: sp_esp_ecu_rollout_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.sp_esp_ecu_rollout_id_seq OWNED BY public.sp_esp_ecu_rollout.id;


--
-- TOC entry 3977 (class 0 OID 0)
-- Dependencies: 301
-- Name: sp_esp_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.sp_esp_id_seq OWNED BY public.sp_esp.id;


--
-- TOC entry 314 (class 1259 OID 3243204)
-- Name: sp_file_processing_error_log_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_file_processing_error_log_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 315 (class 1259 OID 3243205)
-- Name: sp_file_processing_error_log; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_file_processing_error_log (
                                                                   id bigint DEFAULT nextval('public.sp_file_processing_error_log_seq'::regclass) NOT NULL,
    file_type character varying(64) NOT NULL,
    log_message text NOT NULL,
    log_type_id bigint NOT NULL,
    storage_type character varying(64) NOT NULL,
    retry_count integer NOT NULL,
    action character varying(64) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    tenant character varying(40) NOT NULL
    );




--
-- TOC entry 312 (class 1259 OID 3225705)
-- Name: sp_general_feedback_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_general_feedback_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 313 (class 1259 OID 3225706)
-- Name: sp_general_feedback; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_general_feedback (
                                                          id bigint DEFAULT nextval('public.sp_general_feedback_seq'::regclass) NOT NULL,
    target_id bigint NOT NULL,
    code integer NOT NULL,
    details text NOT NULL,
    execution character varying(4) NOT NULL,
    error_code text,
    time_stamp bigint NOT NULL,
    optlock_revision bigint,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64)
    );




--
-- TOC entry 274 (class 1259 OID 131795)
-- Name: sp_permission_sec; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_permission_sec
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 276 (class 1259 OID 131804)
-- Name: sp_permission; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_permission (
                                                    id bigint DEFAULT nextval('public.sp_permission_sec'::regclass) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    name character varying(100) NOT NULL,
    description character varying(512) NOT NULL,
    active character varying(10)
    );




--
-- TOC entry 266 (class 1259 OID 131677)
-- Name: sp_polling_feedback_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_polling_feedback_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 265 (class 1259 OID 131676)
-- Name: sp_polling_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_polling_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 273 (class 1259 OID 131794)
-- Name: sp_role_sec; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_role_sec
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 275 (class 1259 OID 131796)
-- Name: sp_role; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_role (
                                              id bigint DEFAULT nextval('public.sp_role_sec'::regclass) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    name character varying(100) NOT NULL,
    description character varying(512) NOT NULL,
    active character varying(10)
    );




--
-- TOC entry 277 (class 1259 OID 131812)
-- Name: sp_role_permission; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_role_permission (
                                                         role_id bigint NOT NULL,
                                                         permission_id bigint NOT NULL
);




--
-- TOC entry 222 (class 1259 OID 131216)
-- Name: sp_rollout_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_rollout_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 242 (class 1259 OID 131283)
-- Name: sp_rollout; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_rollout (
                                                 id bigint DEFAULT nextval('public.sp_rollout_seq'::regclass) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    tenant character varying(40) NOT NULL,
    description character varying(512),
    name character varying(128),
    last_check bigint,
    group_theshold real,
    status integer NOT NULL,
    distribution_set bigint,
    target_filter character varying(1024),
    forced_time bigint,
    total_targets bigint,
    rollout_groups_created bigint,
    start_at bigint,
    deleted boolean,
    user_acceptance_required integer NOT NULL,
    approval_decided_by character varying(64),
    approval_remark character varying(255),
    weight integer,
    connectivity_type integer DEFAULT 0 NOT NULL,
    end_at bigint,
    collection_required boolean DEFAULT false NOT NULL,
    max_success_vin integer DEFAULT 100 NOT NULL,
    max_failure_vin integer DEFAULT 20 NOT NULL,
    max_all_file_size integer DEFAULT 1048576 NOT NULL,
    max_number_of_files integer DEFAULT 5 NOT NULL,
    download_retry_count bigint DEFAULT 0 NOT NULL,
    max_download_duration_timer bigint DEFAULT 0 NOT NULL,
    max_update_time bigint DEFAULT 1800 NOT NULL,
    required_media integer DEFAULT 0 NOT NULL,
    downgrade_allowed integer DEFAULT 0 NOT NULL,
    required_state_of_charge character varying(255),
    max_download_wifi_duration_timer bigint DEFAULT 0 NOT NULL,
    max_download_cellular_duration_timer bigint DEFAULT 0 NOT NULL,
    downloadretrycount bigint DEFAULT 0 NOT NULL,
    max_download_duration bigint DEFAULT 0 NOT NULL,
    max_each_file_size integer NOT NULL,
    start_type integer NOT NULL,
    priority integer NOT NULL,
    maxdownloadduration bigint DEFAULT 0 NOT NULL,
    max_size integer DEFAULT 1048576 NOT NULL,
    max_file integer DEFAULT 5 NOT NULL,
    deployment_estimated_update_time integer DEFAULT 1000 NOT NULL,
    max_package_size bigint
    );




--
-- TOC entry 223 (class 1259 OID 131217)
-- Name: sp_rolloutgroup_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_rolloutgroup_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 243 (class 1259 OID 131289)
-- Name: sp_rolloutgroup; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_rolloutgroup (
                                                      id bigint DEFAULT nextval('public.sp_rolloutgroup_seq'::regclass) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    tenant character varying(40) NOT NULL,
    description character varying(512),
    name character varying(128),
    error_condition integer,
    error_condition_exp character varying(512),
    error_action integer,
    error_action_exp character varying(512),
    success_condition integer NOT NULL,
    success_condition_exp character varying(512) NOT NULL,
    success_action integer NOT NULL,
    success_action_exp character varying(512),
    status integer NOT NULL,
    parent_id bigint,
    rollout bigint NOT NULL,
    total_targets bigint,
    target_percentage real,
    target_filter character varying(1024),
    confirmation_required boolean
    );




--
-- TOC entry 309 (class 1259 OID 2415682)
-- Name: sp_rollouts_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_rollouts_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 319 (class 1259 OID 3346743)
-- Name: sp_rollouts; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_rollouts (
                                                  id bigint DEFAULT nextval('public.sp_rollouts_seq'::regclass) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    deleted boolean,
    tenant character varying(40) NOT NULL,
    description character varying(512),
    name character varying(128) NOT NULL,
    priority integer NOT NULL,
    start_type integer NOT NULL,
    user_acceptance_required integer NOT NULL,
    connectivity_type integer NOT NULL,
    start_at bigint,
    end_at bigint NOT NULL,
    downgrade_allowed integer NOT NULL,
    required_media integer NOT NULL,
    required_state_of_charge character varying(255),
    status integer NOT NULL,
    collection_required boolean NOT NULL,
    max_failure_vin integer NOT NULL,
    max_success_vin integer NOT NULL,
    max_number_of_files integer NOT NULL,
    max_all_file_size integer NOT NULL,
    max_each_file_size integer NOT NULL,
    max_download_cellular_duration_timer integer NOT NULL,
    max_download_duration_timer integer NOT NULL,
    max_download_wifi_duration_timer integer NOT NULL,
    download_retry_count integer NOT NULL,
    max_update_time integer NOT NULL
    );




--
-- TOC entry 244 (class 1259 OID 131295)
-- Name: sp_rollouttargetgroup; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_rollouttargetgroup (
                                                            target_id bigint NOT NULL,
                                                            rolloutgroup_id bigint NOT NULL
);

--
-- TOC entry 320 (class 1259 OID 3346744)
-- Name: sp_action_artifact; Type: TABLE; Schema: public; Owner: postgres
--

 CREATE TABLE IF NOT EXISTS public.sp_action_artifact (
                                                            action_id bigint NOT NULL,
                                                            artifact_id bigint NOT NULL
 );


--
-- TOC entry 303 (class 1259 OID 1546523)
-- Name: sp_rsp_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_rsp_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 304 (class 1259 OID 1546524)
-- Name: sp_rsp; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_rsp (
                                             id bigint DEFAULT nextval('public.sp_rsp_id_seq'::regclass) NOT NULL,
                                             file_name character varying(50) NOT NULL,
    file_type integer NOT NULL,
    file_url text,
    sha_256 character varying(64) NOT NULL,
    md5 character varying(32),
    file_version character varying NOT NULL,
    file_content_description character varying,
    file_info_url text,
    file_metadata character varying,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    tenant character varying(40) NOT NULL,
    file_status character varying(50),
    file_size bigint
    );



--
-- TOC entry 3978 (class 0 OID 0)
-- Dependencies: 303
-- Name: sp_rsp_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.sp_rsp_id_seq OWNED BY public.sp_rsp.id;


--
-- TOC entry 307 (class 1259 OID 1546553)
-- Name: sp_rsp_rollout_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_rsp_rollout_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- TOC entry 308 (class 1259 OID 1546554)
-- Name: sp_rsp_rollout; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_rsp_rollout (
                                                     id bigint DEFAULT nextval('public.sp_rsp_rollout_id_seq'::regclass) NOT NULL,
                                                     package_id bigint NOT NULL,
                                                     rollout_id bigint NOT NULL,
                                                     created_at bigint,
                                                     created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    tenant character varying(40) NOT NULL
    );


--
-- TOC entry 3979 (class 0 OID 0)
-- Dependencies: 307
-- Name: sp_rsp_rollout_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.sp_rsp_rollout_id_seq OWNED BY public.sp_rsp_rollout.id;

--
-- TOC entry 280 (class 1259 OID 31602)
-- Name: sp_signing_certificate_configuration_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.sp_signing_certificate_configuration_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 281 (class 1259 OID 31603)
-- Name: sp_signing_certificate_configuration; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.sp_signing_certificate_configuration (
    id bigint DEFAULT nextval('public.sp_signing_certificate_configuration_seq'::regclass) NOT NULL,
    pki character varying(128),
    ecu_id_issuer character varying(128) NOT NULL,
    dd_certificate_path character varying(256) NOT NULL,
    esp_certificate_path character varying(256) NOT NULL,
    rsp_certificate_path character varying(256) NOT NULL,
    dd_private_key_path character varying(256) NOT NULL,
    esp_private_key_path character varying(256) NOT NULL,
    rsp_private_key_path character varying(256) NOT NULL,
    optlock_revision bigint,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    intermediate_ca_certificate_path character varying(256)
);


--
-- TOC entry 288 (class 1259 OID 400246)
-- Name: sp_software_ecu_model; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_software_ecu_model (
                                                            software_module_id bigint,
                                                            ecu_model_id bigint
);




--
-- TOC entry 295 (class 1259 OID 638325)
-- Name: sp_software_installer_type_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_software_installer_type_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 296 (class 1259 OID 638326)
-- Name: sp_software_installer_type; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_software_installer_type (
                                                                 id bigint DEFAULT nextval('public.sp_software_installer_type_seq'::regclass) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    name character varying(50) NOT NULL,
    description character varying(512),
    deleted boolean
    );




--
-- TOC entry 260 (class 1259 OID 131643)
-- Name: sp_software_module_format_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_software_module_format_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 261 (class 1259 OID 131644)
-- Name: sp_software_module_format; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_software_module_format (
                                                                id bigint DEFAULT nextval('public.sp_software_module_format_seq'::regclass) NOT NULL,
    created_at bigint NOT NULL,
    created_by character varying(40) NOT NULL,
    last_modified_at bigint NOT NULL,
    last_modified_by character varying(40) NOT NULL,
    optlock_revision integer,
    tenant character varying(40) NOT NULL,
    description character varying(512),
    name character varying(64) NOT NULL,
    deleted boolean,
    type_key character varying(64) NOT NULL
    );




--
-- TOC entry 224 (class 1259 OID 131218)
-- Name: sp_software_module_type_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_software_module_type_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 245 (class 1259 OID 131298)
-- Name: sp_software_module_type; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_software_module_type (
                                                              id bigint DEFAULT nextval('public.sp_software_module_type_seq'::regclass) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    tenant character varying(40) NOT NULL,
    description character varying(512),
    name character varying(128),
    colour character varying(16),
    deleted boolean,
    type_key character varying(64) NOT NULL,
    max_ds_assignments integer NOT NULL
    );




--
-- TOC entry 271 (class 1259 OID 131732)
-- Name: sp_software_versions_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_software_versions_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 272 (class 1259 OID 131733)
-- Name: sp_software_versions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_software_versions (
                                                           id bigint DEFAULT nextval('public.sp_software_versions_seq'::regclass) NOT NULL,
    name character varying(100) NOT NULL,
    description character varying(100),
    number integer NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    software_module_id integer NOT NULL
    );




--
-- TOC entry 246 (class 1259 OID 131304)
-- Name: sp_sw_metadata; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_sw_metadata (
                                                     meta_key character varying(128) NOT NULL,
    meta_value character varying(4000),
    sw_id bigint NOT NULL,
    target_visible boolean
    );




--
-- TOC entry 226 (class 1259 OID 131220)
-- Name: sp_target_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_target_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 247 (class 1259 OID 131309)
-- Name: sp_target; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_target (
                                                id bigint DEFAULT nextval('public.sp_target_seq'::regclass) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    tenant character varying(40) NOT NULL,
    description character varying(512),
    name character varying(128),
    controller_id character varying(256),
    sec_token character varying(128) NOT NULL,
    assigned_distribution_set bigint,
    install_date bigint,
    address character varying(512),
    last_target_query bigint,
    request_controller_attributes boolean NOT NULL,
    installed_distribution_set bigint,
    update_status integer NOT NULL,
    target_type bigint,
    serial_number character varying(256) DEFAULT ''::character varying NOT NULL,
    vehicle_model_id bigint,
    vin character varying(255) NOT NULL
    );




--
-- TOC entry 248 (class 1259 OID 131315)
-- Name: sp_target_attributes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_target_attributes (
                                                           target_id bigint NOT NULL,
                                                           attribute_value character varying(2048),
    attribute_key character varying(128) NOT NULL
    );


--
-- TOC entry 258 (class 1259 OID 131634)
-- Name: sp_target_conf_status_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_target_conf_status_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 259 (class 1259 OID 131635)
-- Name: sp_target_conf_status; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_target_conf_status (
                                                            id bigint DEFAULT nextval('public.sp_target_conf_status_id_seq'::regclass) NOT NULL,
                                                            target_id bigint NOT NULL,
                                                            initiator character varying(64),
    remark character varying(512),
    tenant character varying(40) NOT NULL,
    created_at bigint NOT NULL,
    created_by character varying(64) NOT NULL,
    last_modified_at bigint NOT NULL,
    last_modified_by character varying(64) NOT NULL,
    optlock_revision bigint
    );


--
-- TOC entry 3980 (class 0 OID 0)
-- Dependencies: 258
-- Name: sp_target_conf_status_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.sp_target_conf_status_id_seq OWNED BY public.sp_target_conf_status.id;


--
-- TOC entry 225 (class 1259 OID 131219)
-- Name: sp_target_filter_query_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_target_filter_query_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 249 (class 1259 OID 131318)
-- Name: sp_target_filter_query; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_target_filter_query (
                                                             id bigint DEFAULT nextval('public.sp_target_filter_query_seq'::regclass) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    tenant character varying(40) NOT NULL,
    name character varying(128),
    query character varying(1024) NOT NULL,
    auto_assign_distribution_set bigint,
    auto_assign_user_acceptance_required integer,
    auto_assign_weight integer,
    auto_assign_initiated_by character varying(64),
    confirmation_required boolean
    );




--
-- TOC entry 289 (class 1259 OID 514834)
-- Name: sp_target_inventory_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_target_inventory_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 290 (class 1259 OID 514835)
-- Name: sp_target_inventory; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_target_inventory (
                                                          id bigint DEFAULT nextval('public.sp_target_inventory_id_seq'::regclass) NOT NULL,
    target_id bigint NOT NULL,
    inventory text,
    raw_inventory text,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint
    );




--
-- TOC entry 250 (class 1259 OID 131324)
-- Name: sp_target_metadata; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_target_metadata (
                                                         meta_key character varying(128) NOT NULL,
    meta_value character varying(4000),
    target_id bigint NOT NULL
    );




--
-- TOC entry 264 (class 1259 OID 131666)
-- Name: sp_target_software; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_target_software (
                                                         target_id bigint NOT NULL,
                                                         node character varying(128) NOT NULL,
    component_id character varying(128) NOT NULL,
    version character varying(128) NOT NULL
    );




--
-- TOC entry 227 (class 1259 OID 131221)
-- Name: sp_target_tag_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_target_tag_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 251 (class 1259 OID 131329)
-- Name: sp_target_tag; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_target_tag (
                                                    id bigint DEFAULT nextval('public.sp_target_tag_seq'::regclass) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    tenant character varying(40) NOT NULL,
    description character varying(512),
    name character varying(128),
    colour character varying(16)
    );




--
-- TOC entry 252 (class 1259 OID 131335)
-- Name: sp_target_target_tag; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_target_target_tag (
                                                           target bigint NOT NULL,
                                                           tag bigint NOT NULL
);




--
-- TOC entry 255 (class 1259 OID 131600)
-- Name: sp_target_type_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_target_type_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 256 (class 1259 OID 131601)
-- Name: sp_target_type; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_target_type (
                                                     id bigint DEFAULT nextval('public.sp_target_type_seq'::regclass) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    tenant character varying(40) NOT NULL,
    description character varying(512),
    name character varying(128),
    colour character varying(16)
    );




--
-- TOC entry 257 (class 1259 OID 131607)
-- Name: sp_target_type_ds_type_relation; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_target_type_ds_type_relation (
                                                                      target_type bigint NOT NULL,
                                                                      distribution_set_type bigint NOT NULL
);




--
-- TOC entry 229 (class 1259 OID 131223)
-- Name: sp_tenant_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_tenant_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 253 (class 1259 OID 131338)
-- Name: sp_tenant; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_tenant (
                                                id bigint DEFAULT nextval('public.sp_tenant_seq'::regclass) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    tenant character varying(40) NOT NULL,
    default_ds_type bigint NOT NULL
    );




--
-- TOC entry 228 (class 1259 OID 131222)
-- Name: sp_tenant_configuration_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_tenant_configuration_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 254 (class 1259 OID 131342)
-- Name: sp_tenant_configuration; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_tenant_configuration (
                                                              id bigint DEFAULT nextval('public.sp_tenant_configuration_seq'::regclass) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    tenant character varying(40) NOT NULL,
    conf_key character varying(128) NOT NULL,
    conf_value character varying(512)
    );




--
-- TOC entry 267 (class 1259 OID 131702)
-- Name: sp_user_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_user_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 268 (class 1259 OID 131703)
-- Name: sp_user; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_user (
                                              id bigint DEFAULT nextval('public.sp_user_seq'::regclass) NOT NULL,
    username character varying(100),
    firstname character varying(100),
    lastname character varying(100),
    created_at bigint,
    created_by character varying(64),
    optlock_revision bigint,
    last_modified_at bigint,
    last_modified_by character varying(64),
    email character varying(100) NOT NULL,
    active character varying(10)
    );




--
-- TOC entry 281 (class 1259 OID 154340)
-- Name: sp_user_audit_sec; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_user_audit_sec
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 282 (class 1259 OID 154341)
-- Name: sp_user_audit; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_user_audit (
                                                    id bigint DEFAULT nextval('public.sp_user_audit_sec'::regclass) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    deleted_at bigint,
    user_id bigint NOT NULL,
    role_id bigint NOT NULL,
    tenant_id bigint NOT NULL
    );




--
-- TOC entry 279 (class 1259 OID 131822)
-- Name: sp_user_configuration_sec; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_user_configuration_sec
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 280 (class 1259 OID 131823)
-- Name: sp_user_configuration; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_user_configuration (
                                                            id bigint DEFAULT nextval('public.sp_user_configuration_sec'::regclass) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint,
    conf_key character varying(100) NOT NULL,
    conf_value character varying(100) NOT NULL,
    user_id bigint NOT NULL
    );




--
-- TOC entry 278 (class 1259 OID 131817)
-- Name: sp_user_role; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_user_role (
                                                   user_id bigint NOT NULL,
                                                   role_id bigint NOT NULL
);




--
-- TOC entry 269 (class 1259 OID 131713)
-- Name: sp_user_tenant_sec; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_user_tenant_sec
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 270 (class 1259 OID 131714)
-- Name: sp_user_tenant; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_user_tenant (
                                                     id bigint DEFAULT nextval('public.sp_user_tenant_sec'::regclass) NOT NULL,
    user_id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    optlock_revision bigint,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64)
    );




--
-- TOC entry 283 (class 1259 OID 250604)
-- Name: sp_vehicle_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE  IF NOT EXISTS public.sp_vehicle_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- TOC entry 317 (class 1259 OID 3346667)
-- Name: sp_vehicle; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_vehicle (
                                                 id bigint DEFAULT nextval('public.sp_vehicle_id_seq'::regclass) NOT NULL,
    vin character varying(64) NOT NULL
    );




--
-- TOC entry 287 (class 1259 OID 376111)
-- Name: sp_vehicle_ecu; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_vehicle_ecu (
                                                     vehicle_model_id bigint NOT NULL,
                                                     ecu_model_id bigint NOT NULL
);




--
-- TOC entry 284 (class 1259 OID 250605)
-- Name: sp_vehicle_model; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.sp_vehicle_model (
                                                       id bigint DEFAULT nextval('public.sp_vehicle_id_seq'::regclass) NOT NULL,
    name character varying(25) NOT NULL,
    created_at bigint,
    created_by character varying(64),
    last_modified_at bigint,
    last_modified_by character varying(64),
    optlock_revision bigint
    );


DO $$
BEGIN

----
---- TOC entry 3538 (class 2606 OID 131378)
---- Name: sp_action pk_sp_action; Type: CONSTRAINT; Schema: public; Owner: postgres
----

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_action') THEN
        ALTER TABLE ONLY public.sp_action
        ADD CONSTRAINT pk_sp_action PRIMARY KEY (id);
    END IF;

--
-- TOC entry 3544 (class 2606 OID 131380)
-- Name: sp_action_status pk_sp_action_status; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_action_status') THEN
        ALTER TABLE ONLY public.sp_action_status
        ADD CONSTRAINT pk_sp_action_status PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3550 (class 2606 OID 131382)
-- Name: sp_artifact pk_sp_artifact; Type: CONSTRAINT; Schema: public; Owner: postgres
--

     IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_artifact') THEN
         ALTER TABLE ONLY public.sp_artifact
         ADD CONSTRAINT pk_sp_artifact PRIMARY KEY (id);
     END IF;


--
-- TOC entry 3555 (class 2606 OID 131384)
-- Name: sp_base_software_module pk_sp_base_software_module; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_base_software_module') THEN
        ALTER TABLE ONLY public.sp_base_software_module
        ADD CONSTRAINT pk_sp_base_software_module PRIMARY KEY (id);
    END IF;

--
-- TOC entry 3709 (class 2606 OID 623926)
-- Name: sp_deployment_log pk_sp_deployment_log; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_deployment_log') THEN
        ALTER TABLE ONLY public.sp_deployment_log
        ADD CONSTRAINT pk_sp_deployment_log PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3562 (class 2606 OID 131388)
-- Name: sp_distribution_set pk_sp_distribution_set; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_distribution_set') THEN
        ALTER TABLE ONLY public.sp_distribution_set
        ADD CONSTRAINT pk_sp_distribution_set PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3570 (class 2606 OID 131392)
-- Name: sp_distribution_set_type pk_sp_distribution_set_type; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_distribution_set_type') THEN
        ALTER TABLE ONLY public.sp_distribution_set_type
        ADD CONSTRAINT pk_sp_distribution_set_type PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3578 (class 2606 OID 131398)
-- Name: sp_distributionset_tag pk_sp_distributionset_tag; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_distributionset_tag') THEN
        ALTER TABLE ONLY public.sp_distributionset_tag
        ADD CONSTRAINT pk_sp_distributionset_tag PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3584 (class 2606 OID 131402)
-- Name: sp_ds_dstag pk_sp_ds_dstag; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_ds_dstag') THEN
        ALTER TABLE ONLY public.sp_ds_dstag
        ADD CONSTRAINT pk_sp_ds_dstag PRIMARY KEY (ds, tag);
    END IF;


--
-- TOC entry 3586 (class 2606 OID 131404)
-- Name: sp_ds_metadata pk_sp_ds_metadata; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_ds_metadata') THEN
        ALTER TABLE ONLY public.sp_ds_metadata
        ADD CONSTRAINT pk_sp_ds_metadata PRIMARY KEY (ds_id, meta_key);
    END IF;


--
-- TOC entry 3588 (class 2606 OID 131778)
-- Name: sp_ds_module pk_sp_ds_module; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_ds_module') THEN
        ALTER TABLE ONLY public.sp_ds_module
        ADD CONSTRAINT pk_sp_ds_module PRIMARY KEY (ds_id, module_id, software_version_id);
    END IF;


--
-- TOC entry 3590 (class 2606 OID 131408)
-- Name: sp_ds_type_element pk_sp_ds_type_element; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_ds_type_element') THEN
        ALTER TABLE ONLY public.sp_ds_type_element
        ADD CONSTRAINT pk_sp_ds_type_element PRIMARY KEY (distribution_set_type, software_module_type);
    END IF;


--
-- TOC entry 3592 (class 2606 OID 131410)
-- Name: sp_rollout pk_sp_rollout; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_rollout') THEN
        ALTER TABLE ONLY public.sp_rollout
        ADD CONSTRAINT pk_sp_rollout PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3599 (class 2606 OID 131414)
-- Name: sp_rolloutgroup pk_sp_rolloutgroup; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_rolloutgroup') THEN
        ALTER TABLE ONLY public.sp_rolloutgroup
        ADD CONSTRAINT pk_sp_rolloutgroup PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3756 (class 2606 OID 3346750)
-- Name: sp_rollouts pk_sp_rollouts; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_rollouts') THEN
        ALTER TABLE ONLY public.sp_rollouts
        ADD CONSTRAINT pk_sp_rollouts PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3603 (class 2606 OID 131418)
-- Name: sp_rollouttargetgroup pk_sp_rollouttargetgroup; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_rollouttargetgroup') THEN
        ALTER TABLE ONLY public.sp_rollouttargetgroup
        ADD CONSTRAINT pk_sp_rollouttargetgroup PRIMARY KEY (rolloutgroup_id, target_id);
    END IF;


--
-- TOC entry 3608 (class 2606 OID 131419)
-- Name: sp_action_artifact sp_action_artifact_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_action_artifact_pkey') THEN
        ALTER TABLE ONLY public.sp_action_artifact
        ADD CONSTRAINT sp_action_artifact_pkey PRIMARY KEY (action_id, artifact_id);
    END IF;


--
-- TOC entry 3605 (class 2606 OID 131420)
-- Name: sp_software_module_type pk_sp_software_module_type; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_software_module_type') THEN
        ALTER TABLE ONLY public.sp_software_module_type
        ADD CONSTRAINT pk_sp_software_module_type PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3670 (class 2606 OID 131742)
-- Name: sp_software_versions pk_sp_software_versions; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_software_versions') THEN
        ALTER TABLE ONLY public.sp_software_versions
        ADD CONSTRAINT pk_sp_software_versions PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3613 (class 2606 OID 131426)
-- Name: sp_sw_metadata pk_sp_sw_metadata; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_sw_metadata') THEN
        ALTER TABLE ONLY public.sp_sw_metadata
        ADD CONSTRAINT pk_sp_sw_metadata PRIMARY KEY (meta_key, sw_id);
    END IF;


--
-- TOC entry 3615 (class 2606 OID 131428)
-- Name: sp_target pk_sp_target; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_target') THEN
        ALTER TABLE ONLY public.sp_target
        ADD CONSTRAINT pk_sp_target PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3626 (class 2606 OID 131432)
-- Name: sp_target_attributes pk_sp_target_attributes; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_target_attributes') THEN
        ALTER TABLE ONLY public.sp_target_attributes
        ADD CONSTRAINT pk_sp_target_attributes PRIMARY KEY (target_id, attribute_key);
    END IF;


--
-- TOC entry 3656 (class 2606 OID 131642)
-- Name: sp_target_conf_status pk_sp_target_conf_status; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_target_conf_status') THEN
        ALTER TABLE ONLY public.sp_target_conf_status
        ADD CONSTRAINT pk_sp_target_conf_status PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3628 (class 2606 OID 131434)
-- Name: sp_target_filter_query pk_sp_target_filter_query; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_target_filter_query') THEN
        ALTER TABLE ONLY public.sp_target_filter_query
        ADD CONSTRAINT pk_sp_target_filter_query PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3632 (class 2606 OID 131438)
-- Name: sp_target_metadata pk_sp_target_metadata; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_target_metadata') THEN
        ALTER TABLE ONLY public.sp_target_metadata
        ADD CONSTRAINT pk_sp_target_metadata PRIMARY KEY (target_id, meta_key);
    END IF;


--
-- TOC entry 3660 (class 2606 OID 131670)
-- Name: sp_target_software pk_sp_target_software; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_target_software') THEN
        ALTER TABLE ONLY public.sp_target_software
        ADD CONSTRAINT pk_sp_target_software PRIMARY KEY (target_id, node, component_id);
    END IF;


--
-- TOC entry 3634 (class 2606 OID 131440)
-- Name: sp_target_tag pk_sp_target_tag; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_target_tag') THEN
        ALTER TABLE ONLY public.sp_target_tag
        ADD CONSTRAINT pk_sp_target_tag PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3640 (class 2606 OID 131444)
-- Name: sp_target_target_tag pk_sp_target_target_tag; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_target_target_tag') THEN
        ALTER TABLE ONLY public.sp_target_target_tag
        ADD CONSTRAINT pk_sp_target_target_tag PRIMARY KEY (target, tag);
    END IF;


--
-- TOC entry 3651 (class 2606 OID 131611)
-- Name: sp_target_type pk_sp_target_type; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_target_type') THEN
        ALTER TABLE ONLY public.sp_target_type
        ADD CONSTRAINT pk_sp_target_type PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3642 (class 2606 OID 131446)
-- Name: sp_tenant pk_sp_tenant; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_tenant') THEN
        ALTER TABLE ONLY public.sp_tenant
        ADD CONSTRAINT pk_sp_tenant PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3647 (class 2606 OID 131450)
-- Name: sp_tenant_configuration pk_sp_tenant_configuration; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_tenant_configuration') THEN
        ALTER TABLE ONLY public.sp_tenant_configuration
        ADD CONSTRAINT pk_sp_tenant_configuration PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3662 (class 2606 OID 131712)
-- Name: sp_user pk_sp_user; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_user') THEN
        ALTER TABLE ONLY public.sp_user
        ADD CONSTRAINT pk_sp_user PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3666 (class 2606 OID 131719)
-- Name: sp_user_tenant pk_sp_user_tenant; Type: CONSTRAINT; Schema: public; Owner: postgres
--
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_sp_user_tenant') THEN
        ALTER TABLE ONLY public.sp_user_tenant
        ADD CONSTRAINT pk_sp_user_tenant PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3722 (class 2606 OID 765354)
-- Name: sp_artifact_software_module sp_artifact_module_link_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_artifact_module_link_pkey') THEN
        ALTER TABLE ONLY public.sp_artifact_software_module
        ADD CONSTRAINT sp_artifact_module_link_pkey PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3752 (class 2606 OID 3346688)
-- Name: sp_artifact_module_link sp_artifact_module_link_pkey1; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_artifact_module_link_pkey1') THEN
        ALTER TABLE ONLY public.sp_artifact_module_link
        ADD CONSTRAINT sp_artifact_module_link_pkey1 PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3707 (class 2606 OID 532885)
-- Name: sp_artifacts sp_artifacts_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_artifacts_pkey') THEN
        ALTER TABLE ONLY public.sp_artifacts
        ADD CONSTRAINT sp_artifacts_pkey PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3746 (class 2606 OID 3346656)
-- Name: sp_ds_module_basic_table sp_ds_module_basic_table_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_ds_module_basic_table_pkey') THEN
        ALTER TABLE ONLY public.sp_ds_module_basic_table
        ADD CONSTRAINT sp_ds_module_basic_table_pkey PRIMARY KEY (ds_id, module_id);
    END IF;


--
-- TOC entry 3697 (class 2606 OID 270016)
-- Name: sp_ecu_model sp_ecu_model_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_ecu_model_pkey') THEN
        ALTER TABLE ONLY public.sp_ecu_model
        ADD CONSTRAINT sp_ecu_model_pkey PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3715 (class 2606 OID 638346)
-- Name: sp_ecu_model_type sp_ecu_model_type_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_ecu_model_type_name_key') THEN
        ALTER TABLE ONLY public.sp_ecu_model_type
        ADD CONSTRAINT sp_ecu_model_type_name_key UNIQUE (name);
    END IF;


--
-- TOC entry 3717 (class 2606 OID 638344)
-- Name: sp_ecu_model_type sp_ecu_model_type_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_ecu_model_type_pkey') THEN
        ALTER TABLE ONLY public.sp_ecu_model_type
        ADD CONSTRAINT sp_ecu_model_type_pkey PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3730 (class 2606 OID 1546540)
-- Name: sp_esp_ecu_rollout sp_esp_ecu_rollout_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_esp_ecu_rollout_pkey') THEN
        ALTER TABLE ONLY public.sp_esp_ecu_rollout
        ADD CONSTRAINT sp_esp_ecu_rollout_pkey PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3726 (class 2606 OID 1546522)
-- Name: sp_esp sp_esp_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_esp_pkey') THEN
        ALTER TABLE ONLY public.sp_esp
        ADD CONSTRAINT sp_esp_pkey PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3743 (class 2606 OID 3243212)
-- Name: sp_file_processing_error_log sp_file_processing_error_log_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_file_processing_error_log_pkey') THEN
        ALTER TABLE ONLY public.sp_file_processing_error_log
        ADD CONSTRAINT sp_file_processing_error_log_pkey PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3741 (class 2606 OID 3225713)
-- Name: sp_general_feedback sp_general_feedback_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_general_feedback_pkey') THEN
        ALTER TABLE ONLY public.sp_general_feedback
        ADD CONSTRAINT sp_general_feedback_pkey PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3678 (class 2606 OID 131811)
-- Name: sp_permission sp_permission_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_permission_pkey') THEN
        ALTER TABLE ONLY public.sp_permission
        ADD CONSTRAINT sp_permission_pkey PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3680 (class 2606 OID 131816)
-- Name: sp_role_permission sp_role_permission_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_role_permission_pkey') THEN
        ALTER TABLE ONLY public.sp_role_permission
        ADD CONSTRAINT sp_role_permission_pkey PRIMARY KEY (role_id, permission_id);
    END IF;

--
-- TOC entry 3676 (class 2606 OID 131803)
-- Name: sp_role sp_role_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_role_pkey') THEN
        ALTER TABLE ONLY public.sp_role
        ADD CONSTRAINT sp_role_pkey PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3728 (class 2606 OID 1546531)
-- Name: sp_rsp sp_rsp_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_rsp_pkey') THEN
        ALTER TABLE ONLY public.sp_rsp
        ADD CONSTRAINT sp_rsp_pkey PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3734 (class 2606 OID 1546559)
-- Name: sp_rsp_rollout sp_rsp_rollout_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_rsp_rollout_pkey') THEN
        ALTER TABLE ONLY public.sp_rsp_rollout
        ADD CONSTRAINT sp_rsp_rollout_pkey PRIMARY KEY (id);
    END IF;


--
-- TOC entry 5194 (class 2606 OID 31859)
-- Name: sp_signing_certificate_configuration sp_signing_certificate_configuration_ecu_id_issuer_key; Type: CONSTRAINT; Schema: public; Owner: -
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_signing_certificate_configuration_ecu_id_issuer_key') THEN
        ALTER TABLE ONLY public.sp_signing_certificate_configuration
        ADD CONSTRAINT sp_signing_certificate_configuration_ecu_id_issuer_key UNIQUE (ecu_id_issuer);
    END IF;


--
-- TOC entry 5196 (class 2606 OID 31861)
-- Name: sp_signing_certificate_configuration sp_signing_certificate_configuration_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_signing_certificate_configuration_pkey') THEN
        ALTER TABLE ONLY public.sp_signing_certificate_configuration
        ADD CONSTRAINT sp_signing_certificate_configuration_pkey PRIMARY KEY (id);
    END IF;

--
-- TOC entry 3711 (class 2606 OID 638335)
-- Name: sp_software_installer_type sp_software_installer_type_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_software_installer_type_name_key') THEN
        ALTER TABLE ONLY public.sp_software_installer_type
        ADD CONSTRAINT sp_software_installer_type_name_key UNIQUE (name);
    END IF;


--
-- TOC entry 3713 (class 2606 OID 638333)
-- Name: sp_software_installer_type sp_software_installer_type_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_software_installer_type_pkey') THEN
        ALTER TABLE ONLY public.sp_software_installer_type
        ADD CONSTRAINT sp_software_installer_type_pkey PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3658 (class 2606 OID 131651)
-- Name: sp_software_module_format sp_software_module_format_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_software_module_format_pkey') THEN
        ALTER TABLE ONLY public.sp_software_module_format
        ADD CONSTRAINT sp_software_module_format_pkey PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3705 (class 2606 OID 514842)
-- Name: sp_target_inventory sp_target_inventory_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_target_inventory_pkey') THEN
        ALTER TABLE ONLY public.sp_target_inventory
        ADD CONSTRAINT sp_target_inventory_pkey PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3688 (class 2606 OID 154346)
-- Name: sp_user_audit sp_user_audit_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_user_audit_pkey') THEN
        ALTER TABLE ONLY public.sp_user_audit
        ADD CONSTRAINT sp_user_audit_pkey PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3684 (class 2606 OID 131828)
-- Name: sp_user_configuration sp_user_configuration_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_user_configuration_pkey') THEN
        ALTER TABLE ONLY public.sp_user_configuration
        ADD CONSTRAINT sp_user_configuration_pkey PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3682 (class 2606 OID 131821)
-- Name: sp_user_role sp_user_role_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_user_role_pkey') THEN
        ALTER TABLE ONLY public.sp_user_role
        ADD CONSTRAINT sp_user_role_pkey PRIMARY KEY (user_id, role_id);
    END IF;


--
-- TOC entry 3664 (class 2606 OID 131710)
-- Name: sp_user sp_user_username_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_user_username_key') THEN
        ALTER TABLE ONLY public.sp_user
        ADD CONSTRAINT sp_user_username_key UNIQUE (username);
    END IF;


--
-- TOC entry 3701 (class 2606 OID 376115)
-- Name: sp_vehicle_ecu sp_vehicle_ecu_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_vehicle_ecu_pkey') THEN
        ALTER TABLE ONLY public.sp_vehicle_ecu
        ADD CONSTRAINT sp_vehicle_ecu_pkey PRIMARY KEY (vehicle_model_id, ecu_model_id);
    END IF;


--
-- TOC entry 3693 (class 2606 OID 250612)
-- Name: sp_vehicle_model sp_vehicle_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_vehicle_name_key') THEN
        ALTER TABLE ONLY public.sp_vehicle_model
        ADD CONSTRAINT sp_vehicle_name_key UNIQUE (name);
    END IF;


--
-- TOC entry 3695 (class 2606 OID 250610)
-- Name: sp_vehicle_model sp_vehicle_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_vehicle_pkey') THEN
        ALTER TABLE ONLY public.sp_vehicle_model
        ADD CONSTRAINT sp_vehicle_pkey PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3748 (class 2606 OID 3346672)
-- Name: sp_vehicle sp_vehicle_pkey1; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_vehicle_pkey1') THEN
        ALTER TABLE ONLY public.sp_vehicle
        ADD CONSTRAINT sp_vehicle_pkey1 PRIMARY KEY (id);
    END IF;


--
-- TOC entry 3750 (class 2606 OID 3346674)
-- Name: sp_vehicle sp_vehicle_vin_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_vehicle_vin_key') THEN
        ALTER TABLE ONLY public.sp_vehicle
        ADD CONSTRAINT sp_vehicle_vin_key UNIQUE (vin);
    END IF;


--
-- TOC entry 3560 (class 2606 OID 131386)
-- Name: sp_base_software_module uk_base_sw_mod_sp_base_software_module; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_base_sw_mod_sp_base_software_module') THEN
        ALTER TABLE ONLY public.sp_base_software_module
        ADD CONSTRAINT uk_base_sw_mod_sp_base_software_module UNIQUE (module_type, name, version, tenant);
    END IF;

--
-- TOC entry 5109 (class 2606 OID 40595)
-- Name: sp_base_software_module uk_base_sw_mod_tenant_name; Type: CONSTRAINT; Schema: public; Owner: -
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_base_sw_mod_tenant_name') THEN
        ALTER TABLE ONLY public.sp_base_software_module
        ADD CONSTRAINT uk_base_sw_mod_tenant_name UNIQUE (tenant, name);
    END IF;

--
-- TOC entry 3622 (class 2606 OID 340982)
-- Name: sp_target uk_controller_id; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_controller_id') THEN
        ALTER TABLE ONLY public.sp_target
        ADD CONSTRAINT uk_controller_id UNIQUE (controller_id);
    END IF;


--
-- TOC entry 3566 (class 2606 OID 2734883)
-- Name: sp_distribution_set uk_distrib_set; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_distrib_set') THEN
        ALTER TABLE ONLY public.sp_distribution_set
        ADD CONSTRAINT uk_distrib_set UNIQUE (name, tenant);
    END IF;


--
-- TOC entry 3568 (class 2606 OID 3346622)
-- Name: sp_distribution_set uk_distrib_set_sp_distribution_set; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_distrib_set_sp_distribution_set') THEN
        ALTER TABLE ONLY public.sp_distribution_set
        ADD CONSTRAINT uk_distrib_set_sp_distribution_set UNIQUE (name, version, tenant);
    END IF;


--
-- TOC entry 3582 (class 2606 OID 131400)
-- Name: sp_distributionset_tag uk_ds_tag_sp_distributionset_tag; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_ds_tag_sp_distributionset_tag') THEN
        ALTER TABLE ONLY public.sp_distributionset_tag
        ADD CONSTRAINT uk_ds_tag_sp_distributionset_tag UNIQUE (name, tenant);
    END IF;


--
-- TOC entry 3574 (class 2606 OID 131394)
-- Name: sp_distribution_set_type uk_dst_key_sp_distribution_set_type; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_dst_key_sp_distribution_set_type') THEN
        ALTER TABLE ONLY public.sp_distribution_set_type
        ADD CONSTRAINT uk_dst_key_sp_distribution_set_type UNIQUE (type_key, tenant);
    END IF;


--
-- TOC entry 3576 (class 2606 OID 131396)
-- Name: sp_distribution_set_type uk_dst_name_sp_distribution_set_type; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_dst_name_sp_distribution_set_type') THEN
        ALTER TABLE ONLY public.sp_distribution_set_type
        ADD CONSTRAINT uk_dst_name_sp_distribution_set_type UNIQUE (name, tenant);
    END IF;


--
-- TOC entry 3699 (class 2606 OID 270018)
-- Name: sp_ecu_model uk_ecu_node_id_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_ecu_node_id_name') THEN
        ALTER TABLE ONLY public.sp_ecu_model
        ADD CONSTRAINT uk_ecu_node_id_name UNIQUE (ecu_model_name, ecu_node_id);
    END IF;


--
-- TOC entry 3724 (class 2606 OID 3346850)
-- Name: sp_artifact_software_module uk_module_source_target_version; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_module_source_target_version') THEN
        ALTER TABLE ONLY public.sp_artifact_software_module
        ADD CONSTRAINT uk_module_source_target_version UNIQUE (artifact_id, software_module_id, source_version, target_version);
    END IF;


--
-- TOC entry 3596 (class 2606 OID 131412)
-- Name: sp_rollout uk_rollout_sp_rollout; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_rollout_sp_rollout') THEN
        ALTER TABLE ONLY public.sp_rollout
        ADD CONSTRAINT uk_rollout_sp_rollout UNIQUE (name, tenant);
    END IF;


--
-- TOC entry 3601 (class 2606 OID 131416)
-- Name: sp_rolloutgroup uk_rolloutgroup_sp_rolloutgroup; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_rolloutgroup_sp_rolloutgroup') THEN
        ALTER TABLE ONLY public.sp_rolloutgroup
        ADD CONSTRAINT uk_rolloutgroup_sp_rolloutgroup UNIQUE (name, rollout, tenant);
    END IF;

--
-- TOC entry 3759 (class 2606 OID 3346752)
-- Name: sp_rollouts uk_rollouts_sp_rollouts; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_rollouts_sp_rollouts') THEN
        ALTER TABLE ONLY public.sp_rollouts
        ADD CONSTRAINT uk_rollouts_sp_rollouts UNIQUE (name, tenant);
    END IF;


--
-- TOC entry 3624 (class 2606 OID 250626)
-- Name: sp_target uk_serial_number_controller_id_sp_target; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_serial_number_controller_id_sp_target') THEN
        ALTER TABLE ONLY public.sp_target
        ADD CONSTRAINT uk_serial_number_controller_id_sp_target UNIQUE (controller_id, serial_number);
    END IF;


--
-- TOC entry 3609 (class 2606 OID 131422)
-- Name: sp_software_module_type uk_smt_name_sp_software_module_type; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_smt_name_sp_software_module_type') THEN
        ALTER TABLE ONLY public.sp_software_module_type
        ADD CONSTRAINT uk_smt_name_sp_software_module_type UNIQUE (name, tenant);
    END IF;


--
-- TOC entry 3611 (class 2606 OID 131424)
-- Name: sp_software_module_type uk_smt_type_key_sp_software_module_type; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_smt_type_key_sp_software_module_type') THEN
        ALTER TABLE ONLY public.sp_software_module_type
        ADD CONSTRAINT uk_smt_type_key_sp_software_module_type UNIQUE (type_key, tenant);
    END IF;


--
-- TOC entry 3732 (class 2606 OID 1546542)
-- Name: sp_esp_ecu_rollout uk_sp_esp_ecu_rollout; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_sp_esp_ecu_rollout') THEN
        ALTER TABLE ONLY public.sp_esp_ecu_rollout
        ADD CONSTRAINT uk_sp_esp_ecu_rollout UNIQUE (package_id, controller_id, rollout_id, ecu_node_addr);
    END IF;


--
-- TOC entry 3736 (class 2606 OID 1546561)
-- Name: sp_rsp_rollout uk_sp_rsp_rollout; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_sp_rsp_rollout') THEN
        ALTER TABLE ONLY public.sp_rsp_rollout
        ADD CONSTRAINT uk_sp_rsp_rollout UNIQUE (package_id, rollout_id);
    END IF;

--
-- TOC entry 5206 (class 2606 OID 40597)
-- Name: sp_software_module_format uk_sw_module_format_tenant_name; Type: CONSTRAINT; Schema: public; Owner: -
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_sw_module_format_tenant_name') THEN
        ALTER TABLE ONLY public.sp_software_module_format
        ADD CONSTRAINT uk_sw_module_format_tenant_name UNIQUE (tenant, name);
    END IF;

--
-- TOC entry 3638 (class 2606 OID 131442)
-- Name: sp_target_tag uk_targ_tag_sp_target_tag; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_targ_tag_sp_target_tag') THEN
        ALTER TABLE ONLY public.sp_target_tag
        ADD CONSTRAINT uk_targ_tag_sp_target_tag UNIQUE (name, tenant);
    END IF;


--
-- TOC entry 3654 (class 2606 OID 3346788)
-- Name: sp_target_type uk_target_type_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_target_type_name') THEN
        ALTER TABLE ONLY public.sp_target_type
        ADD CONSTRAINT uk_target_type_name UNIQUE (name, tenant);
    END IF;


--
-- TOC entry 3630 (class 2606 OID 131436)
-- Name: sp_target_filter_query uk_tenant_custom_filter_name_sp_target_filter_query; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_tenant_custom_filter_name_sp_target_filter_query') THEN
        ALTER TABLE ONLY public.sp_target_filter_query
        ADD CONSTRAINT uk_tenant_custom_filter_name_sp_target_filter_query UNIQUE (name, tenant);
    END IF;


--
-- TOC entry 3649 (class 2606 OID 131452)
-- Name: sp_tenant_configuration uk_tenant_key_sp_tenant_configuration; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_tenant_key_sp_tenant_configuration') THEN
        ALTER TABLE ONLY public.sp_tenant_configuration
        ADD CONSTRAINT uk_tenant_key_sp_tenant_configuration UNIQUE (conf_key, tenant);
    END IF;


--
-- TOC entry 3645 (class 2606 OID 131448)
-- Name: sp_tenant uk_tenantmd_tenant_sp_tenant; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_tenantmd_tenant_sp_tenant') THEN
        ALTER TABLE ONLY public.sp_tenant
        ADD CONSTRAINT uk_tenantmd_tenant_sp_tenant UNIQUE (tenant);
    END IF;


--
-- TOC entry 3686 (class 2606 OID 131830)
-- Name: sp_user_configuration uk_user_conf_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_user_conf_key') THEN
        ALTER TABLE ONLY public.sp_user_configuration
        ADD CONSTRAINT uk_user_conf_key UNIQUE (user_id, conf_key);
    END IF;


--
-- TOC entry 3690 (class 2606 OID 154348)
-- Name: sp_user_audit uk_user_role_tenant; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_user_role_tenant') THEN
        ALTER TABLE ONLY public.sp_user_audit
        ADD CONSTRAINT uk_user_role_tenant UNIQUE (user_id, role_id, tenant_id);
    END IF;


--
-- TOC entry 3668 (class 2606 OID 131731)
-- Name: sp_user_tenant uk_user_tenant_association; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_user_tenant_association') THEN
        ALTER TABLE ONLY public.sp_user_tenant
        ADD CONSTRAINT uk_user_tenant_association UNIQUE (user_id, tenant_id);
    END IF;


--
-- TOC entry 3739 (class 2606 OID 3170419)
-- Name: sp_action_status_user_acceptance unique_action_status_id; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'unique_action_status_id') THEN
        ALTER TABLE ONLY public.sp_action_status_user_acceptance
        ADD CONSTRAINT unique_action_status_id UNIQUE (action_status_id);
    END IF;


--
-- TOC entry 3754 (class 2606 OID 3346690)
-- Name: sp_artifact_module_link unique_artifact_software_module; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'unique_artifact_software_module') THEN
        ALTER TABLE ONLY public.sp_artifact_module_link
        ADD CONSTRAINT unique_artifact_software_module UNIQUE (artifact_id, software_module_id);
    END IF;


--
-- TOC entry 3672 (class 2606 OID 131773)
-- Name: sp_software_versions unique_constraint_name_sid; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'unique_constraint_name_sid') THEN
        ALTER TABLE ONLY public.sp_software_versions
        ADD CONSTRAINT unique_constraint_name_sid UNIQUE (name, software_module_id);
    END IF;


--
-- TOC entry 3674 (class 2606 OID 131775)
-- Name: sp_software_versions unique_constraint_number_sid; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'unique_constraint_number_sid') THEN
        ALTER TABLE ONLY public.sp_software_versions
        ADD CONSTRAINT unique_constraint_number_sid UNIQUE (number, software_module_id);
    END IF;


--
-- TOC entry 3703 (class 2606 OID 400250)
-- Name: sp_software_ecu_model unique_constraint_smid_ecuid; Type: CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'unique_constraint_smid_ecuid') THEN
        ALTER TABLE ONLY public.sp_software_ecu_model
        ADD CONSTRAINT unique_constraint_smid_ecuid UNIQUE (software_module_id, ecu_model_id);
    END IF;


--
-- TOC entry 3812 (class 2606 OID 623927)
-- Name: sp_deployment_log fk_act_log_action; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_act_log_action') THEN
        ALTER TABLE ONLY public.sp_deployment_log
        ADD CONSTRAINT fk_act_log_action FOREIGN KEY (action) REFERENCES public.sp_action(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3764 (class 2606 OID 131473)
-- Name: sp_action_status fk_act_stat_action; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_act_stat_action') THEN
        ALTER TABLE ONLY public.sp_action_status
        ADD CONSTRAINT fk_act_stat_action FOREIGN KEY (action) REFERENCES public.sp_action(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3821 (class 2606 OID 3170420)
-- Name: sp_action_status_user_acceptance fk_act_stat_usr_acc; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_act_stat_usr_acc') THEN
        ALTER TABLE ONLY public.sp_action_status_user_acceptance
        ADD CONSTRAINT fk_act_stat_usr_acc FOREIGN KEY (action_status_id) REFERENCES public.sp_action_status(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3760 (class 2606 OID 131453)
-- Name: sp_action fk_action_ds; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_action_ds') THEN
        ALTER TABLE ONLY public.sp_action
        ADD CONSTRAINT fk_action_ds FOREIGN KEY (distribution_set) REFERENCES public.sp_distribution_set(id) ON UPDATE RESTRICT ON DELETE RESTRICT;
    END IF;


--
-- TOC entry 3761 (class 2606 OID 131458)
-- Name: sp_action fk_action_rollout; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_action_rollout') THEN
        ALTER TABLE ONLY public.sp_action
        ADD CONSTRAINT fk_action_rollout FOREIGN KEY (rollout) REFERENCES public.sp_rollout(id) ON UPDATE RESTRICT ON DELETE RESTRICT;
    END IF;


--
-- TOC entry 3762 (class 2606 OID 131463)
-- Name: sp_action fk_action_rolloutgroup; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_action_rolloutgroup') THEN
        ALTER TABLE ONLY public.sp_action
        ADD CONSTRAINT fk_action_rolloutgroup FOREIGN KEY (rolloutgroup) REFERENCES public.sp_rolloutgroup(id) ON UPDATE RESTRICT ON DELETE RESTRICT;
    END IF;


--
-- TOC entry 3813 (class 2606 OID 765357)
-- Name: sp_artifact_software_module fk_artifact_module_link_artifact; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_artifact_module_link_artifact') THEN
        ALTER TABLE ONLY public.sp_artifact_software_module
        ADD CONSTRAINT fk_artifact_module_link_artifact FOREIGN KEY (artifact_id) REFERENCES public.sp_artifacts(id);
    END IF;


--
-- TOC entry 3825 (class 2606 OID 3346691)
-- Name: sp_artifact_module_link fk_artifact_module_link_artifact; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_artifact_module_link_artifact') THEN
        ALTER TABLE ONLY public.sp_artifact_module_link
        ADD CONSTRAINT fk_artifact_module_link_artifact FOREIGN KEY (artifact_id) REFERENCES public.sp_artifacts(id);
    END IF;


--
-- TOC entry 3814 (class 2606 OID 765362)
-- Name: sp_artifact_software_module fk_artifact_module_link_software_module; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_artifact_module_link_software_module') THEN
        ALTER TABLE ONLY public.sp_artifact_software_module
        ADD CONSTRAINT fk_artifact_module_link_software_module FOREIGN KEY (software_module_id) REFERENCES public.sp_base_software_module(id);
    END IF;


--
-- TOC entry 3826 (class 2606 OID 3346696)
-- Name: sp_artifact_module_link fk_artifact_module_link_software_module; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_artifact_module_link_software_module') THEN
        ALTER TABLE ONLY public.sp_artifact_module_link
        ADD CONSTRAINT fk_artifact_module_link_software_module FOREIGN KEY (software_module_id) REFERENCES public.sp_base_software_module(id);
    END IF;


--
-- TOC entry 3815 (class 2606 OID 765367)
-- Name: sp_artifact_software_module fk_artifact_module_link_source_version; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_artifact_module_link_source_version') THEN
        ALTER TABLE ONLY public.sp_artifact_software_module
        ADD CONSTRAINT fk_artifact_module_link_source_version FOREIGN KEY (source_version) REFERENCES public.sp_software_versions(id);
    END IF;


--
-- TOC entry 3827 (class 2606 OID 3346701)
-- Name: sp_artifact_module_link fk_artifact_module_link_source_version; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_artifact_module_link_source_version') THEN
        ALTER TABLE ONLY public.sp_artifact_module_link
        ADD CONSTRAINT fk_artifact_module_link_source_version FOREIGN KEY (source_version) REFERENCES public.sp_software_versions(id);
    END IF;


--
-- TOC entry 3816 (class 2606 OID 765372)
-- Name: sp_artifact_software_module fk_artifact_module_link_target_version; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_artifact_module_link_target_version') THEN
        ALTER TABLE ONLY public.sp_artifact_software_module
        ADD CONSTRAINT fk_artifact_module_link_target_version FOREIGN KEY (target_version) REFERENCES public.sp_software_versions(id);
    END IF;


--
-- TOC entry 3828 (class 2606 OID 3346706)
-- Name: sp_artifact_module_link fk_artifact_module_link_target_version; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_artifact_module_link_target_version') THEN
        ALTER TABLE ONLY public.sp_artifact_module_link
        ADD CONSTRAINT fk_artifact_module_link_target_version FOREIGN KEY (target_version) REFERENCES public.sp_software_versions(id);
    END IF;


--
-- TOC entry 3766 (class 2606 OID 131483)
-- Name: sp_artifact fk_assigned_sm; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_assigned_sm') THEN
        ALTER TABLE ONLY public.sp_artifact
        ADD CONSTRAINT fk_assigned_sm FOREIGN KEY (software_module) REFERENCES public.sp_base_software_module(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3773 (class 2606 OID 131498)
-- Name: sp_ds_dstag fk_ds_dstag_ds; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ds_dstag_ds') THEN
        ALTER TABLE ONLY public.sp_ds_dstag
        ADD CONSTRAINT fk_ds_dstag_ds FOREIGN KEY (ds) REFERENCES public.sp_distribution_set(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3774 (class 2606 OID 131503)
-- Name: sp_ds_dstag fk_ds_dstag_tag; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ds_dstag_tag') THEN
        ALTER TABLE ONLY public.sp_ds_dstag
        ADD CONSTRAINT fk_ds_dstag_tag FOREIGN KEY (tag) REFERENCES public.sp_distributionset_tag(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3772 (class 2606 OID 131493)
-- Name: sp_distribution_set fk_ds_dstype_ds; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ds_dstype_ds') THEN
        ALTER TABLE ONLY public.sp_distribution_set
        ADD CONSTRAINT fk_ds_dstype_ds FOREIGN KEY (ds_id) REFERENCES public.sp_distribution_set_type(id) ON UPDATE RESTRICT ON DELETE RESTRICT;
    END IF;


--
-- TOC entry 3776 (class 2606 OID 131513)
-- Name: sp_ds_module fk_ds_module_ds; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ds_module_ds') THEN
        ALTER TABLE ONLY public.sp_ds_module
        ADD CONSTRAINT fk_ds_module_ds FOREIGN KEY (ds_id) REFERENCES public.sp_distribution_set(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3823 (class 2606 OID 3346657)
-- Name: sp_ds_module_basic_table fk_ds_module_ds_basic; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ds_module_ds_basic') THEN
        ALTER TABLE ONLY public.sp_ds_module_basic_table
        ADD CONSTRAINT fk_ds_module_ds_basic FOREIGN KEY (ds_id) REFERENCES public.sp_distribution_set(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3777 (class 2606 OID 131518)
-- Name: sp_ds_module fk_ds_module_module; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ds_module_module') THEN
        ALTER TABLE ONLY public.sp_ds_module
        ADD CONSTRAINT fk_ds_module_module FOREIGN KEY (module_id) REFERENCES public.sp_base_software_module(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3824 (class 2606 OID 3346662)
-- Name: sp_ds_module_basic_table fk_ds_module_module_basic; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ds_module_module_basic') THEN
        ALTER TABLE ONLY public.sp_ds_module_basic_table
        ADD CONSTRAINT fk_ds_module_module_basic FOREIGN KEY (module_id) REFERENCES public.sp_base_software_module(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3779 (class 2606 OID 131523)
-- Name: sp_ds_type_element fk_ds_type_element_element; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ds_type_element_element') THEN
        ALTER TABLE ONLY public.sp_ds_type_element
        ADD CONSTRAINT fk_ds_type_element_element FOREIGN KEY (distribution_set_type) REFERENCES public.sp_distribution_set_type(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3780 (class 2606 OID 131528)
-- Name: sp_ds_type_element fk_ds_type_element_smtype; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ds_type_element_smtype') THEN
        ALTER TABLE ONLY public.sp_ds_type_element
        ADD CONSTRAINT fk_ds_type_element_smtype FOREIGN KEY (software_module_type) REFERENCES public.sp_software_module_type(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3807 (class 2606 OID 376121)
-- Name: sp_vehicle_ecu fk_ecu_model; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ecu_model') THEN
        ALTER TABLE ONLY public.sp_vehicle_ecu
        ADD CONSTRAINT fk_ecu_model FOREIGN KEY (ecu_model_id) REFERENCES public.sp_ecu_model(id);
    END IF;


--
-- TOC entry 3806 (class 2606 OID 706936)
-- Name: sp_ecu_model fk_ecu_model_type; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ecu_model_type') THEN
        ALTER TABLE ONLY public.sp_ecu_model
        ADD CONSTRAINT fk_ecu_model_type FOREIGN KEY (ecu_model_type) REFERENCES public.sp_ecu_model_type(id);
    END IF;


--
-- TOC entry 3817 (class 2606 OID 1546543)
-- Name: sp_esp_ecu_rollout fk_esp_package_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_esp_package_id') THEN
        ALTER TABLE ONLY public.sp_esp_ecu_rollout
        ADD CONSTRAINT fk_esp_package_id FOREIGN KEY (package_id) REFERENCES public.sp_esp(id);
    END IF;


--
-- TOC entry 3818 (class 2606 OID 1546548)
-- Name: sp_esp_ecu_rollout fk_esp_rollout_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_esp_rollout_id') THEN
        ALTER TABLE ONLY public.sp_esp_ecu_rollout
        ADD CONSTRAINT fk_esp_rollout_id FOREIGN KEY (rollout_id) REFERENCES public.sp_rollout(id);
    END IF;


--
-- TOC entry 3822 (class 2606 OID 3225714)
-- Name: sp_general_feedback fk_feedback_target; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_feedback_target') THEN
        ALTER TABLE ONLY public.sp_general_feedback
        ADD CONSTRAINT fk_feedback_target FOREIGN KEY (target_id) REFERENCES public.sp_target(id);
    END IF;


--
-- TOC entry 3790 (class 2606 OID 131573)
-- Name: sp_target_filter_query fk_filter_auto_assign_ds; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_filter_auto_assign_ds') THEN
        ALTER TABLE ONLY public.sp_target_filter_query
        ADD CONSTRAINT fk_filter_auto_assign_ds FOREIGN KEY (auto_assign_distribution_set) REFERENCES public.sp_distribution_set(id) ON UPDATE RESTRICT ON DELETE SET NULL;
    END IF;


--
-- TOC entry 3811 (class 2606 OID 514843)
-- Name: sp_target_inventory fk_inventory_target; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_inventory_target') THEN
        ALTER TABLE ONLY public.sp_target_inventory
        ADD CONSTRAINT fk_inventory_target FOREIGN KEY (target_id) REFERENCES public.sp_target(id);
    END IF;


--
-- TOC entry 3775 (class 2606 OID 131508)
-- Name: sp_ds_metadata fk_metadata_ds; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_metadata_ds') THEN
        ALTER TABLE ONLY public.sp_ds_metadata
        ADD CONSTRAINT fk_metadata_ds FOREIGN KEY (ds_id) REFERENCES public.sp_distribution_set(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3785 (class 2606 OID 131553)
-- Name: sp_sw_metadata fk_metadata_sw; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_metadata_sw') THEN
        ALTER TABLE ONLY public.sp_sw_metadata
        ADD CONSTRAINT fk_metadata_sw FOREIGN KEY (sw_id) REFERENCES public.sp_base_software_module(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3791 (class 2606 OID 131578)
-- Name: sp_target_metadata fk_metadata_target; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_metadata_target') THEN
        ALTER TABLE ONLY public.sp_target_metadata
        ADD CONSTRAINT fk_metadata_target FOREIGN KEY (target_id) REFERENCES public.sp_target(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3769 (class 2606 OID 131652)
-- Name: sp_base_software_module fk_module_format; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_module_format') THEN
        ALTER TABLE ONLY public.sp_base_software_module
        ADD CONSTRAINT fk_module_format FOREIGN KEY (module_format) REFERENCES public.sp_software_module_format(id) ON UPDATE RESTRICT ON DELETE RESTRICT;
    END IF;


--
-- TOC entry 3770 (class 2606 OID 131488)
-- Name: sp_base_software_module fk_module_type; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_module_type') THEN
        ALTER TABLE ONLY public.sp_base_software_module
        ADD CONSTRAINT fk_module_type FOREIGN KEY (module_type) REFERENCES public.sp_software_module_type(id) ON UPDATE RESTRICT ON DELETE RESTRICT;
    END IF;


--
-- TOC entry 3781 (class 2606 OID 131533)
-- Name: sp_rollout fk_rollout_ds; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_rollout_ds') THEN
        ALTER TABLE ONLY public.sp_rollout
        ADD CONSTRAINT fk_rollout_ds FOREIGN KEY (distribution_set) REFERENCES public.sp_distribution_set(id) ON UPDATE RESTRICT ON DELETE RESTRICT;
    END IF;


--
-- TOC entry 3782 (class 2606 OID 131538)
-- Name: sp_rolloutgroup fk_rolloutgroup_rollout; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_rolloutgroup_rollout') THEN
        ALTER TABLE ONLY public.sp_rolloutgroup
        ADD CONSTRAINT fk_rolloutgroup_rollout FOREIGN KEY (rollout) REFERENCES public.sp_rollout(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3783 (class 2606 OID 131543)
-- Name: sp_rollouttargetgroup fk_rollouttargetgroup_rolloutgroup; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_rollouttargetgroup_rolloutgroup') THEN
        ALTER TABLE ONLY public.sp_rollouttargetgroup
        ADD CONSTRAINT fk_rollouttargetgroup_rolloutgroup FOREIGN KEY (rolloutgroup_id) REFERENCES public.sp_rolloutgroup(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3898 (class 2606 OID 131547)
-- Name: sp_action_artifact fk_action; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_action') THEN
        ALTER TABLE ONLY public.sp_action_artifact
        ADD CONSTRAINT fk_action FOREIGN KEY (action_id) REFERENCES sp_action(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3784 (class 2606 OID 131548)
-- Name: sp_rollouttargetgroup fk_rollouttargetgroup_target; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_rollouttargetgroup_target') THEN
        ALTER TABLE ONLY public.sp_rollouttargetgroup
        ADD CONSTRAINT fk_rollouttargetgroup_target FOREIGN KEY (target_id) REFERENCES public.sp_target(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3899 (class 2606 OID 131549)
-- Name: sp_action_artifact fk_artifact; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_artifact') THEN
        ALTER TABLE ONLY public.sp_action_artifact
        ADD CONSTRAINT fk_artifact FOREIGN KEY (artifact_id) REFERENCES sp_artifacts(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3819 (class 2606 OID 1546562)
-- Name: sp_rsp_rollout fk_rsp_package_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_rsp_package_id') THEN
        ALTER TABLE ONLY public.sp_rsp_rollout
        ADD CONSTRAINT fk_rsp_package_id FOREIGN KEY (package_id) REFERENCES public.sp_rsp(id);
    END IF;


--
-- TOC entry 3820 (class 2606 OID 1546567)
-- Name: sp_rsp_rollout fk_rsp_rollout_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_rsp_rollout_id') THEN
        ALTER TABLE ONLY public.sp_rsp_rollout
        ADD CONSTRAINT fk_rsp_rollout_id FOREIGN KEY (rollout_id) REFERENCES public.sp_rollout(id);
    END IF;


--
-- TOC entry 3771 (class 2606 OID 901767)
-- Name: sp_base_software_module fk_software_installer_type; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_software_installer_type') THEN
        ALTER TABLE ONLY public.sp_base_software_module
        ADD CONSTRAINT fk_software_installer_type FOREIGN KEY (software_installer_type) REFERENCES public.sp_software_installer_type(id);
    END IF;


--
-- TOC entry 3797 (class 2606 OID 131671)
-- Name: sp_target_software fk_software_target; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_software_target') THEN
        ALTER TABLE ONLY public.sp_target_software
        ADD CONSTRAINT fk_software_target FOREIGN KEY (target_id) REFERENCES public.sp_target(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3778 (class 2606 OID 131750)
-- Name: sp_ds_module fk_software_version; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_software_version') THEN
        ALTER TABLE ONLY public.sp_ds_module
        ADD CONSTRAINT fk_software_version FOREIGN KEY (software_version_id) REFERENCES public.sp_software_versions(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3800 (class 2606 OID 131743)
-- Name: sp_software_versions fk_sp_software_versions; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sp_software_versions') THEN
        ALTER TABLE ONLY public.sp_software_versions
        ADD CONSTRAINT fk_sp_software_versions FOREIGN KEY (software_module_id) REFERENCES public.sp_base_software_module(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3765 (class 2606 OID 131478)
-- Name: sp_action_status_messages fk_stat_msg_act_stat; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_stat_msg_act_stat') THEN
        ALTER TABLE ONLY public.sp_action_status_messages
        ADD CONSTRAINT fk_stat_msg_act_stat FOREIGN KEY (action_status_id) REFERENCES public.sp_action_status(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3763 (class 2606 OID 131468)
-- Name: sp_action fk_targ_act_hist_targ; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_targ_act_hist_targ') THEN
        ALTER TABLE ONLY public.sp_action
        ADD CONSTRAINT fk_targ_act_hist_targ FOREIGN KEY (target) REFERENCES public.sp_target(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3789 (class 2606 OID 131568)
-- Name: sp_target_attributes fk_targ_attrib_target; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_targ_attrib_target') THEN
        ALTER TABLE ONLY public.sp_target_attributes
        ADD CONSTRAINT fk_targ_attrib_target FOREIGN KEY (target_id) REFERENCES public.sp_target(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3792 (class 2606 OID 131583)
-- Name: sp_target_target_tag fk_targ_targtag_tag; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_targ_targtag_tag') THEN
        ALTER TABLE ONLY public.sp_target_target_tag
        ADD CONSTRAINT fk_targ_targtag_tag FOREIGN KEY (tag) REFERENCES public.sp_target_tag(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3793 (class 2606 OID 131588)
-- Name: sp_target_target_tag fk_targ_targtag_target; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_targ_targtag_target') THEN
        ALTER TABLE ONLY public.sp_target_target_tag
        ADD CONSTRAINT fk_targ_targtag_target FOREIGN KEY (target) REFERENCES public.sp_target(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3786 (class 2606 OID 131558)
-- Name: sp_target fk_target_assign_ds; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_target_assign_ds') THEN
        ALTER TABLE ONLY public.sp_target
        ADD CONSTRAINT fk_target_assign_ds FOREIGN KEY (assigned_distribution_set) REFERENCES public.sp_distribution_set(id) ON UPDATE RESTRICT ON DELETE RESTRICT;
    END IF;


--
-- TOC entry 3787 (class 2606 OID 131563)
-- Name: sp_target fk_target_inst_ds; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_target_inst_ds') THEN
        ALTER TABLE ONLY public.sp_target
        ADD CONSTRAINT fk_target_inst_ds FOREIGN KEY (installed_distribution_set) REFERENCES public.sp_distribution_set(id) ON UPDATE RESTRICT ON DELETE RESTRICT;
    END IF;


--
-- TOC entry 3788 (class 2606 OID 131615)
-- Name: sp_target fk_target_relation_target_type; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_target_relation_target_type') THEN
        ALTER TABLE ONLY public.sp_target
        ADD CONSTRAINT fk_target_relation_target_type FOREIGN KEY (target_type) REFERENCES public.sp_target_type(id) ON UPDATE RESTRICT ON DELETE SET NULL;
    END IF;


--
-- TOC entry 3795 (class 2606 OID 131625)
-- Name: sp_target_type_ds_type_relation fk_target_type_relation_ds_type; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_target_type_relation_ds_type') THEN
        ALTER TABLE ONLY public.sp_target_type_ds_type_relation
        ADD CONSTRAINT fk_target_type_relation_ds_type FOREIGN KEY (distribution_set_type) REFERENCES public.sp_distribution_set_type(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3796 (class 2606 OID 131620)
-- Name: sp_target_type_ds_type_relation fk_target_type_relation_target_type; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_target_type_relation_target_type') THEN
        ALTER TABLE ONLY public.sp_target_type_ds_type_relation
        ADD CONSTRAINT fk_target_type_relation_target_type FOREIGN KEY (target_type) REFERENCES public.sp_target_type(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3794 (class 2606 OID 131593)
-- Name: sp_tenant fk_tenant_md_default_ds_type; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_tenant_md_default_ds_type') THEN
        ALTER TABLE ONLY public.sp_tenant
        ADD CONSTRAINT fk_tenant_md_default_ds_type FOREIGN KEY (default_ds_type) REFERENCES public.sp_distribution_set_type(id) ON UPDATE RESTRICT ON DELETE RESTRICT;
    END IF;


--
-- TOC entry 3803 (class 2606 OID 154354)
-- Name: sp_user_audit fk_user_audit_role; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_user_audit_role') THEN
        ALTER TABLE ONLY public.sp_user_audit
        ADD CONSTRAINT fk_user_audit_role FOREIGN KEY (role_id) REFERENCES public.sp_role(id) ON UPDATE RESTRICT ON DELETE RESTRICT;
    END IF;


--
-- TOC entry 3804 (class 2606 OID 154359)
-- Name: sp_user_audit fk_user_audit_tenant; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_user_audit_tenant') THEN
        ALTER TABLE ONLY public.sp_user_audit
        ADD CONSTRAINT fk_user_audit_tenant FOREIGN KEY (tenant_id) REFERENCES public.sp_tenant(id) ON UPDATE RESTRICT ON DELETE RESTRICT;
    END IF;


--
-- TOC entry 3805 (class 2606 OID 154349)
-- Name: sp_user_audit fk_user_audit_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_user_audit_user') THEN
        ALTER TABLE ONLY public.sp_user_audit
        ADD CONSTRAINT fk_user_audit_user FOREIGN KEY (user_id) REFERENCES public.sp_user(id) ON UPDATE RESTRICT ON DELETE RESTRICT;
    END IF;


--
-- TOC entry 3802 (class 2606 OID 131831)
-- Name: sp_user_configuration fk_user_configuration_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_user_configuration_user') THEN
        ALTER TABLE ONLY public.sp_user_configuration
        ADD CONSTRAINT fk_user_configuration_user FOREIGN KEY (user_id) REFERENCES public.sp_user(id) ON UPDATE RESTRICT ON DELETE RESTRICT;
    END IF;


--
-- TOC entry 3798 (class 2606 OID 131720)
-- Name: sp_user_tenant fk_user_tenant_tenant; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_user_tenant_tenant') THEN
        ALTER TABLE ONLY public.sp_user_tenant
        ADD CONSTRAINT fk_user_tenant_tenant FOREIGN KEY (tenant_id) REFERENCES public.sp_tenant(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3799 (class 2606 OID 131725)
-- Name: sp_user_tenant fk_user_tenant_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_user_tenant_user') THEN
        ALTER TABLE ONLY public.sp_user_tenant
        ADD CONSTRAINT fk_user_tenant_user FOREIGN KEY (user_id) REFERENCES public.sp_user(id) ON UPDATE RESTRICT ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3808 (class 2606 OID 3346814)
-- Name: sp_vehicle_ecu fk_vehicle_model; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_vehicle_model') THEN
        ALTER TABLE ONLY public.sp_vehicle_ecu
        ADD CONSTRAINT fk_vehicle_model FOREIGN KEY (vehicle_model_id) REFERENCES public.sp_vehicle_model(id) ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3767 (class 2606 OID 131779)
-- Name: sp_artifact fk_version_relation_source; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_version_relation_source') THEN
        ALTER TABLE ONLY public.sp_artifact
        ADD CONSTRAINT fk_version_relation_source FOREIGN KEY (source_version) REFERENCES public.sp_software_versions(id) ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3768 (class 2606 OID 131784)
-- Name: sp_artifact fk_version_relation_target; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_version_relation_target') THEN
        ALTER TABLE ONLY public.sp_artifact
        ADD CONSTRAINT fk_version_relation_target FOREIGN KEY (target_version) REFERENCES public.sp_software_versions(id) ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3801 (class 2606 OID 131789)
-- Name: sp_software_versions fk_version_sm; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_version_sm') THEN
        ALTER TABLE ONLY public.sp_software_versions
        ADD CONSTRAINT fk_version_sm FOREIGN KEY (software_module_id) REFERENCES public.sp_base_software_module(id) ON DELETE CASCADE;
    END IF;


--
-- TOC entry 3809 (class 2606 OID 400256)
-- Name: sp_software_ecu_model sp_software_ecu_model_ecu_model_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_software_ecu_model_ecu_model_id_fkey') THEN
        ALTER TABLE ONLY public.sp_software_ecu_model
        ADD CONSTRAINT sp_software_ecu_model_ecu_model_id_fkey FOREIGN KEY (ecu_model_id) REFERENCES public.sp_ecu_model(id);
    END IF;


--
-- TOC entry 3810 (class 2606 OID 400251)
-- Name: sp_software_ecu_model sp_software_ecu_model_software_module_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sp_software_ecu_model_software_module_id_fkey') THEN
        ALTER TABLE ONLY public.sp_software_ecu_model
        ADD CONSTRAINT sp_software_ecu_model_software_module_id_fkey FOREIGN KEY (software_module_id) REFERENCES public.sp_base_software_module(id);
    END IF;

END $$;

--
-- TOC entry 3597 (class 1259 OID 131367)
-- Name: fk_rolloutgroup_rolloutgroup_sp_rolloutgroup; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  fk_rolloutgroup_rolloutgroup_sp_rolloutgroup ON public.sp_rolloutgroup USING btree (parent_id);


--
-- TOC entry 3718 (class 1259 OID 765377)
-- Name: idx_sp_artifact_module_link_artifact_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  idx_sp_artifact_module_link_artifact_id ON public.sp_artifact_software_module USING btree (artifact_id);


--
-- TOC entry 3719 (class 1259 OID 765379)
-- Name: idx_sp_artifact_module_link_artifact_module; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  idx_sp_artifact_module_link_artifact_module ON public.sp_artifact_software_module USING btree (artifact_id, software_module_id);


--
-- TOC entry 3720 (class 1259 OID 765378)
-- Name: idx_sp_artifact_module_link_software_module_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  idx_sp_artifact_module_link_software_module_id ON public.sp_artifact_software_module USING btree (software_module_id);

--
-- TOC entry 3539 (class 1259 OID 131348)
-- Name: sp_idx_action_01_sp_action; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_action_01_sp_action ON public.sp_action USING btree (tenant, distribution_set);


--
-- TOC entry 3540 (class 1259 OID 131349)
-- Name: sp_idx_action_02_sp_action; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_action_02_sp_action ON public.sp_action USING btree (tenant, target, active);


--
-- TOC entry 3541 (class 1259 OID 131350)
-- Name: sp_idx_action_external_ref_sp_action; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_action_external_ref_sp_action ON public.sp_action USING btree (external_ref);


--
-- TOC entry 3542 (class 1259 OID 131351)
-- Name: sp_idx_action_prim_sp_action; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_action_prim_sp_action ON public.sp_action USING btree (tenant, id);


--
-- TOC entry 3545 (class 1259 OID 131352)
-- Name: sp_idx_action_status_02_sp_action_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_action_status_02_sp_action_status ON public.sp_action_status USING btree (tenant, action, status);


--
-- TOC entry 3546 (class 1259 OID 131633)
-- Name: sp_idx_action_status_03; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_action_status_03 ON public.sp_action_status USING btree (tenant, code);


--
-- TOC entry 3548 (class 1259 OID 131354)
-- Name: sp_idx_action_status_msgs_01_sp_action_status_messages; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_action_status_msgs_01_sp_action_status_messages ON public.sp_action_status_messages USING btree (action_status_id);


--
-- TOC entry 3547 (class 1259 OID 131353)
-- Name: sp_idx_action_status_prim_sp_action_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_action_status_prim_sp_action_status ON public.sp_action_status USING btree (tenant, id);


--
-- TOC entry 3737 (class 1259 OID 3170425)
-- Name: sp_idx_action_status_user_acceptance_01; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_action_status_user_acceptance_01 ON public.sp_action_status_user_acceptance USING btree (action_status_id);

--
-- TOC entry 3737 (class 1259 OID 3170425)
-- Name: sp_idx_sp_signing_certificate_configuration_01; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_sp_signing_certificate_configuration_01 ON public.sp_signing_certificate_configuration USING btree (ecu_id_issuer);

--
-- TOC entry 3551 (class 1259 OID 131355)
-- Name: sp_idx_artifact_01_sp_artifact; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_artifact_01_sp_artifact ON public.sp_artifact USING btree (tenant, software_module);


--
-- TOC entry 3552 (class 1259 OID 131356)
-- Name: sp_idx_artifact_02_sp_artifact; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_artifact_02_sp_artifact ON public.sp_artifact USING btree (tenant, sha1_hash);


--
-- TOC entry 3553 (class 1259 OID 131357)
-- Name: sp_idx_artifact_prim_sp_artifact; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_artifact_prim_sp_artifact ON public.sp_artifact USING btree (tenant, id);


--
-- TOC entry 3556 (class 1259 OID 131358)
-- Name: sp_idx_base_sw_module_01_sp_base_software_module; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_base_sw_module_01_sp_base_software_module ON public.sp_base_software_module USING btree (tenant, deleted, name, version);


--
-- TOC entry 3557 (class 1259 OID 131359)
-- Name: sp_idx_base_sw_module_02_sp_base_software_module; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_base_sw_module_02_sp_base_software_module ON public.sp_base_software_module USING btree (tenant, deleted, module_type);


--
-- TOC entry 3558 (class 1259 OID 131360)
-- Name: sp_idx_base_sw_module_prim_sp_base_software_module; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_base_sw_module_prim_sp_base_software_module ON public.sp_base_software_module USING btree (tenant, id);


--
-- TOC entry 3563 (class 1259 OID 131361)
-- Name: sp_idx_distribution_set_01_sp_distribution_set; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_distribution_set_01_sp_distribution_set ON public.sp_distribution_set USING btree (tenant, deleted, complete);


--
-- TOC entry 3564 (class 1259 OID 131362)
-- Name: sp_idx_distribution_set_prim_sp_distribution_set; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_distribution_set_prim_sp_distribution_set ON public.sp_distribution_set USING btree (tenant, id);


--
-- TOC entry 3579 (class 1259 OID 131365)
-- Name: sp_idx_distribution_set_tag_01_sp_distributionset_tag; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_distribution_set_tag_01_sp_distributionset_tag ON public.sp_distributionset_tag USING btree (tenant, name);


--
-- TOC entry 3580 (class 1259 OID 131366)
-- Name: sp_idx_distribution_set_tag_prim_sp_distributionset_tag; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_distribution_set_tag_prim_sp_distributionset_tag ON public.sp_distributionset_tag USING btree (tenant, id);


--
-- TOC entry 3571 (class 1259 OID 131363)
-- Name: sp_idx_distribution_set_type_01_sp_distribution_set_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_distribution_set_type_01_sp_distribution_set_type ON public.sp_distribution_set_type USING btree (tenant, deleted);


--
-- TOC entry 3572 (class 1259 OID 131364)
-- Name: sp_idx_distribution_set_type_prim_sp_distribution_set_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_distribution_set_type_prim_sp_distribution_set_type ON public.sp_distribution_set_type USING btree (tenant, id);


--
-- TOC entry 3744 (class 1259 OID 3243213)
-- Name: sp_idx_file_processing_error_log_log_type_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_file_processing_error_log_log_type_id ON public.sp_file_processing_error_log USING btree (log_type_id);


--
-- TOC entry 3593 (class 1259 OID 131776)
-- Name: sp_idx_rollout_end_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_rollout_end_at ON public.sp_rollout USING btree (end_at);


--
-- TOC entry 3594 (class 1259 OID 131630)
-- Name: sp_idx_rollout_status_tenant; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_rollout_status_tenant ON public.sp_rollout USING btree (tenant, status);


--
-- TOC entry 3757 (class 1259 OID 3346753)
-- Name: sp_idx_rollouts_status_tenant; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_rollouts_status_tenant ON public.sp_rollouts USING btree (tenant, status);


--
-- TOC entry 3606 (class 1259 OID 131368)
-- Name: sp_idx_software_module_type_01_sp_software_module_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_software_module_type_01_sp_software_module_type ON public.sp_software_module_type USING btree (tenant, deleted);


--
-- TOC entry 3607 (class 1259 OID 131369)
-- Name: sp_idx_software_module_type_prim_sp_software_module_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_software_module_type_prim_sp_software_module_type ON public.sp_software_module_type USING btree (tenant, id);


--
-- TOC entry 3616 (class 1259 OID 131370)
-- Name: sp_idx_target_01_sp_target; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_target_01_sp_target ON public.sp_target USING btree (tenant, name, assigned_distribution_set);


--
-- TOC entry 3617 (class 1259 OID 131371)
-- Name: sp_idx_target_03_sp_target; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_target_03_sp_target ON public.sp_target USING btree (tenant, controller_id, assigned_distribution_set);


--
-- TOC entry 3618 (class 1259 OID 131372)
-- Name: sp_idx_target_04_sp_target; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_target_04_sp_target ON public.sp_target USING btree (tenant, created_at);


--
-- TOC entry 3619 (class 1259 OID 131599)
-- Name: sp_idx_target_05; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_target_05 ON public.sp_target USING btree (tenant, last_modified_at);


--
-- TOC entry 3620 (class 1259 OID 131373)
-- Name: sp_idx_target_prim_sp_target; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_target_prim_sp_target ON public.sp_target USING btree (tenant, id);


--
-- TOC entry 3635 (class 1259 OID 131374)
-- Name: sp_idx_target_tag_01_sp_target_tag; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_target_tag_01_sp_target_tag ON public.sp_target_tag USING btree (tenant, name);


--
-- TOC entry 3636 (class 1259 OID 131375)
-- Name: sp_idx_target_tag_prim_sp_target_tag; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_target_tag_prim_sp_target_tag ON public.sp_target_tag USING btree (tenant, id);


--
-- TOC entry 3652 (class 1259 OID 131614)
-- Name: sp_idx_target_type_prim; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_target_type_prim ON public.sp_target_type USING btree (tenant, id);


--
-- TOC entry 3643 (class 1259 OID 131376)
-- Name: sp_idx_tenant_prim_sp_tenant; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_tenant_prim_sp_tenant ON public.sp_tenant USING btree (tenant, id);


--
-- TOC entry 3691 (class 1259 OID 250613)
-- Name: sp_idx_vehicle_name; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX IF NOT EXISTS  sp_idx_vehicle_name ON public.sp_vehicle_model USING btree (name);


-- Completed on 2025-03-26 22:29:24

--
-- PostgreSQL database dump complete
--

INSERT INTO sp_ecu_model_type (created_at, created_by, last_modified_at, last_modified_by, optlock_revision, name, description, deleted)
SELECT EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, 'ST', 'Small Target', false
    WHERE NOT EXISTS (
    SELECT 1 FROM sp_ecu_model_type WHERE name = 'ST'
)
UNION ALL
SELECT EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, 'BT', 'Big Target', false
    WHERE NOT EXISTS (
    SELECT 1 FROM sp_ecu_model_type WHERE name = 'BT'
)
UNION ALL
SELECT EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, 'BTND', 'Big Target without Downloader', false
    WHERE NOT EXISTS (
    SELECT 1 FROM sp_ecu_model_type WHERE name = 'BTND'
)
UNION ALL
SELECT EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, 'BTMHND', 'Big Target Multi Hardware without Downloader', false
    WHERE NOT EXISTS (
    SELECT 1 FROM sp_ecu_model_type WHERE name = 'BTMHND'
)
UNION ALL
SELECT EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, 'BTD', 'Big Target with Downloader', false
    WHERE NOT EXISTS (
    SELECT 1 FROM sp_ecu_model_type WHERE name = 'BTD'
)
UNION ALL
SELECT EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, 'BTMSD', 'Big Target Multi Software with Downloader', false
    WHERE NOT EXISTS (
    SELECT 1 FROM sp_ecu_model_type WHERE name = 'BTMSD'
)
UNION ALL
SELECT EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, 'BTMHD', 'Big Target Multi Hardware with Downloader', false
    WHERE NOT EXISTS (
    SELECT 1 FROM sp_ecu_model_type WHERE name = 'BTMHD'
)
UNION ALL
SELECT EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, 'OM', 'OTA Master', false
    WHERE NOT EXISTS (
    SELECT 1 FROM sp_ecu_model_type WHERE name = 'OM'
)
UNION ALL
SELECT EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, 'SU', 'Self Updater', false
    WHERE NOT EXISTS (
    SELECT 1 FROM sp_ecu_model_type WHERE name = 'SU'
);

INSERT INTO sp_software_installer_type (created_at, created_by, last_modified_at, last_modified_by, optlock_revision, name, description, deleted)
SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '0', 'Firmware (for FUMO installations)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '0')
UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '9', 'Firmware (for SCOMO installations)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '9')
          UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '10', 'Android application package file', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '10')
                    UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '11', 'Android embedded application package file', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '11')
                              UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '104', 'Smart Delta: Head Unit (104)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '104')
                                        UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '200', 'Generic installer type (200), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '200')
                                                  UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '201', 'Generic installer type (201), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '201')
                                                            UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '202', 'Generic installer type (202), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '202')
                                                                      UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '203', 'Generic installer type (203), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '203')
                                                                                UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '204', 'Generic installer type (204), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '204')
                                                                                          UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '205', 'Generic installer type (205), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '205')
                                                                                                    UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '206', 'Generic installer type (206), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '206')
                                                                                                              UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '207', 'Generic installer type (207), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '207')
                                                                                                                        UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '208', 'Generic installer type (208), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '208')
                                                                                                                                  UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '209', 'Generic installer type (209), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '209')
                                                                                                                                            UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '210', 'Generic installer type (210), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '210')
                                                                                                                                                      UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '211', 'Generic installer type (211), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '211')
                                                                                                                                                                UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '212', 'Generic installer type (212), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '212')
                                                                                                                                                                          UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '213', 'Generic installer type (213), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '213')
                                                                                                                                                                                    UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '214', 'Generic installer type (214), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '214')
                                                                                                                                                                                              UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '215', 'Generic installer type (215), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '215')
                                                                                                                                                                                                        UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '216', 'Generic installer type (216), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '216')
                                                                                                                                                                                                                  UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '217', 'Generic installer type (217), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '217')
                                                                                                                                                                                                                            UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '218', 'Generic installer type (218), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '218')
                                                                                                                                                                                                                                      UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '219', 'Generic installer type (219), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '219')
                                                                                                                                                                                                                                                UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '220', 'Generic installer type (220), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '220')
                                                                                                                                                                                                                                                          UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '221', 'Generic installer type (221), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '221')
                                                                                                                                                                                                                                                                    UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '222', 'Generic installer type (222), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '222')
                                                                                                                                                                                                                                                                              UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '223', 'Generic installer type (223), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '223')
                                                                                                                                                                                                                                                                                        UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '224', 'Generic installer type (224), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '224')
                                                                                                                                                                                                                                                                                                  UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '225', 'Generic installer type (225), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '225')
                                                                                                                                                                                                                                                                                                            UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '226', 'Generic installer type (226), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '226')
                                                                                                                                                                                                                                                                                                                      UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '227', 'Generic installer type (227), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '227')
                                                                                                                                                                                                                                                                                                                                UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '228', 'Generic installer type (228), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '228')
                                                                                                                                                                                                                                                                                                                                          UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '229', 'Generic installer type (229), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '229')
                                                                                                                                                                                                                                                                                                                                                    UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '230', 'Generic installer type (230), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '230')
                                                                                                                                                                                                                                                                                                                                                              UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '252', 'Smart Delta: Generic installer type (252)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '252')
                                                                                                                                                                                                                                                                                                                                                                        UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '253', 'Smart Delta: Generic installer type (253)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '253')
                                                                                                                                                                                                                                                                                                                                                                                  UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '298', 'Smart Delta: Generic installer type (298)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '298')
                                                                                                                                                                                                                                                                                                                                                                                            UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '299', 'Smart Delta: Generic installer type (299)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '299')
                                                                                                                                                                                                                                                                                                                                                                                                      UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '310', 'Smart Delta: ECU (310)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '310')
                                                                                                                                                                                                                                                                                                                                                                                                                UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1310', 'Smart Delta: ECU (1310)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1310')
                                                                                                                                                                                                                                                                                                                                                                                                                          UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1311', 'Smart Delta: ECU (1311)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1311')
                                                                                                                                                                                                                                                                                                                                                                                                                                    UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '311', 'Smart Delta: ECU (311)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '311')
                                                                                                                                                                                                                                                                                                                                                                                                                                              UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '312', 'Smart Delta: ECU (312)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '312')
                                                                                                                                                                                                                                                                                                                                                                                                                                                        UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '313', 'Smart Delta: ECU (313)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '313')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                  UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '314', 'Smart Delta: ECU (314)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '314')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                            UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '315', 'Smart Delta: ECU (315)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '315')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '323', 'Smart Delta: ECU FUSE (323)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '323')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '324', 'Smart Delta: ECU FUSE (324)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '324')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '325', 'Smart Delta: ECU FUSE (325)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '325')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '326', 'Smart Delta: ECU FUSE (326)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '326')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '327', 'Smart Delta: ECU FUSE (327)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '327')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '328', 'Smart Delta: ECU FUSE (328)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '328')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '329', 'Smart Delta: ECU FUSE (329)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '329')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '330', 'Smart Delta: ECU full image with delta (330)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '330')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '331', 'Smart Delta: ECU full image with delta (331)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '331')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '332', 'Smart Delta: ECU full image with delta (332)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '332')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '333', 'Smart Delta: ECU full image with delta (333)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '333')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '334', 'Smart Delta: ECU full image with delta (334)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '334')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '335', 'Smart Delta: ECU full image with delta (335)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '335')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '336', 'Smart Delta: ECU full image with delta (336)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '336')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '337', 'Smart Delta: ECU full image with delta (337)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '337')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '338', 'Smart Delta: ECU full image with delta (338)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '338')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '339', 'Smart Delta: ECU full image with delta (339)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '339')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '340', 'ECU full image without delta (340), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '340')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '341', 'ECU full image without delta (341), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '341')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '342', 'ECU full image without delta (342), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '342')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '343', 'ECU full image without delta (343), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '343')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '344', 'ECU full image without delta (344), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '344')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '345', 'ECU full image without delta (345), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '345')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '346', 'ECU full image without delta (346), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '346')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '347', 'ECU full image without delta (347), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '347')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '348', 'ECU full image without delta (348), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '348')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '349', 'ECU full image without delta (349), Native', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '349')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1316', 'Smart Delta: ECU (1316)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1316')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1317', 'Smart Delta: ECU (1317)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1317')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1318', 'Smart Delta: ECU (1318)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1318')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1319', 'Smart Delta: ECU (1319)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1319')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1320', 'Smart Delta: ECU (1320)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1320')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1327', 'Smart Delta: ECU (1327)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1327')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1328', 'Smart Delta: ECU (1328)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1328')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1329', 'Smart Delta: ECU (1329)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1329')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1330', 'Smart Delta: ECU (1330)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1330')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1346', 'Smart Delta: ECU (1346)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1346')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1347', 'Smart Delta: ECU (1347)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1347')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1348', 'Smart Delta: ECU (1348)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1348')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1349', 'Smart Delta: ECU (1349)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1349')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1350', 'Smart Delta: ECU (1350)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1350')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1351', 'Smart Delta: ECU (1351)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1351')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1356', 'Smart Delta: ECU (1356)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1356')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1357', 'Smart Delta: ECU (1357)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1357')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1358', 'Smart Delta: ECU (1358)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1358')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1359', 'Smart Delta: ECU (1359)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1359')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1360', 'Smart Delta: ECU (1360)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1360')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1361', 'Smart Delta: ECU (1361)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1361')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1362', 'Smart Delta: ECU (1362)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1362')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1363', 'Smart Delta: ECU (1363)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1363')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1364', 'Smart Delta: ECU (1364)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1364')
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    UNION ALL SELECT  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin',  EXTRACT(EPOCH FROM CURRENT_TIMESTAMP), 'admin', 1, '1499', 'Smart Delta: ECU (1499)', false WHERE NOT EXISTS (SELECT 1 FROM sp_software_installer_type WHERE name = '1499');