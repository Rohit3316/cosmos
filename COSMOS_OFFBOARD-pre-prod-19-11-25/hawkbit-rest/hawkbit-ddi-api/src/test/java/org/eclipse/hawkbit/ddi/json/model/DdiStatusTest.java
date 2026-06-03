/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 * <p>
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
import java.io.IOException;
import java.util.Collections;
import java.util.stream.Stream;

import org.cosmos.models.ddi.DdiDownload;
import org.cosmos.models.ddi.DdiPackage;
import org.cosmos.models.ddi.DdiStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.cosmos.models.ddi.DdiStatus.ExecutionStatus.DOWNLOAD_IN_PROGRESS;

/**
 * Test serializability of DDI api model 'DdiStatus'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
public class DdiStatusTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private static Stream<Arguments> ddiStatusPossibilities() {
        final DdiPackage ddiPackage = new DdiPackage(30, 100);
        final DdiDownload ddiDownload = new DdiDownload(80, ddiPackage);
        return Stream.of(
                Arguments.of(ddiDownload, new DdiStatus(DOWNLOAD_IN_PROGRESS, ddiDownload, null, Collections.emptyList(), 1735684800000L)),
                Arguments.of(ddiDownload, new DdiStatus(DOWNLOAD_IN_PROGRESS, ddiDownload, null, Collections.singletonList("testMessage"), 1737089013000L)),
                Arguments.of(ddiDownload, new DdiStatus(DOWNLOAD_IN_PROGRESS, ddiDownload, 12, Collections.emptyList(), 1737089013000L)));
    }

    @ParameterizedTest
    @MethodSource("ddiStatusPossibilities")
    @Description("Verify the correct serialization and deserialization of the model")
    public void shouldSerializeAndDeserializeObject(final DdiDownload ddiDownload, final DdiStatus ddiStatus) throws IOException {
        // Test
        String serializedDdiStatus = mapper.writeValueAsString(ddiStatus);
        DdiStatus deserializedDdiStatus = mapper.readValue(serializedDdiStatus, DdiStatus.class);

        assertThat(serializedDdiStatus).contains(ddiStatus.getExecution().getName(), ddiDownload.getPercentage().toString(),
                ddiDownload.getPackage().getCnt().toString(), ddiDownload.getPackage().getOf().toString());
        assertThat(deserializedDdiStatus.getExecution()).isEqualTo(ddiStatus.getExecution());
        assertThat(deserializedDdiStatus.getDownload().getPercentage()).isEqualTo(ddiStatus.getDownload().getPercentage());
        assertThat(deserializedDdiStatus.getDownload().getPackage().getCnt()).isEqualTo(
                ddiStatus.getDownload().getPackage().getCnt());
        assertThat(deserializedDdiStatus.getDownload().getPackage().getOf()).isEqualTo(
                ddiStatus.getDownload().getPackage().getOf());
        assertThat(deserializedDdiStatus.getDetails()).isEqualTo(ddiStatus.getDetails());
    }

    @Test
    @Description("Verify the correct deserialization of a model with a additional unknown property")
    public void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        final String serializedDdiStatus = "{\"execution\":\"download_in_progress\",\"download\":{\"percentage\":80,"
                + "\"package\":{\"cnt\":30,\"of\":100}},\"details\":[],\"unknownProperty\":\"test\"}";

        // Test
        final DdiStatus ddiStatus = mapper.readValue(serializedDdiStatus, DdiStatus.class);

        assertThat(ddiStatus.getExecution()).isEqualTo(DOWNLOAD_IN_PROGRESS);
        assertThat(ddiStatus.getCode()).isNull();
        assertThat(ddiStatus.getDownload().getPercentage()).isEqualTo(80);
        assertThat(ddiStatus.getDownload().getPackage().getCnt()).isEqualTo(30);
        assertThat(ddiStatus.getDownload().getPackage().getOf()).isEqualTo(100);
    }

    @Test
    @Description("Verify the correct deserialization of a model with a provided code (optional)")
    public void shouldDeserializeObjectWithOptionalCode() throws IOException {
        // Setup
        String serializedDdiStatus = "{\"execution\":\"download_in_progress\",\"download\":{\"percentage\":80,"
                + "\"package\":{\"cnt\":30,\"of\":100}},\"code\": 12,\"details\":[]}";

        // Test
        DdiStatus ddiStatus = mapper.readValue(serializedDdiStatus, DdiStatus.class);

        assertThat(ddiStatus.getExecution()).isEqualTo(DOWNLOAD_IN_PROGRESS);
        assertThat(ddiStatus.getCode()).isEqualTo(12);
        assertThat(ddiStatus.getDownload().getPercentage()).isEqualTo(80);
        assertThat(ddiStatus.getDownload().getPackage().getCnt()).isEqualTo(30);
        assertThat(ddiStatus.getDownload().getPackage().getOf()).isEqualTo(100);
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        String serializedDdiStatus = "{\"execution\":[\"proceeding\"],\"download\":{\"percentage\":\"none\","
                + "\"package\":{\"cnt\":30,\"of\":100}},\"details\":[]}";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class).isThrownBy(
                () -> mapper.readValue(serializedDdiStatus, DdiStatus.class));
    }
}