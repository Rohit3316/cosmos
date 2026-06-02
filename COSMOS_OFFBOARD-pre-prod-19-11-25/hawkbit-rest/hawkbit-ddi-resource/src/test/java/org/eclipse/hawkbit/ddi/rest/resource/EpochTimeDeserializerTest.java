package org.eclipse.hawkbit.ddi.rest.resource;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import org.cosmos.models.annotations.EpochTimeDeserializer;
import org.cosmos.models.annotations.InvalidEpochTimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Description;

import java.io.IOException;
import java.time.DateTimeException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class EpochTimeDeserializerTest {

    private EpochTimeDeserializer deserializer;
    private JsonParser jsonParser;
    private DeserializationContext context;

    @BeforeEach
    public void setUp() {
        deserializer = new EpochTimeDeserializer();
        jsonParser = Mockito.mock(JsonParser.class);
        context = Mockito.mock(DeserializationContext.class);
    }

    @Test
    @Description("Given epoch timestamp in milliseconds, when deserialized, then it is converted to epoch seconds")
    void givenEpochTimestampInMillisecondsWhenDeserializedThenConvertedToEpochSeconds() throws IOException {
        when(jsonParser.getLongValue()).thenReturn(1_500_000_000_000L); // Example epoch time in milliseconds

        Long result = deserializer.deserialize(jsonParser, context);

        assertEquals(1_500_000_000L, result); // Expected epoch time in seconds
    }

    @Test
    @Description("Given epoch timestamp in seconds, when deserialized, then it is returned as epoch seconds")
    void givenEpochTimestampInSecondsWhenDeserializedThenReturnedAsEpochSeconds() throws IOException {
        when(jsonParser.getLongValue()).thenReturn(1_500_000_000L); // Example epoch time in seconds

        Long result = deserializer.deserialize(jsonParser, context);

        assertEquals(1_500_000_000L, result); // Expected epoch time in seconds
    }

    @Test
    @Description("Given a past epoch timestamp (Before September 9, 2001 at 1:46:40 AM UTC), when deserialized, then it throws InvalidEpochTimeException")
    void givenInvalidOrPastEpochTimestampWhenDeserializedThenThrowsInvalidEpochTimeException() throws IOException {
        when(jsonParser.getLongValue()).thenReturn(500_000_000L); // Past epoch time

        assertThrows(InvalidEpochTimeException.class, () -> deserializer.deserialize(jsonParser, context));
    }

    @Test
    @Description("Given a DateTimeException is thrown by the deserializer, when deserializing, then it is caught and rethrown as InvalidEpochTimeException")
    void givenDateTimeExceptionWhenDeserializingThenThrowsInvalidEpochTimeException() throws IOException {
        when(jsonParser.getLongValue()).thenThrow(new DateTimeException("Invalid date"));

        assertThrows(InvalidEpochTimeException.class, () -> deserializer.deserialize(jsonParser, context));
    }

    @Test
    @Description("Given a NumberFormatException is thrown by the deserializer, when deserializing, then it is caught and rethrown as InvalidEpochTimeException")
    void givenNumberFormatExceptionWhenDeserializingThenThrowsInvalidEpochTimeException() throws IOException {
        when(jsonParser.getLongValue()).thenThrow(new NumberFormatException("Invalid number"));

        assertThrows(InvalidEpochTimeException.class, () -> deserializer.deserialize(jsonParser, context));
    }
}