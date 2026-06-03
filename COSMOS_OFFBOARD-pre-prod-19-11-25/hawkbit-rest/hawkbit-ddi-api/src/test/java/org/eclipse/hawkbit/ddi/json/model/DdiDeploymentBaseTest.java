/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.ddi.json.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.cosmos.models.ddi.DdiActionHistory;
import org.cosmos.models.ddi.DdiDeployment;
import org.cosmos.models.ddi.DdiDeploymentBase;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.cosmos.models.ddi.DdiDeployment.DdiMaintenanceWindowStatus.AVAILABLE;
import static org.cosmos.models.ddi.DdiDeployment.HandlingType.ATTEMPT;
import static org.cosmos.models.ddi.DdiDeployment.HandlingType.FORCED;

/**
 * Test serialization of DDI api model 'DdiDeploymentBase'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serialization of DDI api Models")
class DdiDeploymentBaseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @Description("Verify the correct serialization and deserialization of the model")
    void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        String id = "1234";
        DdiDeployment ddiDeployment = new DdiDeployment(FORCED, ATTEMPT, Collections.emptyList(), AVAILABLE);
        String actionStatus = "TestAction";
        DdiActionHistory ddiActionHistory = new DdiActionHistory(actionStatus,
                Arrays.asList("Action status message 1", "Action status message 2"));
        DdiDeploymentBase ddiDeploymentBase = new DdiDeploymentBase(id, ddiDeployment, ddiActionHistory);

        // Test
        String serializedDdiDeploymentBase = mapper.writeValueAsString(ddiDeploymentBase);
        DdiDeploymentBase deserializedDdiDeploymentBase = mapper.readValue(serializedDdiDeploymentBase,
                DdiDeploymentBase.class);

        assertThat(serializedDdiDeploymentBase).contains(id, FORCED.getName(), ATTEMPT.getName(), AVAILABLE.getStatus(),
                actionStatus);
        assertThat(deserializedDdiDeploymentBase.getDeployment().getDownload()).isEqualTo(ddiDeployment.getDownload());
        assertThat(deserializedDdiDeploymentBase.getDeployment().getUpdate()).isEqualTo(ddiDeployment.getUpdate());
        assertThat(deserializedDdiDeploymentBase.getDeployment().getMaintenanceWindow()).isEqualTo(
                ddiDeployment.getMaintenanceWindow());
        assertThat(deserializedDdiDeploymentBase.getActionHistory().toString()).isEqualTo(ddiActionHistory.toString());
    }

    @Test
    @Description("Verify the correct deserialization of a model with a additional unknown property")
    void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        String serializedDdiDeploymentBase = "{\"id\":\"1234\",\"deployment\":{\"download\":\"forced\","
                + "\"update\":\"attempt\",\"maintenanceWindow\":\"available\",\"chunks\":[]},"
                + "\"actionHistory\":{\"status\":\"TestAction\",\"messages\":[\"Action status message 1\","
                + "\"Action status message 2\"]},\"links\":[],\"unknownProperty\":\"test\"}";

        // Test
        DdiDeploymentBase ddiDeploymentBase = mapper.readValue(serializedDdiDeploymentBase, DdiDeploymentBase.class);

        assertThat(ddiDeploymentBase.getDeployment().getDownload().getName()).isEqualTo(FORCED.getName());
        assertThat(ddiDeploymentBase.getDeployment().getUpdate().getName()).isEqualTo(ATTEMPT.getName());
        assertThat(ddiDeploymentBase.getDeployment().getMaintenanceWindow().getStatus()).isEqualTo(
                AVAILABLE.getStatus());
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        String serializedDdiDeploymentBase = "{\"id\":[\"1234\"],\"deployment\":{\"download\":\"forced\","
                + "\"update\":\"attempt\",\"maintenanceWindow\":\"available\",\"chunks\":[]},"
                + "\"actionHistory\":{\"status\":\"TestAction\",\"messages\":[\"Action status message 1\","
                + "\"Action status message 2\"]},\"links\":[]}";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class).isThrownBy(
                () -> mapper.readValue(serializedDdiDeploymentBase, DdiDeploymentBase.class));
    }
}