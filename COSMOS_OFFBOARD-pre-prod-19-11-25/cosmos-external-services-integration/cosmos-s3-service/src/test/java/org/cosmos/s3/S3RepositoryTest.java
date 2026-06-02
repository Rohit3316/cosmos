package org.cosmos.s3;

import io.qameta.allure.Description;
import java.nio.file.Path;
import org.cosmos.s3.exception.S3Exception;
import org.cosmos.s3.model.S3FileUpload;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3RepositoryTest {
    private static final String TEST_FILENAME_PREFIX = "test";
    private static final String TEST_FILENAME_SUFFIX = ".txt";
    private static final String TEST_FILENAME = TEST_FILENAME_PREFIX + TEST_FILENAME_SUFFIX;
    private static final String TEST_BUCKET_NAME = "bucket_name";
    private static final String TEST_KEY_PATH = "/test/path";
    private static final String TEST_FILE_PATH = "/testRepo";
    @Mock
    private S3Client s3Client;
    @InjectMocks
    private S3Repository s3Repository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @Description("Verifies that the file is uploaded to the S3.")
    void givenS3FileUploadWhenUploadThenUploadToS3() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(Path.class))).thenReturn(null);
        S3FileUpload fileUpload = S3FileUpload.builder()
                .bucketName(TEST_BUCKET_NAME)
                .keyPath(TEST_KEY_PATH)
                .filename(TEST_FILENAME)
                .filePath(TEST_FILE_PATH)
                .build();
        s3Repository.uploadFileToS3(fileUpload);
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(Path.class));
    }

    @Test
    @Description("Verifies that the empty file upload throws exception.")
    void givenNullS3FileUploadWhenUploadThenThrowException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> s3Repository.uploadFileToS3(null));
    }

    @Test
    @Description("Verifies that the empty bucket name upload throws exception.")
    void givenNullBucketNameWhenUploadThenThrowException() {
        S3FileUpload fileUpload = S3FileUpload.builder()
                .bucketName(null)
                .keyPath(TEST_KEY_PATH)
                .filename(TEST_FILENAME)
                .filePath(TEST_FILE_PATH)
                .build();

        Assertions.assertThrows(IllegalArgumentException.class, () -> s3Repository.uploadFileToS3(fileUpload));
    }

    @Test
    @Description("Verifies that the empty key path upload throws exception.")
    void givenEmptyKeyPathWhenUploadThenThrowException() {
        S3FileUpload fileUpload = S3FileUpload.builder()
                .bucketName(TEST_BUCKET_NAME)
                .keyPath("")
                .filename(TEST_FILENAME)
                .filePath(TEST_FILE_PATH)
                .build();

        Assertions.assertThrows(IllegalArgumentException.class, () -> s3Repository.uploadFileToS3(fileUpload));
    }

    @Test
    @Description("Verifies that the empty filename throws exception.")
    void givenEmptyFileNameWhenUploadThenThrowException() {
        S3FileUpload fileUpload = S3FileUpload.builder()
                .bucketName(TEST_BUCKET_NAME)
                .keyPath(TEST_KEY_PATH)
                .filename("")
                .filePath(TEST_FILE_PATH)
                .build();
        Assertions.assertThrows(IllegalArgumentException.class, () -> s3Repository.uploadFileToS3(fileUpload));
    }

    @Test
    @Description("Verifies that the empty file path upload throws exception.")
    void givenEmptyFilePathWhenUploadThenThrowException() {
        S3FileUpload fileUpload = S3FileUpload.builder()
                .bucketName(TEST_BUCKET_NAME)
                .keyPath(TEST_KEY_PATH)
                .filename(TEST_FILENAME)
                .filePath("")
                .build();

        Assertions.assertThrows(IllegalArgumentException.class, () -> s3Repository.uploadFileToS3(fileUpload));
    }

    @Test
    @Description("Verifies that file upload to S3 fails throws S3Exception.")
    void givenUploadFailureWhenUploadThenThrowS3Exception() {
        S3FileUpload fileUpload = S3FileUpload.builder()
                .bucketName(TEST_BUCKET_NAME)
                .keyPath(TEST_KEY_PATH)
                .filename(TEST_FILENAME)
                .filePath(TEST_FILE_PATH)
                .build();

        doThrow(RuntimeException.class).when(s3Client).putObject(any(PutObjectRequest.class), any(Path.class));

        Assertions.assertThrows(S3Exception.class, () -> s3Repository.uploadFileToS3(fileUpload));
    }

    @Test
    @Description("Verifies that the file is downloaded from S3.")
    void givenS3FileUploadWhenDownloadThenDownloadToS3() {
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(null);
        S3FileUpload fileUpload = S3FileUpload.builder()
                .bucketName(TEST_BUCKET_NAME)
                .keyPath(TEST_KEY_PATH)
                .filename(TEST_FILENAME)
                .build();
        s3Repository.downloadFileFromS3(fileUpload);
        verify(s3Client, times(1)).getObject(any(GetObjectRequest.class));
    }

    @Test
    @Description("Verifies that file download from S3 fails throws S3Exception.")
    void givenDownloadFailureWhenDownloadThenThrowS3Exception() {
        S3FileUpload fileUpload = S3FileUpload.builder()
                .bucketName(TEST_BUCKET_NAME)
                .keyPath(TEST_KEY_PATH)
                .filename(TEST_FILENAME)
                .build();

        doThrow(RuntimeException.class).when(s3Client).getObject(any(GetObjectRequest.class));

        Assertions.assertThrows(S3Exception.class, () -> s3Repository.downloadFileFromS3(fileUpload));
    }

    @Test
    @Description("Verifies that the file is deleted from S3.")
    void givenS3FileUploadWhenDeletedThenDeleteFromS3() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(null);
        S3FileUpload fileUpload = S3FileUpload.builder()
                .bucketName(TEST_BUCKET_NAME)
                .keyPath(TEST_KEY_PATH)
                .filename(TEST_FILENAME)
                .build();
        SdkHttpResponse sdkHttpResponse = SdkHttpResponse.builder()
                .statusCode(200) // Successful response status
                .build();

        SdkResponse sdkResponse = DeleteObjectResponse.builder().sdkHttpResponse(sdkHttpResponse).build();
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn((DeleteObjectResponse) sdkResponse);
        s3Repository.deleteFileFromS3(fileUpload);
        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @Description("Verifies that file delete from S3 fails throws S3Exception.")
    void givenDeleteFailureWhenDeleteThenThrowS3Exception() {
        S3FileUpload fileUpload = S3FileUpload.builder()
                .bucketName(TEST_BUCKET_NAME)
                .keyPath(TEST_KEY_PATH)
                .filename(TEST_FILENAME)
                .build();

        doThrow(S3Exception.class).when(s3Client).deleteObject(any(DeleteObjectRequest.class));

        Assertions.assertThrows(S3Exception.class, () -> s3Repository.deleteFileFromS3(fileUpload));
    }

    @Test
    @Description("Given a valid S3FileUpload, when getFileSizeFromS3 is called, then it returns the correct file size.")
    void givenValidS3FileWhenGetFileSizeFromS3ThenReturnsCorrectFileSize() {
        S3FileUpload fileUpload = S3FileUpload.builder()
                .bucketName(TEST_BUCKET_NAME)
                .keyPath(TEST_KEY_PATH)
                .filename(TEST_FILENAME)
                .build();

        HeadObjectResponse mockResponse = (HeadObjectResponse) HeadObjectResponse.builder()
                .contentLength(12345L)
                .sdkHttpResponse(SdkHttpResponse.builder().statusCode(200).build())
                .build();

        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(mockResponse);

        Long size = s3Repository.getFileSizeFromS3(fileUpload);

        Assertions.assertEquals(12345L, size, "File size should match the mocked content length");
        verify(s3Client, times(1)).headObject(any(HeadObjectRequest.class));
    }

    @Test
    @Description("Given an invalid S3FileUpload, when getFileSizeFromS3 is called, then S3Exception is thrown.")
    void givenInvalidS3FileWhenGetFileSizeFromS3ThenThrowsS3Exception() {
        S3FileUpload fileUpload = S3FileUpload.builder()
                .bucketName(TEST_BUCKET_NAME)
                .keyPath(TEST_KEY_PATH)
                .filename(TEST_FILENAME)
                .build();

        HeadObjectResponse mockResponse = (HeadObjectResponse) HeadObjectResponse.builder()
                .contentLength(0L)
                .sdkHttpResponse(SdkHttpResponse.builder().statusCode(404).build())
                .build();

        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(mockResponse);

        S3Exception exception = Assertions.assertThrows(
                S3Exception.class,
                () -> s3Repository.getFileSizeFromS3(fileUpload),
                "Expected S3Exception when headObject response is unsuccessful"
        );
        Assertions.assertTrue(exception.getMessage().contains("Failed to perform head request on S3") ||
                        exception.getMessage().contains("Error occurred while performing head request on S3"),
                "Exception message should indicate head request failure");
        verify(s3Client, times(1)).headObject(any(HeadObjectRequest.class));
    }

    @Test
    @Description("Given headObject throws an exception, when getFileSizeFromS3 is called, then S3Exception is thrown.")
    void givenHeadObjectThrowsExceptionWhenGetFileSizeFromS3ThenThrowsS3Exception() {
        S3FileUpload fileUpload = S3FileUpload.builder()
                .bucketName(TEST_BUCKET_NAME)
                .keyPath(TEST_KEY_PATH)
                .filename(TEST_FILENAME)
                .build();

        RuntimeException awsException = new S3Exception("AWS error");
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(awsException);

        S3Exception thrown = Assertions.assertThrows(
                S3Exception.class,
                () -> s3Repository.getFileSizeFromS3(fileUpload),
                "Expected S3Exception when headObject throws an exception"
        );
        Assertions.assertTrue(
                thrown.getMessage().contains("Error occurred while performing head request on S3"),
                "Exception message should indicate head request failure"
        );
        Assertions.assertEquals(awsException, thrown.getCause(), "Original exception should be the cause");
        verify(s3Client, times(1)).headObject(any(HeadObjectRequest.class));
    }

    @Test
    @Description("Given invalid input, when getFileSizeFromS3 is called, then IllegalArgumentException is thrown.")
    void givenInvalidInputWhenGetFileSizeFromS3ThenThrowIllegalArgumentException() {
        // Null input
        Assertions.assertThrows(IllegalArgumentException.class, () -> s3Repository.getFileSizeFromS3(null), "Should throw for null input");

        // Empty bucket name
        S3FileUpload emptyBucket = S3FileUpload.builder()
                .bucketName("")
                .keyPath(TEST_KEY_PATH)
                .filename(TEST_FILENAME)
                .build();
        Assertions.assertThrows(IllegalArgumentException.class, () -> s3Repository.getFileSizeFromS3(emptyBucket), "Should throw for empty bucket name");

        // Empty key path
        S3FileUpload emptyKeyPath = S3FileUpload.builder()
                .bucketName(TEST_BUCKET_NAME)
                .keyPath("")
                .filename(TEST_FILENAME)
                .build();
        Assertions.assertThrows(IllegalArgumentException.class, () -> s3Repository.getFileSizeFromS3(emptyKeyPath), "Should throw for empty key path");

        // Empty filename
        S3FileUpload emptyFilename = S3FileUpload.builder()
                .bucketName(TEST_BUCKET_NAME)
                .keyPath(TEST_KEY_PATH)
                .filename("")
                .build();
        Assertions.assertThrows(IllegalArgumentException.class, () -> s3Repository.getFileSizeFromS3(emptyFilename), "Should throw for empty filename");
    }
}