/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import org.cosmos.models.mgmt.PagedList;
import org.cosmos.models.mgmt.system.dto.MgmtSystemTenantConfigurationValue;
import org.cosmos.models.mgmt.system.constants.MgmtSystemTenantConfigurationValueRequest;
import org.cosmos.models.mgmt.systemmanagement.dto.MgmtTenant;
import org.cosmos.models.mgmt.systemmanagement.dto.MgmtTenantCloneRequestBody;
import org.cosmos.models.mgmt.systemmanagement.dto.MgmtTenantRequestBody;
import org.cosmos.models.mgmt.systemmanagement.dto.MgmtTenantResponse;
import org.cosmos.models.mgmt.systemmanagement.dto.MgmtTenantValidationRequest;
import org.cosmos.models.mgmt.systemmanagement.dto.MgmtTenantValidationResponse;
import org.cosmos.models.mgmt.systemmanagement.dto.MgmtValidTenant;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTenantManagementRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtTenantManagementMapper;
import org.eclipse.hawkbit.pagination.PagingUtility;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.exception.InvalidTenantConfigurationKeyException;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.rest.aspect.TenantAware;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource for handling tenant specific configuration operations.
 */
@RestController
@Tag(name = "Tenants")
public class MgmtTenantManagementResource implements MgmtTenantManagementRestApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(MgmtTenantManagementResource.class);

    private final SystemManagement systemManagement;

    private final TenantConfigurationManagement tenantConfigurationManagement;
    private final TenantConfigurationProperties tenantConfigurationProperties;
    private final SystemSecurityContext systemSecurityContext;


    MgmtTenantManagementResource(final SystemManagement systemManagement,
                                 final TenantConfigurationManagement tenantConfigurationManagement, final TenantConfigurationProperties tenantConfigurationProperties, SystemSecurityContext systemSecurityContext) {
        this.systemManagement = systemManagement;
        this.tenantConfigurationManagement = tenantConfigurationManagement;
        this.tenantConfigurationProperties = tenantConfigurationProperties;
        this.systemSecurityContext = systemSecurityContext;
    }

    @Override
    @TenantAware
    public ResponseEntity<Map<String, MgmtSystemTenantConfigurationValue>> getTenantConfiguration(@PathVariable("tenantId") long tenantId) {
        //validate given tenant exists in the system.
        //In future once the tenant is changed to tenantId an implementation with an annotation can be added for ease of use.
        TenantMetaData result = systemManagement.getTenantMetadata(tenantId);
        LOGGER.debug("All configs fetched for tenant {}, return status {}", result.getTenant(), HttpStatus.OK);
        return ResponseEntity.ok(
                MgmtTenantManagementMapper.toResponse(tenantConfigurationManagement, tenantConfigurationProperties, result.getTenant(), tenantId));
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtSystemTenantConfigurationValue> getTenantConfigurationValue(
            @PathVariable("tenantId") long tenantId, @PathVariable("key") final String key) {
        //validate given tenant exists in the system.
        //In future once the tenant is changed to tenantId an implementation with an annotation can be added for ease of use.
        TenantMetaData result = systemManagement.getTenantMetadata(tenantId);
        LOGGER.debug("{} config value fetched for tenant {}, return status {}", key, result.getTenant(), HttpStatus.OK);
        return ResponseEntity.ok(MgmtTenantManagementMapper.toResponse(key,
                tenantConfigurationManagement.getConfigurationValue(key, result.getTenant()), tenantId));
    }

    @Override
    @TenantAware
    public ResponseEntity<Void> deleteTenantConfigurationValue(@PathVariable("tenantId") long tenantId, @PathVariable("key") final String key) {

        //Check for tenant exists else throw exception
        systemManagement.getTenantMetadata(tenantId);
        //Check if the key is present in Tenant Config else throw exception
        if (Boolean.FALSE.equals(tenantConfigurationManagement.existsByKey(key))) {
            throw new InvalidTenantConfigurationKeyException(String.format("The given configuration key %s does not " +
                    "exist in the given tenant %s", key, tenantId));
        }
        // delete the config key from given tenant config repository
        tenantConfigurationManagement.deleteConfiguration(key);

        LOGGER.debug("{} config value deleted, return status {}", key, HttpStatus.OK);
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtSystemTenantConfigurationValue> updateTenantConfigurationValue(
            @PathVariable("tenantId") long tenantId, @PathVariable("key") final String key,
            @RequestBody final MgmtSystemTenantConfigurationValueRequest configurationValueRest) {

        final TenantConfigurationValue<? extends Serializable> updatedValue = tenantConfigurationManagement
                .addOrUpdateConfiguration(key, configurationValueRest.getValue());

        return ResponseEntity.ok(MgmtTenantManagementMapper.toResponse(key, updatedValue, tenantId));
    }

    /**
     * Deletes the tenant data of a given tenant. USE WITH CARE!
     *
     * @return HttpStatus.OK
     */
    @Override
    @TenantAware
    public ResponseEntity<Void> deleteTenant(@PathVariable("tenantId") final long tenantId) {
        TenantMetaData result = systemManagement.getTenantMetadata(tenantId);
        systemManagement.deleteTenant(result.getTenant());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> createTenant(MgmtTenantRequestBody tenantRequestBody) {
        systemManagement.createTenant(tenantRequestBody.getTenant());
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtTenantResponse> getTenant(@PathVariable("tenantId") final long tenantId) {
        TenantMetaData tenant = systemManagement.getTenantMetadata(tenantId);
        TenantMetaData result = systemManagement.getTenantMetadataOrThrowException(tenant.getTenant());

        final MgmtTenantResponse response = MgmtTenantManagementMapper.toResponse(result, tenantConfigurationManagement, tenantConfigurationProperties, result.getTenant());

        return ResponseEntity.ok(response);

    }

    @Override
    @TenantAware
    public ResponseEntity<Void> cloneTenant(@PathVariable("tenantId") final long tenantId, MgmtTenantCloneRequestBody tenantRequestBody) {
        TenantMetaData result = systemManagement.getTenantMetadata(tenantId);
        systemManagement.cloneTenant(result.getTenant(), tenantRequestBody.getTenant());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<PagedList<MgmtTenant>> getAllTenants(int pagingOffsetParam, int pagingLimitParam, String sortParam, String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTenantSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Page<TenantMetaData> tenantsSlice;
        long totalTenants = 0L;
        if (Objects.nonNull(rsqlParam)) {
            tenantsSlice = systemManagement.findTenantsByRsql(pageable, rsqlParam);
            if (Objects.nonNull(tenantsSlice)) {
                totalTenants = tenantsSlice.getTotalElements();
            }
        } else {
            tenantsSlice = systemManagement.findTenantMetaData(pageable);
            totalTenants = systemManagement.countTenants();
        }
        final List<MgmtTenant> tenantDtos = tenantsSlice == null ? List.of() :
                tenantsSlice.getContent().stream()
                        .map(MgmtTenantManagementMapper::toResponse)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(new PagedList<>(tenantDtos, totalTenants));
    }

    /**
     * Validates the given tenants and returns the valid ones with their IDs.
     * @param request
     * @return
     */
    @Override
    public ResponseEntity<MgmtTenantValidationResponse> validateTenants(@Valid @RequestBody MgmtTenantValidationRequest request) {
        List<MgmtValidTenant> validTenants = systemManagement.validateTenants(request.getTenants());
        return ResponseEntity.ok(new MgmtTenantValidationResponse(validTenants));
    }
}