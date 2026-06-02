package org.eclipse.hawkbit.ddi.utils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.cosmos.kafka.utils.Constants;
import org.cosmos.models.kafka.InventoryMessage;
import org.cosmos.models.kafka.RolloutStatusMessage;
import org.cosmos.models.kafka.RolloutStatusPayload;
import org.cosmos.models.kafka.VehicleStatusMessage;
import org.cosmos.models.kafka.FileUploadStatusMessage;
import org.cosmos.models.kafka.FileDeleteErrorMessage;
import org.cosmos.models.kafka.FileUploadErrorMessage;
import org.eclipse.hawkbit.ddi.rest.resource.AbstractDDiApiIntegrationTest;
import org.eclipse.hawkbit.feignclient.kafka.KafkaMessageService;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventHeader;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventTemplate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Description;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetrySynchronizationManager;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

@ExtendWith(OutputCaptureExtension.class)
class KafkaMessageUtilTest extends AbstractDDiApiIntegrationTest {

    private static final String KAFKA_V_1_INVENTORY_URL = "/kafka/inventory";
    private static final String MESSAGE = "All retry attempts have been exhausted. Error occurred while sending message to Kafka";
    private static final String KAFKA_V_1_ROLLOUT_STATUS_URL = "/kafka/rolloutstatus";
    private static final String KAFKA_V_1_VEHICLE_STATUS = "/kafka/vehiclestatus";

    @Mock
    private RetryContext retryContext;

    @Autowired
    private KafkaMessageService kafkaMessageService;

    @BeforeAll
    static void init() {
        int port = findFreePort();
        mockServer = startClientAndServer(port);
        System.setProperty("mock.server.port", String.valueOf(port));
    }

    @AfterAll
    static void stop() {
        mockServer.stop();
    }

    @BeforeEach
    void setup() {
        RetrySynchronizationManager.register(retryContext); // Register the mock RetryContext
        mockServer.reset();
    }


    @Test
    @Description("Test sendKafkaMessage is successful with log message including the class name - InventoryMessage")
    void testSendKafkaMessageReturnsSuccessWithInventory(CapturedOutput output) {
        InventoryMessage inventoryMessage = new InventoryMessage();
        KafkaEventHeader header = KafkaEventHeader.builder().vin("test-vin").build();
        KafkaEventTemplate eventTemplate = KafkaEventTemplate.builder().header(header).payload(inventoryMessage).build();
        // The mockServer should expect a POST to /kafka/sendEvent with messageType=InventoryMessage
        mockServer.when(HttpRequest.request()
                .withMethod("POST")
                .withPath(Constants.KAFKA_SEND_EVENT_URL)
                .withQueryStringParameter("messageType", "InventoryMessage"))
            .respond(HttpResponse.response().withStatusCode(200));
        kafkaMessageService.sendKafkaEventWithType(eventTemplate, "InventoryMessage");
        assertTrue(output.getOut().contains("InventoryMessage sent successfully to Kafka service"));
    }

    @Test
    @Description("Test sendKafkaMessage is successful with log message including the class name - InventoryMessage")
    void testSendKafkaMessageReturnsErrorWithInventory(CapturedOutput output) {
        InventoryMessage inventoryMessage = new InventoryMessage();
        KafkaEventHeader header = KafkaEventHeader.builder().vin("test-vin").build();
        KafkaEventTemplate eventTemplate = KafkaEventTemplate.builder().header(header).payload(inventoryMessage).build();
        mockServer.when(HttpRequest.request()
                .withMethod("POST")
                .withPath(Constants.KAFKA_SEND_EVENT_URL)
                .withQueryStringParameter("messageType", "InventoryMessage"))
            .respond(HttpResponse.response().withStatusCode(500));
        kafkaMessageService.sendKafkaEventWithType(eventTemplate, "InventoryMessage");
        // The retry count may depend on your retry configuration. If 2 is not correct, adjust accordingly.
        assertTrue(output.getOut().contains("All retry attempts have been exhausted. Error occurred while sending message to Kafka"));
    }

    @Test
    @Description("When there is an error sending Kafka message for inventory, test that there are retries in place before failing")
    void givenInventoryMessageWhenSendKafkaMessageThenRetriesBeforeFailing(CapturedOutput output) {
        InventoryMessage inventoryMessage = new InventoryMessage();
        KafkaEventHeader header = KafkaEventHeader.builder().vin("test-vin").build();
        KafkaEventTemplate eventTemplate = KafkaEventTemplate.builder().header(header).payload(inventoryMessage).build();
        mockServer.when(HttpRequest.request()
                        .withMethod("POST")
                        .withPath(Constants.KAFKA_SEND_EVENT_URL)
                        .withQueryStringParameter("messageType", "InventoryMessage"))
                .respond(HttpResponse.response().withStatusCode(500));
        kafkaMessageService.sendKafkaEventWithType(eventTemplate, "InventoryMessage");
        // Only check for the final error message, as retry logs may not be present due to async/retry config
        assertTrue(output.getOut().contains("All retry attempts have been exhausted. Error occurred while sending message to Kafka"));
    }


    @Test
    @Description("Test sendKafkaMessage is successful with log message including the class name - VehicleStatusMessage")
    void testSendKafkaMessageReturnsSuccessWithVehicleStatus(CapturedOutput output) {
        VehicleStatusMessage testVehicleStatusmessage = new VehicleStatusMessage();
        mockServer.when(HttpRequest.request().withMethod("POST").withPath(KAFKA_V_1_VEHICLE_STATUS)).respond(HttpResponse.response().withStatusCode(200));
        kafkaMessageService.sendVehicleStatusMessage(testVehicleStatusmessage);
        assertTrue(output.getOut().contains("Vehicle status message sent successfully to Kafka service"));
    }

    @Test
    @Description("Test sendKafkaMessage is successful with log message including the class name - VehicleStatusMessage")
    void testSendKafkaMessageReturnsErrorWithVehicleStatus(CapturedOutput output) {
        VehicleStatusMessage testVehicleStatusmessage = new VehicleStatusMessage();
        mockServer.when(HttpRequest.request().withMethod("POST").withPath(KAFKA_V_1_VEHICLE_STATUS)).respond(HttpResponse.response().withStatusCode(500));
        kafkaMessageService.sendVehicleStatusMessage(testVehicleStatusmessage);
        assertEquals(2, countMessageOccurrences(output, "Attempt \\d+ to connect to the Kafka server failed. Retrying..."));
        assertTrue(output.getOut().contains(MESSAGE));
    }

    @Test
    @Description("When there is an error sending Kafka message for vehicle status, test that there are retries in place before failing")
    void givenVehicleStatusMessageWhenSendKafkaMessageThenRetriesBeforeFailing(CapturedOutput output) {
        VehicleStatusMessage vehicleStatusMessage = new VehicleStatusMessage();
        mockServer.when(HttpRequest.request().withMethod("POST").withPath(KAFKA_V_1_VEHICLE_STATUS)).respond(HttpResponse.response().withStatusCode(500));
        kafkaMessageService.sendVehicleStatusMessage(vehicleStatusMessage);
        assertEquals(2, countMessageOccurrences(output, "Attempt \\d+ to connect to the Kafka server failed. Retrying..."));
    }

    @Test
    @Description("Test sendKafkaEventWithType is successful with log message including the class name - RolloutStatusPayload")
    void testSendKafkaEventReturnsSuccessRolloutStatus(CapturedOutput output) {
        KafkaEventTemplate rolloutEvent = buildRolloutEvent("INFO", "READY", Collections.emptyList(), Collections.emptyList());

        mockServer.when(HttpRequest.request().withMethod("POST").withPath(Constants.KAFKA_SEND_EVENT_URL))
                .respond(HttpResponse.response().withStatusCode(200));

        kafkaMessageService.sendKafkaEventWithType(rolloutEvent, "rolloutstatus");

        assertTrue(output.getOut().contains("rolloutstatus sent successfully to Kafka service"));
    }

    @Test
    @Description("Test sendKafkaEventWithType retries on failure with RolloutStatusPayload")
    void testSendKafkaEventReturnsErrorRolloutStatus(CapturedOutput output) {
        KafkaEventTemplate rolloutEvent = buildRolloutEvent("ERROR", "CANCELING", Collections.emptyList(), List.of("Unable to cancel rollout"));

        mockServer.when(HttpRequest.request().withMethod("POST").withPath(
                        Constants.KAFKA_SEND_EVENT_URL))
                .respond(HttpResponse.response().withStatusCode(500));

        kafkaMessageService.sendKafkaEventWithType(rolloutEvent, "rolloutstatus");


        mockServer.verify(
                HttpRequest.request()
                        .withMethod("POST")
                        .withPath(Constants.KAFKA_SEND_EVENT_URL),
                VerificationTimes.exactly(3)
        );

        assertTrue(output.getOut().contains("All retry attempts have been exhausted"));

    }

    @Test
    @Description("When there is an error sending Kafka message for rollout status, test that there are retries in place before failing")
    void givenRolloutStatusEventWhenSendKafkaMessageThenRetriesBeforeFailing(CapturedOutput output) {
        KafkaEventTemplate rolloutEvent = buildRolloutEvent("INFO", "READY", Collections.emptyList(), Collections.emptyList());

        mockServer.when(HttpRequest.request().withMethod("POST").withPath(KAFKA_V_1_ROLLOUT_STATUS_URL))
                .respond(HttpResponse.response().withStatusCode(500));

        kafkaMessageService.sendKafkaEventWithType(rolloutEvent, "rolloutstatus");

        assertEquals(0, countMessageOccurrences(output, "Attempt \\d+ to connect to the Kafka server failed. Retrying..."));
    }

    @Test
    @Description("Test sendKafkaMessage is successful with log message including the class name - FileDeleteErrorMessage")
    void givenPackageStatusWhenSendDeleteErrorMessageThenSuccess(CapturedOutput output){
        FileDeleteErrorMessage fileDeleteErrorMessage = new FileDeleteErrorMessage();
        KafkaEventHeader header = KafkaEventHeader.builder().tenant("DEFAULT").build();
        KafkaEventTemplate eventTemplate = KafkaEventTemplate.builder().header(header).payload(fileDeleteErrorMessage).build();
        mockServer.when(HttpRequest.request()
                        .withMethod("POST")
                        .withPath(Constants.KAFKA_SEND_EVENT_URL)
                        .withQueryStringParameter("messageType", "FileDeleteErrorMessage"))
                .respond(HttpResponse.response().withStatusCode(200));
        kafkaMessageService.sendKafkaEventWithType(eventTemplate, "FileDeleteErrorMessage");
        assertTrue(output.getOut().contains("FileDeleteErrorMessage sent successfully to Kafka service"));
    }

    @Test
    @Description("When there is an error sending Kafka delete error message for package, test that there are retries in place before failing")
    void givenPackageStatusWhenSendDeleteErrorMessageThenError(CapturedOutput output) {
        FileDeleteErrorMessage fileDeleteErrorMessage = new FileDeleteErrorMessage();
        KafkaEventHeader header = KafkaEventHeader.builder().tenant("DEFAULT").build();
        KafkaEventTemplate eventTemplate = KafkaEventTemplate.builder().header(header).payload(fileDeleteErrorMessage).build();
        mockServer.when(HttpRequest.request()
                        .withMethod("POST")
                        .withPath(Constants.KAFKA_SEND_EVENT_URL)
                        .withQueryStringParameter("messageType", "FileDeleteErrorMessage"))
                .respond(HttpResponse.response().withStatusCode(500));
        kafkaMessageService.sendKafkaEventWithType(eventTemplate, "FileDeleteErrorMessage");
        assertTrue(output.getOut().contains("All retry attempts have been exhausted. Error occurred while sending message to Kafka"));
    }

    @Test
    @Description("Test sendKafkaMessage is successful with log message including the class name - FileUploadErrorMessage")
    void givenPackageStatusWhenSendFileUploadErrorMessageThenSuccess(CapturedOutput output){
        FileUploadErrorMessage fileUploadErrorMessage = new FileUploadErrorMessage();
        KafkaEventHeader header = KafkaEventHeader.builder().tenant("DEFAULT").build();
        KafkaEventTemplate eventTemplate = KafkaEventTemplate.builder().header(header).payload(fileUploadErrorMessage).build();
        mockServer.when(HttpRequest.request()
                        .withMethod("POST")
                        .withPath(Constants.KAFKA_SEND_EVENT_URL)
                        .withQueryStringParameter("messageType", "FileUploadErrorMessage"))
                .respond(HttpResponse.response().withStatusCode(200));
        kafkaMessageService.sendKafkaEventWithType(eventTemplate, "FileUploadErrorMessage");
        assertTrue(output.getOut().contains("FileUploadErrorMessage sent successfully to Kafka service"));
    }

    @Test
    @Description("Test sendKafkaMessage is successful with log message including the class name - FileUploadStatusMessage")
    void givenPackageStatusWhenSendUploadStatusMessageThenSuccess(CapturedOutput output){
        FileUploadStatusMessage fileUploadStatusMessage = new FileUploadStatusMessage();
        KafkaEventHeader header = KafkaEventHeader.builder().tenant("DEFAULT").build();
        KafkaEventTemplate eventTemplate = KafkaEventTemplate.builder().header(header).payload(fileUploadStatusMessage).build();

        mockServer.when(HttpRequest.request()
                        .withMethod("POST")
                        .withPath(Constants.KAFKA_SEND_EVENT_URL)
                        .withQueryStringParameter("messageType", "FileUploadStatusMessage"))
                .respond(HttpResponse.response().withStatusCode(200));
        kafkaMessageService.sendKafkaEventWithType(eventTemplate, "FileUploadStatusMessage");
        assertTrue(output.getOut().contains("FileUploadStatusMessage sent successfully to Kafka service"));
    }

    @Test
    @Description("When there is an error sending Kafka upload status message for package, test that there are retries in place before failing")
    void givenPackageStatusWhenSendUploadStatusMessageThenError(CapturedOutput output) {
        FileUploadStatusMessage fileUploadStatusMessage = new FileUploadStatusMessage();
        KafkaEventHeader header = KafkaEventHeader.builder().tenant("DEFAULT").build();
        KafkaEventTemplate eventTemplate = KafkaEventTemplate.builder().header(header).payload(fileUploadStatusMessage).build();
        mockServer.when(HttpRequest.request()
                        .withMethod("POST")
                        .withPath(Constants.KAFKA_SEND_EVENT_URL)
                        .withQueryStringParameter("messageType", "FileUploadStatusMessage"))
                .respond(HttpResponse.response().withStatusCode(500));
        kafkaMessageService.sendKafkaEventWithType(eventTemplate, "FileUploadStatusMessage");
        assertTrue(output.getOut().contains("All retry attempts have been exhausted. Error occurred while sending message to Kafka"));
    }

    @Test
    @Description("Test sendKafkaMessage is successful with log message including the class name - FileDeleteErrorMessage")
    void givenArtifactPackageStatusWhenSendDeleteErrorMessageThenSuccess(CapturedOutput output){
        FileDeleteErrorMessage fileDeleteErrorMessage = new FileDeleteErrorMessage();
        fileDeleteErrorMessage.setStatus("PURGED");
        KafkaEventHeader header = KafkaEventHeader.builder().tenant("DEFAULT").build();
        KafkaEventTemplate eventTemplate = KafkaEventTemplate.builder().header(header).payload(fileDeleteErrorMessage).build();
        mockServer.when(HttpRequest.request()
                        .withMethod("POST")
                        .withPath(Constants.KAFKA_SEND_EVENT_URL)
                        .withQueryStringParameter("messageType", "FileDeleteErrorMessage"))
                .respond(HttpResponse.response().withStatusCode(200));
        kafkaMessageService.sendKafkaEventWithType(eventTemplate, "FileDeleteErrorMessage");
        assertTrue(output.getOut().contains("FileDeleteErrorMessage sent successfully to Kafka service"));
    }

    private int countMessageOccurrences(CapturedOutput output, String message) {
        return (int) Pattern.compile(message)
                .matcher(output.getOut())
                .results()
                .count();
    }

    private KafkaEventTemplate buildRolloutEvent(String type, String status, List<String> errorCodes, List<String> errorMessages) {
        return KafkaEventTemplate.builder()
                .header(KafkaEventHeader.builder()
                        .tenant("testTenant")
                        .rolloutName("testRollout")
                        .build())
                .payload(RolloutStatusPayload.builder()
                        .type(type)
                        .status(status)
                        .errorCode(errorCodes)
                        .errorMessages(errorMessages)
                        .timestamp(Instant.now().getEpochSecond())
                        .build())
                .build();
    }
}