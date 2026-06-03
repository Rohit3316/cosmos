/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
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
import org.cosmos.models.ddi.DdiDsMetadata;
import org.cosmos.models.ddi.DdiMetadata;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test serializability of DDI api model 'DdiDsMetadata'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
public class DdiDsMetadataTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    @Description("Verify the correct serialization and deserialization of the model")
    public void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        String key = "testKey";
        String value = "testValue";
        DdiDsMetadata ddiDsMetadata = new DdiDsMetadata();
        ddiDsMetadata.put(key, value);

        // Test
        String serializedDdiDsMetadata = mapper.writeValueAsString(ddiDsMetadata);
        DdiMetadata deserializedDdiDsMetadata = mapper.readValue(serializedDdiDsMetadata, DdiMetadata.class);

        assertThat(serializedDdiDsMetadata).contains(key, value);
        assertThat(deserializedDdiDsMetadata.get(key)).isEqualTo(ddiDsMetadata.get(key));
    }

    @Test
    @Description("Verify the correct deserialization of a model with a additional unknown property")
    public void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        String key = "testKey";
        String value = "testValue";
        String serializedDdiDsMetadata = "{\"" + key + "\": \"" + value + "\",\"unknownProperty\":\"test\"}";

        // Test
        DdiDsMetadata ddiDsMetadata = mapper.readValue(serializedDdiDsMetadata, DdiDsMetadata.class);

        assertThat(ddiDsMetadata.get(key)).isEqualTo(value);
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        String serializedDdiDsMetadata = "{\"key\":[\"testKey\"],\"value\":\"testValue\"}";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class).isThrownBy(
                () -> mapper.readValue(serializedDdiDsMetadata, DdiDsMetadata.class));
    }
}