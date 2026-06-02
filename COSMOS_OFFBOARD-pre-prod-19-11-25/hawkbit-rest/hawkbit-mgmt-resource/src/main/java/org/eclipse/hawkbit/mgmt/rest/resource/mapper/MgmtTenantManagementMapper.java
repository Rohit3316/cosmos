/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.cosmos.models.mgmt.system.dto.MgmtSystemTenantConfigurationValue;
import org.cosmos.models.mgmt.systemmanagement.dto.MgmtAllTenantResponse;
import org.cosmos.models.mgmt.systemmanagement.dto.MgmtTenant;
import org.cosmos.models.mgmt.systemmanagement.dto.MgmtTenantResponse;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTypeRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTenantManagementRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.MgmtTenantManagementResource;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 */
public final class MgmtTenantManagementMapper {

    private MgmtTenantManagementMapper() {
        // Utility class
    }

    public static Map<String, MgmtSystemTenantConfigurationValue> toResponse(
            final TenantConfigurationManagement tenantConfigurationManagement,
            final TenantConfigurationProperties tenantConfigurationProperties, final String pathTenant, final long tenantId) {

        return tenantConfigurationProperties.getConfigurationKeys().stream()
                .collect(Collectors.toMap(TenantConfigurationProperties.TenantConfigurationKey::getKeyName, key -> toResponse(key.getKeyName(),
                        tenantConfigurationManagement.getConfigurationValue(key.getKeyName(), pathTenant), tenantId)));
    }

    public static MgmtSystemTenantConfigurationValue toResponse(final String key,
                                                         final TenantConfigurationValue<?> repoConfValue, final long tenantId) {
        final MgmtSystemTenantConfigurationValue restConfValue = new MgmtSystemTenantConfigurationValue();

        restConfValue.setValue(repoConfValue.getValue());
        restConfValue.setGlobal(repoConfValue.isGlobal());
        restConfValue.setCreatedAt(repoConfValue.getCreatedAt());
        restConfValue.setCreatedBy(repoConfValue.getCreatedBy());
        restConfValue.setLastModifiedAt(repoConfValue.getLastModifiedAt());
        restConfValue.setLastModifiedBy(repoConfValue.getLastModifiedBy());

        restConfValue.add(WebMvcLinkBuilder.linkTo(methodOn(MgmtTenantManagementResource.class).getTenantConfigurationValue(tenantId, key))
                .withSelfRel().expand());

        return restConfValue;
    }

    /**
     * Create a response for tenant
     *
     * @param
     * @return the response
     */
    public static MgmtTenantResponse toResponse(final TenantMetaData tenantMetaData, final TenantConfigurationManagement tenantConfigurationManagement,
                                                final TenantConfigurationProperties tenantConfigurationProperties, final String pathTenant) {
        if (tenantMetaData == null) {
            return null;
        }
        final MgmtTenantResponse tenant = new MgmtTenantResponse();
        tenant.setCreatedAt(tenantMetaData.getCreatedAt());
        tenant.setCreatedBy(tenantMetaData.getCreatedBy());

        tenant.setLastModifiedBy(tenantMetaData.getLastModifiedBy());
        tenant.setLastModifiedAt(tenantMetaData.getLastModifiedAt());

        tenant.setOptLockRevision(tenantMetaData.getOptLockRevision());

        tenant.setTenant(tenantMetaData.getTenant());
        tenant.setDefaultDistributionType(tenantMetaData.getDefaultDsType().getId());

        tenant.add(linkTo(methodOn(MgmtDistributionSetTypeRestApi.class).getDistributionSetType(tenantMetaData.getTenantId(), tenantMetaData.getDefaultDsType().getId()))
                .withRel("distributionsettypes").expand());

        tenant.setConfigurations(toResponse(tenantConfigurationManagement, tenantConfigurationProperties, pathTenant, tenantMetaData.getTenantId()));

        return tenant;
    }

    public static MgmtAllTenantResponse toResponse(final List<TenantMetaData> tenantMetaDataList) {
        if (tenantMetaDataList == null) {
            return null;
        }

        final MgmtAllTenantResponse mgmtAllTenantResponse = new MgmtAllTenantResponse();
        List<MgmtTenant> tenants = new ArrayList<>();
        for (TenantMetaData tenantMetaData : tenantMetaDataList) {
            tenants.add(new MgmtTenant(tenantMetaData.getTenantId(), tenantMetaData.getTenant(),
                    linkTo(methodOn(MgmtTenantManagementRestApi.class).getTenant(tenantMetaData.getTenantId()))
                            .withSelfRel().expand()));
        }
        mgmtAllTenantResponse.setTenants(tenants);
        return mgmtAllTenantResponse;
    }
    public static MgmtTenant toResponse(TenantMetaData tenantMetaData) {
        return new MgmtTenant(tenantMetaData.getTenantId(), tenantMetaData.getTenant(),
                linkTo(methodOn(MgmtTenantManagementRestApi.class).getTenant(tenantMetaData.getTenantId()))
                        .withSelfRel().expand());
    }
}