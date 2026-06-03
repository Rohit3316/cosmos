/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.ddi;

/**
 * Constants for the direct device integration rest resources.
 */
public final class DdiRestConstants {

    /**
     * The base URL mapping of the direct device integration rest resources.
     */
    public static final String PATH_SEPARATOR = "/";
    public static final String DEVICE = "device";
    public static final String V1 = "v1";
    public static final String V2 = "v2";
    public static final String TENANTS = "tenants";
    public static final String TENANT_PATH = "{tenant}";
    public static final String CONTROLLERS = "controllers";

    /**
     * Deployment action resources.
     */
    public static final String DEPLOYMENT_BASE_ACTION = "deploymentBase";

    public static final String DEPLOYED_ACTION = "deployed";

    /**
     * Confirmation base resource.
     */
    public static final String CONFIRMATION_BASE = "confirmationBase";

    public static final String CONFIRMATION = "confirmation";

    /**
     * Activate auto-confirm
     */
    public static final String AUTO_CONFIRM_ACTIVATE = "activateAutoConfirm";

    public static final String ACTIVATE = "activate";

    /**
     * Deactivate auto-confirm
     */
    public static final String AUTO_CONFIRM_DEACTIVATE = "deactivateAutoConfirm";

    public static final String DEACTIVATE = "deactivate";

    /**
     * Installed action resources.
     */
    public static final String INSTALLED_BASE_ACTION = "installedBase";

    public static final String INSTALLED_ACTION = "installed";

    /**
     * Cancel action resources.
     */
    public static final String CANCEL_ACTION = "cancelAction";

    public static final String CANCELED = "canceled";

    public static final String ACTIONS = "actions";
    public static final String ACTION_ID_PARAM = "{actionId}";

    /**
     * Feedback channel.
     */
    public static final String FEEDBACK = "feedback";

    /**
     * Feedback channel.
     */
    public static final String FEEDBACK_LIST = "allFeedback";

    /**
     * Feedback's deployment logs request.
     */
    public static final String LOG_COLLECTION = "logCollection";

    /**
     * Config data action resources.
     */
    public static final String CONFIG_DATA_ACTION = "configData";

    public static final String CONFIGS_ACTION = "configs";

    public static final String POLLINGS_ACTION = "pollings";

    /**
     * Target polling resources using inventory hash.
     */

    public static final String INVENTORY = "inventory";

    /**
     * Target polling resources using inventory hash.
     */
    public static final String GENERIC_FEEDBACK = "genericFeedback";

    public static final String INVENTORYDETAILS = "inventoryDetails";


    /**
     * Default value specifying that no action history to be sent as part of
     * response to deploymentBase
     */
    public static final String NO_ACTION_HISTORY = "0";

    /**
     * Media type for CBOR content. Unfortunately, there is no other constant we
     * can reuse - even the Jackson data converter simply hardcodes this.
     */
    public static final String MEDIA_TYPE_CBOR = "application/cbor";

    /**
     * The rollout connectivity type REST mapping.
     */
    public static final String CONNECTIVITY_TYPE_WIFI = "wifi_only";
    public static final String CONNECTIVITY_TYPE_CELLULAR = "cellular";
    public static final String CONNECTIVITY_TYPE_WIFI_PREFERRED = "wifi_preferred";
    public static final String CONNECTIVITY_TYPE_BOTH = "both";

    /**
     * The file extension for certificates.
     */
    public static final String CERTIFICATE_FILE_EXTENSION = ".pem";

    /**
     * The rollout connectivity type REST mapping.
     */

    public static final String CONNECTIVITY_TYPE_CELLULAR_CAPS = "CELLULAR";
    public static final String CONNECTIVITY_TYPE_WIFI_ONLY = "WIFI_ONLY";
    public static final String HASH = "hash";
    public static final Long DEFAULT_VEHICLE_MODEL_ID = 1L;
    public static final String CONTROLLER_ID_PARAM = "{controllerId}";
    public static final String FILE_NAME_PARAM = "{fileName}";
    public static final String SOFTWARE_MODULES = "softwaremodules";
    public static final String SOFTWARE_MODULE_ID_PARAM = "{softwareModuleId}";

    public static final String ARTIFACTS = "artifacts";
    public static final String ARTIFACT = "artifact";
    public static final String RSP = "RSP";
    public static final String ESP = "ESP";
    public static final String POLLING_ID_PARAM = "{pollingId}";
    public static final String LOGS = "logs";
    public static final String BASE_DEVICE_V1_REQUEST_MAPPING = PATH_SEPARATOR + DEVICE + PATH_SEPARATOR + V1;
    public static final String BASE_DEVICE_V2_REQUEST_MAPPING = PATH_SEPARATOR + DEVICE + PATH_SEPARATOR + V2;

    public static final String DEVICE_V1_TENANTS_REQUEST_MAPPING = BASE_DEVICE_V1_REQUEST_MAPPING + PATH_SEPARATOR + CONTROLLERS;

    public static final String DEVICE_V1_NO_TENANTS_REQUEST_MAPPING = BASE_DEVICE_V1_REQUEST_MAPPING + PATH_SEPARATOR + CONTROLLERS;
    public static final String DEVICE_V2_TENANTS_REQUEST_MAPPING = BASE_DEVICE_V2_REQUEST_MAPPING + PATH_SEPARATOR + TENANTS + PATH_SEPARATOR + TENANT_PATH + PATH_SEPARATOR + CONTROLLERS;
    public static final String DEVICE_V2_NO_TENANTS_REQUEST_MAPPING = BASE_DEVICE_V2_REQUEST_MAPPING + PATH_SEPARATOR + CONTROLLERS;

    //PATH API
    public static final String DOWNLOAD_ARTIFACT_PATH = DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + PATH_SEPARATOR + CONTROLLER_ID_PARAM
            + PATH_SEPARATOR + SOFTWARE_MODULES + PATH_SEPARATOR + SOFTWARE_MODULE_ID_PARAM + PATH_SEPARATOR + ARTIFACTS + PATH_SEPARATOR + FILE_NAME_PARAM;
    public static final String CONTROLLER_BASE_DEPLOYMENT_ACTION_PATH = DEVICE_V2_NO_TENANTS_REQUEST_MAPPING + PATH_SEPARATOR + CONTROLLER_ID_PARAM + PATH_SEPARATOR
            + ACTIONS + PATH_SEPARATOR + ACTION_ID_PARAM + PATH_SEPARATOR + DEPLOYED_ACTION;

    public static final String BASE_DEPLOYMENT_ACTION_FEEDBACK_PATH = DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + PATH_SEPARATOR
            + CONTROLLER_ID_PARAM + PATH_SEPARATOR + ACTIONS + PATH_SEPARATOR + ACTION_ID_PARAM
            + PATH_SEPARATOR + DEPLOYED_ACTION + PATH_SEPARATOR + FEEDBACK;

    public static final String GET_INVENTORY_HASH_PATH = DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + PATH_SEPARATOR
            + CONTROLLER_ID_PARAM + PATH_SEPARATOR + INVENTORY;

    public static final String GENERAL_FEEDBACK_PATH = DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + PATH_SEPARATOR + CONTROLLER_ID_PARAM
            + PATH_SEPARATOR + FEEDBACK;

    public static final String PUT_POLLING_DATA_PATH = DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + PATH_SEPARATOR + CONTROLLER_ID_PARAM
            + PATH_SEPARATOR + POLLINGS_ACTION + PATH_SEPARATOR + POLLING_ID_PARAM;

    public static final String CONTROLLER_CANCEL_ACTION_PATH = DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + PATH_SEPARATOR
            + CONTROLLER_ID_PARAM + PATH_SEPARATOR + ACTIONS + PATH_SEPARATOR + ACTION_ID_PARAM + PATH_SEPARATOR + CANCELED;

    public static final String POST_CANCEL_ACTION_FEEDBACK_PATH = DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + PATH_SEPARATOR
            + CONTROLLER_ID_PARAM + PATH_SEPARATOR + ACTIONS + PATH_SEPARATOR + ACTION_ID_PARAM
            + PATH_SEPARATOR + CANCELED;

    public static final String GET_INSTALLED_ACTION_PATH = DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + PATH_SEPARATOR
            + CONTROLLER_ID_PARAM + PATH_SEPARATOR + ACTIONS + PATH_SEPARATOR + ACTION_ID_PARAM
            + PATH_SEPARATOR + INSTALLED_ACTION;

    public static final String GET_CONFIRMATION_BASE_PATH = DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + PATH_SEPARATOR
            + CONTROLLER_ID_PARAM + PATH_SEPARATOR + CONFIRMATION;

    public static final String GET_CONFIRMATION_BASE_ACTION_PATH = DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + PATH_SEPARATOR
            + CONTROLLER_ID_PARAM + PATH_SEPARATOR + CONFIRMATION + PATH_SEPARATOR + ACTIONS + PATH_SEPARATOR + ACTION_ID_PARAM;

    public static final String POST_CONFIRMATION_ACTION_FEEDBACK_PATH = DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + PATH_SEPARATOR
            + CONTROLLER_ID_PARAM + PATH_SEPARATOR + CONFIRMATION + PATH_SEPARATOR + ACTIONS + PATH_SEPARATOR + ACTION_ID_PARAM
            + PATH_SEPARATOR + FEEDBACK;

    public static final String POST_ACTIVATE_AUTO_CONFIRMATION_PATH = DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + PATH_SEPARATOR
            + CONTROLLER_ID_PARAM + PATH_SEPARATOR + CONFIRMATION + PATH_SEPARATOR + ACTIVATE;

    public static final String POST_DEACTIVATE_AUTO_CONFIRMATION_PATH = DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + PATH_SEPARATOR
            + CONTROLLER_ID_PARAM + PATH_SEPARATOR + CONFIRMATION + PATH_SEPARATOR + DEACTIVATE;

    public static final String POST_DEPLOYED_ACTION_FEEDBACK_LIST_PATH = DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + PATH_SEPARATOR
            + CONTROLLER_ID_PARAM + PATH_SEPARATOR + ACTIONS + PATH_SEPARATOR + ACTION_ID_PARAM + PATH_SEPARATOR
            + DEPLOYED_ACTION + PATH_SEPARATOR + FEEDBACK_LIST;

    public static final String PUT_DEVICE_INVENTORY_PATH = DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + PATH_SEPARATOR
            + CONTROLLER_ID_PARAM + PATH_SEPARATOR + INVENTORY;

    public static final String POST_DEPLOYMENT_LOGS_PATH = DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + PATH_SEPARATOR
            + CONTROLLER_ID_PARAM + PATH_SEPARATOR + ACTIONS + PATH_SEPARATOR + ACTION_ID_PARAM + PATH_SEPARATOR
            + DEPLOYED_ACTION + PATH_SEPARATOR + LOGS;

    public static final String GET_TARGET_BASED_DEPLOYMENT_ACTION_PATH = DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + PATH_SEPARATOR
            + CONTROLLER_ID_PARAM + PATH_SEPARATOR + ACTIONS + PATH_SEPARATOR + ACTION_ID_PARAM + PATH_SEPARATOR
            + DEPLOYED_ACTION;

    public static final String GET_INVENTORY_DETAILS = DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + PATH_SEPARATOR
            + CONTROLLER_ID_PARAM + PATH_SEPARATOR + INVENTORYDETAILS;

    private DdiRestConstants() {
        // constant class, private constructor.
    }
}
