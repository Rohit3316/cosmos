package org.eclipse.hawkbit.ddi.rest.resource;

import org.eclipse.hawkbit.feignclient.kafka.KafkaMessageService;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventTemplate;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class DataConversionHelperTest {

    private static final Logger LOG = LoggerFactory.getLogger(DataConversionHelperTest.class);

    private KafkaMessageService kafkaMessageService;

    @BeforeEach
    void setUp() {
        kafkaMessageService = mock(KafkaMessageService.class);
        DataConversionHelper.initializeKafkaMessageService(kafkaMessageService);
    }

    @Test
    void testIsArtifactExpired_WhenExpired() {
        // Arrange
        Artifacts artifact = mock(Artifacts.class);
        long nowEpochSeconds = Instant.now().getEpochSecond();
        when(artifact.getExpiryDate()).thenReturn(nowEpochSeconds - 1000); // Expired
        when(artifact.getFileName()).thenReturn("expiredArtifact");
        when(artifact.getId()).thenReturn(123L);

        // Act
        boolean result = DataConversionHelper.isArtifactExpired(artifact, nowEpochSeconds);

        // Assert
        assertTrue(result, "Expected the artifact to be marked as expired");

        ArgumentCaptor<KafkaEventTemplate> eventCaptor = ArgumentCaptor.forClass(KafkaEventTemplate.class);
        verify(kafkaMessageService, times(1)).sendKafkaEventWithType(eventCaptor.capture(), Mockito.eq("filedeleteError"));
        KafkaEventTemplate sentEvent = eventCaptor.getValue();
        assertEquals("ARTIFACT", sentEvent.getHeader().getFileType());
    }

    @Test
    void testIsArtifactExpired_WhenNotExpired() {
        // Arrange
        Artifacts artifact = mock(Artifacts.class);
        long nowEpochSeconds = Instant.now().getEpochSecond();
        when(artifact.getExpiryDate()).thenReturn(nowEpochSeconds + 1000); // Not expired

        // Act
        boolean result = DataConversionHelper.isArtifactExpired(artifact, nowEpochSeconds);

        // Assert
        assertFalse(result, "Expected the artifact to not be marked as expired");
        verify(kafkaMessageService, never()).sendDdArtifactExpiry(Mockito.any());
    }
}
