/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.utils;

import java.util.Collections;

import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;

import java.util.Arrays;
import java.util.List;

import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ARTIFACT_DOWNGRADE_ENABLED;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.MULTI_ASSIGNMENTS_ENABLED;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.RAW_SIGNATURE_VALIDATION_ENABLED;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.SIGNATURE_VALIDATION_ENABLED;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.STATIC_SIGNATURE_VALIDATION_ENABLED;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.USER_CONFIRMATION_ENABLED;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.VEHICLE_LOG_LEVEL;

/**
 * A collection of static helper methods for the tenant configuration
 */
public final class TenantConfigHelper {

    private final TenantConfigurationManagement tenantConfigurationManagement;
    private final SystemSecurityContext systemSecurityContext;

    private TenantConfigHelper(final SystemSecurityContext systemSecurityContext,
                               final TenantConfigurationManagement tenantConfigurationManagement) {
        this.systemSecurityContext = systemSecurityContext;
        this.tenantConfigurationManagement = tenantConfigurationManagement;
    }

    /**
     * Setting the context of the tenant.
     *
     * @param systemSecurityContext
     *            Security context used to get the tenant and for execution
     * @param tenantConfigurationManagement
     *            to get the value from
     * @return is active
     */
    public static TenantConfigHelper usingContext(final SystemSecurityContext systemSecurityContext,
                                                  final TenantConfigurationManagement tenantConfigurationManagement) {
        return new TenantConfigHelper(systemSecurityContext, tenantConfigurationManagement);
    }

    /**
     * Is multi-assignments enabled for the current tenant
     *
     * @return is active
     */
    public boolean isMultiAssignmentsEnabled() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(MULTI_ASSIGNMENTS_ENABLED, Boolean.class).getValue());
    }

    /**
     * Is confirmation flow enabled for the current tenant
     *
     * @return is enabled
     */
    public boolean isConfirmationFlowEnabled() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(USER_CONFIRMATION_ENABLED, Boolean.class).getValue());
    }

    /**
     * Is distribution software downgrade is enabled for the current tenant
     *
     * @return is enabled
     */
    public boolean isDistributionSoftwareDowngradeEnabled() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(ARTIFACT_DOWNGRADE_ENABLED, Boolean.class).getValue());
    }

    public Integer getTenantConfigurationMaxRetryCountKey() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.ARTIFACT_DOWNLOAD_RETRY_COUNT, Integer.class).getValue());
    }

    public Integer getTenantConfigurationMaxDownloadKey() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.ARTIFACT_MAX_DOWNLOAD_DURATION_TIMER, Integer.class).getValue());
    }

    public Integer getTenantConfigurationMaxWifiDownloadKey() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.ARTIFACT_MAX_WIFI_DOWNLOAD_DURATION_TIMER, Integer.class).getValue());
    }

    public Integer getTenantConfigurationMaxCellularDownloadKey() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.ARTIFACT_MAX_CELLULAR_DOWNLOAD_DURATION_TIMER, Integer.class).getValue());
    }

    /**
     *
     * @return maximum time in seconds required to run update including rollback time.
     */
    public Integer getTenantConfigurationMaxUpdateTimeKey() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.DEPLOYMENT_MAX_UPDATE_TIME, Integer.class).getValue());
    }

    public List<MgmtSupportPackageFileType> getTenantConfigurationMandatoryRsp() {
        String mandatoryRspValues = systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.MANDATORY_RSP_KEY, String.class).getValue());
        if (mandatoryRspValues == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(mandatoryRspValues.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(esp -> Enum.valueOf(MgmtSupportPackageFileType.class, esp))
                .toList();
    }

    public List<MgmtSupportPackageFileType> getTenantConfigurationMandatoryEsp() {
        String mandatoryEspValues = systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.MANDATORY_ESP_KEY, String.class).getValue());
        if (mandatoryEspValues == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(mandatoryEspValues.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(esp -> Enum.valueOf(MgmtSupportPackageFileType.class, esp))
                .toList();
    }

    /**
     * Is Signature validation is enabled for the current tenant
     *
     * @return is enabled
     */
    public boolean isInventorySignatureValidationEnabled() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(SIGNATURE_VALIDATION_ENABLED, Boolean.class).getValue());
    }

    public boolean isRetryAllSucceededVehiclesEnabled() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.RETRY_ROLLOUT_ALL_SUCCEEDED_VEHICLES, Boolean.class)
                .getValue());
    }

    public boolean isRetryAllFailedVehiclesEnabled() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.RETRY_ROLLOUT_ALL_FAILED_VEHICLES, Boolean.class)
                .getValue());
    }

    public boolean isRetryAllCanceledVehiclesEnabled() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.RETRY_ROLLOUT_ALL_CANCELED_VEHICLES, Boolean.class)
                .getValue());
    }

    public boolean isRetryAllNotExecutedVehiclesEnabled() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.RETRY_ROLLOUT_ALL_NOTEXECUTED_VEHICLES, Boolean.class)
                .getValue());
    }

    public boolean isFullRolloutEnabled() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.RETRY_ROLLOUT_ALL_VEHICLES, Boolean.class)
                .getValue());
    }

    public boolean isCloneRolloutEnabled() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.CLONE_ROLLOUT, Boolean.class)
                .getValue());
    }

    public boolean isRetryIndividualSucceededVehiclesEnabled() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.RETRY_ROLLOUT_INDIVIDUAL_SUCCEEDED_VEHICLES, Boolean.class)
                .getValue());
    }

    public boolean isRetryIndividualFailedVehiclesEnabled() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.RETRY_ROLLOUT_INDIVIDUAL_FAILED_VEHICLES, Boolean.class)
                .getValue());
    }

    public boolean isRetryIndividualCanceledVehiclesEnabled() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.RETRY_ROLLOUT_INDIVIDUAL_CANCELED_VEHICLES, Boolean.class)
                .getValue());
    }

    public boolean isRetryIndividualNotExecutedVehiclesEnabled() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.RETRY_ROLLOUT_INDIVIDUAL_NOTEXECUTED_VEHICLES, Boolean.class)
                .getValue());
    }

    public boolean isStaticInventorySignatureValidationEnabled() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(STATIC_SIGNATURE_VALIDATION_ENABLED, Boolean.class).getValue());
    }

    public boolean isRawInventorySignatureValidationEnabled() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(RAW_SIGNATURE_VALIDATION_ENABLED, Boolean.class).getValue());
    }

    public Integer getVehicleLogLevel() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(VEHICLE_LOG_LEVEL, Integer.class).getValue());
    }
}
