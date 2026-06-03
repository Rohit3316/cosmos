package org.eclipse.hawkbit.mgmt.rest.resource;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import io.qameta.allure.Description;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.cosmos.models.mgmt.FileType;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.artifacts.constants.ArtifactsStatus;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifactSoftwareModuleAssociationEntity;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaVersion;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Version;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.mgmt.rest.resource.MgmtTargetResourceTest.JAKARTA_VALIDATION_VALIDATION_EXCEPTION;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
public class MgmtArtifactsResource2Test extends AbstractManagementRolloutApiIntegrationTest {

    private static final String FILE_URL = "https://docs.aws.amazon.com/it_it/whitepapers/latest/overview-aws-cloud-adoption-framework/overview-aws-cloud-adoption-framework.pdf";

    @MockBean
    private S3Client s3Client;

    @PersistenceContext
    private EntityManager entityManager;

    AutoCloseable mocks;

    private static final String TEST_CONTENT = "Test content";
    private static final String TEST_FILE_NAME_TXT = "test_file.txt";
    private static final String TEST_SHA256 = "9d9595c5d94fb65b824f56e9999527dba9542481580d69feb89056aabaa0aa87";
    private static final String TEST_DESCRIPTION = "Description";
    private final String TEST_SIGNATURE_EXPIRY_DATE = String.valueOf(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());
    private static final String FILE_TYPE = "$.fileType";
    private static final String JSON_PATH_SW_MODULES = "$.softwareModules";
    private static final String JSON_PATH_SW_MODULE_ID = "$.softwareModules[0].id";
    private static final String JSON_PATH_SOURCE_VERSION_FULL = "$.softwareModules[0].sourceVersionsForFull";
    private static final String JSON_PATH_SOURCE_VERSION_DELTA = "$.softwareModules[0].sourceVersionForDelta";
    private static final String JSON_PATH_TARGET_VERSION = "$.softwareModules[0].targetVersion";

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

    @AfterEach
    void tearDown() throws Exception {
        mocks.close(); // ensure mocks are reset between tests
    }

    private void setupS3ClientMock() {
        CreateMultipartUploadResponse createMultipartUploadResponse = CreateMultipartUploadResponse.builder()
                .uploadId("dummyUploadId")
                .build();
        doReturn(createMultipartUploadResponse).when(s3Client).createMultipartUpload(any(CreateMultipartUploadRequest.class));

        UploadPartResponse dummyResponse = UploadPartResponse.builder()
                .eTag("dummyETag")
                .checksumSHA256("FDWd9tdAThpL2l6kH9BkaZaOOo88Wjh0FbTF9kAL0z4=-1")
                .build();
        doReturn(dummyResponse).when(s3Client).uploadPart(any(UploadPartRequest.class), any(RequestBody.class));

        CompleteMultipartUploadResponse completeMultipartUploadResponse = CompleteMultipartUploadResponse.builder()
                .location("dummyLocation")
                .bucket("dummyBucket")
                .key("dummyKey")
                .eTag("dummyETag")
                .checksumSHA256("FDWd9tdAThpL2l6kH9BkaZaOOo88Wjh0FbTF9kAL0z4=-1")
                .build();
        doReturn(completeMultipartUploadResponse).when(s3Client).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
        SdkHttpResponse sdkHttpResponse = SdkHttpResponse.builder()
                .statusCode(200) // Successful response status
                .build();

        SdkResponse sdkResponse = DeleteObjectResponse.builder().sdkHttpResponse(sdkHttpResponse).build();
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn((DeleteObjectResponse) sdkResponse);

    }

    @BeforeEach
    void setup() {
        JdbcTestUtils.deleteFromTables(
                jdbcTemplate,
                SP_ARTIFACT_SOFTWARE_MODULE, SP_ARTIFACTS, SP_SOFTWARE_VERSIONS,
                SP_SOFTWARE_ECU_MODEL, SP_VEHICLE_MODEL, SP_TARGET_INVENTORY, SP_RSP_ROLLOUT, SP_ESP_ECU_ROLLOUT,
                SP_ESP, SP_RSP, SP_TARGET, SP_SOFTWARE_ECU_MODEL,
                SP_BASE_SOFTWARE_MODULE, SP_ROLLOUT
        );
        // Re-init mocks
        mocks = MockitoAnnotations.openMocks(this);

        PublishResponse mockPublishResponse = PublishResponse.builder()
                .messageId("mockMessageId")
                .build();
        // Create a completed CompletableFuture with the mock response
        CompletableFuture<PublishResponse> completedFuture = CompletableFuture.completedFuture(mockPublishResponse);
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(completedFuture);
        setupS3ClientMock();
        entityManager.clear();
    }

    @Test
    @Description("Given an existing artifact and an invalid tenant is provided for replacement, " +
            "When the replace artifact API is called, " +
            "Then a 400 Bad Request is returned indicating the tenant is invalid.")
    void givenInvalidTenantWhenReplaceArtifactThenReturnBadRequest() throws Exception {

        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate("test", FileType.DELTA, "desc",
                "100", "abc", Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING, 1000, artifact.getSha256Hash())
                        .param("fileURL", FILE_URL)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", FileType.DELTA.name())
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Given an existing artifact and a case insensitive filetype is provided for replacement, " +
            "When the replace artifact API is called, " +
            "Then the artifact is replaced successfully.")
    void givenCaseInsensitiveFileTypeWhenReplaceArtifactThenArtifactIsReplacedSuccessfully() throws Exception {

        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate("test", FileType.FULL, "desc",
                "100", "abc", Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.toString());

        JpaArtifacts jpaArtifacts = (JpaArtifacts) artifact;
        jpaArtifacts.setFileStatus(FileTransferStatus.UPLOADING_TO_STORAGE.name());
        artifactsRepository.save(jpaArtifacts);

        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING, 1L, artifact.getSha256Hash())
                        .param("fileURL", FILE_URL)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", "fUlL")
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());
    }

    @Test
    @Description("Given an existing artifact and both a valid file URL and a multipart file are provided for replacement, " +
            "When the replace artifact API is called, " +
            "Then the file URL is preferred over the multipart file, " +
            "and the artifact is replaced successfully with the new file from the URL.")
    void givenFileURLAndMultipartFileWhenReplaceArtifactThenFileURLIsPreferredAndArtifactIsReplacedSuccessfully() throws Exception {

        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate("test", FileType.DELTA,
                "desc", "100", "abc", Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.toString());

        MockMultipartFile file = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, TEST_CONTENT.getBytes());

        JpaArtifacts jpaArtifacts = (JpaArtifacts) artifact;
        jpaArtifacts.setFileStatus(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.name());
        artifactsRepository.save(jpaArtifacts);

        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING,
                                1L, artifact.getSha256Hash())
                        .file(file)
                        .param("fileURL", FILE_URL)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", FileType.DELTA.name())
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value(TEST_FILE_NAME_TXT))
                .andExpect(jsonPath("$.hashes.sha256").value(TEST_SHA256))
                .andExpect(jsonPath("$.description").value(TEST_DESCRIPTION))
                .andExpect(jsonPath(FILE_TYPE).value(FileType.DELTA.name()));

        Artifacts artifacts = artifactsRepository.getArtifactsById(artifact.getId() + 1)
                .orElseThrow(() -> new AssertionError("Artifact with ID 2L not found"));

        assertEquals(TEST_FILE_NAME_TXT, artifacts.getFileName(), "Filename should match");
        assertEquals(TEST_SHA256, artifacts.getSha256Hash(), "SHA256 should match");
        assertEquals(TEST_DESCRIPTION, artifacts.getDescription(), "Description should match");
        assertEquals(FileType.DELTA, artifacts.getFileType(), "FileType should be DELTA");
        assertEquals(ArtifactsStatus.ACTIVE.name(), artifacts.getArtifactStatus());

        Artifacts oldArtifacts = artifactsRepository.getArtifactsById(artifact.getId()).get();
        assertEquals(ArtifactsStatus.REPLACED.name(), oldArtifacts.getArtifactStatus());

    }

    @Test
    @Description("Given an existing artifact and a valid file URL is provided for replacement, " +
            "When the replace artifact API is called, " +
            "and the artifact is replaced successfully with the new file from the URL.")
    void givenFileURLWhenReplaceArtifactThenArtifactIsReplacedSuccessfully() throws Exception {

        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate("test", FileType.DELTA, "desc",
                "100", "abc", Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.toString());

        JpaArtifacts jpaArtifacts = (JpaArtifacts) artifact;
        jpaArtifacts.setFileStatus(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.name());
        artifactsRepository.save(jpaArtifacts);

        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING,
                                1L, artifact.getSha256Hash())
                        .param("fileURL", FILE_URL)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", FileType.DELTA.name())
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value(TEST_FILE_NAME_TXT))
                .andExpect(jsonPath("$.hashes.sha256").value(TEST_SHA256))
                .andExpect(jsonPath("$.description").value(TEST_DESCRIPTION))
                .andExpect(jsonPath(FILE_TYPE).value(FileType.DELTA.name()));

        Artifacts artifacts = artifactsRepository.getArtifactsById(artifact.getId() + 1)
                .orElseThrow(() -> new AssertionError("Artifact with ID 2L not found"));

        assertEquals(TEST_FILE_NAME_TXT, artifacts.getFileName(), "Filename should match");
        assertEquals(TEST_SHA256, artifacts.getSha256Hash(), "SHA256 should match");
        assertEquals(TEST_DESCRIPTION, artifacts.getDescription(), "Description should match");
        assertEquals(FileType.DELTA, artifacts.getFileType(), "FileType should be DELTA");
        assertEquals(ArtifactsStatus.ACTIVE.name(), artifacts.getArtifactStatus());

        Artifacts oldArtifacts = artifactsRepository.getArtifactsById(artifact.getId()).get();
        assertEquals(ArtifactsStatus.REPLACED.name(), oldArtifacts.getArtifactStatus());
    }

    @Test
    @Description("Given an existing artifact and a multipart file is provided for replacement, " +
            "When the replace artifact API is called, " +
            "and the artifact is replaced successfully with the new multipart file.")
    void givenMultipartFileWhenReplaceArtifactThenArtifactIsReplacedSuccessfully() throws Exception {

        MockMultipartFile file = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, TEST_CONTENT.getBytes());

        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate("test", FileType.DELTA, "desc",
                "100", "abc", Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.toString());

        JpaArtifacts jpaArtifacts = (JpaArtifacts) artifact;
        jpaArtifacts.setFileStatus(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.name());
        artifactsRepository.save(jpaArtifacts);

        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING,
                                1L, artifact.getSha256Hash())
                        .file(file)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", FileType.DELTA.name())
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

        Artifacts artifacts = artifactsRepository.getArtifactsById(artifact.getId() + 1)
                .orElseThrow(() -> new AssertionError("Artifact with ID 2L not found"));

        assertEquals(TEST_FILE_NAME_TXT, artifacts.getFileName(), "Filename should match");
        assertEquals(TEST_SHA256, artifacts.getSha256Hash(), "SHA256 should match");
        assertEquals(TEST_DESCRIPTION, artifacts.getDescription(), "Description should match");
        assertEquals(FileType.DELTA, artifacts.getFileType(), "FileType should be DELTA");
        assertEquals(ArtifactsStatus.ACTIVE.name(), artifacts.getArtifactStatus());

        Artifacts oldArtifacts = artifactsRepository.getArtifactsById(artifact.getId()).get();
        assertEquals(ArtifactsStatus.REPLACED.name(), oldArtifacts.getArtifactStatus());
    }

    @Test
    @Description("Given an existing artifact and an invalid file URL is provided for replacement, " +
            "When the replace artifact API is called, " +
            "Then a 400 Bad Request is returned indicating the file URL is invalid.")
    void givenInvalidFileURLWhenReplaceArtifactThenReturnBadRequest() throws Exception {

        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate("test", FileType.DELTA, "desc",
                "100", "abc", Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING, 1L, artifact.getSha256Hash())
                        .param("fileURL", "invalid-url")
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", FileType.DELTA.name())
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(JAKARTA_VALIDATION_VALIDATION_EXCEPTION)))
                .andExpect(jsonPath(JSON_PATH_DEBUG, notNullValue()))
                .andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("'fileURL' should be a valid URL to download the artifact")));
    }

    @Test
    @Description("Given an existing artifact and an invalid file type is provided for replacement, " +
            "When the replace artifact API is called, " +
            "Then a 400 Bad Request is returned indicating the file type is invalid.")
    void givenInvalidFileTypeWhenReplaceArtifactThenReturnBadRequest() throws Exception {
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate("test", FileType.DELTA, "desc",
                "100", "abc", Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING, 1L, artifact.getSha256Hash())
                        .param("fileURL", FILE_URL)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", "INVALID")
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(JAKARTA_VALIDATION_VALIDATION_EXCEPTION)))
                .andExpect(jsonPath(JSON_PATH_DEBUG, notNullValue()))
                .andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("'fileType' should be either 'DELTA' or 'FULL'")));

        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING, 1L, artifact.getSha256Hash())
                        .param("fileURL", FILE_URL)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo("org.springframework.web.bind.MissingServletRequestParameterException")))
                .andExpect(jsonPath(JSON_PATH_DEBUG, notNullValue()))
                .andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("Required request parameter 'fileType' for method parameter type String is not present")));
    }

    @Test
    @Description("Given an existing artifact with mismatched filetype for replacement, " +
            "When the replace artifact API is called, " +
            "Then a 400 Bad Request is returned indicating the file type is invalid.")
    void givenExistingArtifactWithMismatchedFileTypeWhenReplaceArtifactThenReturnBadRequest() throws Exception {
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate("test", FileType.FULL, "desc", "100", "abc", Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING, 1L, artifact.getSha256Hash())
                        .param("fileURL", FILE_URL)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", FileType.DELTA.name())
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(JAKARTA_VALIDATION_VALIDATION_EXCEPTION)))
                .andExpect(jsonPath(JSON_PATH_DEBUG, notNullValue()))
                .andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("'fileType' should be the same as the fileType of the artifact being replaced")));
    }

    @Test
    @Description("Given an existing artifact and an invalid old sha256 is provided for replacement, " +
            "When the replace artifact API is called, " +
            "Then a 400 Bad Request is returned indicating the sha256 is invalid.")
    void givenInvalidOldSha256WhenReplaceArtifactThenReturnBadRequest() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING, 1L, TEST_SHA256)
                        .param("fileURL", FILE_URL)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", FileType.DELTA.name())
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo("ENTITY_NOT_EXISTS")))
                .andExpect(jsonPath(JSON_PATH_DEBUG, notNullValue()))
                .andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("No artifact found with sha256")));

        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING, 1L, null)
                        .param("fileURL", FILE_URL)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", FileType.DELTA.name())
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnsupportedMediaType());

    }

    @Test
    @Description("Given an existing artifact and an invalid new sha256 is provided for replacement, " +
            "When the replace artifact API is called, " +
            "Then a 400 Bad Request is returned indicating the sha256 is invalid.")
    void givenInvalidNewSha256WhenReplaceArtifactThenReturnBadRequest() throws Exception {
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate("test", FileType.FULL, "desc", "100", "abc", Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING, 1L, artifact.getSha256Hash())
                        .param("fileURL", FILE_URL)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", FileType.DELTA.name())
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo("org.springframework.web.bind.MissingServletRequestParameterException")))
                .andExpect(jsonPath(JSON_PATH_DEBUG, notNullValue()))
                .andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("Required request parameter 'sha256' for method parameter type String is not present")));
    }

    @Test
    @Description("Given an existing artifact and a duplicate new sha256 is provided for replacement, " +
            "When the replace artifact API is called, " +
            "Then a 400 Bad Request is returned indicating the sha256 is duplicate.")
    void givenDuplicateNewSha256WhenReplaceArtifactThenReturnBadRequest() throws Exception {
        // Simulate duplicate sha256 in testdataFactory or mock
        Artifacts artifacts = testdataFactory.createArtifactsWithExpiryDate("test", FileType.DELTA, "desc", "100", TEST_SHA256, Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING, 1L, artifacts.getSha256Hash())
                        .param("fileURL", FILE_URL)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", FileType.DELTA.name())
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo("ENTITY_ALREADY_EXISTS")))
                .andExpect(jsonPath(JSON_PATH_DEBUG, notNullValue()))
                .andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("File already exists in COSMOS with SHA-256: " +
                        "9d9595c5d94fb65b824f56e9999527dba9542481580d69feb89056aabaa0aa87")));
    }

    @Test
    @Description("Given an existing artifact and a past signature expiry date is provided for replacement, " +
            "When the replace artifact API is called, " +
            "Then a 400 Bad Request is returned indicating the signature expiry date is past.")
    void givenSignatureExpiryDateIsPastWhenReplaceArtifactThenReturnBadRequest() throws Exception {
        String pastDate = String.valueOf(Instant.now().minus(1, ChronoUnit.DAYS).getEpochSecond());
        Artifacts artifacts = testdataFactory.createArtifactsWithExpiryDate("test", FileType.DELTA, "desc", "100", TEST_SHA256, Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING, 1L, artifacts.getSha256Hash())
                        .param("fileURL", FILE_URL)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", FileType.DELTA.name())
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", pastDate)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(JAKARTA_VALIDATION_VALIDATION_EXCEPTION)))
                .andExpect(jsonPath(JSON_PATH_DEBUG, notNullValue()))
                .andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("'signatureExpiryDate' should not be past date or current date")));
    }

    @Test
    @Description("Given an existing artifact and an invalid signature expiry date is provided for replacement, " +
            "When the replace artifact API is called, " +
            "Then a 400 Bad Request is returned indicating the signature expiry date is invalid.")
    void givenInvalidSignatureExpiryDateWhenReplaceArtifactThenReturnBadRequest() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING, 1L, TEST_SHA256)
                        .param("fileURL", FILE_URL)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", FileType.DELTA.name())
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo("org.springframework.web.bind.MissingServletRequestParameterException")))
                .andExpect(jsonPath(JSON_PATH_DEBUG, notNullValue()))
                .andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("Required request parameter 'signatureExpiryDate' for method parameter type Long is not present")));
    }

    @Test
    @Description("Given an existing artifact and an invalid filename is provided for replacement, " +
            "When the replace artifact API is called, " +
            "Then a 400 Bad Request is returned indicating the file name is invalid.")
    void givenInvalidFileNameWhenReplaceArtifactThenReturnBadRequest() throws Exception {
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate("test", FileType.DELTA, "desc", "100", "abc", Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING, 1L, artifact.getSha256Hash())
                        .param("fileURL", FILE_URL)
                        .param("filename", "") // Invalid filename
                        .param("fileType", FileType.DELTA.name())
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo("CONSTRAINT_VIOLATION")))
                .andExpect(jsonPath(JSON_PATH_DEBUG, notNullValue()))
                .andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("replaceArtifacts.filename must not be empty.")));

        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING, 1L, artifact.getSha256Hash())
                        .param("fileURL", FILE_URL)
                        .param("fileType", FileType.DELTA.name())
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo("org.springframework.web.bind.MissingServletRequestParameterException")))
                .andExpect(jsonPath(JSON_PATH_DEBUG, notNullValue()))
                .andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("Required request parameter 'filename' for method parameter type String is not present")));
    }

    @Test
    @Description("Given an existing artifact and, file URL and multipart file is not provided for replacement, " +
            "When the replace artifact API is called, " +
            "Then a 400 Bad Request is returned indicating either the file URL or multipart file should be provided.")
    void givenMissingFileURLAndMultipartFileWhenReplaceArtifactThenReturnBadRequest() throws Exception {
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate("test", FileType.DELTA, "desc", "100", "abc", Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING, 1L, artifact.getSha256Hash())
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", FileType.DELTA.name())
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(JAKARTA_VALIDATION_VALIDATION_EXCEPTION)))
                .andExpect(jsonPath(JSON_PATH_DEBUG, notNullValue()))
                .andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("Either a file or fileURL must be provided for replacement")));
    }

    @Test
    @Description("Given an existing artifact and it is inactive, multipart file is provided for replacement, " +
            "When the replace artifact API is called, " +
            "Then a 400 Bad Request is returned indicating the existing artifact is inactive.")
    void givenInactiveExistingArtifactWhenReplaceArtifactThenReturnBadRequest() throws Exception {
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate("test", FileType.DELTA, "desc", "100", "abc", Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        artifactsManagement.purgeArtifacts(artifact);

        MockMultipartFile file = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, TEST_CONTENT.getBytes());
        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING, 1L, artifact.getSha256Hash())
                        .file(file)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", FileType.DELTA.name())
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(JAKARTA_VALIDATION_VALIDATION_EXCEPTION)))
                .andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("File with provided sha256 does not exist in COSMOS")));
    }

    @Test
    @Description("Given an existing artifact that has been uploaded to CDN, "
            + "When the replace artifact API is called with a new file, "
            + "Then the artifact is removed from both CDN and S3 storage, and the replacement is successful.")
    void givenArtifactUploadedToCdnWhenReplaceArtifactThenRemovesFromCdnAndS3(CapturedOutput output) throws Exception {
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate("test", FileType.DELTA, "desc",
                "100", "abc", Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.toString());

        JpaArtifacts jpaArtifacts = (JpaArtifacts) artifact;
        jpaArtifacts.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.name());
        artifactsRepository.save(jpaArtifacts);

        MockMultipartFile file = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, TEST_CONTENT.getBytes());
        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING,
                                1L, artifact.getSha256Hash())
                        .file(file)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", FileType.DELTA.name())
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value(TEST_FILE_NAME_TXT))
                .andExpect(jsonPath("$.hashes.sha256").value(TEST_SHA256))
                .andExpect(jsonPath("$.description").value(TEST_DESCRIPTION))
                .andExpect(jsonPath(FILE_TYPE).value(FileType.DELTA.name()));

        // Verify that the file removal from CDN and storage was triggered

        verify(snsAsyncClient, times(2)).publish(any(PublishRequest.class));
        assertTrue(output.getOut().contains("Removing artifact file with id " + artifact.getId() + " from CDN"));
        assertTrue(output.getOut().contains("Removing artifact file with id " + artifact.getId() + " from storage"));
        assertTrue(output.getOut().contains("Message published to SNS with messageId"));

    }

    @Test
    @Description("Given an existing artifact that has been uploaded to storage, "
            + "When the replace artifact API is called with a new file, "
            + "Then the artifact is removed from S3 storage, and the replacement is successful.")
    void givenArtifactUploadedToStorageWhenReplaceArtifactThenRemovesFromS3(CapturedOutput output) throws Exception {
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate("test", FileType.DELTA, "desc",
                "100", "abc", Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.toString());

        JpaArtifacts jpaArtifacts = (JpaArtifacts) artifact;
        jpaArtifacts.setFileStatus(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.name());
        artifactsRepository.save(jpaArtifacts);

        MockMultipartFile file = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, TEST_CONTENT.getBytes());
        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING,
                                1L, artifact.getSha256Hash())
                        .file(file)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", FileType.DELTA.name())
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value(TEST_FILE_NAME_TXT))
                .andExpect(jsonPath("$.hashes.sha256").value(TEST_SHA256))
                .andExpect(jsonPath("$.description").value(TEST_DESCRIPTION))
                .andExpect(jsonPath(FILE_TYPE).value(FileType.DELTA.name()));

        // Verify that the file removal from CDN and storage was triggered

        verify(snsAsyncClient, times(1)).publish(any(PublishRequest.class));
        assertFalse(output.getOut().contains("Removing artifact file with id " + artifact.getId() + " from CDN"));
        assertTrue(output.getOut().contains("Removing artifact file with id " + artifact.getId() + " from storage"));
        assertTrue(output.getOut().contains("Message published to SNS with messageId"));

    }

    @Test
    @Description("Given an existing artifact that has not been uploaded to storage, "
            + "When the replace artifact API is called with a new file, "
            + "Then the artifact removal is not triggered, and the replacement is successful.")
    void givenArtifactNotActiveWhenReplaceArtifactThenNoRemovalIsTriggered(CapturedOutput output) throws Exception {
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate("test", FileType.DELTA, "desc",
                "100", "abc", Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.toString());

        JpaArtifacts jpaArtifacts = (JpaArtifacts) artifact;
        jpaArtifacts.setFileStatus(FileTransferStatus.UPLOADING_TO_STORAGE.name());
        artifactsRepository.save(jpaArtifacts);

        MockMultipartFile file = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, TEST_CONTENT.getBytes());
        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING,
                                1L, artifact.getSha256Hash())
                        .file(file)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", FileType.DELTA.name())
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value(TEST_FILE_NAME_TXT))
                .andExpect(jsonPath("$.hashes.sha256").value(TEST_SHA256))
                .andExpect(jsonPath("$.description").value(TEST_DESCRIPTION))
                .andExpect(jsonPath(FILE_TYPE).value(FileType.DELTA.name()));

        // Verify that the file removal from CDN and storage was triggered

        verify(snsAsyncClient, times(0)).publish(any(PublishRequest.class));
        assertFalse(output.getOut().contains("Removing artifact file with id " + artifact.getId() + " from CDN"));
        assertFalse(output.getOut().contains("Removing artifact file with id " + artifact.getId() + " from storage"));
        assertFalse(output.getOut().contains("Message published to SNS with messageId"));

    }

    @ParameterizedTest
    @MethodSource("artifactReplacementTestArguments")
    @Description("Given an existing FULL or DELTA artifact associated with a software module, " +
            "When a new multipart file is uploaded via replace API, " +
            "Then the artifact is replaced, the new one is active and has preserved associations.")
    void givenArtifactAssociatedWithSoftwareModuleWhenMultipartFileReplacesArtifactThenArtifactIsReplacedSuccessfully(
            FileType fileType,
            String softwareModuleName,
            String sourceVersionName,
            String targetVersionName
    ) throws Exception {

        resetTableSequence();

        // ---------- Arrange ----------

        // 1. Create a mock multipart file
        MockMultipartFile file = new MockMultipartFile(
                "file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, TEST_CONTENT.getBytes()
        );

        // 2. Create the original artifact
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate(
                "test", fileType, "desc", "100", "abc",
                Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.toString()
        );
        JpaArtifacts jpaArtifact = (JpaArtifacts) artifact;
        jpaArtifact.setFileStatus(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.name());
        artifactsRepository.save(jpaArtifact);

        JpaArtifacts updatedArtifact = artifactsRepository.findById(artifact.getId()).get();

        // 3. Create software module and versions
        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(softwareModuleName);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), targetVersionName);
        Version sourceVersion = (fileType == FileType.DELTA)
                ? testdataFactory.createVersion(softwareModule.getId(), sourceVersionName)
                : null;

        // 4. Create association
        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder()
                .artifact(updatedArtifact)
                .softwareModule((JpaSoftwareModule) softwareModule)
                .sourceVersion(fileType == FileType.DELTA ? (JpaVersion) sourceVersion : null)
                .targetVersion((JpaVersion) targetVersion)
                .build();

        testdataFactory.createArtifactsSoftwareModuleAssociation(Set.of(association));

        // ---------- Assert: Original artifact association BEFORE replacement ----------
        MvcResult beforeReplacementResult = mvc.perform(
                        get(MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING + "/" + artifact.getId(), 1)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(artifact.getId()))
                .andExpect(jsonPath(FILE_TYPE).value(fileType.name()))
                .andExpect(jsonPath(JSON_PATH_SW_MODULES, hasSize(1)))
                .andExpect(jsonPath(JSON_PATH_SW_MODULE_ID).value(softwareModule.getId()))
                .andExpect(jsonPath(JSON_PATH_TARGET_VERSION).value(targetVersion.getName()))
                .andReturn();

        String beforeJson = beforeReplacementResult.getResponse().getContentAsString();
        Configuration config = Configuration.builder()
                .options(Option.SUPPRESS_EXCEPTIONS)
                .build();

        DocumentContext jsonDoc = JsonPath.using(config).parse(beforeJson);

        if (fileType == FileType.FULL) {
            assertThat(jsonDoc.read(JSON_PATH_SOURCE_VERSION_FULL, List.class)).isEmpty();

            String deltaSourceVersion = jsonDoc.read(JSON_PATH_SOURCE_VERSION_DELTA);
            assertThat(deltaSourceVersion).isNull();
        } else {
            assertThat(jsonDoc.read(JSON_PATH_SOURCE_VERSION_DELTA, String.class)).isEqualTo(sourceVersion.getName());

            List<String> fullSourceVersions = jsonDoc.read(JSON_PATH_SOURCE_VERSION_FULL, List.class);
            assertThat(fullSourceVersions).isNull();
        }

        // ---------- Act: Replace artifact ----------
        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING,
                                1L, artifact.getSha256Hash())
                        .file(file)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", fileType.name())
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value(TEST_FILE_NAME_TXT))
                .andExpect(jsonPath("$.hashes.sha256").value(TEST_SHA256))
                .andExpect(jsonPath("$.description").value(TEST_DESCRIPTION))
                .andExpect(jsonPath(FILE_TYPE).value(fileType.name()));

        // ---------- Assert: Old artifact should be replaced and disassociated ----------
        mvc.perform(get(MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING + "/" + artifact.getId(), 1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(artifact.getId()))
                .andExpect(jsonPath(JSON_PATH_SW_MODULES, hasSize(0)));
        //.andReturn();

        Artifacts replacedArtifact = artifactsRepository.getArtifactsById(artifact.getId())
                .orElseThrow(() -> new AssertionError("Old artifact not found"));
        assertEquals(ArtifactsStatus.REPLACED.name(), replacedArtifact.getArtifactStatus());

        // ---------- Assert: New artifact has associations preserved ----------
        long newArtifactId = artifact.getId() + 1;
        MvcResult newArtifactResult = mvc.perform(
                        get(MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING + "/" + newArtifactId, 1)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(newArtifactId))
                .andExpect(jsonPath(FILE_TYPE).value(fileType.name()))
                .andExpect(jsonPath(JSON_PATH_SW_MODULES, hasSize(1)))
                .andExpect(jsonPath(JSON_PATH_SW_MODULE_ID).value(softwareModule.getId()))
                .andExpect(jsonPath(JSON_PATH_TARGET_VERSION).value(targetVersion.getName()))
                .andReturn();

        String newJson = newArtifactResult.getResponse().getContentAsString();
        DocumentContext newJsonDoc = JsonPath.using(config).parse(newJson);

        if (fileType == FileType.FULL) {
            assertThat(newJsonDoc.read(JSON_PATH_SOURCE_VERSION_FULL, List.class)).isEmpty();

            String deltaSourceVersion = newJsonDoc.read(JSON_PATH_SOURCE_VERSION_DELTA);
            assertThat(deltaSourceVersion).isNull();
        } else {
            assertThat(newJsonDoc.read(JSON_PATH_SOURCE_VERSION_DELTA, String.class)).isEqualTo(sourceVersion.getName());

            List<String> fullSourceVersions = newJsonDoc.read(JSON_PATH_SOURCE_VERSION_FULL, List.class);
            assertThat(fullSourceVersions).isNull();
        }

        Artifacts newArtifact = artifactsRepository.getArtifactsById(newArtifactId)
                .orElseThrow(() -> new AssertionError("New artifact not found"));

        assertEquals(TEST_FILE_NAME_TXT, newArtifact.getFileName());
        assertEquals(TEST_SHA256, newArtifact.getSha256Hash());
        assertEquals(TEST_DESCRIPTION, newArtifact.getDescription());
        assertEquals(fileType, newArtifact.getFileType());
        assertEquals(ArtifactsStatus.ACTIVE.name(), newArtifact.getArtifactStatus());
    }

    @ParameterizedTest
    @MethodSource("artifactReplacementTestArguments")
    @Description("Given an existing artifact associated with a software module and a valid file URL is provided for replacement, " +
            "When the replace artifact API is called, " +
            "Then the artifact is replaced successfully, and associations are preserved for FULL and DELTA types.")
    void givenFileURLWhenReplaceArtifactWithAssociationThenArtifactIsReplacedSuccessfully(
            FileType fileType,
            String softwareModuleName,
            String sourceVersionName,
            String targetVersionName) throws Exception {
        // ---------- Arrange ----------

        // 1. Create the original artifact
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate(
                "test", fileType, "desc", "100", "abc",
                Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.toString()
        );
        JpaArtifacts jpaArtifact = (JpaArtifacts) artifact;
        jpaArtifact.setFileStatus(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.name());
        artifactsRepository.save(jpaArtifact);

        JpaArtifacts updatedArtifact = artifactsRepository.findById(artifact.getId()).get();

        // 2. Create software module and versions
        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(softwareModuleName);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), targetVersionName);
        Version sourceVersion = (fileType == FileType.DELTA)
                ? testdataFactory.createVersion(softwareModule.getId(), sourceVersionName)
                : null;

        // 3. Create association
        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder()
                .artifact(updatedArtifact)
                .softwareModule((JpaSoftwareModule) softwareModule)
                .sourceVersion(fileType == FileType.DELTA ? (JpaVersion) sourceVersion : null)
                .targetVersion((JpaVersion) targetVersion)
                .build();

        testdataFactory.createArtifactsSoftwareModuleAssociation(Set.of(association));

        // ---------- Assert: Original artifact association BEFORE replacement ----------
        MvcResult beforeReplacementResult = mvc.perform(
                        get(MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING + "/" + artifact.getId(), 1)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(artifact.getId()))
                .andExpect(jsonPath(FILE_TYPE).value(fileType.name()))
                .andExpect(jsonPath(JSON_PATH_SW_MODULES, hasSize(1)))
                .andExpect(jsonPath(JSON_PATH_SW_MODULE_ID).value(softwareModule.getId()))
                .andExpect(jsonPath(JSON_PATH_TARGET_VERSION).value(targetVersion.getName()))
                .andReturn();

        String beforeJson = beforeReplacementResult.getResponse().getContentAsString();
        Configuration config = Configuration.builder()
                .options(Option.SUPPRESS_EXCEPTIONS)
                .build();

        DocumentContext jsonDoc = JsonPath.using(config).parse(beforeJson);
        System.out.println("first json doc");

        if (fileType == FileType.FULL) {
            assertThat(jsonDoc.read(JSON_PATH_SOURCE_VERSION_FULL, List.class)).isEmpty();

            String deltaSourceVersion = jsonDoc.read(JSON_PATH_SOURCE_VERSION_DELTA);
            assertThat(deltaSourceVersion).isNull();
        } else {
            assertThat(jsonDoc.read(JSON_PATH_SOURCE_VERSION_DELTA, String.class)).isEqualTo(sourceVersion.getName());

            List<String> fullSourceVersions = jsonDoc.read(JSON_PATH_SOURCE_VERSION_FULL, List.class);
            assertThat(fullSourceVersions).isNull();
        }

        // ---------- Act: Replace artifact ----------
        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING,
                                1L, artifact.getSha256Hash())
                        .param("fileURL", FILE_URL)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", fileType.name())
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value(TEST_FILE_NAME_TXT))
                .andExpect(jsonPath("$.hashes.sha256").value(TEST_SHA256))
                .andExpect(jsonPath("$.description").value(TEST_DESCRIPTION))
                .andExpect(jsonPath(FILE_TYPE).value(fileType.name()));

        // ---------- Assert: Old artifact should be replaced and disassociated ----------
        mvc.perform(get(MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING + "/" + artifact.getId(), 1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(artifact.getId()))
                .andExpect(jsonPath(JSON_PATH_SW_MODULES, hasSize(0)));
        //.andReturn();

        Artifacts replacedArtifact = artifactsRepository.getArtifactsById(artifact.getId())
                .orElseThrow(() -> new AssertionError("Old artifact not found"));
        assertEquals(ArtifactsStatus.REPLACED.name(), replacedArtifact.getArtifactStatus());

        // ---------- Assert: New artifact has associations preserved ----------
        long newArtifactId = artifact.getId() + 1;
        MvcResult newArtifactResult = mvc.perform(
                        get(MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING + "/" + newArtifactId, 1)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(newArtifactId))
                .andExpect(jsonPath(FILE_TYPE).value(fileType.name()))
                .andExpect(jsonPath(JSON_PATH_SW_MODULES, hasSize(1)))
                .andExpect(jsonPath(JSON_PATH_SW_MODULE_ID).value(softwareModule.getId()))
                .andExpect(jsonPath(JSON_PATH_TARGET_VERSION).value(targetVersion.getName()))
                .andReturn();

        String newJson = newArtifactResult.getResponse().getContentAsString();
        DocumentContext newJsonDoc = JsonPath.using(config).parse(newJson);
        System.out.println("second json doc");

        if (fileType == FileType.FULL) {
            assertThat(newJsonDoc.read(JSON_PATH_SOURCE_VERSION_FULL, List.class)).isEmpty();

            String deltaSourceVersion = newJsonDoc.read(JSON_PATH_SOURCE_VERSION_DELTA);
            assertThat(deltaSourceVersion).isNull();
        } else {
            assertThat(newJsonDoc.read(JSON_PATH_SOURCE_VERSION_DELTA, String.class)).isEqualTo(sourceVersion.getName());

            List<String> fullSourceVersions = newJsonDoc.read(JSON_PATH_SOURCE_VERSION_FULL, List.class);
            assertThat(fullSourceVersions).isNull();
        }

        Artifacts newArtifact = artifactsRepository.getArtifactsById(newArtifactId)
                .orElseThrow(() -> new AssertionError("New artifact not found"));

        assertEquals(TEST_FILE_NAME_TXT, newArtifact.getFileName());
        assertEquals(TEST_SHA256, newArtifact.getSha256Hash());
        assertEquals(TEST_DESCRIPTION, newArtifact.getDescription());
        assertEquals(fileType, newArtifact.getFileType());
        assertEquals(ArtifactsStatus.ACTIVE.name(), newArtifact.getArtifactStatus());
    }

    @ParameterizedTest
    @MethodSource("artifactReplacementTestArguments")
    @Description("Given an existing artifact associated with a software module and an invalid file URL is provided for replacement, " +
            "When the replace artifact API is called, " +
            "Then a 400 Bad Request is returned indicating the file URL is invalid.")
    void givenInvalidFileURLWhenReplaceArtifactWithAssociationThenReturnBadRequest(
            FileType fileType,
            String softwareModuleName,
            String sourceVersionName,
            String targetVersionName) throws Exception {

        // Arrange artifact and association
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate("test", fileType, "desc",
                "100", "abc", Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.toString());

        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(softwareModuleName);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), targetVersionName);
        Version sourceVersion = sourceVersionName != null ? testdataFactory.createVersion(softwareModule.getId(), sourceVersionName) : null;

        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder()
                .artifact((JpaArtifacts) artifact)
                .softwareModule((JpaSoftwareModule) softwareModule)
                .sourceVersion((JpaVersion) sourceVersion)
                .targetVersion((JpaVersion) targetVersion)
                .build();

        testdataFactory.createArtifactsSoftwareModuleAssociation(Set.of(association));

        // Act & Assert
        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING,
                                1L, artifact.getSha256Hash())
                        .param("fileURL", "invalid-url")
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", fileType.name())
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(JAKARTA_VALIDATION_VALIDATION_EXCEPTION)))
                .andExpect(jsonPath(JSON_PATH_DEBUG, notNullValue()))
                .andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("'fileURL' should be a valid URL to download the artifact")));
    }

    @ParameterizedTest
    @MethodSource("artifactReplacementTestArguments")
    @Description("Given an existing artifact associated with a software module and an invalid file type is provided for replacement, " +
            "When the replace artifact API is called, " +
            "Then a 400 Bad Request is returned indicating the file type is invalid.")
    void givenInvalidFileTypeWhenReplaceArtifactWithAssociationThenReturnBadRequest(
            FileType fileType,
            String softwareModuleName,
            String sourceVersionName,
            String targetVersionName) throws Exception {

        // Arrange artifact and association
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate("test", fileType, "desc",
                "100", "abc", Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.toString());

        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(softwareModuleName);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), targetVersionName);
        Version sourceVersion = sourceVersionName != null ? testdataFactory.createVersion(softwareModule.getId(), sourceVersionName) : null;

        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder()
                .artifact((JpaArtifacts) artifact)
                .softwareModule((JpaSoftwareModule) softwareModule)
                .sourceVersion((JpaVersion) sourceVersion)
                .targetVersion((JpaVersion) targetVersion)
                .build();

        testdataFactory.createArtifactsSoftwareModuleAssociation(Set.of(association));

        // Act & Assert invalid fileType param
        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING,
                                1L, artifact.getSha256Hash())
                        .param("fileURL", FILE_URL)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", "INVALID")
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(JAKARTA_VALIDATION_VALIDATION_EXCEPTION)))
                .andExpect(jsonPath(JSON_PATH_DEBUG, notNullValue()))
                .andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("'fileType' should be either 'DELTA' or 'FULL'")));

        // Act & Assert missing fileType param
        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING,
                                1L, artifact.getSha256Hash())
                        .param("fileURL", FILE_URL)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo("org.springframework.web.bind.MissingServletRequestParameterException")))
                .andExpect(jsonPath(JSON_PATH_DEBUG, notNullValue()))
                .andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("Required request parameter 'fileType' for method parameter type String is not present")));
    }

    @ParameterizedTest
    @MethodSource("artifactReplacementTestArguments")
    @Description("Given an existing artifact with mismatched file type and software module association, " +
            "When the replace artifact API is called, " +
            "Then a 400 Bad Request is returned indicating file type mismatch.")
    void givenExistingArtifactWithMismatchedFileTypeWhenReplaceArtifactWithAssociationThenReturnBadRequest(
            FileType fileType,
            String softwareModuleName,
            String sourceVersionName,
            String targetVersionName) throws Exception {

        // Arrange artifact with fileType
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate("test", fileType, "desc",
                "100", "abc", Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.toString());

        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(softwareModuleName);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), targetVersionName);
        Version sourceVersion = sourceVersionName != null ? testdataFactory.createVersion(softwareModule.getId(), sourceVersionName) : null;

        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder()
                .artifact((JpaArtifacts) artifact)
                .softwareModule((JpaSoftwareModule) softwareModule)
                .sourceVersion((JpaVersion) sourceVersion)
                .targetVersion((JpaVersion) targetVersion)
                .build();

        testdataFactory.createArtifactsSoftwareModuleAssociation(Set.of(association));

        // Intentionally send wrong fileType for replacement (flip DELTA <-> FULL)
        FileType wrongFileType = fileType == FileType.DELTA ? FileType.FULL : FileType.DELTA;

        // Act & Assert
        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING,
                                1L, artifact.getSha256Hash())
                        .param("fileURL", FILE_URL)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", wrongFileType.name())
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(JAKARTA_VALIDATION_VALIDATION_EXCEPTION)))
                .andExpect(jsonPath(JSON_PATH_DEBUG, notNullValue()))
                .andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("'fileType' should be the same as the fileType of the artifact being replaced")));
    }

    @ParameterizedTest
    @MethodSource("artifactReplacementTestArguments")
    @Description("Given an existing artifact and an invalid old SHA256 hash is provided when replacing artifact with software module association, " +
            "When the replace artifact API is called, " +
            "Then a 400 Bad Request is returned indicating the old SHA256 is invalid.")
    void givenInvalidOldSha256WhenReplaceArtifactWithAssociationThenReturnBadRequest(
            FileType fileType,
            String softwareModuleName,
            String sourceVersionName,
            String targetVersionName) throws Exception {

        // Arrange artifact and association
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate("test", fileType, "desc",
                "100", "abc", Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(),FileTransferStatus.UPLOADING_TO_STORAGE.toString());

        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(softwareModuleName);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), targetVersionName);
        Version sourceVersion = sourceVersionName != null ? testdataFactory.createVersion(softwareModule.getId(), sourceVersionName) : null;

        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder()
                .artifact((JpaArtifacts) artifact)
                .softwareModule((JpaSoftwareModule) softwareModule)
                .sourceVersion((JpaVersion) sourceVersion)
                .targetVersion((JpaVersion) targetVersion)
                .build();

        testdataFactory.createArtifactsSoftwareModuleAssociation(Set.of(association));

        // Act & Assert wrong old SHA256 param
        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING,
                                1L, "invalidOldSha256Hash")
                        .param("fileURL", FILE_URL)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", fileType.name())
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo("ENTITY_NOT_EXISTS")))
                .andExpect(jsonPath(JSON_PATH_DEBUG, notNullValue()))
                .andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("No artifact found with sha256")));
    }

    @ParameterizedTest
    @MethodSource("artifactReplacementTestArguments")
    @Description("Given an existing artifact and a duplicate new sha256 is provided when replacing artifact with software module association, " +
            "When the replace artifact API is called, " +
            "Then a 409 Conflict is returned indicating the sha256 already exists.")
    void givenDuplicateNewSha256WhenReplaceArtifactWithAssociationThenReturnConflict(
            FileType fileType,
            String softwareModuleName,
            String sourceVersionName,
            String targetVersionName) throws Exception {

        // Arrange: Create duplicate SHA256 artifact and association
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate("test", fileType, "desc",
                "100", TEST_SHA256, Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.toString());

        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(softwareModuleName);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), targetVersionName);
        Version sourceVersion = sourceVersionName != null ? testdataFactory.createVersion(softwareModule.getId(), sourceVersionName) : null;

        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder()
                .artifact((JpaArtifacts) artifact)
                .softwareModule((JpaSoftwareModule) softwareModule)
                .sourceVersion((JpaVersion) sourceVersion)
                .targetVersion((JpaVersion) targetVersion)
                .build();

        testdataFactory.createArtifactsSoftwareModuleAssociation(Set.of(association));

        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING, 1L, artifact.getSha256Hash())
                        .param("fileURL", FILE_URL)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", fileType.name())
                        .param("sha256", TEST_SHA256) // Duplicate SHA
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo("ENTITY_ALREADY_EXISTS")))
                .andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("File already exists in COSMOS with SHA-256: " + TEST_SHA256)))
                .andExpect(jsonPath(JSON_PATH_DEBUG, notNullValue()));
    }

    @ParameterizedTest
    @MethodSource("artifactReplacementTestArguments")
    @Description("Given an existing artifact and a past signature expiry date is provided when replacing artifact with software module association, " +
            "When the replace artifact API is called, " +
            "Then a 400 Bad Request is returned indicating the signature expiry date is past.")
    void givenSignatureExpiryDateIsPastWhenReplaceArtifactWithAssociationThenReturnBadRequest(
            FileType fileType,
            String softwareModuleName,
            String sourceVersionName,
            String targetVersionName) throws Exception {

        String pastDate = String.valueOf(Instant.now().minus(1, ChronoUnit.DAYS).getEpochSecond());

        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate("test", fileType, "desc",
                "100", "abc", Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.toString());

        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(softwareModuleName);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), targetVersionName);
        Version sourceVersion = sourceVersionName != null ? testdataFactory.createVersion(softwareModule.getId(), sourceVersionName) : null;

        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder()
                .artifact((JpaArtifacts) artifact)
                .softwareModule((JpaSoftwareModule) softwareModule)
                .sourceVersion((JpaVersion) sourceVersion)
                .targetVersion((JpaVersion) targetVersion)
                .build();

        testdataFactory.createArtifactsSoftwareModuleAssociation(Set.of(association));

        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING, 1L, artifact.getSha256Hash())
                        .param("fileURL", FILE_URL)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", fileType.name())
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", pastDate)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(JAKARTA_VALIDATION_VALIDATION_EXCEPTION)))
                .andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("'signatureExpiryDate' should not be past date or current date")))
                .andExpect(jsonPath(JSON_PATH_DEBUG, notNullValue()));
    }

    @ParameterizedTest(name = "{4}")
    @MethodSource("artifactAssociationUploadStatusArguments")
    @Description("Given an existing artifact associated to a software module with a specific upload status, " +
            "When the replace artifact API is called with a new file, " +
            "Then the correct removal actions are taken (CDN, S3), and replacement is successful.")
    void givenArtifactAssociatedWithSoftwareModuleWhenReplaceArtifactApiCalledThenCorrectRemovalAndReplacementOccurs(
            FileTransferStatus initialStatus,
            int expectedSnsPublishCount,
            boolean expectCdnRemoval,
            boolean expectStorageRemoval,
            String testName,
            CapturedOutput output) throws Exception {

        resetTableSequence();

        MockMultipartFile file = new MockMultipartFile("file", TEST_FILE_NAME_TXT,
                MediaType.TEXT_PLAIN_VALUE, TEST_CONTENT.getBytes());

        // create artifact
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate("test", FileType.FULL, "desc",
                "100", "abc", Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.toString());

        JpaArtifacts updatedArtifact = artifactsRepository.findById(artifact.getId()).get();

        // create software module
        SoftwareModule softwareModuleFull = testdataFactory.createSoftwareModule("testSMForFullAssociation");

        // create version
        Version targetVersion = testdataFactory.createVersion(softwareModuleFull.getId(), "testTargetVersionForFullAssociation1");

        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder()
                .artifact(updatedArtifact)
                .softwareModule((JpaSoftwareModule) softwareModuleFull)
                .sourceVersion((JpaVersion) targetVersion)
                .targetVersion((JpaVersion) targetVersion)
                .build();

        Set<ArtifactSoftwareModuleAssociation> iAssociationList = new HashSet<>();
        iAssociationList.add(association);
        testdataFactory.createArtifactsSoftwareModuleAssociation(iAssociationList);

        // detach and re-fetch to avoid optimistic locking
        entityManager.detach(updatedArtifact);
        updatedArtifact = artifactsRepository.findById(artifact.getId()).get();
        updatedArtifact.setFileStatus(initialStatus.name());
        artifactsRepository.save(updatedArtifact);

        mvc.perform(MockMvcRequestBuilders
                        .multipart(HttpMethod.PUT, MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING,
                                1L, artifact.getSha256Hash())
                        .file(file)
                        .param("filename", TEST_FILE_NAME_TXT)
                        .param("fileType", FileType.FULL.name())
                        .param("sha256", TEST_SHA256)
                        .param("description", TEST_DESCRIPTION)
                        .param("signatureExpiryDate", TEST_SIGNATURE_EXPIRY_DATE)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value(TEST_FILE_NAME_TXT))
                .andExpect(jsonPath("$.hashes.sha256").value(TEST_SHA256))
                .andExpect(jsonPath("$.description").value(TEST_DESCRIPTION))
                .andExpect(jsonPath(FILE_TYPE).value(FileType.FULL.name()));

        verify(snsAsyncClient, times(expectedSnsPublishCount)).publish(any(PublishRequest.class));

        if (expectCdnRemoval) {
            assertTrue(output.getOut().contains("Removing artifact file with id " + artifact.getId() + " from CDN"));
        } else {
            assertFalse(output.getOut().contains("Removing artifact file with id " + artifact.getId() + " from CDN"));
        }

        if (expectStorageRemoval) {
            assertTrue(output.getOut().contains("Removing artifact file with id " + artifact.getId() + " from storage"));
        } else {
            assertFalse(output.getOut().contains("Removing artifact file with id " + artifact.getId() + " from storage"));
        }

        assertTrue(output.getOut().contains("Message published to SNS with messageId"));
    }

    @Test
    @Description("Given multiple artifacts exist for a tenant, "
            + "When fetching artifacts with various pagination, sorting, and filtering parameters, "
            + "Then the API returns the correct content, total count, and handles not found and edge cases appropriately.")
    void givenMultipleArtifactsWhenFetchingArtifactsWithVariousParametersThenReturnsExpectedResults() throws Exception {
        // Given: Two artifacts exist for tenant 1
        Artifacts artifactDelta = testdataFactory.createArtifactsWithExpiryDate("asc", FileType.DELTA, "desc", "100", "abc", Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.name());
        Artifacts artifactFull = testdataFactory.createArtifactsWithExpiryDate("desc", FileType.FULL, "desc", "100", "abc", Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), FileTransferStatus.UPLOADING_TO_STORAGE.name());

        // When & Then: Fetch all artifacts for tenant 1
        mvc.perform(get(MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.total").value(2));

        // When & Then: Fetch first page with limit 1
        mvc.perform(get(MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING + "?offset=0&limit=1", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.total").value(2));

        // When & Then: Fetch page with offset beyond available artifacts
        mvc.perform(get(MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING + "?offset=3&limit=1", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.total").value(2));

        // When & Then: Sort by name ascending
        mvc.perform(get(MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING + "?sort=name:ASC", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].filename", equalTo("asc")));

        // When & Then: Sort by name descending
        mvc.perform(get(MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING + "?sort=name:DESC", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].filename", equalTo("desc")));

        // When & Then: Filter by name 'desc'
        mvc.perform(get(MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING + "?q=name==desc", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].filename", equalTo("desc")));

        // When & Then: Filter by name 'asc'
        mvc.perform(get(MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING + "?q=name==asc", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].filename", equalTo("asc")));

        // When & Then: Filter by name 'asc' for non-existent tenant
        mvc.perform(get(MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING + "?q=name==asc", 1000L))
                .andExpect(status().isNotFound());

        // Additional cases

        // When & Then: Empty artifact list for a new tenant
        mvc.perform(get(MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING, 2L))
                .andExpect(status().isNotFound());

        // When & Then: Invalid offset/limit parameters (negative values)
        mvc.perform(get(MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING + "?offset=-1&limit=-5", 1L))
                .andExpect(status().isBadRequest());

        // When & Then: Unsupported sort field
        mvc.perform(get(MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING + "?sort=unknownField:ASC", 1L))
                .andExpect(status().isBadRequest());

        // When & Then: Unsupported filter field
        mvc.perform(get(MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING + "?q=unknownField==value", 1L))
                .andExpect(status().isBadRequest());
    }

}
