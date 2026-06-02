/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.distributionset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.qameta.allure.Story;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cosmos.models.mgmt.distributionset.dto.MgmtActionId;
import org.cosmos.models.mgmt.distributionset.dto.MgmtTargetAssignmentResponseBody;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Description;

@Story("Retrieve all open action ids")
@Description("Tests for the MgmtTargetAssignmentResponseBody")
public class MgmtTargetAssignmentResponseBodyTest {

    private static final List<Long> ASSIGNED_ACTIONS = Arrays.asList(4L, 5L, 6L);
    private static final int ALREADY_ASSIGNED_COUNT = 3;
    private static final String CONTROLLER_ID = "target";
    private static final Long TENANT_ID = 1L;
   private static final String ASSIGNED = "assigned";
   private static final String DESCRIPTION = "the assigned targets count";
   private static final String ALREADY_ASSIGNED = "alreadyAssigned";
   private static final String TARGETS_COUNT = "the already assigned targets count";
   private static final String TOTAL_TARGETS_COUNT = "the total targets count";
   private static final String TOTAL = "total";
   private static final String ACTIONS = "The created actions in result of this assignment";
   private static final String ASSIGNED_ACTIONS1 = "assignedActions";
   private static final String DESCRIPTION1 = "A created action in result of this assignment";

    @Test
    @Description("Tests that the ActionIds are serialized correctly in MgmtTargetAssignmentResponseBody")
    public void testActionIdsSerialization() throws IOException {
        final MgmtTargetAssignmentResponseBody responseBody = generateResponseBody();
        final ObjectMapper objectMapper = new ObjectMapper();
        final String responseBodyAsString = objectMapper.writeValueAsString(responseBody);
        final JsonNode jsonNode = objectMapper.readTree(responseBodyAsString);

        assertThat(jsonNode.has(ASSIGNED)).as(DESCRIPTION).isTrue();
        assertThat(jsonNode.get(ASSIGNED).isNumber()).as(DESCRIPTION).isTrue();
        assertThat(jsonNode.get(ASSIGNED).asLong()).as(DESCRIPTION)
                .isEqualTo(ASSIGNED_ACTIONS.size());

        assertThat(jsonNode.has(ALREADY_ASSIGNED)).as(TARGETS_COUNT).isTrue();
        assertThat(jsonNode.get(ALREADY_ASSIGNED).isNumber()).as(TARGETS_COUNT).isTrue();
        assertThat(jsonNode.get(ALREADY_ASSIGNED).asLong()).as(TARGETS_COUNT)
                .isEqualTo(ALREADY_ASSIGNED_COUNT);

        assertThat(jsonNode.has(TOTAL)).as(TOTAL_TARGETS_COUNT).isTrue();
        assertThat(jsonNode.get(TOTAL).isNumber()).as(TOTAL_TARGETS_COUNT).isTrue();
        assertThat(jsonNode.get(TOTAL).asLong()).as(TOTAL_TARGETS_COUNT)
                .isEqualTo(ALREADY_ASSIGNED_COUNT + ASSIGNED_ACTIONS.size());

        assertThat(jsonNode.has(ASSIGNED_ACTIONS1)).as(ACTIONS).isTrue();
        assertThat(jsonNode.get(ASSIGNED_ACTIONS1).isArray()).as(ACTIONS)
                .isTrue();
        assertThat(jsonNode.get(ASSIGNED_ACTIONS1).size()).as(ACTIONS)
                .isEqualTo(3);

        assertThat(jsonNode.get(ASSIGNED_ACTIONS1).get(0).isObject())
                .as(DESCRIPTION1).isTrue();
        assertThat(jsonNode.get(ASSIGNED_ACTIONS1).get(0).has("id")).as(DESCRIPTION1)
                .isTrue();
        assertThat(jsonNode.get(ASSIGNED_ACTIONS1).get(0).get("id").isNumber())
                .as(DESCRIPTION1).isTrue();
        assertThat(ASSIGNED_ACTIONS).as("The expected action ids")
                .contains(jsonNode.get(ASSIGNED_ACTIONS1).get(0).get("id").asLong());
    }

    private static MgmtTargetAssignmentResponseBody generateResponseBody() {
        MgmtTargetAssignmentResponseBody response = new MgmtTargetAssignmentResponseBody();
        response.setAssignedActions(
                ASSIGNED_ACTIONS.stream().map(id -> new MgmtActionId(CONTROLLER_ID, id,TENANT_ID)).collect(Collectors.toList()));
        response.setAlreadyAssigned(ALREADY_ASSIGNED_COUNT);
        return response;
    }
}
