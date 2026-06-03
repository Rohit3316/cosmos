package org.cosmos.s3;
import java.io.ByteArrayInputStream;
import jdk.jfr.Description;
import org.cosmos.s3.exception.S3Exception;
import org.cosmos.s3.model.S3FileUpload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class S3MutlipartFileUploadTest {

    private static final String TEST_BUCKET_NAME = "test-bucket";
    private static final String TEST_KEY_PATH = "test/key/path/";
    private static final String TEST_FILENAME = "test-file.txt";
    private static final String TEST_FILE_PATH = "/path/to/test-file.txt";
    private static final String UPLOAD_ID = "upload-id";

    @Mock
    private S3Client s3Client;

    @Mock
    private DataSizeConverter dataSizeConverter;

    @InjectMocks
    private S3MultipartFileUpload s3MultipartFileUpload;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        s3MultipartFileUpload = new S3MultipartFileUpload(s3Client);
        s3MultipartFileUpload.setDataSizeConverter(dataSizeConverter);
        lenient().when(dataSizeConverter.getChunkSizeInBytes()).thenReturn(5 * 1024 * 1024); // 5 MB
    }


    @Test
    @Description("Validates S3 multipart file upload, including creation, part upload, and completion steps with correct responses.")
    void givenMultipartUploadWhenUploadingFileThenSuccess() throws Exception {
        // Prepare a temporary test file
        File tempFile = File.createTempFile("test-file", ".txt");
        tempFile.deleteOnExit();

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("This is a test file with some dummy data.");
        }

        assertTrue(tempFile.exists(), "The temporary file should exist before the test.");

        // Mock the initial multipart upload creation response
        CreateMultipartUploadResponse createMultipartUploadResponse =
                CreateMultipartUploadResponse.builder().uploadId(UPLOAD_ID).build();
        doReturn(createMultipartUploadResponse)
                .when(s3Client)
                .createMultipartUpload(any(CreateMultipartUploadRequest.class));

        UploadPartResponse uploadPartResponse =
                UploadPartResponse.builder().eTag("djFFssxnKzhNaUiPyRMKA60og9wj9afs43uFpLjtseo=-").build();
        doReturn(uploadPartResponse)
                .when(s3Client)
                .uploadPart(any(UploadPartRequest.class), any(RequestBody.class));

        // Mock the complete multipart upload response
        CompleteMultipartUploadResponse completeMultipartUploadResponse =
                CompleteMultipartUploadResponse.builder()
                        .checksumSHA256("djFFssxnKzhNaUiPyRMKA60og9wj9afs43uFpLjtseo=-1") // Correct format
                        .build();
        doReturn(completeMultipartUploadResponse)
                .when(s3Client)
                .completeMultipartUpload(any(CompleteMultipartUploadRequest.class));


        S3FileUpload fileUpload = S3FileUpload.builder()
                .bucketName(TEST_BUCKET_NAME)
                .keyPath(TEST_KEY_PATH)
                .filename(TEST_FILENAME)
                .filePath(tempFile.getAbsolutePath())
                .build();

        try (InputStream inputStream = new FileInputStream(tempFile)) {
            assertDoesNotThrow(() -> s3MultipartFileUpload.uploadFileToS3Multipart(fileUpload, inputStream),
                    "The upload should complete successfully with a valid checksum.");
        }
    }

    @Test
    @Description("Validates failure during part upload.")
    void givenMultipartUploadWhenPartUploadFailsThenThrowException() throws Exception {
        // Mock the multipart upload creation response
        CreateMultipartUploadResponse createMultipartUploadResponse =
                CreateMultipartUploadResponse.builder().uploadId(UPLOAD_ID).build();
        doReturn(createMultipartUploadResponse)
                .when(s3Client)
                .createMultipartUpload(any(CreateMultipartUploadRequest.class));

        // Simulate failure in part upload
        doThrow(SdkClientException.builder().message("Part upload failed").build())
                .when(s3Client)
                .uploadPart(any(UploadPartRequest.class), any(RequestBody.class));

        S3FileUpload fileUpload = S3FileUpload.builder()
                .bucketName(TEST_BUCKET_NAME)
                .keyPath(TEST_KEY_PATH)
                .filename(TEST_FILENAME)
                .filePath("/dummy/path")
                .build();

        try (InputStream inputStream = new ByteArrayInputStream("Dummy Data".getBytes())) {
            assertThrows(Exception.class,
                    () -> s3MultipartFileUpload.uploadFileToS3Multipart(fileUpload, inputStream),
                    "An exception should be thrown if part upload fails.");
        }
    }


    @Test
    @Description("Validates failure when checksum verification fails.")
    void givenMultipartUploadWhenChecksumVerificationFailsThenThrowException() throws Exception {
        // Mock the multipart upload creation response
        CreateMultipartUploadResponse createMultipartUploadResponse =
                CreateMultipartUploadResponse.builder().uploadId(UPLOAD_ID).build();
        doReturn(createMultipartUploadResponse)
                .when(s3Client)
                .createMultipartUpload(any(CreateMultipartUploadRequest.class));

        // Mock successful part upload
        UploadPartResponse uploadPartResponse =
                UploadPartResponse.builder().eTag("dummy-etag").checksumSHA256("invalid-checksum").build();
        doReturn(uploadPartResponse)
                .when(s3Client)
                .uploadPart(any(UploadPartRequest.class), any(RequestBody.class));

        // Mock the complete multipart upload response with mismatched checksum
        CompleteMultipartUploadResponse completeMultipartUploadResponse =
                CompleteMultipartUploadResponse.builder()
                        .checksumSHA256("mismatched-checksum-123") // Incorrect checksum
                        .build();
        doReturn(completeMultipartUploadResponse)
                .when(s3Client)
                .completeMultipartUpload(any(CompleteMultipartUploadRequest.class));

        S3FileUpload fileUpload = S3FileUpload.builder()
                .bucketName(TEST_BUCKET_NAME)
                .keyPath(TEST_KEY_PATH)
                .filename(TEST_FILENAME)
                .filePath("/dummy/path")
                .build();

        try (InputStream inputStream = new ByteArrayInputStream("Dummy Data".getBytes())) {
            assertThrows(Exception.class,
                    () -> s3MultipartFileUpload.uploadFileToS3Multipart(fileUpload, inputStream),
                    "An exception should be thrown if checksum verification fails.");
        }
    }

    @Test
    @Description("Validates that the multipart upload process fails with the expected error message when the specified file does not exist.")
    void givenFileDoesNotExistWhenUploadingThenThrowException() {
        // Mock the S3FileUpload object to use the mocked file
        S3FileUpload fileUpload = S3FileUpload.builder()
                .bucketName(TEST_BUCKET_NAME)
                .keyPath(TEST_KEY_PATH)
                .filename(TEST_FILENAME)
                .filePath(TEST_FILE_PATH) // This path will be used for the mocked file
                .build();

        CreateMultipartUploadResponse createMultipartUploadResponse =
                CreateMultipartUploadResponse.builder().uploadId(UPLOAD_ID).build();
        doReturn(createMultipartUploadResponse)
                .when(s3Client)
                .createMultipartUpload(any(CreateMultipartUploadRequest.class));

        InputStream invalidInputStream = null;

        Exception thrownException = assertThrows(Exception.class, () ->
            s3MultipartFileUpload.uploadFileToS3Multipart(fileUpload, invalidInputStream)
        );

        // Verify the exception message
        assertEquals("Error during multipart upload", thrownException.getMessage());

    }

    @Test
    @Description(" S3 Unavailable")
    void givenMultipartUploadWhenInitiateMultipartUploadFailsThenThrowSdkServiceException(){
        // Mock the multipart upload creation response
        when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
                .thenThrow(SdkServiceException.builder().message("S3 service is Unavailable").build());

        S3FileUpload fileUpload = S3FileUpload.builder()
                .bucketName(TEST_BUCKET_NAME)
                .keyPath(TEST_KEY_PATH)
                .filename(TEST_FILENAME)
                .filePath("/dummy/path")
                .build();

        InputStream inputStream = new ByteArrayInputStream("Dummy Data".getBytes());
        Exception exception = assertThrows(Exception.class,
                () -> s3MultipartFileUpload.uploadFileToS3Multipart(fileUpload, inputStream));
        assertEquals("S3 service is unavailable. Please try again later.", exception.getMessage());
    }

    @Test
    @Description("S3 Permission Denied")
    void givenMultipartUploadWhenInitiateMultipartUploadFailsThenThrowS3Exception() {
        // Mock the multipart upload creation response to throw an S3Exception
        when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
                .thenThrow(software.amazon.awssdk.services.s3.model.S3Exception.builder().message("Permission denied for the S3 bucket. Check your permissions.").build());

        S3FileUpload fileUpload = S3FileUpload.builder()
                .bucketName(TEST_BUCKET_NAME)
                .keyPath(TEST_KEY_PATH)
                .filename(TEST_FILENAME)
                .filePath("/dummy/path")
                .build();

        InputStream inputStream = new ByteArrayInputStream("Dummy Data".getBytes());
        S3Exception exception = assertThrows(S3Exception.class,
                () -> s3MultipartFileUpload.uploadFileToS3Multipart(fileUpload, inputStream));
        assertEquals("Permission denied for the S3 bucket. Check your permissions.", exception.getMessage());
    }

    @Test
    @Description("Unexpected error during upload.")
    void givenMultipartUploadWhenInitiateMultipartUploadFailsThenThrowException() {
        // Mock the multipart upload creation response to throw an S3Exception
        when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
                .thenThrow(new RuntimeException("Unexpected error during upload."));

        S3FileUpload fileUpload = S3FileUpload.builder()
                .bucketName(TEST_BUCKET_NAME)
                .keyPath(TEST_KEY_PATH)
                .filename(TEST_FILENAME)
                .filePath("/dummy/path")
                .build();

        InputStream inputStream = new ByteArrayInputStream("Dummy Data".getBytes());
        S3Exception exception = assertThrows(S3Exception.class,
                () -> s3MultipartFileUpload.uploadFileToS3Multipart(fileUpload, inputStream));
        assertEquals("Unexpected error during upload.", exception.getMessage());
    }
}