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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cosmos.models.ddi.DdiConfigData;
import org.cosmos.models.ddi.DdiConfigDataDevice;
import org.cosmos.models.ddi.DdiConfigDataSoftware;
import org.cosmos.models.ddi.DdiUpdateMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test serializability of DDI api model 'DdiConfigData'
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
public class DdiConfigDataTest {

    private static final String DEVICE = "device1";
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @Description("Verify the correct serialization and deserialization of the model")
    public void shouldSerializeAndDeserializeObject() throws IOException {
        List<DdiConfigDataSoftware> softwareList = List.of(
                new DdiConfigDataSoftware("SW123", "1.0.0")
        );

        DdiConfigDataDevice device = new DdiConfigDataDevice(
                "1.0", // hwVersion
                "SN123456", // serialNumber
                "HW1234", // hwModel
                softwareList, // sw
                "typeExample", // type
                "valueExample" // value
        );

        Map<String, DdiConfigDataDevice> data = new HashMap<>();
        data.put(DEVICE, device);

        DdiConfigData ddiConfigData = new DdiConfigData(data, DdiUpdateMode.REPLACE);

        // Test serialization
        String serializedDdiConfigData = mapper.writeValueAsString(ddiConfigData);

        DdiConfigData deserializedDdiConfigData = mapper.readValue(serializedDdiConfigData, DdiConfigData.class);

        assertThat(serializedDdiConfigData).contains(DEVICE, "hwVersion", "serialNumber", "hwModel", "type", "value");

        assertThat(deserializedDdiConfigData.getMode()).isEqualTo(DdiUpdateMode.REPLACE);

        DdiConfigDataDevice deserializedDevice = deserializedDdiConfigData.getData().get(DEVICE);
        assertThat(deserializedDevice).isNotNull();
        assertThat(deserializedDevice.getHwVersion()).isEqualTo("1.0");
        assertThat(deserializedDevice.getSerialNumber()).isEqualTo("SN123456");
        assertThat(deserializedDevice.getHwModel()).isEqualTo("HW1234");
        assertThat(deserializedDevice.getSw()).hasSize(1);
        assertThat(deserializedDevice.getSw().get(0).getSwComponentID()).isEqualTo("SW123");
        assertThat(deserializedDevice.getSw().get(0).getSwVersion()).isEqualTo("1.0.0");
        assertThat(deserializedDevice.getType()).isEqualTo("typeExample");
        assertThat(deserializedDevice.getValue()).isEqualTo("valueExample");
    }


    @Test
    @Description("Verify the correct deserialization of a model with an additional unknown property")
    public void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        String serializedDdiConfigData = "{\"mode\":\"replace\",\"data\":{\"0xF161\":{\"hwVersion\":\"1231231236\",\"serialNumber\":\"T1237813xx\",\"hwModel\":\"hwpartnumber\",\"sw\":[{\"swComponentID\":\"SOCxxx1\",\"swVersion\":\"980000x1\"}]}},\"unknownProperty\":\"test\"}";

        // Test
        DdiConfigData ddiConfigData = mapper.readValue(serializedDdiConfigData, DdiConfigData.class);

        assertThat(ddiConfigData.getMode()).isEqualTo(DdiUpdateMode.REPLACE);
    }

    @Test
    @Description("Verify that deserialization fails for known properties with a wrong datatype")
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        String serializedDdiConfigData = "{\"data\":{\"test\":\"data\"},\"mode\":[\"replace\"],\"unknownProperty\":\"test\"}";

        // Test
        assertThatExceptionOfType(MismatchedInputException.class).isThrownBy(
                () -> mapper.readValue(serializedDdiConfigData, DdiConfigData.class));
    }

    @Test
    @Description("Verify the correct deserialization of a model with removed unused status property")
    public void shouldDeserializeObjectWithStatusProperty() throws IOException {
        // We formerly falsely required a 'status' property object when using the
        // configData endpoint. It was removed as a requirement from code and
        // documentation, as it was unused. This test ensures we still behave correctly
        // (and just ignore the 'status' property) if it is still provided by the
        // client.

        // Setup
        String serializedDdiConfigData = "{\"id\":123,\"time\":\"20190809T121314\","
                + "\"status\":{\"execution\":\"closed\",\"result\":{\"finished\":\"success\",\"progress\":null},"
                + "\"details\":[]},\"mode\":\"replace\"}";

        // Test
        DdiConfigData ddiConfigData = mapper.readValue(serializedDdiConfigData, DdiConfigData.class);

        assertThat(ddiConfigData.getMode()).isEqualTo(DdiUpdateMode.REPLACE);
    }
}