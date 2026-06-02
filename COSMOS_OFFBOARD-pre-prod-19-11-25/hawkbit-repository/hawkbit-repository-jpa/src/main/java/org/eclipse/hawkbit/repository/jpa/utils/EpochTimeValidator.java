package org.eclipse.hawkbit.repository.jpa.utils;

import jakarta.validation.ValidationException;

public class EpochTimeValidator {

    /**
     * Validates whether the given epoch time is in seconds.
     * If the value is in milliseconds (i.e., divisible by 1000 and has more than 10 digits),
     * an exception is thrown.
     *
     * @param epochTime the epoch time to validate
     * @throws ValidationException if the epoch time is in milliseconds instead of seconds
     */

    public static void validateEpochTimeInSeconds(long epochTime) {
        if (epochTime % 1000 == 0 && String.valueOf(epochTime).length() > 10) {
            throw new ValidationException("Epoch time should be provided in seconds, not milliseconds");
        }
    }
}
