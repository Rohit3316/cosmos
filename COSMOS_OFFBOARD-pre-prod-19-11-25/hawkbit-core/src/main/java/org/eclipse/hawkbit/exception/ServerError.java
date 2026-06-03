/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.exception;

/**
 * Define the Error code for Error handling
 */

public enum ServerError {

    /**
     *
     */
    GENERIC_ERROR("hawkbit.server.error.repo.genericError", "unknown error occurred"),
    /**
     *
     */
    ENTITY_ALREADY_EXISTS("hawkbit.server.error.repo.entitiyAlreayExists",
            "The given entity already exists in database"),

    /**
     * Error indicating that the uploaded package size exceeds the allowed maximum limit.
     */
    MAX_PACKAGE_SIZE_EXCEEDED("cosmos.server.error.packageSizeExceeded",
            "The uploaded package size exceeds the allowed maximum limit"),

    /**
     *
     */
    FIELD_ERROR("hawkbit.server.error.repo.fieldValueWrongOrNull",
            "The field value is wrong or empty"),

    /**
     *
     */
    AUTO_CONFIRMATION_ALREADY_ACTIVE("hawkbit.server.error.repo.autoConfAlreadyActive",
            "Auto confirmation is already active"),


    /**
     *
     */
    CONFIRMATION_FEEDBACK_INVALID("hawkbit.server.confirmation.feedback.invalid",
            "Confirmation feedback is not valid"),

    /**
     *
     */
    CONSTRAINT_VIOLATION("hawkbit.server.error.repo.constraintViolation",
            "The given entity cannot be saved due to Constraint Violation"),

    /**
     *
     */
    INVALID_TARGET_ADDRESS("hawkbit.server.error.repo.invalidTargetAddress",
            "The target address is not well formed"),

    /**
     *
     */
    ENTITY_NOT_EXISTS("hawkbit.server.error.repo.entitiyNotFound",
            "The given entity does not exist in the repository"),

    /**
     *
     */
    CONCURRENT_MODIFICATION("hawkbit.server.error.repo.concurrentModification",
            "The given entity has been changed by another user/session"),

    /**
     *
     */
    TARGET_ATTRIBUTES_INVALID("hawkbit.server.error.repo.invalidTargetAttributes",
            "The given target attributes are invalid"),

    /**
     *
     */
    REST_SORT_PARAM_SYNTAX("hawkbit.server.error.rest.param.sortParamSyntax",
            "The given sort parameter is not well formed"),

    /**
     *
     */
    REST_RSQL_SEARCH_PARAM_SYNTAX("hawkbit.server.error.rest.param.rsqlParamSyntax",
            "The given search parameter is not well formed"),

    /**
     *
     */
    REST_RSQL_PARAM_INVALID_FIELD("hawkbit.server.error.rest.param.rsqlInvalidField",
            "The given search parameter field does not exist"),

    /**
     *
     */
    REST_SORT_PARAM_INVALID_FIELD("hawkbit.server.error.rest.param.invalidField",
            "The given sort parameter field does not exist"),

    /**
     *
     */
    REST_SORT_PARAM_INVALID_DIRECTION("hawkbit.server.error.rest.param.invalidDirection",
            "The given sort parameter direction does not exist"),

    /**
     *
     */
    REST_BODY_NOT_READABLE("hawkbit.server.error.rest.body.notReadable",
            "The given request body is not well formed"),

    /**
     *
     */
    ARTIFACT_UPLOAD_FAILED("hawkbit.server.error.artifact.uploadFailed",
            "Upload of artifact failed with internal server error."),

    /**
     *
     */
    ARTIFACT_ENCRYPTION_NOT_SUPPORTED("hawkbit.server.error.artifact.encryptionNotSupported",
            "Artifact encryption is not supported."),

    /**
     *
     */
    ARTIFACT_ENCRYPTION_FAILED("hawkbit.server.error.artifact.encryptionFailed",
            "Artifact encryption operation failed."),

    /**
     *
     */
    ARTIFACT_UPLOAD_FAILED_MD5_MATCH("hawkbit.server.error.artifact.uploadFailed.checksum.md5.match",
            "Upload of artifact failed as the provided MD5 checksum did not match with the provided artifact."),

    /**
     *
     */
    ARTIFACT_UPLOAD_FAILED_SHA1_MATCH("hawkbit.server.error.artifact.uploadFailed.checksum.sha1.match",
            "Upload of artifact failed as the provided SHA1 checksum did not match with the provided artifact."),

    /**
     *
     */
    ARTIFACT_UPLOAD_FAILED_SHA256_MATCH("hawkbit.server.error.artifact.uploadFailed.checksum.sha256.match",
            "Upload of artifact failed as the provided SHA256 checksum did not match with the provided artifact."),

    /**
     *
     */
    DS_CREATION_FAILED_MISSING_MODULE("hawkbit.server.error.distributionset.creationFailed.missingModule",
            "Creation if Distribution Set failed as module is missing that is configured as mandatory."),

    /**
     *
     */
    INSUFFICIENT_PERMISSION("hawkbit.server.error.insufficientpermission", "Insufficient Permission"),

    /**
     *
     */
    ARTIFACT_DELETE_FAILED("hawkbit.server.error.artifact.deleteFailed",
            "Deletion of artifact failed with internal server error."),

    /**
     *
     */
    ARTIFACT_LOAD_FAILED("hawkbit.server.error.artifact.loadFailed",
            "Load of artifact failed with internal server error."),

    /**
     *
     */
    ARTIFACT_BINARY_DELETED("hawkbit.server.error.artifact.binaryDeleted",
            "The artifact binary does not exist anymore."),

    /**
     *
     */
    QUOTA_EXCEEDED("hawkbit.server.error.quota.tooManyEntries", "Too many entries have been inserted."),

    /**
     * error that describes that size of uploaded file exceeds size quota
     */
    FILE_SIZE_QUOTA_EXCEEDED("hawkbit.server.error.quota.fileSizeExceeded", "File exceeds size quota."),

    /**
     * error that describes that size of uploaded file exceeds storage quota
     */
    STORAGE_QUOTA_EXCEEDED("hawkbit.server.error.quota.storageExceeded",
            "Storage quota will be exceeded if file is uploaded."),

    /**
     * error message, which describes that the action can not be canceled cause
     * the action is inactive.
     */
    ACTION_NOT_CANCELABLE("hawkbit.server.error.action.notcancelable",
            "Only active actions which are in status pending are cancelable."),

    /**
     * error message, which describes that the action can not be force quit
     * cause the action is inactive.
     */
    ACTION_NOT_FORCE_QUITABLE("hawkbit.server.error.action.notforcequitable",
            "Only active actions which are in status pending can be force quit."),

    /**
     *
     */
    DS_INCOMPLETE("hawkbit.server.error.distributionset.incomplete",
            "Distribution set is assigned to a target that is incomplete (i.e. mandatory modules are missing)"),

    /**
     *
     */
    DS_INVALID("hawkbit.server.error.distributionset.invalid", "Invalid distribution set is assigned to a target"),

    /**
     *
     */
    DS_TYPE_UNDEFINED("hawkbit.server.error.distributionset.type.undefined",
            "Distribution set type is not yet defined. Modules cannot be added until definition."),

    /**
     *
     */
    DS_MODULE_UNSUPPORTED("hawkbit.server.error.distributionset.modules.unsupported",
            "Distribution set type does not contain the given module, i.e. is incompatible."),

    /**
     *
     */
    TENANT_NOT_EXISTS("hawkbit.server.error.repo.tenantNotExists",
            "The entity cannot be inserted due the tenant does not exists"),

    /**
     *
     */
    ENTITY_LOCKED("hawkbit.server.error.entitiylocked", "The given entity is locked by the server."),

    /**
     *
     */
    ENTITY_READ_ONLY("hawkbit.server.error.entityreadonly",
            "The given entity is read only and the change cannot be completed."),

    /**
     *
     */
    CONFIGURATION_VALUE_INVALID("hawkbit.server.error.configValueInvalid",
            "The given configuration value is invalid."),

    /**
     *
     */
    CONFIGURATION_KEY_INVALID("hawkbit.server.error.configKeyInvalid", "The given configuration key is invalid."),

    /**
     *
     */
    ROLLOUT_ILLEGAL_STATE("hawkbit.server.error.rollout.illegalstate",
            "The rollout is in the wrong state for the requested operation"),

    /**
     *
     */
    ROLLOUT_VERIFICATION_FAILED("hawkbit.server.error.rollout.verificationFailed",
            "The rollout configuration could not be verified successfully"),

    /**
     *
     */
    OPERATION_NOT_SUPPORTED("hawkbit.server.error.operation.notSupported",
            "Operation or method is (no longer) supported by service."),

    /**
     * Error message informing that the maintenance schedule is invalid.
     */
    MAINTENANCE_SCHEDULE_INVALID("hawkbit.server.error.maintenanceScheduleInvalid",
            "Information for schedule, duration or timezone is missing; or there is no valid maintenance window available in future."),

    /**
     * Error message informing that the user acceptance required for auto-assignment is
     * invalid.
     */
    AUTO_ASSIGN_USER_ACCEPTANCE_REQUIRED_INVALID("hawkbit.server.error.repo.invalidAutoAssignUserAcceptanceRequired",
            "The given user acceptance required for auto-assignment is invalid: allowed values are ['yes', 'no']"),

    /**
     * Error message informing the user that the requested tenant configuration
     * change is not allowed.
     */
    CONFIGURATION_VALUE_CHANGE_NOT_ALLOWED("hawkbit.server.error.repo.tenantConfigurationValueChangeNotAllowed",
            "The requested tenant configuration value modification is not allowed."),

    /**
     *
     */
    MULTIASSIGNMENT_NOT_ENABLED("hawkbit.server.error.multiassignmentNotEnabled",
            "The requested operation requires multi assignments to be enabled."),

    /**
     *
     */
    NO_WEIGHT_PROVIDED_IN_MULTIASSIGNMENT_MODE("hawkbit.server.error.noWeightProvidedInMultiAssignmentMode",
            "The requested operation requires a weight to be specified when multi assignments is enabled."),

    TARGET_TYPE_IN_USE("hawkbit.server.error.target.type.used", "Target type is still in use by a target."),

    TARGET_TYPE_INCOMPATIBLE("hawkbit.server.error.target.type.incompatible",
            "Target type of target is not compatible with distribution set."),

    STOP_ROLLOUT_FAILED("hawkbit.server.error.stopRolloutFailed", "Stopping the rollout failed"),
    /**
     * Represents a specific error code for server-side errors related to file download operations.
     */
    FILE_DOWNLOAD_FAILED("cosmos.server.error.fileDownloadFailed", "File Download Failed"),

    /**
     *
     */
    DEPLOYMENT_LOG_UPLOAD_FAILED("hawkbit.server.error.deploymentlog.uploadFailed",
            "Upload of deployment log failed with internal server error."),

    /**
     * Represents a specific error code for client-side errors related to bad requests.
     */
    CLIENT_BAD_REQUEST("hawkbit.server.error.client.badRequest", "Bad Request"),

    /**
     * Represents a specific error code for client-side errors related to unauthorized access.
     */
    CLIENT_UNAUTHORIZED("hawkbit.server.error.client.unauthorized", "Unauthorized"),

    /**
     * Represents a specific error code for client-side errors related to forbidden access.
     */
    CLIENT_FORBIDDEN("hawkbit.server.error.client.forbidden", "Forbidden"),

    /**
     * Represents a specific error code for client-side errors related to resource not found.
     */
    CLIENT_NOT_FOUND("hawkbit.server.error.client.notFound", "Not Found"),

    /**
     * Represents a specific error code for client-side errors related to method not allowed.
     */
    CLIENT_METHOD_NOT_ALLOWED("hawkbit.server.error.client.methodNotAllowed", "Method Not Allowed"),

    /**
     * Represents a specific error code for client-side errors related to conflict.
     */
    CLIENT_CONFLICT("hawkbit.server.error.client.conflict", "Conflict"),

    /**
     * Represents a specific error code for client-side errors related to unsupported media type.
     */
    CLIENT_UNSUPPORTED_MEDIA_TYPE("hawkbit.server.error.client.unsupportedMediaType", "Unsupported Media Type"),

    /**
     * Represents a specific error code for client-side errors related to too many requests.
     */
    CLIENT_TOO_MANY_REQUESTS("hawkbit.server.error.client.tooManyRequests", "Too Many Requests"),

    /**
     * Represents a specific error code for server-side errors related to internal server error.
     */
    SERVER_INTERNAL_SERVER_ERROR("hawkbit.server.error.server.internalServerError", "Internal Server Error"),

    /**
     * Represents a specific error code for server-side errors related to not implemented functionality.
     */
    SERVER_NOT_IMPLEMENTED("hawkbit.server.error.server.notImplemented", "Not Implemented"),

    /**
     * Represents a specific error code for server-side errors related to bad gateway.
     */
    SERVER_BAD_GATEWAY("hawkbit.server.error.server.badGateway", "Bad Gateway"),

    /**
     * Represents a specific error code for server-side errors related to service unavailability.
     */
    SERVER_SERVICE_UNAVAILABLE("hawkbit.server.error.server.serviceUnavailable", "Service Unavailable"),

    /**
     * Represents a specific error code for server-side errors related to gateway timeout.
     */
    SERVER_GATEWAY_TIMEOUT("hawkbit.server.error.server.gatewayTimeout", "Gateway Timeout"),

    /**
     * Represents a specific error code for server-side errors related to features not allowed.
     */
    FEATURE_NOT_ALLOWED("hawkbit.server.error.featureNotAllowed","The requested feature is not allowed.");

    private final String key;
    private final String message;

    /*
     * Repository side Error codes
     */
    ServerError(final String errorKey, final String message) {
        key = errorKey;
        this.message = message;
    }

    /**
     * Gets the key of the error
     *
     * @return the key of the error
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the message of the error
     *
     * @return message of the error
     */
    public String getMessage() {
        return message;
    }

}
