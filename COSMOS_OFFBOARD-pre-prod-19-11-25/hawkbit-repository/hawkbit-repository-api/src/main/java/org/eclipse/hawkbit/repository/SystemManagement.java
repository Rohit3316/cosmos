/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.cosmos.models.mgmt.systemmanagement.dto.MgmtValidTenant;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.repository.model.User;
import org.eclipse.hawkbit.repository.report.model.SystemUsageReport;
import org.eclipse.hawkbit.repository.report.model.SystemUsageReportWithTenants;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Central system management operations of the update server.
 */
public interface SystemManagement {

    /**
     * Checks if a specific tenant exists. The tenant will not be created lazy.
     *
     * @return {@code true} in case the tenant exits or {@code false} if not
     */
    String currentTenant();

    /**
     * Deletes all data related to a given tenant.
     *
     * @param tenant to delete
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.HAS_AUTH_TENANT_DELETE)
    void deleteTenant(@NotNull String tenant);


    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.HAS_AUTH_TENANT_CREATE)
    TenantMetaData createTenant(@NotNull String tenant);

    TenantMetaData createTenantWithoutPermission(@NotNull String tenant);

    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.HAS_AUTH_TENANT_CREATE)
    TenantMetaData cloneTenant(@NotNull @NotEmpty String ownerTenant, @NotNull @NotEmpty String targetTenant);

    /**
     * @return {@link TenantMetaData}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.HAS_AUTH_TENANT_READ)
    TenantMetaData getTenantMetadataOrThrowException(@NotNull String tenant);

    /**
     * @param pageable for paging information
     * @return list of all tenant names in the system.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.HAS_AUTH_ALL_TENANT_READ)
    Page<String> findTenants(@NotNull Pageable pageable);

    /**
     * @param pageable for paging information
     * @return list of all tenant names in the system.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.HAS_AUTH_ALL_TENANT_READ)
    Page<TenantMetaData> findTenantMetaData(@NotNull Pageable pageable);


    /**
     * Get all tenant metadata with read permission.
     *
     * @return list of tenant.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    List<MgmtValidTenant> validateTenants(List<String> requestedTenants);

    /**
     * Get all tenant metadata of user authorized tenants.
     *
     * @return list of tenant.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.HAS_AUTH_TENANT_READ)
    List<TenantMetaData> findAllUserAuthorizedTenants();

    /**
     * Runs consumer for each teant as
     * {@link TenantAware#runAsTenant(String, org.eclipse.hawkbit.tenancy.TenantAware.TenantRunner)}
     * sliently (i.e. exceptions will be logged but operations will continue for
     * further tenants).
     *
     * @param consumer to run as teanant
     */
    @PreAuthorize(SpringEvalExpressions.IS_SYSTEM_CODE)
    void forEachTenant(Consumer<String> consumer);

    /**
     * Calculated system usage statistics, both overall for the entire system
     * and per tenant;
     *
     * @return SystemUsageReport of the current system
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.HAS_AUTH_ALL_TENANT_READ)
    SystemUsageReportWithTenants getSystemUsageStatisticsWithTenants();

    /**
     * Calculated overall system usage statistics
     *
     * @return SystemUsageReport of the current system
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.HAS_AUTH_ALL_TENANT_READ)
    SystemUsageReport getSystemUsageStatistics();

    /**
     * @return {@link TenantMetaData} of {@link TenantAware#getCurrentTenant()}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.HAS_AUTH_READ_TARGET + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.HAS_AUTH_TENANT_READ + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    TenantMetaData getTenantMetadata();

    /**
     * @return {@link TenantMetaData} of {@link UserConfigurationManagement#getUserConfiguration(User, String)} ()}
     * where user is current authenticated user and String key is "tenant"
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.HAS_AUTH_TENANT_CONFIG_READ + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_SYSTEM_CODE)
    TenantMetaData getUserPreferredTenant();

    /**
     * Get {@link TenantMetaData} for the given {@link User}
     *
     * @param username {@link User#getUsername()}
     * @return {@link TenantMetaData}
     */
    TenantMetaData getUserPreferredTenant(final String username);

    /**
     * Update call for {@link TenantMetaData} of the user's preferred tenant.
     *
     * @param tenant to update
     * @return updated {@link TenantMetaData} entity
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN
            + SpringEvalExpressions.HAS_AUTH_OR + SpringEvalExpressions.HAS_AUTH_TENANT_CONFIG_CREATE
            + SpringEvalExpressions.HAS_AUTH_OR + SpringEvalExpressions.HAS_AUTH_TENANT_CONFIG_UPDATE)
    TenantMetaData updateUserPreferredTenant(final String tenant);

    /**
     * Get cdn for the current tenant
     *
     * @return cdn
     */
    String getTenantCdn();

    /**
     * Get cdn for the current tenant or else get default cdn.
     *
     * @return cdn
     */
    String getTenantCdnOrElseDefault();

    /**
     * Returns {@link TenantMetaData} of given and current tenant. Creates for
     * new tenants also two {@link SoftwareModuleType} (os and app) and
     * {@link RepositoryConstants#DEFAULT_DS_TYPES_IN_TENANT}
     * {@link DistributionSetType}s (os and os_app).
     * <p>
     * DISCLAIMER: this variant is used during initial login (where the tenant
     * is not yet in the session). Please user {@link #getTenantMetadata()} for
     * regular requests.
     *git
     * @param tenant to retrieve data for
     * @return {@link TenantMetaData} of given tenant
     */
    @PreAuthorize(SpringEvalExpressions.IS_SYSTEM_CODE + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.HAS_AUTH_TENANT_READ)
    TenantMetaData getTenantMetadata(@NotNull String tenant);

    /**
     * Update call for {@link TenantMetaData} of the current tenant.
     *
     * @param defaultDsType to update
     * @return updated {@link TenantMetaData} entity
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN
            + SpringEvalExpressions.HAS_AUTH_OR + SpringEvalExpressions.HAS_AUTH_TENANT_UPDATE
            + SpringEvalExpressions.HAS_AUTH_OR + SpringEvalExpressions.HAS_AUTH_TENANT_CONFIG_UPDATE)
    TenantMetaData updateTenantDsType(long defaultDsType);

    /**
     * Returns {@link TenantMetaData} of given tenant ID.
     *
     * @param tenantId to retrieve data for
     * @return {@link TenantMetaData} of given tenant
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.HAS_AUTH_TENANT_READ + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_SYSTEM_CODE)
    TenantMetaData getTenantMetadata(long tenantId);


    TenantMetaData getTenantMetadataNoPermission(long tenantId);


    List<TenantMetaData> getTenants(@NotEmpty Collection<Long> ids);

    Optional<String> getTenantByControllerId(String controllerId);

    /**
     * Counts the number of tenants in the system.
     *
     * @return the number of tenants
     */
    long countTenants();

    /**
     * Finds all tenants by the given tenant id.
     *
     * @param pageable the page request to page the result
     * @return a paged result of all metadata entries for a given
     * tenant id
     * @throws EntityNotFoundException if metadata with given tenant ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.HAS_AUTH_ALL_TENANT_READ)
    Page<TenantMetaData> findTenantsByRsql(@NotNull Pageable pageable, @NotNull String rsqlParam);

}
