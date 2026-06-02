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
import org.cosmos.models.ddi.DdiDownload;
import org.cosmos.models.ddi.DdiPackage;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test serializability of DDI api model 'DdiDownload'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
public class DdiDownloadTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @Description("Verify the correct serialization and deserialization of the model")
    public void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        DdiPackage ddiPackage = new DdiPackage(30, 100);
        DdiDownload ddiDownload = new DdiDownload(80, ddiPackage);

        // Test
        String serializedDdiDownload = mapper.writeValueAsString(ddiDownload);
        DdiDownload deserializedDdiDownload = mapper.readValue(serializedDdiDownload, DdiDownload.class);

        assertThat(serializedDdiDownload).contains(ddiDownload.getPercentage().toString(), ddiPackage.getCnt().toString(),
                ddiPackage.getOf().toString());
        assertThat(deserializedDdiDownload.getPercentage()).isEqualTo(ddiDownload.getPercentage());
        assertThat(deserializedDdiDownload.getPackage().getCnt()).isEqualTo(ddiPackage.getCnt());
        assertThat(deserializedDdiDownload.getPackage().getOf()).isEqualTo(ddiPackage.getOf());
    }

    @Test
    @Description("Verify the correct deserialization of a model with a additional unknown property")
    public void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        String serializedDdiDownload = "{\"percentage\":70,\"package\":{\"cnt\":30,\"of\":100},\"unknownProperty\":\"test\"}";

        // Test
        DdiDownload ddiDownload = mapper.readValue(serializedDdiDownload, DdiDownload.class);

        assertThat(ddiDownload.getPercentage()).isEqualTo(70);
        assertThat(ddiDownload.getPackage().getCnt()).isEqualTo(30);
        assertThat(ddiDownload.getPackage().getOf()).isEqualTo(100);
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        String serializedDdiDownload = "{\"percentage\":\"test\",\"package\":{\"cnt\":30,\"of\":100}}";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class).isThrownBy(
                () -> mapper.readValue(serializedDdiDownload, DdiDownload.class));
    }
}