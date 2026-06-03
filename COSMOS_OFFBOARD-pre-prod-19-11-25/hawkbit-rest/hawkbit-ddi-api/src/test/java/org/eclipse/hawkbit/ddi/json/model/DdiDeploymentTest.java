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
import org.cosmos.models.ddi.DdiDeployment;
import org.cosmos.models.ddi.DdiDsMetadata;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.cosmos.models.ddi.DdiDeployment.DdiMaintenanceWindowStatus.AVAILABLE;
import static org.cosmos.models.ddi.DdiDeployment.HandlingType.ATTEMPT;
import static org.cosmos.models.ddi.DdiDeployment.HandlingType.FORCED;

/**
 * Test serialization of DDI api model 'DdiDeployment'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serialization of DDI api Models")
class DdiDeploymentTest {

    private static final String TEST_KEY = "testKey";
    private ObjectMapper mapper = new ObjectMapper();

    @Test
    @Description("Verify the correct serialization and deserialization of the model")
    void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        DdiDsMetadata datasetMetadata = new DdiDsMetadata();
        datasetMetadata.put(TEST_KEY, "testValue");
        DdiDeployment ddiDeployment = new DdiDeployment(FORCED, ATTEMPT, Collections.emptyList(),
                AVAILABLE, datasetMetadata, DdiDeployment.ConnectivityType.BOTH);

        // Test
        String serializedDdiDeployment = mapper.writeValueAsString(ddiDeployment);
        DdiDeployment deserializedDdiDeployment = mapper.readValue(serializedDdiDeployment, DdiDeployment.class);

        assertThat(serializedDdiDeployment).contains(ddiDeployment.getDownload().getName(),
                ddiDeployment.getMaintenanceWindow().getStatus());
        assertThat(deserializedDdiDeployment.getDownload().getName()).isEqualTo(ddiDeployment.getDownload().getName());
        assertThat(deserializedDdiDeployment.getUpdate().getName()).isEqualTo(ddiDeployment.getUpdate().getName());
        assertThat(deserializedDdiDeployment.getMaintenanceWindow().getStatus()).isEqualTo(
                ddiDeployment.getMaintenanceWindow().getStatus());
        assertThat(deserializedDdiDeployment.getDatasetMetadata().get(TEST_KEY)).isEqualTo(
                ddiDeployment.getDatasetMetadata().get(TEST_KEY));
        assertThat(deserializedDdiDeployment.getConnectivityType()).isEqualTo(ddiDeployment.getConnectivityType());
    }

    @Test
    @Description("Verify the correct deserialization of a model with a additional unknown property")
    void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        String serializedDdiDeployment = "{\"download\":\"forced\",\"update\":\"attempt\", "
                + "\"maintenanceWindow\":\"available\",\"chunks\":[], \"ddiMetadata\" : { \"key\":\"value\" }, "
                + "\"connectivityType\": \"both\", "
                + "\"unknownProperty\":\"test\"}";

        // Test
        DdiDeployment ddiDeployment = mapper.readValue(serializedDdiDeployment, DdiDeployment.class);

        assertThat(ddiDeployment.getDownload().getName()).isEqualTo(FORCED.getName());
        assertThat(ddiDeployment.getUpdate().getName()).isEqualTo(ATTEMPT.getName());
        assertThat(ddiDeployment.getMaintenanceWindow().getStatus()).isEqualTo(AVAILABLE.getStatus());
        assertThat(ddiDeployment.getDatasetMetadata().get("key")).contains("value");
        assertThat(ddiDeployment.getConnectivityType()).isEqualTo(DdiDeployment.ConnectivityType.BOTH);
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        String serializedDdiDeployment = "{\"download\":[\"forced\"],\"update\":\"attempt\", "
                + "\"maintenanceWindow\":\"available\",\"chunks\":[], \"ddiMetadata\" : {}}";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class).isThrownBy(
                () -> mapper.readValue(serializedDdiDeployment, DdiDeployment.class));
    }
}