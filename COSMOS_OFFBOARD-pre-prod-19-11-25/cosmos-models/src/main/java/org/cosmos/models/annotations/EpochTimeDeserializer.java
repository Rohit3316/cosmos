package org.cosmos.models.annotations;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;

/**
 * Custom deserializer for epoch time values.
 * <p>
 * This deserializer handles both millisecond and second precision epoch times.
 * If the epoch time is greater than 1,000,000,000,000, it is treated as milliseconds,
 * otherwise it is treated as seconds.
 * Timestamp should be from September 9, 2001 1:46:40 AM UTC until November 20, 33658 1:46:40 AM UTC
 * If we provide milliseconds before September 9, 2001 1:46:40 AM UTC, it will be treated as seconds.
 */
@Slf4j
public class EpochTimeDeserializer extends JsonDeserializer<Long> {

    private static final long MILLIS_THRESHOLD = 1_000_000_000_000L; // 1 trillion milliseconds (1 billion seconds)
    private static final long SECONDS_THRESHOLD = 1_000_000_000L;

    @Override
    public Long deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
        try {
            // Parse the epoch time from the JSON input
            long epochTime = jsonParser.getLongValue();

            // Check if epoch time is in milliseconds
            if (epochTime >= MILLIS_THRESHOLD) {
                return Instant.ofEpochMilli(epochTime).getEpochSecond(); // Convert milliseconds to seconds
            }

            if (epochTime >= SECONDS_THRESHOLD) {
                return epochTime; // It's already in seconds
            }

            throw new InvalidEpochTimeException("The provided timestamp must be between September 9, 2001 1:46:40 AM UTC " +
                    "and November 20, 33658 1:46:40 AM UTC");
        } catch (DateTimeException | NumberFormatException e) {
            log.error("Error parsing epoch time", e);
            throw new InvalidEpochTimeException("Invalid epoch time format");
        }
    }
}
