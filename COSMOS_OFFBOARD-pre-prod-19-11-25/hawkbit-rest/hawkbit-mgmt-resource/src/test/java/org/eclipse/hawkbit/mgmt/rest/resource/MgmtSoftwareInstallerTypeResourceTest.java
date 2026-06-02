/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.mgmt.rest.resource;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.softwareinstallertype.dto.SoftwareInstallerTypeResponse;
import org.eclipse.hawkbit.repository.SoftwareInstallerTypeManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link MgmtSoftwareInstallerTypeResource} {@link RestController}.
 */

@Feature("Component Tests - Management API")
@Story("Software Installer Type")
public class MgmtSoftwareInstallerTypeResourceTest extends AbstractManagementApiIntegrationTest {

    @Mock
    private SoftwareInstallerTypeManagement softwareInstallerTypeRepository;

    @InjectMocks
    private MgmtSoftwareInstallerTypeResource mgmtSoftwareInstallerTypeResource;

    private final ObjectMapper objectMapper = new ObjectMapper();


    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @Description("Ensures that all the software installer types are successfully fetched")
    public void givenValidRequestWhenGetAllSoftwareInstallerTypesThenGetAllSoftwareInstallerTypes() throws Exception {
        MvcResult mvcResult = mvc.perform(get(MgmtRestConstants.SOFTWARE_INSTALLER_TYPES_MAPPING)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andReturn();
        List<SoftwareInstallerTypeResponse> responseList = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), objectMapper.getTypeFactory().constructCollectionType(List.class, SoftwareInstallerTypeResponse.class));
        assertNotNull(responseList);
        assertFalse(responseList.isEmpty());
        assertEquals(Integer.parseInt(MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT), responseList.size());
    }

    @Test
    @Description("Ensure the endpoint handles the case when there are no software installer types available")
    public void givenValidRequestWhenNoSoftwareInstallerTypesExistsThenReturnEntityNotFound() {
        when(softwareInstallerTypeRepository.findAllSoftwareInstallerTypes(any())).thenReturn(Collections.emptyList());
        Assertions.assertThrows(EntityNotFoundException.class, () -> mgmtSoftwareInstallerTypeResource.getSoftwareInstallerTypes(50, 200));
    }

    @Test
    @Description("Ensures that software installer types are correctly retrieved based on the specified offset and limit")
    public void givenValidPageRequestWhenGetAllSoftwareInstallerTypesThenGetAllSoftwareInstallerTypes() throws Exception {
        int startOffset = 0;
        int limit = 2;
        MvcResult mvcResult = mvc.perform(get(MgmtRestConstants.SOFTWARE_INSTALLER_TYPES_MAPPING + "?" + MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET + "=" + startOffset + "&" + MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT + "=" + limit)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andReturn();
        List<SoftwareInstallerTypeResponse> responseList = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), objectMapper.getTypeFactory().constructCollectionType(List.class, SoftwareInstallerTypeResponse.class));
        assertEquals(2, responseList.size());
    }

    @Test
    @Description("Ensures that the request for software installer types handles invalid offset and limit parameters correctly")
    public void givenInvalidPageRequestWhenGetAllSoftwareInstallerTypesThenGetAllSoftwareInstallerTypes() throws Exception {
        int startOffset = -1;
        int limit = 100000;
        MvcResult mvcResult = mvc.perform(get(MgmtRestConstants.SOFTWARE_INSTALLER_TYPES_MAPPING + "?" + MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET + "=" + startOffset + "&" + MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT + "=" + limit)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andReturn();
        List<SoftwareInstallerTypeResponse> responseList = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), objectMapper.getTypeFactory().constructCollectionType(List.class, SoftwareInstallerTypeResponse.class));
        assertTrue(responseList.size() < MgmtRestConstants.REQUEST_PARAMETER_PAGING_MAX_LIMIT);
    }

}
