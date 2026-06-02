package utils;

import io.qameta.allure.Description;
import org.cosmos.sns.models.CdnUploadRequest;
import org.cosmos.sns.models.S3FileDeletionRequest;
import org.cosmos.sns.models.S3FileTransferRequest;
import org.cosmos.sns.services.impl.CdnDeleteSnsService;
import org.cosmos.sns.services.impl.CdnUploadSnsService;
import org.cosmos.sns.services.impl.S3FileDeleteSnsService;
import org.cosmos.sns.services.impl.S3FileTransferSnsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.cosmos.sns.models.CdnDeleteRequest;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SnsServiceImplTest {
    private static final String BUCKET_NAME = "test-bucket";
    private static final String VALUE = "test-arn";
    private static final String TEST_MESSAGE_ID = "test-message-id";
    private static final String MESSAGE = "SNS publish failed";
    private static final String MESSAGE1 = "JSON serialization error";
    private static final Long TENANT_ID = 1L;
    @Mock
    private SnsAsyncClient snsAsyncClient;

    @InjectMocks
    private S3FileTransferSnsService s3FileTransferSnsService;

    @InjectMocks
    private CdnUploadSnsService cdnUploadSnsService;

    @InjectMocks
    private CdnDeleteSnsService cdnDeleteSnsService;

    @InjectMocks
    private S3FileDeleteSnsService s3FileDeleteSnsService;

    private S3FileTransferRequest s3FileTransferRequest;

    private CdnUploadRequest cdnUploadRequest;

    private S3FileDeletionRequest s3FileDeletionRequest;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private CdnDeleteRequest cdnDeleteRequest;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        s3FileTransferRequest = new S3FileTransferRequest();
        s3FileTransferRequest.setFileId(1234L);
        s3FileTransferRequest.setFileURL("https://example.com/file.zip");
        s3FileTransferRequest.setBucketName(BUCKET_NAME);
        s3FileTransferRequest.setChecksum("sha256checksum");
        s3FileTransferRequest.setFileName("file.zip");
        cdnUploadRequest = new CdnUploadRequest();
        cdnUploadRequest.setFileId(1234L);
        cdnUploadRequest.setBucketName(BUCKET_NAME);
        cdnUploadRequest.setCdnFileName("file.zip");
        cdnUploadRequest.setCdn("https://example.com/file.zip");
        cdnUploadRequest.setFileType("Artifact");
        cdnUploadRequest.setFileId(45L);
        s3FileDeletionRequest = new S3FileDeletionRequest();
        s3FileDeletionRequest.setFileId(45L);
        s3FileDeletionRequest.setFilePath("/path/to/file");
        s3FileDeletionRequest.setBucketName(BUCKET_NAME);
        s3FileDeletionRequest.setFileType("rsp");
        cdnDeleteRequest = new CdnDeleteRequest();
        cdnDeleteRequest.setFileId(20L);
        cdnDeleteRequest.setFileType("test-type");
        cdnDeleteRequest.setFilePath("test-path");
        //Set the topic ARNs that comes from properties file
        ReflectionTestUtils.setField(cdnUploadSnsService, "cdnUploadSns", VALUE);
        ReflectionTestUtils.setField(cdnDeleteSnsService, "cdnDeleteSns", VALUE);
        ReflectionTestUtils.setField(s3FileTransferSnsService, "urlToS3TransferSns", VALUE);
    }

    @Test
    @Description("Verifies that the S3 file transfer message is published to the SNS topic.")
    void givenS3FileTransferRequestWhenPublishedThenMessageIsSuccessful() throws Exception {
        // Mocking the PublishResponse and CompletableFuture
        PublishResponse publishResponse = PublishResponse.builder().messageId(TEST_MESSAGE_ID).build();
        CompletableFuture<PublishResponse> futureResponse = CompletableFuture.completedFuture(publishResponse);

        // Mock the snsAsyncClient to return the future response
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(futureResponse);

        // Call the method to test
        CompletableFuture<PublishResponse> result = s3FileTransferSnsService.publishMessage(s3FileTransferRequest);

        // Assert that the result is completed and successful
        assertTrue(result.isDone());
        assertEquals(TEST_MESSAGE_ID, result.get().messageId());

        // Verify that the SNS client was called with the correct request
        verify(snsAsyncClient, times(1)).publish(any(PublishRequest.class));
    }

    @Test
    @Description("Verifies that result is completed exceptionally and the S3 file transfer message is published to the SNS topic.")
    void givenS3FileTransferRequestWhenPublishFailsThenExceptionIsThrown() {
        // Mocking an exception to be thrown when the publish method is called
        CompletableFuture<PublishResponse> futureException = CompletableFuture.failedFuture(new RuntimeException(MESSAGE));

        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(futureException);

        // Call the method to test
        CompletableFuture<PublishResponse> result = s3FileTransferSnsService.publishMessage(s3FileTransferRequest);

        // Assert that the result is completed exceptionally
        assertTrue(result.isCompletedExceptionally());

        // Verify that the SNS client was called with the correct request
        verify(snsAsyncClient, times(1)).publish(any(PublishRequest.class));


    }

    @Test
    @Description("Verifies that the result is completed exceptionally due to serialization error and S3 file transfer message is published to the SNS topic.")
    void givenS3FileTransferRequestWhenJsonSerializationErrorThenExceptionIsThrown() {
        // Mock a CompletableFuture to return an exception (like a serialization error)
        CompletableFuture<PublishResponse> futureException = CompletableFuture.failedFuture(new RuntimeException(MESSAGE1));

        // Mock snsAsyncClient.publish to return the failed future
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(futureException);

        // Call the method to test
        CompletableFuture<PublishResponse> result = s3FileTransferSnsService.publishMessage(s3FileTransferRequest);

        // Assert that the result is completed exceptionally due to serialization failure
        assertTrue(result.isCompletedExceptionally());

        // Verify that the SNS client was called once
        verify(snsAsyncClient, times(1)).publish(any(PublishRequest.class));
    }

    @Test
    @Description("Verifies that the CDN upload message is published to the SNS topic.")
    void givenCdnUploadRequestWhenPublishedThenMessageIsSuccessful() throws Exception {
        // Mocking the PublishResponse and CompletableFuture
        PublishResponse publishResponse = PublishResponse.builder().messageId(TEST_MESSAGE_ID).build();
        CompletableFuture<PublishResponse> futureResponse = CompletableFuture.completedFuture(publishResponse);

        // Mock the snsAsyncClient to return the future response
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(futureResponse);

        // Call the method to test
        CompletableFuture<PublishResponse> result = cdnUploadSnsService.publishMessage(cdnUploadRequest);

        // Assert that the result is completed and successful
        assertTrue(result.isDone());
        assertEquals(TEST_MESSAGE_ID, result.get().messageId());

        // Verify that the SNS client was called with the correct request
        verify(snsAsyncClient, times(1)).publish(any(PublishRequest.class));
    }

    @Test
    @Description("Verifies that result is completed exceptionally and the CDN upload message is published to the SNS topic.")
     void givenCdnUploadRequestWhenPublishFailsThenExceptionIsThrown() {
        // Mocking an exception to be thrown when the publish method is called
        CompletableFuture<PublishResponse> futureException = CompletableFuture.failedFuture(new RuntimeException(MESSAGE));

        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(futureException);

        // Call the method to test
        CompletableFuture<PublishResponse> result = cdnUploadSnsService.publishMessage(cdnUploadRequest);

        // Assert that the result is completed exceptionally
        assertTrue(result.isCompletedExceptionally());

        // Verify that the SNS client was called with the correct request
        verify(snsAsyncClient, times(1)).publish(any(PublishRequest.class));


    }

    @Test
    @Description("Verifies that the result is completed exceptionally due to serialization error and the CDN upload message is published to the SNS topic.")
     void givenCdnUploadRequestWhenJsonSerializationErrorThenExceptionIsThrown() {
        // Mock a CompletableFuture to return an exception (like a serialization error)
        CompletableFuture<PublishResponse> futureException = CompletableFuture.failedFuture(new RuntimeException(MESSAGE1));

        // Mock snsAsyncClient.publish to return the failed future
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(futureException);

        // Call the method to test
        CompletableFuture<PublishResponse> result = cdnUploadSnsService.publishMessage(cdnUploadRequest);

        // Assert that the result is completed exceptionally due to serialization failure
        assertTrue(result.isCompletedExceptionally());

        // Verify that the SNS client was called once
        verify(snsAsyncClient, times(1)).publish(any(PublishRequest.class));
    }

    @Test
    @Description("Verifies that the CDN delete message is published to the SNS topic.")
    void givenCdnDeleteRequestWhenPublishedThenMessageIsSuccessful() throws Exception {
        // Mocking the PublishResponse and CompletableFuture
        PublishResponse publishResponse = PublishResponse.builder().messageId(TEST_MESSAGE_ID).build();
        CompletableFuture<PublishResponse> futureResponse = CompletableFuture.completedFuture(publishResponse);

        // Mock the snsAsyncClient to return the future response
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(futureResponse);

        // Call the method to test
        CompletableFuture<PublishResponse> result = cdnDeleteSnsService.publishMessage(cdnDeleteRequest);

        // Assert that the result is completed and successful
        assertTrue(result.isDone());
        assertEquals(TEST_MESSAGE_ID, result.get().messageId());

        // Verify that the SNS client was called with the correct request
        verify(snsAsyncClient, times(1)).publish(any(PublishRequest.class));
    }

    @Test
    @Description("Verifies that result is completed exceptionally and the CDN delete message is published to the SNS topic.")
    void givenCdnDeleteRequestWhenPublishFailsThenExceptionIsThrown() {
        // Mocking an exception to be thrown when the publish method is called
        CompletableFuture<PublishResponse> futureException = CompletableFuture.failedFuture(new RuntimeException(MESSAGE));

        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(futureException);

        // Call the method to test
        CompletableFuture<PublishResponse> result = cdnDeleteSnsService.publishMessage(cdnDeleteRequest);

        // Assert that the result is completed exceptionally
        assertTrue(result.isCompletedExceptionally());

        // Verify that the SNS client was called with the correct request
        verify(snsAsyncClient, times(1)).publish(any(PublishRequest.class));


    }

    @Test
    @Description("Verifies that the result is completed exceptionally due to serialization error and the CDN delete message is published to the SNS topic.")
    void givenCdnDeleteRequestWhenJsonSerializationErrorThenExceptionIsThrown() {
        // Mock a CompletableFuture to return an exception (like a serialization error)
        CompletableFuture<PublishResponse> futureException = CompletableFuture.failedFuture(new RuntimeException(MESSAGE1));

        // Mock snsAsyncClient.publish to return the failed future
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(futureException);

        // Call the method to test
        CompletableFuture<PublishResponse> result = cdnDeleteSnsService.publishMessage(cdnDeleteRequest);

        // Assert that the result is completed exceptionally due to serialization failure
        assertTrue(result.isCompletedExceptionally());

        // Verify that the SNS client was called once
        verify(snsAsyncClient, times(1)).publish(any(PublishRequest.class));
    }

    @Test
    @Description("Verifies that a valid S3 file deletion request passes validation without any constraint violations.")
    void givenValidS3FileDeletionRequestWhenValidatedThenNoConstraintViolations() {
        S3FileDeletionRequest request = S3FileDeletionRequest.builder()
                .fileType("rsp")
                .filePath("/path/to/file")
                .bucketName("my-bucket")
                .fileId(123L)
                .tenantId(TENANT_ID)
                .build();

        Set<ConstraintViolation<S3FileDeletionRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Expected no constraint violations");
    }

    @Test
    @Description("Verifies that an invalid S3 file deletion request fails validation with the expected constraint violations.")
     void givenInvalidS3FileDeletionRequestWhenValidatedThenConstraintViolationsAreFound() {
        S3FileDeletionRequest request = S3FileDeletionRequest.builder()
                .fileType("")
                .filePath("")
                .tenantId(TENANT_ID)
                .bucketName("")
                .fileId(null)
                .build();

        Set<ConstraintViolation<S3FileDeletionRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Expected constraint violations");
        assertEquals(4, violations.size(), "Expected 4 constraint violations");
    }

    @Test
    @Description("Verifies that the S3 delete message is published to the SNS topic.")
     void givenS3DeleteRequestWhenPublishedThenMessageIsSuccessful() throws Exception {
        // Mocking the PublishResponse and CompletableFuture
        PublishResponse publishResponse = PublishResponse.builder().messageId(TEST_MESSAGE_ID).build();
        CompletableFuture<PublishResponse> futureResponse = CompletableFuture.completedFuture(publishResponse);

        // Mock the snsAsyncClient to return the future response
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(futureResponse);

        // Call the method to test
        CompletableFuture<PublishResponse> result = s3FileDeleteSnsService.publishMessage(s3FileDeletionRequest);

        // Assert that the result is completed and successful
        assertTrue(result.isDone());
        assertEquals(TEST_MESSAGE_ID, result.get().messageId());

        // Verify that the SNS client was called with the correct request
        verify(snsAsyncClient, times(1)).publish(any(PublishRequest.class));
    }

    @Test
    @Description("Verifies that result is completed exceptionally and the S3 delete message is not published to the SNS topic.")
    void givenS3DeleteRequestWhenPublishFailsThenExceptionIsThrown() {
        // Mocking an exception to be thrown when the publish method is called
        CompletableFuture<PublishResponse> futureException = CompletableFuture.failedFuture(new RuntimeException(MESSAGE));

        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(futureException);

        // Call the method to test
        CompletableFuture<PublishResponse> result = s3FileDeleteSnsService.publishMessage(s3FileDeletionRequest);

        // Assert that the result is completed exceptionally
        assertTrue(result.isCompletedExceptionally());

        // Verify that the SNS client was called with the correct request
        verify(snsAsyncClient, times(1)).publish(any(PublishRequest.class));


    }

    @Test
    @Description("Verifies that the result is completed exceptionally due to serialization error and the S3 delete message is published to the SNS topic.")
     void givenS3DeleteRequestWhenJsonSerializationErrorThenExceptionIsThrown() {
        // Mock a CompletableFuture to return an exception (like a serialization error)
        CompletableFuture<PublishResponse> futureException = CompletableFuture.failedFuture(new RuntimeException(MESSAGE1));

        // Mock snsAsyncClient.publish to return the failed future
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(futureException);

        // Call the method to test
        CompletableFuture<PublishResponse> result = s3FileDeleteSnsService.publishMessage(s3FileDeletionRequest);

        // Assert that the result is completed exceptionally due to serialization failure
        assertTrue(result.isCompletedExceptionally());

        // Verify that the SNS client was called once
        verify(snsAsyncClient, times(1)).publish(any(PublishRequest.class));
    }
}