package org.cosmos.kafka.utils;

/**
 * A utility class that holds constant values used throughout the Kafka module.
 * These constants are used to identify various types of message queues.
 */
public final class Constants {

    /**
     * The name of the queueType for inventory messages.
     */
    public static final String INVENTORY = "inventory";

    /**
     * The name of the queueType for rollout status messages.
     */
    public static final String ROLLOUT_STATUS = "rolloutstatus";

    /**
     * The name of the queueType for vehicle status messages.
     */
    public static final String VEHICLE_STATUS = "vehiclestatus";

    /*
     * The name of the queueType for rollout error messages.
     */
    public static final String ROLLOUT_ERROR = "rollouterror";
    /**
     * Base URL for Kafka endpoints.
     */
    public static final String BASE_URL = "/kafka";

    /**
     * The name of the queueType for file upload status messages.
     */
    public static final String FILE_UPLOAD = "fileupload";

    /**
     * The name of the queueType for file upload error messages.
     */
    public static final String FILE_UPLOAD_ERROR = "fileuploaderror";

    /**
     * The name of the queueType for file delete error messages.
     */
    public static final String FILE_DELETE_ERROR = "filedeleteError";


    /**
     * The name of the queueType for general feedback idle enpoint.
     */
    public static final String GENERAL_IDLE= "generalidle";

    /**
     * The name of the queueType for  general feedback error enpoint.
     */
    public static final String GENERAL_ERROR = "generalerror";

    /**
     * URL for inventory endpoint.
     */
    public static final String INVENTORY_URL = "/inventory";

    /**
     * URL for rollout status endpoint.
     */
    public static final String ROLLOUT_STATUS_URL = "/rolloutstatus";

    /**
     * URL for vehicle status endpoint.
     */
    public static final String VEHICLE_STATUS_URL = "/vehiclestatus";

    /**
     * URL for file upload status endpoint.
     */
    public static final String FILE_UPLOAD_URL = "/fileupload";

    /**
     * URL for file upload error endpoint.
     */
    public static final String FILE_UPLOAD_ERROR_URL = "/fileupload/error";

    /**
     * URL for file delete error endpoint.
     */
    public static final String FILE_DELETE_ERROR_URL = "/fileDelete/error";

    /**
     * URL for rollout Error Endpoint.
     */
    public static final String ROLLOUT_ERROR_URL = "/rollouterror";

    /**
     * URL for general feedback idle enpoint.
     */
    public static final String GENERAL_IDLE_URL= "/general/idle";

    /**
     * URL for general feedback error enpoint.
     */
    public static final String GENERAL_ERROR_URL = "/general/error";

    /**
     * URL for general feedback error enpoint.
     */
    public static final String DD_ARTIFACT_EXPIRY = "/artifact/error";

    public static final String SEND_EVENT = "/sendEvent";

    public static final String SEND = "/send";

    public static final String KAFKA_SEND_EVENT_URL = "/kafka/sendEvent";


    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private Constants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
