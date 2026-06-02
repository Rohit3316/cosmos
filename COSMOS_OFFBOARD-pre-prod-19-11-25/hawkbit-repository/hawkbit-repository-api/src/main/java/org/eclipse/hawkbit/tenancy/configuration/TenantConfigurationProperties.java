/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.tenancy.configuration;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.hawkbit.ControllerPollProperties;
import org.eclipse.hawkbit.HawkbitServerProperties.Anonymous.Download;
import org.eclipse.hawkbit.repository.exception.InvalidTenantConfigurationKeyException;
import org.eclipse.hawkbit.tenancy.configuration.validator.TenantConfigurationStringValidator;
import org.eclipse.hawkbit.tenancy.configuration.validator.TenantConfigurationValidator;
import org.eclipse.hawkbit.tenancy.configuration.validator.TenantConfigurationValidatorException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;

/**
 * Properties for tenant configuration default values.
 */
@ConfigurationProperties("hawkbit.server.tenant")
public class TenantConfigurationProperties {

    private final Map<String, TenantConfigurationKey> configuration = new HashMap<>();
    /**
     * CDN configuration
     */
    private String[] cdn = new String[]{};

    /**
     * @return full map of all configured tenant properties
     */
    public Map<String, TenantConfigurationKey> getConfiguration() {
        return configuration;
    }

    /**
     * @return full list of {@link TenantConfigurationKey}s
     */
    public Collection<TenantConfigurationKey> getConfigurationKeys() {
        return configuration.values();
    }

    /**
     * @param keyName name of the TenantConfigurationKey
     * @return the TenantConfigurationKey with the name keyName
     */
    public TenantConfigurationKey fromKeyName(final String keyName) {
        return configuration.values().stream().filter(conf -> conf.getKeyName().equals(keyName)).findAny()
                .orElseThrow(() -> new InvalidTenantConfigurationKeyException(
                        "The given configuration key " + keyName + " does not exist."));
    }

    /**
     * Cdn getter
     *
     * @return String[] the cdn array
     */
    public String[] getCdn() {
        return cdn;
    }

    /**
     * Cdn setter
     *
     * @param cdn the cdn array
     */
    public void setCdn(String[] cdn) {
        this.cdn = cdn;
    }

    /**
     * Tenant specific configurations which can be configured for each tenant
     * separately by means of override of the system defaults.
     */
    public static class TenantConfigurationKey {

        /**
         * Header based authentication enabled.
         */
        public static final String AUTHENTICATION_MODE_HEADER_ENABLED = "authentication.header.enabled";

        /**
         * Header based authentication authority name.
         */
        public static final String AUTHENTICATION_MODE_HEADER_AUTHORITY_NAME = "authentication.header.authority";

        /**
         * Target token based authentication enabled.
         */
        public static final String AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED = "authentication.targettoken.enabled";

        /**
         * Gateway token based authentication enabled.
         */
        public static final String AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_ENABLED = "authentication.gatewaytoken.enabled";

        /**
         * Gateway token value.
         */
        public static final String AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY = "authentication.gatewaytoken.key";

        /**
         * See system default in
         * {@link ControllerPollProperties#getPollingTime()}.
         */
        public static final String POLLING_TIME_INTERVAL = "pollingTime";

        /**
         * See system default in
         * {@link ControllerPollProperties#getMinPollingTime()}.
         */
        public static final String MIN_POLLING_TIME_INTERVAL = "minPollingTime";

        /**
         * See system default in
         * {@link ControllerPollProperties#getMaintenanceWindowPollCount()}.
         */
        public static final String MAINTENANCE_WINDOW_POLL_COUNT = "maintenanceWindowPollCount";

        /**
         * See system default in
         * {@link ControllerPollProperties#getPollingOverdueTime()}.
         */
        public static final String POLLING_OVERDUE_TIME_INTERVAL = "pollingOverdueTime";

        /**
         * See system default {@link Download#isEnabled()}.
         */
        public static final String ANONYMOUS_DOWNLOAD_MODE_ENABLED = "anonymous.download.enabled";

        /**
         * Represents setting if approval for a rollout is needed.
         */
        public static final String ROLLOUT_APPROVAL_ENABLED = "rollout.approval.enabled";

        /**
         * Repository on autoclose mode instead of canceling in case of new DS
         * assignment over active actions.
         */
        public static final String REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED = "repository.actions.autoclose.enabled";

        /**
         * Switch to enable/disable automatic action cleanup.
         */
        public static final String ACTION_CLEANUP_ENABLED = "action.cleanup.enabled";

        /**
         * Specifies the action expiry in milli-seconds.
         */
        public static final String ACTION_CLEANUP_ACTION_EXPIRY = "action.cleanup.actionExpiry";

        /**
         * Specifies the action status.
         */
        public static final String ACTION_CLEANUP_ACTION_STATUS = "action.cleanup.actionStatus";

        /**
         * Switch to enable/disable the multi-assignment feature.
         */
        public static final String MULTI_ASSIGNMENTS_ENABLED = "multi.assignments.enabled";

        /**
         * Switch to enable/disable the batch-assignment feature.
         */
        public static final String BATCH_ASSIGNMENTS_ENABLED = "batch.assignments.enabled";

        /**
         * Switch to enable/disable the user-confirmation flow
         */
        public static final String USER_CONFIRMATION_ENABLED = "user.confirmation.flow.enabled";

        /**
         * Switch to enable/disable the user-confirmation flow
         */
        public static final String ARTIFACT_DOWNGRADE_ENABLED = "distribution.software.downgrade.enabled";

        /**
         * Maximum artifact download duration in days
         * A -ve one (-1) is infinite days
         */
        public static final String ARTIFACT_MAX_DOWNLOAD_DURATION_TIMER = "artifact.max.download.duration.timer";

        /**
         * Ability to request onboard to share DTC or not
         */
        public static final String DTC_REQUIRED_VALUE = "dtc.required.value";

        /**
         * Maximum artifact download retry count
         */
        public static final String ARTIFACT_DOWNLOAD_RETRY_COUNT = "artifact.download.retry.count";

        /**
         * Maximum time in seconds required for update to finish including rollback time.
         */
        public static final String DEPLOYMENT_MAX_UPDATE_TIME = "deployment.max.update.time";

        /**
         * Minimum time in seconds required for update to finish including rollback time.
         */
        public static final String DEPLOYMENT_MIN_UPDATE_TIME = "deployment.min.update.time";

        /**
         * Maximum time in seconds required for update to finish including rollback time for WIFI_ONLY connectivity type.
         */
        public static final String ARTIFACT_MAX_WIFI_DOWNLOAD_DURATION_TIMER = "artifact.max.wifi.download.duration.timer";

        /**
         * Maximum time in seconds required for update to finish including rollback time for CELLULAR connectivity type.
         */
        public static final String ARTIFACT_MAX_CELLULAR_DOWNLOAD_DURATION_TIMER = "artifact.max.cellular.download.duration.timer";

        /**
         * Default CDN configuration key
         */
        public static final String DEFAULT_CDN = "default.cdn";

        /**
         * PKI source name to fetch private key
         */
        public static final String COSMOS_SERVER_PKI_SOURCE_NAME = "cosmos.server.pki.source.name";

        /**
         * List of PKI source names to fetch private key
         */
        public static final String COSMOS_SERVER_PKI_SOURCE_NAME_LIST = "cosmos.server.pki.source.name.list";

        /**
         * INTERNAL PKI source name to fetch private key from internal certificate path
         */
        public static final String INTERNAL_PKI_SOURCE_NAME = "INTERNAL";

        /**
         * Configuration property for the maximum size of each log file in bytes for deployment logs
         */
        public static final String DEPLOYMENT_LOG_MAX_EACH_FILE_SIZE = "deployment.log.max.each.file.size";

        /**
         * Configuration property for the target percentage for a rollout group
         */
        public static final String ROLLOUT_GROUP_TARGET_PERCENTAGE = "rollout.group.target.percentage";

        /**
         * Configuration property for the percentage for a rollout group success trigger threshold
         */
        public static final String ROLLOUT_GROUP_TRIGGER_THRESHOLD_PERCENTAGE = "rollout.group.trigger.threshold.percentage";

        /**
         * Configuration property for the percentage for a rollout group error trigger threshold
         */
        public static final String ROLLOUT_GROUP_ERROR_THRESHOLD_PERCENTAGE = "rollout.group.error.threshold.percentage";

        /**
         * Configuration property for the collection required for deployment log
         */
        public static final String DEPLOYMENT_LOG_COLLECTION_REQUIRED = "deployment.log.collection.required";

        /**
         * Configuration property for the Maximum success vin for deployment log
         */
        public static final String DEPLOYMENT_LOG_MAX_SUCCESS_VIN = "deployment.log.max.success.vin";

        /**
         * Configuration property for the Maximum Failure vin for deployment log
         */
        public static final String DEPLOYMENT_LOG_MAX_FAILURE_VIN = "deployment.log.max.failure.vin";

        /**
         * Configuration property for the Maximum All file size for deployment log
         */
        public static final String DEPLOYMENT_LOG_MAX_ALL_FILE_SIZE = "deployment.log.max.all.file.size";

        /**
         * Configuration property for the Maximum number of files for deployment log
         */
        public static final String DEPLOYMENT_LOG_MAX_NUMBER_OF_FILES = "deployment.log.max.number.of.files";
        /**
         * Configuration property for Mandatory RSP file types
         */
        public static final String MANDATORY_RSP_KEY="rollout.mandatory.support-package.rsp";
        /**
         * Configuration property for Mandatory ESP file types
         */
        public static final String MANDATORY_ESP_KEY="rollout.mandatory.support-package.esp";
        /**
         * Configuration property for Signature validation
         */
        public static final String SIGNATURE_VALIDATION_ENABLED = "ddi.inventory.signatureValidationEnabled";

        /**
         * Configuration property for Signature validation
         */
        public static final String STATIC_SIGNATURE_VALIDATION_ENABLED = "ddi.staticInventory.signatureValidationEnabled";

        /**
         * Configuration property for Signature validation
         */
        public static final String RAW_SIGNATURE_VALIDATION_ENABLED = "ddi.rawInventory.signatureValidationEnabled";

        /**
         * Configuration property for Retry rollout for all vehicles.
         */
        public static final String RETRY_ROLLOUT_ALL_VEHICLES = "rollout.retry.all.vehicles";


        public static final String CLONE_ROLLOUT ="rollout.clone.enabled";

        /**
         * Configuration property for Retry rollout for all succeeded vehicles.
         */
        public static final String RETRY_ROLLOUT_ALL_SUCCEEDED_VEHICLES = "rollout.retry.all.succeeded.vehicles";

        /**
         * Configuration property for Retry rollout for all failed vehicles.
         */
        public static final String RETRY_ROLLOUT_ALL_FAILED_VEHICLES = "rollout.retry.all.failed.vehicles";

        /**
         * Configuration property for Retry rollout for all canceled vehicles.
         */
        public static final String RETRY_ROLLOUT_ALL_CANCELED_VEHICLES = "rollout.retry.all.canceled.vehicles";

        /**
         * Configuration property for Retry rollout for all not executed vehicles.
         */
        public static final String RETRY_ROLLOUT_ALL_NOTEXECUTED_VEHICLES = "rollout.retry.all.notexecuted.vehicles";


        /**
         * Configuration property for Retry rollout for individual succeeded vehicles.
         */
        public static final String RETRY_ROLLOUT_INDIVIDUAL_SUCCEEDED_VEHICLES = "rollout.retry.individual.succeeded.vehicles";

        /**
         * Configuration property for Retry rollout for individual failed vehicles.
         */
        public static final String RETRY_ROLLOUT_INDIVIDUAL_FAILED_VEHICLES = "rollout.retry.individual.failed.vehicles";

        /**
         * Configuration property for Retry rollout for individual canceled vehicles.
         */
        public static final String RETRY_ROLLOUT_INDIVIDUAL_CANCELED_VEHICLES = "rollout.retry.individual.canceled.vehicles";

        /**
         * Configuration property for Retry rollout for individual not executed vehicles.
         */
        public static final String RETRY_ROLLOUT_INDIVIDUAL_NOTEXECUTED_VEHICLES = "rollout.retry.individual.notexecuted.vehicles";

        /**
         * Common log level configuration for COSMOS components.
         */
        public static final String VEHICLE_LOG_LEVEL = "common.log.level";


        private String keyName;
        private String defaultValue = "";
        private Class<?> dataType = String.class;
        private Class<? extends TenantConfigurationValidator> validator = TenantConfigurationStringValidator.class;

        public String getKeyName() {
            return keyName;
        }

        public void setKeyName(final String keyName) {
            this.keyName = keyName;
        }

        /**
         * @return the data type of the tenant configuration value. (e.g.
         * Integer.class, String.class)
         */
        @SuppressWarnings("unchecked")
        public <T> Class<T> getDataType() {
            return (Class<T>) dataType;
        }

        public void setDataType(final Class<?> dataType) {
            this.dataType = dataType;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(final String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public Class<? extends TenantConfigurationValidator> getValidator() {
            return validator;
        }

        public void setValidator(final Class<? extends TenantConfigurationValidator> validator) {
            this.validator = validator;
        }

        /**
         * validates if a object matches the allowed data format of the
         * corresponding key
         *
         * @param context application context
         * @param value   which will be validated
         * @throws TenantConfigurationValidatorException is thrown, when object is invalid
         */
        public void validate(final ApplicationContext context, final Object value) {
            final TenantConfigurationValidator createdBean = context.getAutowireCapableBeanFactory()
                    .createBean(validator);
            try {
                createdBean.validate(value);
            } finally {
                context.getAutowireCapableBeanFactory().destroyBean(createdBean);
            }
        }

    }
}
