package org.cosmos.sns.models;

public class SnsConstants {

    private SnsConstants() {
        // Private constructor to prevent instantiation
    }

    /**
     * The maximum number of SNS retry attempts.
     */
    public static final int SNS_RETRY_COUNT = 3;

    /**
     * The delay in milliseconds between SNS retry attempts.
     */
    public static final long SNS_RETRY_DELAY_IN_MILLIS = 2000L;

}
