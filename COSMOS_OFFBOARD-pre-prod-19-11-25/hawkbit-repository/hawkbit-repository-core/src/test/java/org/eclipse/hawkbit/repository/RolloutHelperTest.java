package org.eclipse.hawkbit.repository;

import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.eclipse.hawkbit.repository.exception.RolloutIllegalStateException;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.springframework.context.annotation.Description;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.ValidationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Unit test cases for {@link RolloutHelper}
 *
 */
class RolloutHelperTest {

    @Description("On giving an invalid rollout status, this method should throw an exception")
    @ParameterizedTest
    @EnumSource(value = RolloutStatus.class, names = {"DELETING", "CANCELING","FINISHING", "FINISHED", "CANCELED", "DELETED"})
    void givenInvalidRolloutStatusWhenVerifyRolloutInStatusForAddDeviceDetailsThenThrowException(RolloutStatus status) {
        Rollout rollout = Mockito.mock(Rollout.class);
        Mockito.when(rollout.getStatus()).thenReturn(status);

        assertThrows(RolloutIllegalStateException.class, () ->
            RolloutHelper.verifyRolloutInStatusForAddDeviceDetails(rollout)
        );
    }

    @Description("On giving a valid rollout status, there should be no exceptions")
    @ParameterizedTest
    @EnumSource(value = RolloutStatus.class, names = {"DRAFT",
            "FREEZING", "READY", "UNFREEZING", "STARTING", "RUNNING", "PAUSING", "PAUSED", "RESUMING",
            "RETRYING"})
    void givenValidRolloutStatusWhenVerifyRolloutInStatusForAddDeviceDetailsThenDoNotThrowException(RolloutStatus status) {
        Rollout rollout = Mockito.mock(Rollout.class);
        Mockito.when(rollout.getStatus()).thenReturn(status);

        assertDoesNotThrow(() ->
            RolloutHelper.verifyRolloutInStatusForAddDeviceDetails(rollout)
        );
    }

    @Test
    @Description("A valid CSV file returns a successful list of controllerIds")
    void givenValidFileWhenGetControllerIdsThenSuccess() throws Exception {
        MultipartFile file = Mockito.mock(MultipartFile.class);
        String content = "controller1\ncontroller2\ncontroller3";
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        List<String> result = RolloutHelper.parseControllerIdsFromCSV(file);

        assertEquals(List.of("controller1", "controller2", "controller3"), result);
    }

    @Test
    @Description("A valid CSV file with a BOM char returns a successful list of controllerIds")
    void givenFileWithBOMWhenGetControllerIdsThenSuccess() throws Exception {
        MultipartFile file = Mockito.mock(MultipartFile.class);
        String content = "\uFEFFcontroller1\ncontroller2";
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        List<String> result = RolloutHelper.parseControllerIdsFromCSV(file);

        assertEquals(List.of("controller1", "controller2"), result);
    }

    @Test
    @Description("empty csv file returns an empty list of controller ids")
    void givenEmptyFileWhenGetControllerIdsThenSuccess() throws Exception {
        MultipartFile file = Mockito.mock(MultipartFile.class);
        String content = "";
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        List<String> result = RolloutHelper.parseControllerIdsFromCSV(file);

        assertEquals(List.of(), result);
    }

    @Test
    @Description("Throws an exception is any problem occurred while reading the file")
    void givenIOExceptionWhenGetControllerIdsThenFailure() throws Exception {
        MultipartFile file = Mockito.mock(MultipartFile.class);
        when(file.getInputStream()).thenThrow(new IOException("Test exception"));

        //Verify validation exception is thrown
        assertThrows(ValidationException.class, () -> RolloutHelper.parseControllerIdsFromCSV(file), "There should be a Validation Exception");
    }

}
