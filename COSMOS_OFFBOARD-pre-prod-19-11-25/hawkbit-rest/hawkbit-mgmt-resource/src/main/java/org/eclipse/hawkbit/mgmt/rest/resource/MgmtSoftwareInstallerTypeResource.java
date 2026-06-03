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
import org.cosmos.models.mgmt.softwareinstallertype.dto.SoftwareInstallerTypeResponse;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareInstallerTypeRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtSoftwareInstallerTypesMapper;
import org.eclipse.hawkbit.pagination.PagingUtility;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.SoftwareInstallerTypeManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.SoftwareInstallerType;
import org.eclipse.hawkbit.rest.swagger.SwaggerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Software installer type management Resources
 */

@RestController
@Tag(name = SwaggerConstants.SOFTWARE_INSTALLER_TYPES)
public class MgmtSoftwareInstallerTypeResource implements MgmtSoftwareInstallerTypeRestApi {

    private static final Logger LOG = LoggerFactory.getLogger(MgmtSoftwareInstallerTypeResource.class);

    @Autowired
    private SoftwareInstallerTypeManagement softwareInstallerTypeManagement;


    /**
     * Handles the GET request for software installer types.
     *
     * @param pagingOffsetParam paging offset
     * @param pagingLimitParam paging limit
     * @return status ok if successful and list of all software
     * installer types
     */

    @Override
    @ResponseStatus(HttpStatus.OK)
    public List<SoftwareInstallerTypeResponse> getSoftwareInstallerTypes(int pagingOffsetParam, int pagingLimitParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam);
        final List<SoftwareInstallerType> softwareInstallerTypes = softwareInstallerTypeManagement.findAllSoftwareInstallerTypes(pageable);
        if (softwareInstallerTypes.isEmpty()) {
            throw new EntityNotFoundException("No Software Installer Types found");
        }
        LOG.debug("All Software Installer Types fetched successfully");
        return MgmtSoftwareInstallerTypesMapper.toSoftwareInstallerTypeResponse(softwareInstallerTypes);
    }
}
