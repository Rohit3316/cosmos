/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt;

/**
 * Constants for RESTful API.
 */
public final class MgmtRestConstants {

    /**
     * API version 1 definition. We are using only major versions.
     */
    public static final String API_VERSION_1 = "v1";
    /**
     * API version 2 definition. We are using only major versions.
     */
    public static final String API_VERSION_2 = "v2";

    public static final String API = "api";

    public static final String TARGET_TYPES_V1_TENANT = "http://localhost/management/v1/tenants/{tenantId}/target-types/";

    public static final String TENANT_API_V1_BASE_URL = "http://localhost/management/v1/tenants/";

    /**
     * Operator path.
     */
    public static final String ACTION_PARAM = "{actionId}";
    public static final String PATH_SEPARATOR = "/";
    public static final String MANAGEMENT = "management";
    public static final String TENANT_PARAM = "{tenantId}";
    public static final String TENANTS = "tenants";
    public static final String ARTIFACTS = "artifacts";
    public static final String DISTRIBUTIONSET = "distributionset";
    public static final String DISTRIBUTIONSETS = "distributionsets";

    public static final String ROLLOUT = "rollouts";

    public static final String RETRY ="retry";

    public static final String FULL = "full";

    public static final String TARGET = "target";
    /**
     * The software module URL with tenant mapping rest resource.
     */

    public static final String INSTALL = "install";
    public static final String ASSIGN = "assign";
    public static final String TARGETS = "targets";
    public static final String SOFTWAREMODULE = "softwaremodules";

    public static final String SCOMO = "scomos";
    public static final String SOFTWAREMODULETYPE = "softwaremodule-types";
    public static final String DISTRIBUTIONSETTYPE = "distributionset-types";
    public static final String TARGETTYPES = "target-types";
    public static final String TARGETTYPE = "target-type";
    public static final String TARGET_TAG = "target-tags";
    public static final String SOFTWARES = "softwares";
    public static final String VERSION = "version";
    public static final String VERSION_ID_PARAM = "{versionId}";
    public static final String DISTRIBUTIONSET_TAG = "distributionset-tags";
    public static final String TAG_ID_PARAM = "{tagId}";
    public static final String TARGET_FILTER = "target-filters";
    public static final String ACTION = "action";
    public static final String CONTROLLERS = "controllers";
    public static final String DEPLOYMENT_LOG = "deployment-logs";
    public static final String VEHICLE_MODEL = "vehicle-models";
    public static final String ECU_MODEL = "ecu-models";
    public static final String SOFTWARE_INSTALLER_TYPES = "software-installer-types";
    public static final String MODULETYPE_ID_PARAM = "{moduleTypeId}";
    public static final String GROUPS = "groups";
    public static final String TYPE_PARAM = "{type}";
    public static final String ADD_DEVICE = "add-device";
    public static final String DEPLOYMENT = "deployment";
    public static final String PACKAGE_ID_PARAM = "{packageId}";
    public static final String DOWNLOAD = "download";
    public static final String DOWNLOAD_ID = "downloadId";
    public static final String DOWNLOAD_ID_PARAM = "{downloadId}";
    public static final String DOWNLOAD_HTTP = "download-http";
    public static final String DOWNLOADSERVER = "downloadserver";
    public static final String ACTIONS = "actions";
    public static final String ACTION_ID_PARAM = "{actionId}";
    public static final String STATUS = "status";
    public static final String VEHICLEMODEL_ID_PARAM = "{vehicleModelId}";
    public static final String ACCOUNTS = "accounts";
    public static final String USERS = "users";
    public static final String USERINFO = "userinfo";
    public static final String CDN = "cdn";
    public static final String SYSTEM = "system";
    public static final String CONFIGS = "configs";
    public static final String CONTROLLER_ID_PARAM = "{controllerId}";
    public static final String VIN_PARAM = "{vin}";
    public static final String HISTORY = "history";
    public static final String DEVICE = "device";
    public static final String GENERIC_FEEDBACK = "generic-feedback";
    /**
     * The target type URL mapping rest resource.
     */
    public static final String TARGETTYPE_V1_DS_TYPES = "distributionset-types";
    public static final String DISTRIBUTIONSET_ID_PARAM = "{distributionSetId}";
    public static final String METADATA = "metadata";
    public static final String CONFIRM = "confirm";
    public static final String ACTIVATE = "activate";
    public static final String DEACTIVATE = "deactivate";
    public static final String UPDATETENANT = "updateTenant";
    public static final String TYPE_ID_PARAM = "{typeId}";
    public static final String SOFTWAREMODULE_ID_PARAM = "{softwareModuleId}";
    public static final String SCOMO_ID_PARAM = "{scomoId}";
    public static final String ARTIFACT_ID_PARAM = "{artifactId}";
    public static final String ECUMODEL_ID_PARAM = "{ecuModelId}";
    public static final String ECU_CERTIFICATE = "ecuCertificate";
    public static final String ECU_CERTIFICATE_CN_PARAM = "{certCN}";
    public static final String ROLLOUT_ID_PARAM = "{rolloutId}";
    public static final String APPROVE = "approve";
    public static final String DENY = "deny";
    public static final String FREEZE = "freeze";
    public static final String START = "start";
    public static final String PAUSE = "pause";
    public static final String CANCEL = "cancel";
    public static final String RESUME = "resume";
    public static final String NEXT = "next";
    public static final String ALL = "all";
    public static final String CLONE = "clone";
    public static final String UNFREEZE = "unfreeze";
    public static final String DEPLOYMENT_ID_PARAM = "{deploymentLogId}";
    public static final String GROUP_ID_PARAM = "{groupId}";
    public static final String KEY_PARAM = "{key}";
    public static final String SUPPORTPACKAGE = "support-packages";
    public static final String FILEURL = "fileURL";
    public static final String FILTER_ID_PARAM = "{filterId}";
    public static final String VINLIST = "vinList";
    public static final String ATTRIBUTES = "attributes";
    public static final String SOFTWAREATTRIBUTES = "software-attributes";
    public static final String STATISTICS = "statistics";
    public static final String CACHES = "caches";
    public static final String DSTYPE_ID_PARAM = "{dsTypeId}";
    public static final String DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULE_TYPES = "softwaremodule-types/mandatory";
    public static final String DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULE_TYPES = "softwaremodule-types/optional";
    /**
     * The target URL mapping, href link for autoConfirm state of a target.
     */
    public static final String TARGET_V1_AUTO_CONFIRM = "confirm";
    /**
     * The target URL mapping, href link activate auto-confirm on a target.
     */
    public static final String TARGET_V1_ACTIVATE_AUTO_CONFIRM = "activate";
    /**
     * The target URL mapping, href link deactivate auto-confirm on a target.
     */
    public static final String TARGET_V1_DEACTIVATE_AUTO_CONFIRM = "deactivate";
    /**
     * The target URL mapping, href link for target attributes.
     */
    public static final String TARGET_V1_ATTRIBUTES = "attributes";
    /**
     * The target URL mapping, href link for target actions.
     */
    public static final String TARGET_V1_ACTIONS = "actions";
    /**
     * The target URL mapping, href link for canceled actions.
     */
    public static final String TARGET_V1_CANCELED_ACTION = "canceledaction";
    /**
     * The target URL mapping, href link for canceled actions.
     */
    public static final String TARGET_V1_ACTION_STATUS = "status";
    /**
     * The target URL mapping, href link for a rollout.
     */
    public static final String TARGET_V1_ROLLOUT = "rollout";

    /**
     * The base URL mapping of the SP rest resources.
     */
    public static final String BASE_V1_REQUEST_MAPPING = PATH_SEPARATOR + MANAGEMENT + PATH_SEPARATOR + API_VERSION_1;
    public static final String BASE_HNDL_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + PATH_SEPARATOR + ACCOUNTS + PATH_SEPARATOR + TENANTS;
    public static final String BASE_HNDL_ID_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + PATH_SEPARATOR + ACCOUNTS + PATH_SEPARATOR + TENANTS + PATH_SEPARATOR + TENANT_PARAM;
    public static final String BASE_CLONE_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + PATH_SEPARATOR + ACCOUNTS + PATH_SEPARATOR + TENANTS + PATH_SEPARATOR + TENANT_PARAM + PATH_SEPARATOR + CLONE;
    public static final String BASE_V1_REQUEST_MAPPING_TENANT = PATH_SEPARATOR + MANAGEMENT + PATH_SEPARATOR + API_VERSION_1 + PATH_SEPARATOR + TENANTS + PATH_SEPARATOR + TENANT_PARAM;

    public static final String BASE_V1_REQUEST_MAPPING_TENANT_TARGET = PATH_SEPARATOR + MANAGEMENT + PATH_SEPARATOR + API_VERSION_1 + PATH_SEPARATOR + TENANTS + PATH_SEPARATOR + TENANT_PARAM + PATH_SEPARATOR + TARGETS + PATH_SEPARATOR + CONTROLLER_ID_PARAM;
    public static final String BASE_V2_REQUEST_MAPPING_TENANT = PATH_SEPARATOR + MANAGEMENT + PATH_SEPARATOR + API_VERSION_2 + PATH_SEPARATOR + TENANTS + PATH_SEPARATOR + TENANT_PARAM;

    public static final String BASE_TENANT_VALIDATE = BASE_HNDL_V1_REQUEST_MAPPING + "/validate";
    public static final String SOFTWAREMODULE_V1_TENANT = BASE_V1_REQUEST_MAPPING + "/tenants/1/softwaremodules/";

    public static final String SOFTWAREMODULE_VERSION_V1_TENANT = SOFTWAREMODULE_V1_TENANT + "{softwareModuleId}/version/";

    public static final String BASE_TENANT_ROLLOUT_SUPPORT_PACKAGES = BASE_V1_REQUEST_MAPPING_TENANT + "/rollouts/{rolloutId}/support-packages";


    /**
     * String representation of
     * {@link #REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE}.
     */
    public static final String REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT = "50";

    /**
     * The default limit parameter in case the limit parameter is not present in
     * the request.
     *
     * @see #REQUEST_PARAMETER_PAGING_LIMIT
     */
    public static final int REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE = Integer.parseInt(REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT);

    /**
     * The software module URL with tenant mapping rest resource.
     */
    public static final String SOFTWAREMODULE_V1_REQUEST_MAPPING = PATH_SEPARATOR + MANAGEMENT + PATH_SEPARATOR + API_VERSION_1 + PATH_SEPARATOR + TENANTS + PATH_SEPARATOR + TENANT_PARAM + PATH_SEPARATOR + SOFTWAREMODULE;

    public static final String SCOMO_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + SCOMO;
    public static final String SOFTWAREMODULE_VERSION_V1_REQUEST_MAPPING = SOFTWAREMODULE_V1_REQUEST_MAPPING + PATH_SEPARATOR + SOFTWAREMODULE_ID_PARAM + PATH_SEPARATOR + VERSION;

    public static final String SCOMO_VERSION_V1_REQUEST_MAPPING = SCOMO_V1_REQUEST_MAPPING + PATH_SEPARATOR + SCOMO_ID_PARAM + PATH_SEPARATOR + VERSION;
    public static final String SCOMO_VERSION_ID_V1_REQUEST_MAPPING = SCOMO_V1_REQUEST_MAPPING + PATH_SEPARATOR + SCOMO_ID_PARAM + PATH_SEPARATOR + VERSION + PATH_SEPARATOR + VERSION_ID_PARAM;
    public static final String SOFTWAREMODULE_VERSION_ID_V1_REQUEST_MAPPING = SOFTWAREMODULE_V1_REQUEST_MAPPING + PATH_SEPARATOR + SOFTWAREMODULE_ID_PARAM + PATH_SEPARATOR + VERSION + PATH_SEPARATOR + VERSION_ID_PARAM;
    public static final String SOFTWAREMODULE_ID_V1_REQUEST_MAPPING = SOFTWAREMODULE_V1_REQUEST_MAPPING + PATH_SEPARATOR + SOFTWAREMODULE_ID_PARAM;
    public static final String SCOMO_ID_V1_REQUEST_MAPPING = SCOMO_V1_REQUEST_MAPPING + PATH_SEPARATOR + SCOMO_ID_PARAM;
    public static final String SOFTWAREMODULE_ECU_MODEL_V1_REQUEST_MAPPING = SOFTWAREMODULE_V1_REQUEST_MAPPING + PATH_SEPARATOR + SOFTWAREMODULE_ID_PARAM + PATH_SEPARATOR + ECU_MODEL;
    public static final String SCOMO_ECU_MODEL_V1_REQUEST_MAPPING = SCOMO_V1_REQUEST_MAPPING + PATH_SEPARATOR + SCOMO_ID_PARAM + PATH_SEPARATOR + ECU_MODEL;
    public static final String SOFTWAREMODULE_METADATA_V1_REQUEST_MAPPING = SOFTWAREMODULE_V1_REQUEST_MAPPING + PATH_SEPARATOR + SOFTWAREMODULE_ID_PARAM + PATH_SEPARATOR + METADATA;
    public static final String SOFTWAREMODULE_METADATA_ID_V1_REQUEST_MAPPING = SOFTWAREMODULE_V1_REQUEST_MAPPING + PATH_SEPARATOR + SOFTWAREMODULE_ID_PARAM + PATH_SEPARATOR + METADATA + PATH_SEPARATOR + KEY_PARAM;
    public static final String DOWNLOAD_ARTIFACT = SOFTWAREMODULE_V1_REQUEST_MAPPING + PATH_SEPARATOR + SOFTWAREMODULE_ID_PARAM + PATH_SEPARATOR + ARTIFACTS + PATH_SEPARATOR + ARTIFACT_ID_PARAM + PATH_SEPARATOR + DOWNLOAD;
    public static final String DOWNLOAD_ID_V1_REQUEST_MAPPING =
            PATH_SEPARATOR + DOWNLOAD + PATH_SEPARATOR + DOWNLOAD_ID + PATH_SEPARATOR + TENANT_PARAM + PATH_SEPARATOR + DOWNLOAD_ID_PARAM;

    public static final String DOWNLOAD_ID_V1_REQUEST_MAPPING_BASE =
            PATH_SEPARATOR + API + PATH_SEPARATOR + API_VERSION_1 + PATH_SEPARATOR + DOWNLOADSERVER;

    public static final String DOWNLOAD_ARTIFACT_ID_V1_REQUEST_MAPPING_BASE = DOWNLOAD_ID_V1_REQUEST_MAPPING_BASE + DOWNLOAD_ID_V1_REQUEST_MAPPING;
    public static final String USER_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + PATH_SEPARATOR + ACCOUNTS;
    /**
     * The basic authentication validation mapping
     */
    public static final String AUTH_V1_REQUEST_MAPPING = USER_V1_REQUEST_MAPPING + PATH_SEPARATOR + USERS + PATH_SEPARATOR + USERINFO;
    /**
     * The base URL mapping for the spring acuator management context path.
     */
    public static final String BASE_SYSTEM_MAPPING = PATH_SEPARATOR + SYSTEM;
    public static final String SYSTEM_V1_MAPPING = BASE_SYSTEM_MAPPING + PATH_SEPARATOR + API_VERSION_1;

    public static final String SYSTEM_STATISTICS_V1_MAPPING = SYSTEM_V1_MAPPING + PATH_SEPARATOR + STATISTICS;

    public static final String SYSTEM_CACHE_V1_MAPPING = SYSTEM_V1_MAPPING + PATH_SEPARATOR + CACHES;


    public static final String TENANTID_CONFIG_SYSTEM_MAPPING = BASE_V1_REQUEST_MAPPING + PATH_SEPARATOR + ACCOUNTS + PATH_SEPARATOR + TENANTS + PATH_SEPARATOR + TENANT_PARAM + PATH_SEPARATOR + CONFIGS;

    public static final String TENANTID_CONFIG_KEY_SYSTEM_MAPPING = TENANTID_CONFIG_SYSTEM_MAPPING + PATH_SEPARATOR + KEY_PARAM;

    public static final String TARGET_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + TARGETS;

    public static final String TARGET_ID_V1_REQUEST_MAPPING = TARGET_V1_REQUEST_MAPPING + PATH_SEPARATOR + CONTROLLER_ID_PARAM;
    public static final String TARGET_ID_FETCH_VIN_HISTORY = TARGET_V1_REQUEST_MAPPING + PATH_SEPARATOR + VIN_PARAM +  PATH_SEPARATOR + HISTORY;
    public static final String TARGET_ID_ASSIGN_DS_V1_REQUEST_MAPPING = TARGET_V1_REQUEST_MAPPING + PATH_SEPARATOR + CONTROLLER_ID_PARAM + PATH_SEPARATOR + DISTRIBUTIONSET;
    public static final String TARGET_ID_INSTALL_V1_REQUEST_MAPPING = TARGET_V1_REQUEST_MAPPING + PATH_SEPARATOR + CONTROLLER_ID_PARAM + PATH_SEPARATOR + DISTRIBUTIONSET + PATH_SEPARATOR + INSTALL;
    public static final String TARGET_ID_METADATA_V1_REQUEST_MAPPING = TARGET_V1_REQUEST_MAPPING + PATH_SEPARATOR + CONTROLLER_ID_PARAM + PATH_SEPARATOR + METADATA;

    public static final String TARGET_ID_CONFIRM_V1_REQUEST_MAPPING = TARGET_V1_REQUEST_MAPPING + PATH_SEPARATOR + CONTROLLER_ID_PARAM + PATH_SEPARATOR + CONFIRM;
    public static final String TARGET_ID_ACTIVATE_V1_REQUEST_MAPPING = TARGET_V1_REQUEST_MAPPING + PATH_SEPARATOR + CONTROLLER_ID_PARAM + PATH_SEPARATOR + ACTIVATE;
    public static final String TARGET_ID_DEACTIVATE_V1_REQUEST_MAPPING = TARGET_V1_REQUEST_MAPPING + PATH_SEPARATOR + CONTROLLER_ID_PARAM + PATH_SEPARATOR + DEACTIVATE;
    public static final String TARGET_ID_UPDATETENANT_V1_REQUEST_MAPPING = TARGET_V1_REQUEST_MAPPING + PATH_SEPARATOR + CONTROLLER_ID_PARAM + PATH_SEPARATOR + UPDATETENANT;
    public static final String TARGET_ID_METADATA_KEY_V1_REQUEST_MAPPING = TARGET_ID_METADATA_V1_REQUEST_MAPPING + PATH_SEPARATOR + KEY_PARAM;
    public static final String TARGET_ID_ACTIONS_V1_REQUEST_MAPPING = TARGET_ID_V1_REQUEST_MAPPING + PATH_SEPARATOR + ACTIONS;

    public static final String TARGET_ID_ACTION_ID_V1_REQUEST_MAPPING = TARGET_ID_V1_REQUEST_MAPPING + PATH_SEPARATOR + ACTIONS + PATH_SEPARATOR + ACTION_ID_PARAM;
    public static final String TARGET_ID_ACTION_STATUS_V1_REQUEST_MAPPING = TARGET_ID_ACTION_ID_V1_REQUEST_MAPPING + PATH_SEPARATOR + STATUS;
    public static final String TARGET_ID_ASSIGN_DSET_V1_REQUEST_MAPPING = TARGET_V1_REQUEST_MAPPING + PATH_SEPARATOR + CONTROLLER_ID_PARAM + PATH_SEPARATOR + DISTRIBUTIONSET + PATH_SEPARATOR + ASSIGN;
    /**
     * The target URL mapping rest resource.
     */
    public static final String TARGET_TARGET_TYPE_V1_REQUEST_MAPPING = PATH_SEPARATOR + CONTROLLER_ID_PARAM + PATH_SEPARATOR + TARGETTYPE;

    public static final String TARGET_MNG_V1_REQUEST_MAPPING = TARGET_V1_REQUEST_MAPPING + TARGET_TARGET_TYPE_V1_REQUEST_MAPPING;
    public static final String TARGET_CNTRL_V1_REQUEST_MAPPING = TARGET_V1_REQUEST_MAPPING + PATH_SEPARATOR + CONTROLLER_ID_PARAM + PATH_SEPARATOR + ATTRIBUTES;

    public static final String TARGET_CNTRL_SA_V1_REQUEST_MAPPING = TARGET_V1_REQUEST_MAPPING + PATH_SEPARATOR + CONTROLLER_ID_PARAM + PATH_SEPARATOR + SOFTWAREATTRIBUTES;

    /**
     * The target URL mapping rest resource.
     */
    public static final String TARGET_TARGET_TYPE_V1_REQUEST_GENERIC_FEEDBACK = PATH_SEPARATOR + CONTROLLER_ID_PARAM + PATH_SEPARATOR + GENERIC_FEEDBACK;
    public static final String TARGET_GEN_FEEDBACK_V1_REQUEST_MAPPING = TARGET_V1_REQUEST_MAPPING + TARGET_TARGET_TYPE_V1_REQUEST_GENERIC_FEEDBACK;
    public static final String TARGET_TYPE_V1_DS_TYPES = "distributionset-types";
    /**
     * The tag URL mapping rest resource.
     */
    public static final String TARGET_TAG_TARGETS_REQUEST_MAPPING = "/{tagId}/targets";

    /**
     * The tag URL mapping rest resource.
     */

    public static final String TARGET_TAG_V1_REQUEST_MAPPING_TENANT = BASE_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + TARGET_TAG;


    public static final String TARGET_TAG_ASSIGN_V1_REQUEST_MAPPING_TENANT = TARGET_TAG_V1_REQUEST_MAPPING_TENANT + TARGET_TAG_TARGETS_REQUEST_MAPPING;

    public static final String TARGET_TAG_UNASSIGN_V1_REQUEST_MAPPING_TENANT = TARGET_TAG_ASSIGN_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + CONTROLLER_ID_PARAM;

    public static final String TARGET_TAG_ID_V1_REQUEST_MAPPING_TENANT = TARGET_TAG_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + TAG_ID_PARAM;

    /**
     * The target type URL mapping rest resource.
     */
    public static final String TARGET_TYPE_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + TARGETTYPES;
    public static final String TARGET_TYPE_ID_V1_REQUEST_MAPPING = TARGET_TYPE_V1_REQUEST_MAPPING + PATH_SEPARATOR + TYPE_ID_PARAM;
    public static final String TARGET_TYPE_COMP_V1_REQUEST_MAPPING = TARGET_TYPE_ID_V1_REQUEST_MAPPING + PATH_SEPARATOR + TARGETTYPE_V1_DS_TYPES;
    public static final String TARGET_TYPE_COMP_ID_V1_REQUEST_MAPPING = TARGET_TYPE_COMP_V1_REQUEST_MAPPING + PATH_SEPARATOR + DSTYPE_ID_PARAM;

    /**
     * The tag URL mapping rest resource.
     */
    public static final String DISTRIBUTIONSET_TAG_DISTRIBUTIONSETS_REQUEST_MAPPING = PATH_SEPARATOR + TAG_ID_PARAM + PATH_SEPARATOR + DISTRIBUTIONSETS;
    /**
     * The tag URL mapping rest resource.
     */
    public static final String DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + DISTRIBUTIONSET_TAG;
    public static final String DISTRIBUTIONSET_TAG_ASSOCIATION_DS_V1_REQUEST_MAPPING = DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING + DISTRIBUTIONSET_TAG_DISTRIBUTIONSETS_REQUEST_MAPPING;
    public static final String DISTRIBUTIONSET_TAG_UNASSOCIATION_ID_V1_REQUEST_MAPPING = DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING + DISTRIBUTIONSET_TAG_DISTRIBUTIONSETS_REQUEST_MAPPING + PATH_SEPARATOR + DISTRIBUTIONSET_ID_PARAM;
    public static final String DISTRIBUTIONSET_TAG_ID_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + DISTRIBUTIONSET_TAG + PATH_SEPARATOR + TAG_ID_PARAM;
    /**
     * The target URL mapping rest resource.
     */
    public static final String TARGET_FILTER_V1_REQUEST_MAPPING_TENANT = BASE_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + TARGET_FILTER;
    public static final String TARGET_FILTER_ID_V1_REQUEST_MAPPING_TENANT = TARGET_FILTER_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + FILTER_ID_PARAM;
    public static final String TARGET_FILTER_DS_V1_REQUEST_MAPPING_TENANT = TARGET_FILTER_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + FILTER_ID_PARAM + PATH_SEPARATOR + DISTRIBUTIONSET;
    public static final String TARGET_FILTER_VINLIST_V1_REQUEST_MAPPING_TENANT = TARGET_FILTER_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + VINLIST;

    /**
     * URL mapping for retrieving the status of a specific action for a controller within a tenant.
     * Example: /management/v1/tenants/{tenantId}/rollouts/{rolloutId}/controllers/{controllerId}/status
     */
    public static final String ACTION_STATUS_V1_REQUEST_MAPPING_TENANT =
            BASE_V1_REQUEST_MAPPING_TENANT +
            PATH_SEPARATOR + ROLLOUT +
            PATH_SEPARATOR + ROLLOUT_ID_PARAM +
            PATH_SEPARATOR + CONTROLLER_ID_PARAM +
            PATH_SEPARATOR + ACTION +
            PATH_SEPARATOR + STATUS;


    /**
     * The action URL mapping rest resource USING tenantId.
     */
    public static final String ACTION_V1_REQUEST_MAPPING_TENANT = BASE_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ACTIONS;
    /**
     * The action URL mapping rest resource USING tenantId and controllerId.
     */
    public static final String ACTION_V1_REQUEST_MAPPING_TENANT_TARGET = BASE_V1_REQUEST_MAPPING_TENANT_TARGET + PATH_SEPARATOR + ACTIONS;

    /**
     * The action's deployment logs download URL mapping rest resource USING tenantId.
     */
    public static final String DEPLOYMENT_LOGS_DOWNLOAD = "download";
    /**
     * The action's deployment logs URL mapping rest resource USING tenantId.
     */
    public static final String DEPLOYMENT_LOGS = "deployment-logs";

    /**
     * The action's deployment logs Link URL mapping.
     */
    public static final String DEPLOYMENT_LOGS_LINK = "deploymentLogs";

    /**
     * The default offset parameter in case the offset parameter is not present
     * in the request.
     *
     * @see #REQUEST_PARAMETER_PAGING_OFFSET
     */
    public static final String REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET = "0";
    /**
     * The default offset parameter in case the offset parameter is not present
     * in the request.
     *
     * @see #REQUEST_PARAMETER_PAGING_OFFSET
     */
    public static final int REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET_VALUE = 0;
    /**
     * Limit http parameter for the limitation of returned values for a paged
     * request.
     */
    public static final String REQUEST_PARAMETER_PAGING_LIMIT = "limit";
    /**
     * The maximum limit of entities returned by rest resources.
     */
    public static final int REQUEST_PARAMETER_PAGING_MAX_LIMIT = 500;
    /**
     * Paging http parameter for the offset for a paged request.
     */
    public static final String REQUEST_PARAMETER_PAGING_OFFSET = "offset";
    /**
     * The request parameter for sorting. The value of the sort parameter must
     * be in the following pattern. Example:
     * http://www.bosch.com/iap/sp/rest/targets?sort=field_1:ASC,field_2:DESC,
     * field_3:ASC
     */
    public static final String REQUEST_PARAMETER_SORTING = "sort";
    /**
     * The request parameter for searching. The value of the search parameter
     * must be in the FIQL syntax.
     */
    public static final String REQUEST_PARAMETER_SEARCH = "q";
    /**
     * The request parameter for specifying the representation mode. The value
     * of this parameter can either be "full" or "compact".
     */
    public static final String REQUEST_PARAMETER_REPRESENTATION_MODE = "representation";
    /**
     * The default representation mode.
     */
    public static final String REQUEST_PARAMETER_REPRESENTATION_MODE_DEFAULT = "compact";

    /**
     * The software module type URL mapping rest resource.
     */
    public static final String SOFTWAREMODULETYPE_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + SOFTWAREMODULETYPE;

    public static final String SOFTWAREMODULETYPE_ID_V1_REQUEST_MAPPING = SOFTWAREMODULETYPE_V1_REQUEST_MAPPING + PATH_SEPARATOR + TYPE_ID_PARAM;


    /**
     * The distributon set base resource.
     */
    public static final String DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + PATH_SEPARATOR + TENANTS + PATH_SEPARATOR + TENANT_PARAM + PATH_SEPARATOR + DISTRIBUTIONSETTYPE;
    public static final String DISTRIBUTIONSETTYPE_ONE_V1_REQUEST_MAPPING = DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + PATH_SEPARATOR + TYPE_ID_PARAM;
    public static final String DISTRIBUTIONSETTYPE_V1_OPT_REQUEST_MAPPING = DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + PATH_SEPARATOR + TYPE_ID_PARAM + PATH_SEPARATOR + DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULE_TYPES;
    public static final String DISTRIBUTIONSETTYPE_V1_MND_REQUEST_MAPPING = DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + PATH_SEPARATOR + TYPE_ID_PARAM + PATH_SEPARATOR + DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULE_TYPES;

    public static final String DISTRIBUTIONSETTYPE_V1_OPT_ID_REQUEST_MAPPING = DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + PATH_SEPARATOR + TYPE_ID_PARAM + PATH_SEPARATOR + DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULE_TYPES + PATH_SEPARATOR + MODULETYPE_ID_PARAM;


    public static final String DISTRIBUTIONSETTYPE_V1_MND_ID_REQUEST_MAPPING = DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + PATH_SEPARATOR + TYPE_ID_PARAM + PATH_SEPARATOR + DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULE_TYPES + PATH_SEPARATOR + MODULETYPE_ID_PARAM;


    /**
     * The artifacts base resource.
     */
    public static final String ARTIFACTS_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING_TENANT
            + "/artifacts";

    public static final String DOWNLOAD_ARTIFACT_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING_TENANT
            + "/artifacts" + "/{artifactId}/download";


    public static final String ARTIFACTS_RESOURCE_V1_REQUEST_MAPPING = ARTIFACTS_V1_REQUEST_MAPPING + PATH_SEPARATOR + ARTIFACT_ID_PARAM;

    public static final String CREATE_ARTIFACTS_SM_ASSOCIATIONN_V1_REQUEST_MAPPING = ARTIFACTS_V1_REQUEST_MAPPING + PATH_SEPARATOR + ARTIFACT_ID_PARAM + PATH_SEPARATOR + SOFTWAREMODULE;
    public static final String CREATE_ARTIFACTS_SCOMOS_ASSOCIATION_V1_REQUEST_MAPPING = ARTIFACTS_V1_REQUEST_MAPPING + PATH_SEPARATOR + ARTIFACT_ID_PARAM + PATH_SEPARATOR + SCOMO;
    public static final String UNLINK_ARTIFACTS_SM_ASSOCIATIONN_V1_REQUEST_MAPPING = ARTIFACTS_V1_REQUEST_MAPPING + PATH_SEPARATOR + ARTIFACT_ID_PARAM + PATH_SEPARATOR + SOFTWAREMODULE + PATH_SEPARATOR + SOFTWAREMODULE_ID_PARAM;

    public static final String UNLINK_ARTIFACT_SCOMO_V1_REQUEST_MAPPING = ARTIFACTS_V1_REQUEST_MAPPING + PATH_SEPARATOR + ARTIFACT_ID_PARAM + PATH_SEPARATOR + SCOMO + PATH_SEPARATOR + SCOMO_ID_PARAM;

    public static final String PURGE_ARTIFACTS_V1_REQUEST_MAPPING = ARTIFACTS_V1_REQUEST_MAPPING + PATH_SEPARATOR + ARTIFACT_ID_PARAM + PATH_SEPARATOR + CDN;

    public static final String REPLACE_ARTIFACTS_V1_REQUEST_MAPPING = ARTIFACTS_V1_REQUEST_MAPPING + PATH_SEPARATOR + "{oldSha256}/replace";
    /**
     * The artifacts base resource using fileURL.
     */
    public static final String ARTIFACTS_V1_REQUEST_MAPPING_FILEURL = BASE_V1_REQUEST_MAPPING_TENANT
            + "/artifacts/fileURL";
    /**
     * The software module URL mapping rest resource.
     */
    public static final String DISTRIBUTIONSET_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + PATH_SEPARATOR + TENANTS + PATH_SEPARATOR + TENANT_PARAM + PATH_SEPARATOR + DISTRIBUTIONSETS;

    public static final String DISTRIBUTIONSET_AUTO_ASSIGN_TARGET_FILTER_V1_REQUEST_MAPPING = MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + PATH_SEPARATOR + DISTRIBUTIONSET_ID_PARAM + PATH_SEPARATOR + TARGETS + PATH_SEPARATOR + TARGET_FILTER;


    public static final String DISTRIBUTIONSET_METADATA_V1_REQUEST_MAPPING = DISTRIBUTIONSET_V1_REQUEST_MAPPING + PATH_SEPARATOR + DISTRIBUTIONSET_ID_PARAM + PATH_SEPARATOR + METADATA;

    public static final String DISTRIBUTIONSET_ONE_V1_REQUEST_MAPPING = DISTRIBUTIONSET_V1_REQUEST_MAPPING + PATH_SEPARATOR + DISTRIBUTIONSET_ID_PARAM;

    public static final String DISTRIBUTIONSET_STATISTICS_V1_REQUEST_MAPPING = DISTRIBUTIONSET_V1_REQUEST_MAPPING + PATH_SEPARATOR + DISTRIBUTIONSET_ID_PARAM + PATH_SEPARATOR + STATISTICS;

    public static final String DISTRIBUTIONSET_AUTO_ASSIGNMENTS_V1_REQUEST_MAPPING = DISTRIBUTIONSET_V1_REQUEST_MAPPING + PATH_SEPARATOR + DISTRIBUTIONSET_ID_PARAM + PATH_SEPARATOR + STATISTICS + PATH_SEPARATOR + TARGETS;

    public static final String DISTRIBUTIONSET_ACTIONS_V1_REQUEST_MAPPING = DISTRIBUTIONSET_V1_REQUEST_MAPPING + PATH_SEPARATOR + DISTRIBUTIONSET_ID_PARAM + PATH_SEPARATOR + STATISTICS + PATH_SEPARATOR + ACTIONS;
    public static final String DISTRIBUTIONSET_ROLLOUTS_V1_REQUEST_MAPPING = DISTRIBUTIONSET_V1_REQUEST_MAPPING + PATH_SEPARATOR + DISTRIBUTIONSET_ID_PARAM + PATH_SEPARATOR + STATISTICS + PATH_SEPARATOR + ROLLOUT;
    public static final String DISTRIBUTIONSET_ASSIGNED_SOFTWAREMODULES_V1_REQUEST_MAPPING = DISTRIBUTIONSET_V1_REQUEST_MAPPING + PATH_SEPARATOR + DISTRIBUTIONSET_ID_PARAM + PATH_SEPARATOR + SOFTWAREMODULE;
    public static final String DISTRIBUTIONSET_CREATE_METADATA_V1_REQUEST_MAPPING = DISTRIBUTIONSET_V1_REQUEST_MAPPING + PATH_SEPARATOR + DISTRIBUTIONSET_ID_PARAM + PATH_SEPARATOR + METADATA;
    public static final String DISTRIBUTIONSET_DELETE_METADATA_V1_REQUEST_MAPPING = DISTRIBUTIONSET_V1_REQUEST_MAPPING + PATH_SEPARATOR + DISTRIBUTIONSET_ID_PARAM + PATH_SEPARATOR + METADATA + PATH_SEPARATOR + KEY_PARAM;


    public static final String DISTRIBUTIONSET_DELETE_SOFTWAREMODULES_V1_REQUEST_MAPPING = MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + PATH_SEPARATOR + DISTRIBUTIONSET_ID_PARAM + PATH_SEPARATOR + SOFTWAREMODULE + PATH_SEPARATOR + SOFTWAREMODULE_ID_PARAM;
    /**
     * The software module URL mapping rest resource using tenantId.
     */
    public static final String DISTRIBUTIONSET_V1_REQUEST_MAPPING_TENANT = BASE_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + DISTRIBUTIONSETS;

    public static final String DISTRIBUTIONSET_CREATE_ASSIGNED_TARGET_V1_REQUEST_MAPPING_TENANT = DISTRIBUTIONSET_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + DISTRIBUTIONSET_ID_PARAM + PATH_SEPARATOR + TARGETS;


    public static final String DISTRIBUTIONSET_INSTALLED_TARGETS_V1_REQUEST_MAPPING_TENANT = MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + DISTRIBUTIONSET_ID_PARAM + PATH_SEPARATOR + TARGETS + PATH_SEPARATOR + INSTALL;

    public static final String DISTRIBUTIONSET_ASSIGNED_TARGETS_V1_REQUEST_MAPPING_TENANT = MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + DISTRIBUTIONSET_ID_PARAM + PATH_SEPARATOR + TARGETS + PATH_SEPARATOR + ASSIGN;



    /**
     * PKI management constants
     * SigningCertificateConfiguration endpoints
     */
    public static final String PKI = "/pki";
    public static final String SIGNING_CERTIFICATE_CONFIGURATION = "/signing-certificate-configuration";
    public static final String ECU_ID_ISSUER = "/{ecuIdIssuer}";
    public static final String SIGNING_CERTIFICATE_CONFIGURATION_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + PKI + SIGNING_CERTIFICATE_CONFIGURATION;
    public static final String SIGNING_CERTIFICATE_CONFIGURATION_V1_REQUEST_MAPPING_BY_ECU_ID_ISSUER = BASE_V1_REQUEST_MAPPING + PKI + SIGNING_CERTIFICATE_CONFIGURATION + ECU_ID_ISSUER;


    /**
     * The rollout URL mapping rest resource.
     */
    public static final String ROLLOUT_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + PATH_SEPARATOR + ROLLOUT;

    /**
     * The rollout URL mapping rest resource using tenantId.
     */
    public static final String ROLLOUT_V1_REQUEST_MAPPING_TENANT = BASE_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT;

    public static final String ROLLOUT_SUPPORTPACKAGE_V1_REQUEST_MAPPING_TENANT_TARGETS = ROLLOUT_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT_ID_PARAM + PATH_SEPARATOR + SUPPORTPACKAGE;
    public static final String ROLLOUT_SUPPORTPACKAGE_FILEURL_V1_REQUEST_MAPPING_TENANT_TARGETS = ROLLOUT_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT_ID_PARAM + PATH_SEPARATOR + SUPPORTPACKAGE + PATH_SEPARATOR + FILEURL;

    public static final String ROLLOUT_SUPPORTPACKAGE_ID_V1_REQUEST_MAPPING_TENANT_TARGETS = ROLLOUT_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT_ID_PARAM + PATH_SEPARATOR + SUPPORTPACKAGE + PATH_SEPARATOR + TYPE_PARAM + PATH_SEPARATOR + PACKAGE_ID_PARAM;
    public static final String ROLLOUT_SUPPORTPACKAGE_DOWNLOAD_V1_REQUEST_MAPPING_TENANT_TARGETS = ROLLOUT_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT_ID_PARAM + PATH_SEPARATOR + SUPPORTPACKAGE + PATH_SEPARATOR + PACKAGE_ID_PARAM + PATH_SEPARATOR + DOWNLOAD;

    public static final String ROLLOUT_UNFREEZE_V1_REQUEST_MAPPING_TENANT = ROLLOUT_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT_ID_PARAM + PATH_SEPARATOR + ACTION + PATH_SEPARATOR + UNFREEZE;

    public static final String ROLLOUT_ASSOCIATE_V1_REQUEST_MAPPING_TENANT = ROLLOUT_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT_ID_PARAM + PATH_SEPARATOR + SOFTWARES;

    public static final String ROLLOUT_NEXT_V1_REQUEST_MAPPING_TENANT = ROLLOUT_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT_ID_PARAM + PATH_SEPARATOR + GROUPS + PATH_SEPARATOR + NEXT;
    public static final String ROLLOUT_GROUP_START_ALL_V1_REQUEST_MAPPING_TENANT = ROLLOUT_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT_ID_PARAM + PATH_SEPARATOR + GROUPS + PATH_SEPARATOR + ALL;
    public static final String ROLLOUT_GROUP_TRG_V1_REQUEST_MAPPING_TENANT = ROLLOUT_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT_ID_PARAM + PATH_SEPARATOR + GROUPS + PATH_SEPARATOR + GROUP_ID_PARAM + PATH_SEPARATOR + TARGETS;

    public static final String ROLLOUT_GROUP_ID_V1_REQUEST_MAPPING_TENANT = ROLLOUT_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT_ID_PARAM + PATH_SEPARATOR + GROUPS + PATH_SEPARATOR + GROUP_ID_PARAM;

    public static final String ROLLOUT_GROUP_V1_REQUEST_MAPPING_TENANT = ROLLOUT_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT_ID_PARAM + PATH_SEPARATOR + GROUPS;
    public static final String ROLLOUT_GROUPID_V1_REQUEST_MAPPING_TENANT = ROLLOUT_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT_ID_PARAM + PATH_SEPARATOR + GROUPS + PATH_SEPARATOR + GROUP_ID_PARAM;
    public static final String ROLLOUT_RESUME_V1_REQUEST_MAPPING_TENANT = ROLLOUT_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT_ID_PARAM + PATH_SEPARATOR + ACTION + PATH_SEPARATOR + RESUME;

    public static final String ROLLOUT_CANCEL_V1_REQUEST_MAPPING_TENANT = ROLLOUT_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT_ID_PARAM + PATH_SEPARATOR + ACTION + PATH_SEPARATOR + CANCEL;

    public static final String ROLLOUT_PAUSE_V1_REQUEST_MAPPING_TENANT = ROLLOUT_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT_ID_PARAM + PATH_SEPARATOR + ACTION + PATH_SEPARATOR + PAUSE;

    public static final String ROLLOUT_DEVICE_ACTION_PAUSE_V1_MAPPING_TENANT = ROLLOUT_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT_ID_PARAM + PATH_SEPARATOR + CONTROLLER_ID_PARAM + PATH_SEPARATOR + ACTION + PATH_SEPARATOR + PAUSE;

    public static final String ROLLOUT_DEVICE_ACTION_CANCEL_V1_MAPPING_TENANT = ROLLOUT_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT_ID_PARAM + PATH_SEPARATOR + CONTROLLER_ID_PARAM + PATH_SEPARATOR + ACTION + PATH_SEPARATOR + CANCEL;

    public static final String ROLLOUT_START_V1_REQUEST_MAPPING_TENANT = ROLLOUT_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT_ID_PARAM + PATH_SEPARATOR + ACTION + PATH_SEPARATOR + START;

    public static final String ROLLOUT_FREEZE_V1_REQUEST_MAPPING_TENANT = ROLLOUT_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT_ID_PARAM + PATH_SEPARATOR + ACTION + PATH_SEPARATOR + FREEZE;

    public static final String ROLLOUT_DENY_V1_REQUEST_MAPPING_TENANT = ROLLOUT_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT_ID_PARAM + PATH_SEPARATOR + DENY;

    public static final String ROLLOUT_TARGETS_V1_REQUEST_MAPPING_TENANT = ROLLOUT_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT_ID_PARAM + PATH_SEPARATOR +TARGETS;


    public static final String ROLLOUT_ID_V1_REQUEST_MAPPING_TENANT = ROLLOUT_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT_ID_PARAM;

    public static final String ROLLOUT_APPROVE_V1_REQUEST_MAPPING_TENANT = ROLLOUT_ID_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + APPROVE;


    public static final String ROLLOUT_V2_REQUEST_MAPPING_TENANT = BASE_V2_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT;

    public static final String ROLLOUT_DEVICE_ACTION_RESUME_V1_MAPPING_TENANT = ROLLOUT_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT_ID_PARAM + PATH_SEPARATOR + CONTROLLER_ID_PARAM + PATH_SEPARATOR + ACTION + PATH_SEPARATOR + RESUME;

    /**
     * The target URL mapping, href link for artifact download.
     */
    public static final String SOFTWAREMODULE_V1_ARTIFACT = "artifacts";
    /**
     * The target URL mapping, href link for software module access.
     */
    public static final String DISTRIBUTIONSET_V1_MODULE = "modules";
    /**
     * The target URL mapping, href link for type information.
     */
    public static final String SOFTWAREMODULE_V1_TYPE = "type";
    public static final String DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULES = "optionalmodules";
    public static final String DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULES = "mandatorymodules";


    /**
     * Request parameter if the artifact url handler should be used
     */
    public static final String REQUEST_PARAMETER_USE_ARTIFACT_URL_HANDLER = "useartifacturlhandler";

    public static final String VEHICLEMODEL_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + VEHICLE_MODEL;
    public static final String VEHICLEMODEL_V1_NO_TENANT_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + PATH_SEPARATOR + VEHICLE_MODEL;

    public static final String VEHICLEMODEL_ID_V1_REQUEST_MAPPING = VEHICLEMODEL_V1_REQUEST_MAPPING + PATH_SEPARATOR + VEHICLEMODEL_ID_PARAM;
    public static final String VEHICLEMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING = VEHICLEMODEL_V1_NO_TENANT_REQUEST_MAPPING + PATH_SEPARATOR + VEHICLEMODEL_ID_PARAM;
    public static final String VEHICLEMODEL_ECU_V1_REQUEST_MAPPING = VEHICLEMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING + PATH_SEPARATOR + ECU_MODEL;


    public static final String ECUMODEL_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ECU_MODEL;
    public static final String ECUMODEL_V1_NO_TENANT_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + PATH_SEPARATOR + ECU_MODEL;

    public static final String ECUMODEL_ID_V1_REQUEST_MAPPING = ECUMODEL_V1_REQUEST_MAPPING + PATH_SEPARATOR + ECUMODEL_ID_PARAM;
    public static final String ECUMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING = ECUMODEL_V1_NO_TENANT_REQUEST_MAPPING + PATH_SEPARATOR + ECUMODEL_ID_PARAM;
    public static final String ECU_CERTIFICATE_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + PATH_SEPARATOR +ECU_CERTIFICATE + PATH_SEPARATOR + ECU_CERTIFICATE_CN_PARAM ;

    public static final String SOFTWARE_INSTALLER_TYPES_MAPPING = BASE_V1_REQUEST_MAPPING + PATH_SEPARATOR + SOFTWARE_INSTALLER_TYPES;
    /**
     * The rollout groups url
     */
    public static final String ADD_DEVICE_DETAILS = PATH_SEPARATOR + ROLLOUT_ID_PARAM + PATH_SEPARATOR + GROUPS;

    public static final String ADD_DEVICE_DETAILS_REQUEST_MAPPING = ROLLOUT_V1_REQUEST_MAPPING_TENANT + ADD_DEVICE_DETAILS;

    public static final String DELETE_DEVICE_DETAILS = PATH_SEPARATOR + ROLLOUT_ID_PARAM + PATH_SEPARATOR + GROUPS;

    public static final String DELETE_DEVICE_DETAILS_REQUEST_MAPPING = ROLLOUT_V1_REQUEST_MAPPING_TENANT + DELETE_DEVICE_DETAILS;

    /**
     * The deploymentLog base resource.
     */
    public static final String DEPLOYMENT_LOG_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + PATH_SEPARATOR + TARGETS + PATH_SEPARATOR + CONTROLLER_ID_PARAM + PATH_SEPARATOR + ACTIONS + PATH_SEPARATOR + ACTION_PARAM + PATH_SEPARATOR + DEPLOYMENT_LOGS;


    public static final String DOWNLOAD_DEPLOYMENT_LOG_V1_REQUEST_MAPPING = MgmtRestConstants.DEPLOYMENT_LOG_V1_REQUEST_MAPPING + PATH_SEPARATOR + DEPLOYMENT_ID_PARAM + PATH_SEPARATOR + DOWNLOAD;

    /**
     * Request body of create software module has version as default '0'
     */
    public static final String DEFAULT_REQUEST_BODY_SM_VERSION = "0";

    /**
     * The rollout connectivity type REST mapping.
     */
    public static final String CONNECTIVITY_TYPE_WIFI = "wifi_only";

    public static final String CONNECTIVITY_TYPE_CELLULAR = "cellular";

    public static final String CONNECTIVITY_TYPE_BOTH = "both";

    public static final String CONNECTIVITY_TYPE_WIFI_PREFERRED = "wifi_preferred";

    public static final String TAG = "tag_";
    public static final String FILTER = "filter_";

    public static final int EMPTY_FILE = -1;

    public static final Long MAX_SIZE_ARTIFACTS = 2147483648L;

    public static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";

    public static final String CONTENT_DISPOSITION_VALUE = "attachment; filename=\\";

    public static final String BYTES = "bytes";

    /**
     * Constant for the artifact download link
     */
    /**
     * Constant for the artifact download HTTP link
     */
    public static final String DELETE_DS = "Delete Distribution Set";

    public static final String ACTION_SINGLE_V1_REQUEST_MAPPING_TENANT = ACTION_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ACTION_PARAM;

    public static final String ACTION_DEPLOYMENT_LOGS_MAPPING_TENANT = ACTION_V1_REQUEST_MAPPING_TENANT_TARGET + PATH_SEPARATOR + ACTION_PARAM
            + PATH_SEPARATOR + DEPLOYMENT_LOGS;

    public static final String EMPTY_JSON = "{}";

    public static final String RETRY_MULTIPLE_DEVICES = BASE_V1_REQUEST_MAPPING_TENANT+ PATH_SEPARATOR + ROLLOUT+PATH_SEPARATOR+ROLLOUT_ID_PARAM+PATH_SEPARATOR+ACTION+PATH_SEPARATOR+RETRY+PATH_SEPARATOR+"category";

    public static final String RETRY_FULL_ROLLOUT = BASE_V1_REQUEST_MAPPING_TENANT+ PATH_SEPARATOR + ROLLOUT+PATH_SEPARATOR+ROLLOUT_ID_PARAM+PATH_SEPARATOR+ACTION+PATH_SEPARATOR+ RETRY+PATH_SEPARATOR+FULL;

    public static final String CLONE_ROLLOUT = BASE_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT+PATH_SEPARATOR+ROLLOUT_ID_PARAM + PATH_SEPARATOR + ACTION+PATH_SEPARATOR + CLONE;

    public static final String RETRY_INDIVIDUAL_DEVICE = BASE_V1_REQUEST_MAPPING_TENANT + PATH_SEPARATOR + ROLLOUT + PATH_SEPARATOR + ROLLOUT_ID_PARAM + PATH_SEPARATOR + CONTROLLERS + PATH_SEPARATOR + CONTROLLER_ID_PARAM + PATH_SEPARATOR + ACTION + PATH_SEPARATOR + RETRY;

    // constant class, private constructor.
    private MgmtRestConstants() {

    }
}