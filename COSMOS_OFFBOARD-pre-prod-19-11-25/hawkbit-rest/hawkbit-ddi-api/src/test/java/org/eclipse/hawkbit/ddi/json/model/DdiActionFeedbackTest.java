/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.ddi.json.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.assertj.core.util.Lists;
import org.cosmos.models.ddi.DdiActionFeedbacks;
import org.cosmos.models.ddi.DdiDownload;
import org.cosmos.models.ddi.DdiPackage;
import org.cosmos.models.ddi.DdiStatus;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test serialization of DDI api model 'DdiActionFeedback'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serialization of DDI api Models")
class DdiActionFeedbackTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private static final long TIMESTAMP_IN_MILLIS = 1737079413000L;

    @Test
    @Description("Verify the correct serialization and deserialization of the model with minimal payload")
    void shouldSerializeAndDeserializeObjectWithoutOptionalValues() throws IOException {
        // Setup
        final List<DdiStatus> ddiStatuses = Arrays.asList(new DdiStatus(DdiStatus.ExecutionStatus.LOG_UPLOAD_SUCCESS, null, null, Lists.emptyList(), TIMESTAMP_IN_MILLIS),
                new DdiStatus(DdiStatus.ExecutionStatus.DD_ACCEPTED, null, null, Lists.emptyList(), TIMESTAMP_IN_MILLIS));
        final DdiActionFeedbacks ddiActionFeedback = new DdiActionFeedbacks(null, ddiStatuses);

        // Test
        final String serializedDdiActionFeedback = mapper.writeValueAsString(ddiActionFeedback);
        final DdiActionFeedbacks deserializedDdiActionFeedback = mapper.readValue(serializedDdiActionFeedback,
                DdiActionFeedbacks.class);

        assertThat(deserializedDdiActionFeedback.getStatuses()).hasToString(ddiStatuses.toString());
    }

    @Test
    @Description("Verify the correct serialization and deserialization of the model with all values provided")
    void shouldSerializeAndDeserializeObjectWithOptionalValues() throws IOException {
        // Setup
        final Long time = System.currentTimeMillis();
        final DdiDownload ddiDownload = new DdiDownload(70, new DdiPackage(10,10));
        final List<DdiStatus> ddiStatus = Arrays.asList(new DdiStatus(DdiStatus.ExecutionStatus.DD_ACCEPTED, ddiDownload, 200, Collections.singletonList("myMessage"), TIMESTAMP_IN_MILLIS),
                new DdiStatus(DdiStatus.ExecutionStatus.DOWNLOAD_COMPLETED, ddiDownload, 200, Collections.singletonList("myMessage"), TIMESTAMP_IN_MILLIS));
        final DdiActionFeedbacks ddiActionFeedback = new DdiActionFeedbacks(time, ddiStatus);

        // Test
        final String serializedDdiActionFeedback = mapper.writeValueAsString(ddiActionFeedback);
        final DdiActionFeedbacks deserializedDdiActionFeedback = mapper.readValue(serializedDdiActionFeedback,
                DdiActionFeedbacks.class);

        assertThat(serializedDdiActionFeedback).contains(Long.toString(time));
        assertThat(deserializedDdiActionFeedback.getTime()).isEqualTo(time);
        assertThat(deserializedDdiActionFeedback.getStatuses()).hasToString(ddiStatus.toString());
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        final String serializedDdiActionFeedback = "{\"time\":\"20190809T121314\",\"statuses\":[{\"execution\":\"[closed]\",\"download\":null,\"details\":[],\"timestamp\":\"2019-08-09T12:13:14Z\"}]}";

        assertThatExceptionOfType(MismatchedInputException.class).isThrownBy(
                () -> mapper.readValue(serializedDdiActionFeedback, DdiActionFeedbacks.class));
    }

    @Test
    @Description("Verify that deserialization works if optional fields are not parsed")
    void shouldConvertItWithoutOptionalFieldTime() throws JsonProcessingException {
        // Setup
        final String serializedDdiActionFeedback = "{\n" + //
                "  \"statuses\" : [{\n" + //
                "    \"execution\" : \"canceled\",\n" + //
                "    \"details\" : [ \"Some message\" ],\n" + //
                "    \"errorCode\" : [ \"ERR_00020181\" ],\n" + //
                "     \"timestamp\" : 1565357594000\n" + //
                "  }]\n" + //
                "}";//

        assertThat(mapper.readValue(serializedDdiActionFeedback, DdiActionFeedbacks.class)).satisfies(deserializedDdiActionFeedback ->  {
            assertThat(deserializedDdiActionFeedback.getTime()).isNull();
            assertThat(deserializedDdiActionFeedback.getStatuses()).isNotNull();
            assertThat(deserializedDdiActionFeedback.getStatuses().get(0).getDownload()).isNull();
            assertThat(deserializedDdiActionFeedback.getStatuses().get(0).getCode()).isNull();
            assertThat(deserializedDdiActionFeedback.getStatuses().get(0).getExecution()).isEqualTo(DdiStatus.ExecutionStatus.CANCELED);
            assertThat(deserializedDdiActionFeedback.getStatuses().get(0).getDetails()).hasSize(1);
            assertThat(deserializedDdiActionFeedback.getStatuses().get(0).getErrorCode().get(0)).isEqualTo("ERR_00020181");
            assertThat(deserializedDdiActionFeedback.getStatuses().get(0).getTimestamp()).isEqualTo(1565357594000L);
        });
    }

}
