package org.eclipse.hawkbit.mgmt.rest.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Description;
import org.cosmos.models.mgmt.FileType;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.artifacts.constants.ArtifactsStatus;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.eclipse.hawkbit.artifact.repository.ArtifactFilesystemProperties;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
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
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
public class MgmtArtifactsMultipartUploadIntegrationTest extends AbstractManagementApiIntegrationTest {

    private static final long TENANT_ID = 1L;
    private static final String ARTIFACTS_ENDPOINT = MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING;
    private static final String TEST_ARTIFACT_SHA256 = "9d9595c5d94fb65b824f56e9999527dba9542481580d69feb89056aabaa0aa87";
    private static final String TEST_FILE_NAME = "test_fileName";
    private static final String DESCRIPTION = "description";
    private static final String FILE_NAME = "filename";
    private static final String FILE_TYPE = "fileType";
    private static final String SIGNATURE_EXPIRY_DATE = "signatureExpiryDate";
    private static final String JSON_PATH_FILE_NAME = "$.filename";
    private static final String JSON_PATH_FILE_TYPE = "$.fileType";
    private static final String JSON_PATH_DESCRIPTION = "$.description";
    private static final String JSON_PATH_EXPIRY_DATE = "$.signatureExpiryDate";
    private static final String JSON_PATH_CREATED_BY = "$.createdBy";
    private static final String JSON_PATH_CREATED_AT = "$.createdAt";
    private static final String JSON_PATH_LAST_MODIFIED_BY = "$.lastModifiedBy";
    private static final String JSON_PATH_LAST_MODIFIED_AT = "$.lastModifiedAt";
    private static final String JSON_PATH_SHA256 = "$.hashes.sha256";
    private static final String JSON_PATH_MD5 = "$.hashes.md5";
    private static final String JSON_PATH_SIZE = "$.size";
    private static final String JSON_PATH_SW_MODULES = "$.softwareModules";
    private static final String TEST_FILE_NAME_TXT = "test_file.txt";
    private static final String TEST_CONTENT = "Test content";
    private static final String TEST_CONTENT_1 = "Test content 1";
    private static final String TEST_SHA256 = "9d9595c5d94fb65b824f56e9999527dba9542481580d69feb89056aabaa0aa87";
    @Autowired
    private ArtifactFilesystemProperties artifactFilesystemProperties;
    @MockBean
    private S3Client s3Client;

    @BeforeEach
    void setUp() throws IOException {
        setupS3ClientMock();
        clearDatabase();
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

    private void clearDatabase() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_artifact_software_module");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_artifacts");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_base_software_module");
    }

    @Test
    @Description("Creates an artifact with a valid file upload request to ensure success response")
    void givenRequestValidWhenArtifactsUploadThenUploadSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, TEST_CONTENT.getBytes());
        final String TEST_SIGNATURE_EXPIRY_DATE = String.valueOf(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());

        mvc.perform(createArtifactRequest(file, TEST_FILE_NAME, FileType.DELTA.name(), DESCRIPTION, TEST_SHA256, TEST_SIGNATURE_EXPIRY_DATE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath(JSON_PATH_FILE_NAME).value(TEST_FILE_NAME))
                .andExpect(jsonPath(JSON_PATH_FILE_TYPE).value(FileType.DELTA.name()))
                .andExpect(jsonPath(JSON_PATH_DESCRIPTION).value(DESCRIPTION))
                .andExpect(jsonPath(JSON_PATH_EXPIRY_DATE).value(TEST_SIGNATURE_EXPIRY_DATE))
                .andExpect(jsonPath(JSON_PATH_CREATED_BY).exists())
                .andExpect(jsonPath(JSON_PATH_CREATED_AT).exists())
                .andExpect(jsonPath(JSON_PATH_LAST_MODIFIED_BY).exists())
                .andExpect(jsonPath(JSON_PATH_LAST_MODIFIED_AT).exists())
                .andExpect(jsonPath(JSON_PATH_SHA256).value(TEST_SHA256))
                .andExpect(jsonPath(JSON_PATH_MD5).exists())
                .andExpect(jsonPath(JSON_PATH_SIZE).value(file.getSize()))
                .andExpect(jsonPath(JSON_PATH_SW_MODULES).doesNotExist());
    }

    @Test
    @Description("Ensures that an Artifact Upload request with a future date return success status")
    void givenValidExpiryDateUploadRequestWhenArtifactsUploadThenUploadSuccess() throws Exception {

        MockMultipartFile file = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, TEST_CONTENT.getBytes());
        final String TEST_SIGNATURE_EXPIRY_DATE = String.valueOf(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());
        mvc.perform(createArtifactRequestWithExpiry(file, TEST_FILE_NAME, FileType.DELTA.name(), DESCRIPTION, TEST_ARTIFACT_SHA256, TEST_SIGNATURE_EXPIRY_DATE))
                .andExpect(status().isCreated());
    }

    @Test
    @Description("Artifact already exists when sha256 is provided in uppercase then return conflict")
    void givenRequestWithExistingArtifactWhenUploadThenUploadFails() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, TEST_CONTENT.getBytes());
        final String TEST_SIGNATURE_EXPIRY_DATE = String.valueOf(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());

        mvc.perform(createArtifactRequest(file, TEST_FILE_NAME, FileType.DELTA.name(), DESCRIPTION, TEST_SHA256, TEST_SIGNATURE_EXPIRY_DATE))
                .andExpect(status().isCreated());

        mvc.perform(createArtifactRequest(file, TEST_FILE_NAME, FileType.DELTA.name(), DESCRIPTION, TEST_SHA256.toUpperCase(), TEST_SIGNATURE_EXPIRY_DATE))
                .andExpect(status().isConflict());
    }

    @Test
    @Description("Artifact with uppercase sha256 is provided then success")
    void givenRequestWithUpperCaseSha256ArtifactWhenUploadThenUploadSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, TEST_CONTENT.getBytes());
        final String TEST_SIGNATURE_EXPIRY_DATE = String.valueOf(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());

        mvc.perform(createArtifactRequest(file, TEST_FILE_NAME, FileType.DELTA.name(), DESCRIPTION, TEST_SHA256.toUpperCase(), TEST_SIGNATURE_EXPIRY_DATE))
                .andExpect(status().isCreated());
    }

    @Test
    @Description("Given a valid request, when uploading an artifact, then the upload is successful")
    void givenValidRequestWhenUploadingArtifactThenUploadSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE,
                TEST_CONTENT.getBytes());
        final String TEST_SIGNATURE_EXPIRY_DATE = String.valueOf(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());

        mvc.perform(createArtifactRequest(file, TEST_FILE_NAME, FileType.DELTA.name(), DESCRIPTION, TEST_SHA256,
                        TEST_SIGNATURE_EXPIRY_DATE))
                .andExpect(status().isCreated());
    }

    @Test
    @Description("Given an invalid SHA256, when uploading an artifact, then the upload fails with a bad request status")
    void givenInvalidSHA256WhenUploadingArtifactThenUploadFails(CapturedOutput output) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE,
                TEST_CONTENT.getBytes());
        final String TEST_SIGNATURE_EXPIRY_DATE = String.valueOf(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());

        mvc.perform(createArtifactRequest(file, TEST_FILE_NAME, FileType.DELTA.name(), DESCRIPTION,
                        "9d9595c5d94fb65b824f56e9999527dba9542481580d69feb89056aabaa0aa88", TEST_SIGNATURE_EXPIRY_DATE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Integrity check failed, SHA-256 mismatch."));

        assertTrue(output.getOut().contains("Successfully deleted file from S3"));
    }

    @Test
    @Description("Attempts to create an artifact with an existing filename and but different SHA256 should success ")
    void givenRequestWithSameFilenameAndDifferentSHA256ThenReturnSuccessForMultipartFile() throws Exception {

        // Create request with first SHA256
        MockMultipartFile file = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, TEST_CONTENT.getBytes());

        MockHttpServletRequestBuilder artifactRequest = createArtifactRequest(
                file, TEST_FILE_NAME_TXT, FileType.DELTA.name(), DESCRIPTION, TEST_SHA256, "1896514417");

        // Perform first request - expect 201 Created
        mvc.perform(artifactRequest).andExpect(status().isCreated());

        MockMultipartFile file1 = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, TEST_CONTENT_1.getBytes());

        // Modify SHA256 to a different value and perform again
        MockHttpServletRequestBuilder duplicateFileNameArtifactRequest = createArtifactRequest(
                file1, TEST_FILE_NAME_TXT, FileType.DELTA.name(), DESCRIPTION, "166cb94a04ebaef4ae79c2a0674d8cea1b7fc354eb2ea436b28c3531de10449c", "1896514417");

        CreateMultipartUploadResponse createMultipartUploadResponse = CreateMultipartUploadResponse.builder()
                .uploadId("dummyUploadId")
                .build();
        doReturn(createMultipartUploadResponse).when(s3Client).createMultipartUpload(any(CreateMultipartUploadRequest.class));

        UploadPartResponse dummyResponse = UploadPartResponse.builder()
                .eTag("dummyETag")
                .checksumSHA256("4UdKuqV4BKX2OgDLXDA/eVcFZJcjFfh3a4hTwaF3/SM=-1")
                .build();
        doReturn(dummyResponse).when(s3Client).uploadPart(any(UploadPartRequest.class), any(RequestBody.class));

        CompleteMultipartUploadResponse completeMultipartUploadResponse = CompleteMultipartUploadResponse.builder()
                .location("dummyLocation")
                .bucket("dummyBucket")
                .key("dummyKey")
                .eTag("dummyETag")
                .checksumSHA256("4UdKuqV4BKX2OgDLXDA/eVcFZJcjFfh3a4hTwaF3/SM=-1")
                .build();
        doReturn(completeMultipartUploadResponse).when(s3Client).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));


        // Perform second request - expect 201 Created
        mvc.perform(duplicateFileNameArtifactRequest).andExpect(status().isCreated());

    }

    @Test
    @Description("Ensures that an Artifact is create with status as ACTIVE for MultipartUpload")
    void givenValidArtifactCreateRequestWhenArtifactsCreateThenUploadSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, TEST_CONTENT.getBytes());

        MockHttpServletRequestBuilder artifactRequest = createArtifactRequest(file, TEST_FILE_NAME, FileType.DELTA.name(), DESCRIPTION, TEST_ARTIFACT_SHA256, String.valueOf(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond()));

        MvcResult mvcResult = mvc.perform(artifactRequest).andExpect(status().isCreated()).andReturn();
        JsonNode response = new ObjectMapper().readTree(mvcResult.getResponse().getContentAsString());
        // Verify artifact status is set to ACTIVE
        long id = response.path("id").asLong();
        Artifacts artifacts = artifactsManagement.getArtifactsById(id).get();
        Assertions.assertEquals(ArtifactsStatus.ACTIVE.name(), artifacts.getArtifactStatus());

    }

    @Test
    @Description("Given an invalid SHA256, when storage deletion failed while uploading the artifact, then return bad request status and verify the failed deletion of file from storage")
    void givenInvalidSHA256WhenDeletionFailedWhileUploadThenReturnBadRequest(CapturedOutput output) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE,
                TEST_CONTENT.getBytes());
        final String TEST_SIGNATURE_EXPIRY_DATE = String.valueOf(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());

        SdkHttpResponse sdkHttpResponse = SdkHttpResponse.builder()
                .statusCode(500)
                .build();

        SdkResponse sdkResponse = DeleteObjectResponse.builder().sdkHttpResponse(sdkHttpResponse).build();
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn((DeleteObjectResponse) sdkResponse);

        mvc.perform(createArtifactRequest(file, TEST_FILE_NAME, FileType.DELTA.name(), DESCRIPTION, "9d9595c5d94fb65b824f56e9999527dba9542481580d69feb89056aabaa0aa88",
                        TEST_SIGNATURE_EXPIRY_DATE))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Error occurred while deleting file from S3"));

        assertTrue(output.getOut().contains("Failed to delete file from S3"));

        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(S3Exception.class);

        mvc.perform(createArtifactRequest(file, TEST_FILE_NAME, FileType.DELTA.name(), DESCRIPTION, "9d9595c5d94fb65b824f56e9999527dba9542481580d69feb89056aabaa0aa88",
                        TEST_SIGNATURE_EXPIRY_DATE))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Error occurred while deleting file from S3"));

        assertTrue(output.getOut().contains("Error occurred while deleting file from S3"));
    }


    private static MockHttpServletRequestBuilder createArtifactRequestWithExpiry(MockMultipartFile file, String filename, String fileType, String description, String sha256, String signatureExpiryDate) {
        return MockMvcRequestBuilders.multipart(ARTIFACTS_ENDPOINT, TENANT_ID)
                .file(file)
                .param(FILE_NAME, filename)
                .param(FILE_TYPE, fileType)
                .param(DESCRIPTION, description)
                .param("sha256", sha256)
                .param(SIGNATURE_EXPIRY_DATE, signatureExpiryDate);
    }

    @Test
    @Description("Ensures that an Artifact is create with file status as STORAGE_UPLOAD_SUCCESSFUL for MultipartUpload")
    void givenValidArtifactWhenArtifactsCreateThenSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, TEST_CONTENT.getBytes());

        MockHttpServletRequestBuilder artifactRequest = createArtifactRequest(file, TEST_FILE_NAME, FileType.DELTA.name(), DESCRIPTION, TEST_ARTIFACT_SHA256, String.valueOf(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond()));

        MvcResult mvcResult = mvc.perform(artifactRequest).andExpect(status().isCreated()).andReturn();
        JsonNode response = new ObjectMapper().readTree(mvcResult.getResponse().getContentAsString());

        long id = response.path("id").asLong();
        Artifacts artifacts = artifactsManagement.getArtifactsById(id).get();
        Assertions.assertEquals(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL , artifacts.getFileStatus());
    }

    @Test
    @Description("Given file name with multiple white spaces, when creating an artifact with multipart, then all the white spaces in filename are trimmed")
    void givenFileNameWithWhiteSpaceWhenCreatingArtifactWithFileThenSpaceTrimmed() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, TEST_CONTENT.getBytes());

        MockHttpServletRequestBuilder artifactRequest = createArtifactRequest(file, " test file     02", FileType.DELTA.name(), DESCRIPTION, TEST_ARTIFACT_SHA256, String.valueOf(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond()));

        MvcResult mvcResult = mvc.perform(artifactRequest).andExpect(status().isCreated()).andReturn();
        JsonNode response = new ObjectMapper().readTree(mvcResult.getResponse().getContentAsString());

        long artifactId = response.path("id").asLong();

        Optional<Artifacts> updatedArtifact = artifactsManagement.getArtifactsById(artifactId);
        Assertions.assertEquals("testfile02", updatedArtifact.get().getFileName());
    }

}
