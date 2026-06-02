/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import net.minidev.json.JSONObject;
import org.cosmos.models.mgmt.FileType;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.artifacts.constants.ArtifactsStatus;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifactsUpdateRequest;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifactsRequest;
import org.cosmos.models.mgmt.artifacts.dto.SoftwareModuleArtifactBindingRequest;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.s3.S3MultipartFileUpload;
import org.cosmos.s3.model.S3FileUpload;
import org.cosmos.sns.models.CdnDeleteRequest;
import org.cosmos.sns.models.CdnUploadRequest;
import org.cosmos.sns.models.S3FileDeletionRequest;
import org.cosmos.sns.models.S3FileTransferRequest;
import org.cosmos.sns.services.impl.CdnDeleteSnsService;
import org.cosmos.sns.services.impl.CdnUploadSnsService;
import org.cosmos.sns.services.impl.S3FileDeleteSnsService;
import org.cosmos.sns.services.impl.S3FileTransferSnsService;
import org.eclipse.hawkbit.api.ArtifactUrlHandlerProperties;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtArtifactsRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.awsServices.MgmtS3Service;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtArtifactSoftwareModuleAssociationMapper;
import org.eclipse.hawkbit.repository.ArtifactsManagement;
import org.eclipse.hawkbit.repository.exception.FileDownloadFailureException;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifactSoftwareModuleAssociationEntity;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaVersion;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.messaging.MessageHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.cosmos.models.mgmt.MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for the {@link MgmtArtifactsRestApi}.
 */
@Feature("Component Tests - Management API")
@Story("Artifacts Resource")
@ExtendWith(OutputCaptureExtension.class)
class MgmtArtifactsResourceTest extends AbstractManagementApiIntegrationTest {

    private static final long TENANT_ID = 1L;
    private static final String ARTIFACTS_ENDPOINT = ARTIFACTS_V1_REQUEST_MAPPING;
    private static final String ARTIFACTS_ENDPOINT_FILEURL = MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING_FILEURL;
    private static final String ARTIFACTS_ENDPOINT_DOWNLOAD = MgmtRestConstants.DOWNLOAD_ARTIFACT_V1_REQUEST_MAPPING;
    private static final String ARTIFACTS_ENDPOINT_DELETE = MgmtRestConstants.ARTIFACTS_RESOURCE_V1_REQUEST_MAPPING;
    private static final String ARTIFACTS_ENDPOINT_PURGE = MgmtRestConstants.PURGE_ARTIFACTS_V1_REQUEST_MAPPING;
    private static final String TEST_ARTIFACT_SHA256 = "9d9595c5d94fb65b824f56e9999527dba9542481580d69feb89056aabaa0aa87";
    private static final String TEST_CDN = "akamai";
    private static final String TEST_CDN_ROOT_DIRECTORY = "root";

    private static final String FILE_URL = "https://docs.aws.amazon.com/it_it/whitepapers/latest/overview-aws-cloud-adoption-framework/overview-aws-cloud-adoption-framework.pdf";
    private static final String TEST_FILE_NAME = "test_fileName";
    private static final String DELTA = "DELTA";
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
    private static final String TEST_SHA256 = "9d9595c5d94fb65b824f56e9999527dba9542481580d69feb89056aabaa0aa87";
    private static final String TEST_EXPIRY_DATE = "1735689600";
    private static final String TEST_FILE_DESCRIPTION = "New file description1";
    private static final String TEST_ARTIFACTS_FILE_NAME = "ResourceUtility.java";
    private static final String TEST_ARTIFACTS_FILE_NAME_1 = "ResourceUtility1.java";
    private static final String TEST_ARTIFACTS_URL = "/{artifactId}/softwaremodules/{softwareModuleId}";
    private static final String TENANT = "{tenant}";
    private static final String SHA256 = "{SHA256}";
    private static final String FAILED = "Failed";
    private static final String MOCK_MESSAGE_ID = "mockMessageId";
    private static final String SHA_256 = "9a0b9d8a717d7e0d6bde36de8481eddd5c48e5fa379d7e1e3201630281d499b6";
    private static final String JSON_PATH_MESSAGE = "$.message";
    private static final String MESSAGE = "Failed to publish message to SNS";
    private static final String TEST_PURGE_SM = "testPurgeSM";
    private static final String TEST_PURGE_SOURCE_VERSION = "testPurgeSourceVersion";
    private static final String TEST_PURGE_TARGET_VERSION = "testPurgeTargetVersion";
    @TempDir
    static Path tempDir;
    @Captor
    ArgumentCaptor<MessageHeaders> headersArgumentCaptor;
    @MockBean
    MgmtS3Service s3Service;
    @Autowired
    private ArtifactUrlHandlerProperties artifactUrlHandlerProperties;
    @MockBean
    private S3MultipartFileUpload s3MultipartFileUpload;
    @Autowired
    private S3Client s3Client;
    @Autowired
    private ArtifactsManagement artifactsManagement;
    @Mock
    private CdnUploadSnsService cdnUploadSnsService;
    @Mock
    private CdnDeleteSnsService cdnDeleteSnsService;
    @Mock
    private S3FileDeleteSnsService s3FileDeleteSnsService;
    @Mock
    private S3FileTransferSnsService s3FileTransferSnsService;
    @MockBean
    private SnsAsyncClient snsAsyncClient;

    private static MgmtArtifactsRequest invalidRequestArtifactsByUrl() {
        MgmtArtifactsRequest request = new MgmtArtifactsRequest();
        request.setFileURL("/latest/overview-aws-cloud-adoption-framework/overview-aws-cloud-adoption-framework.pdf");
        request.setFilename(TEST_FILE_NAME);
        request.setFileType(DELTA);
        request.setDescription(DESCRIPTION);
        request.setSignatureExpiryDate(1735689600L);

        return request;
    }

    private static MgmtArtifactsRequest invalidRequestArtifactsByExpiryDate() {
        MgmtArtifactsRequest request = new MgmtArtifactsRequest();
        request.setFileURL(FILE_URL);
        request.setFilename(TEST_FILE_NAME);
        request.setFileType(DELTA);
        request.setDescription(DESCRIPTION);
        request.setSignatureExpiryDate(315532800L);

        return request;
    }

    private static String invalidRequestArtifactsByFileType() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("fileURL", FILE_URL);
        jsonObject.put(FILE_NAME, TEST_FILE_NAME);
        jsonObject.put(FILE_TYPE, "empty");
        jsonObject.put(DESCRIPTION, DESCRIPTION);
        jsonObject.put(SIGNATURE_EXPIRY_DATE, 1735689600L);

        return jsonObject.toString();
    }

    private static MgmtArtifactsUpdateRequest  updateDescriptionArtifactsRequest() {
        MgmtArtifactsUpdateRequest  request = new MgmtArtifactsUpdateRequest ();
        request.setDescription("Updated description");

        return request;
    }

    private static MgmtArtifactsRequest createArtifactsRequestWithExpiryAndSHA256(Long artifactExpiryDate, String sha256) {
        MgmtArtifactsRequest request = new MgmtArtifactsRequest();
        request.setFileURL(FILE_URL);
        request.setFilename(TEST_FILE_NAME);
        request.setFileType(DELTA);
        request.setDescription(DESCRIPTION);
        request.setSignatureExpiryDate(artifactExpiryDate);
        request.setSha256(sha256);

        return request;
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

    @BeforeEach
    void setUp() {
        // Create a mock PublishResponse
        PublishResponse mockPublishResponse = PublishResponse.builder()
                .messageId(MOCK_MESSAGE_ID)
                .build();
        // Create a completed CompletableFuture with the mock response
        CompletableFuture<PublishResponse> completedFuture = CompletableFuture.completedFuture(mockPublishResponse);
        when(cdnUploadSnsService.publishMessage(any(CdnUploadRequest.class))).thenReturn(completedFuture);

        when(cdnDeleteSnsService.publishMessage(any(CdnDeleteRequest.class))).thenReturn(completedFuture);
        when(s3FileTransferSnsService.publishMessage(any(S3FileTransferRequest.class))).thenReturn(completedFuture);
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(completedFuture);

    }

    @AfterEach
    void tearDown() throws IOException {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_artifact_software_module");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_artifacts");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_base_software_module");
    }

    @Test
    @Description("Creates an artifact with a valid request to ensure success response, including SHA256 validation")
    void givenRequestWithValidSHA256WhenArtifactsCreateThenCreateSuccess() throws Exception {

        // Create request with SHA256 included
        MgmtArtifactsRequest artifactsRequest = createArtifactsRequestWithExpiryAndSHA256(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(),
                SHA_256);
        ObjectMapper objectMapper = new ObjectMapper();
        String requestJson = objectMapper.writeValueAsString(artifactsRequest);
        // Create a mock PublishResponse
        PublishResponse mockPublishResponse = PublishResponse.builder()
                .messageId(MOCK_MESSAGE_ID)
                .build();
        // Create a completed CompletableFuture with the mock response
        CompletableFuture<PublishResponse> completedFuture = CompletableFuture.completedFuture(mockPublishResponse);

        // Mock the behavior of publishMessage to return the completed future
        when(s3FileTransferSnsService.publishMessage(any(S3FileTransferRequest.class))).thenReturn(completedFuture);

        mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(requestJson)
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath(JSON_PATH_FILE_NAME).value(artifactsRequest.getFilename()))
                .andExpect(jsonPath(JSON_PATH_FILE_TYPE).value(artifactsRequest.getFileType()))
                .andExpect(jsonPath(JSON_PATH_DESCRIPTION).value(artifactsRequest.getDescription()))
                .andExpect(jsonPath(JSON_PATH_EXPIRY_DATE).value(artifactsRequest.getSignatureExpiryDate()))
                .andExpect(jsonPath(JSON_PATH_CREATED_BY).exists())
                .andExpect(jsonPath(JSON_PATH_CREATED_AT).exists())
                .andExpect(jsonPath(JSON_PATH_LAST_MODIFIED_BY).exists())
                .andExpect(jsonPath(JSON_PATH_LAST_MODIFIED_AT).exists())
                .andExpect(jsonPath(JSON_PATH_SW_MODULES).doesNotExist());
        // Verify the mock behavior
        verify(snsAsyncClient, atLeastOnce()).publish(any(PublishRequest.class));
    }

    @Test
    @Description("Ensures that a request to an invalid URL returns a bad request status")
    void givenInvalidUrlWhenArtifactsCreateThenUploadFails() throws Exception {

        MgmtArtifactsRequest artifactsRequest = invalidRequestArtifactsByUrl();

        ObjectMapper objectMapper = new ObjectMapper();
        String requestJson = objectMapper.writeValueAsString(artifactsRequest);

        mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(requestJson)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensures that a request with an invalid expiry date returns a bad request status")
    void givenInvalidExpiryDateWhenArtifactsCreateThenUploadFails() throws Exception {

        MgmtArtifactsRequest artifactsRequest = invalidRequestArtifactsByExpiryDate();
        ObjectMapper objectMapper = new ObjectMapper();
        String requestJson = objectMapper.writeValueAsString(artifactsRequest);

        mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(requestJson)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensures that a request with an invalid file type returns a bad request status")
    void givenInvalidFileTypeWhenArtifactsCreateThenUploadFails() throws Exception {

        String artifactsRequest = invalidRequestArtifactsByFileType();
        mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(artifactsRequest)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Attempts to create an artifact with an existing filename to test creates successfully")
    void givenArtifactWithExistingFilenameUploadSucceeds() throws Exception {
        testdataFactory.createArtifacts(TEST_FILE_NAME, FileType.valueOf(DELTA), "New file description", "123", "123");

        MgmtArtifactsRequest artifactsRequest = createArtifactsRequestWithExpiryAndSHA256(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), SHA_256);
        ObjectMapper objectMapper = new ObjectMapper();
        String requestJson = objectMapper.writeValueAsString(artifactsRequest);

        mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID)
                        .content(requestJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    @Description("Verifies that uploading an artifact and that the MD5 field is not present in the response.")
    void givenArtifactWhenUploadThenSucceedsAndMd5IsNotPresent() throws Exception {
        MgmtArtifactsRequest artifactsRequest = createArtifactsRequestWithExpiryAndSHA256(
                Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), SHA_256);
        ObjectMapper objectMapper = new ObjectMapper();
        String requestJson = objectMapper.writeValueAsString(artifactsRequest);

        mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID)
                        .content(requestJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath(JSON_PATH_MD5).doesNotExist());
    }


    @Test
    @Description("Ensures that a request with an empty file returns a bad request status")
    void givenEmptyFileWhenArtifactsUploadThenUploadFails() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, new byte[0]);
        mvc.perform(createArtifactRequest(emptyFile, TEST_FILE_NAME, FileType.DELTA.name(), DESCRIPTION, "9575cfb404a084def9f9773623e12c20895008e5b169c4fbf4aa222b7d2fcd00", TEST_EXPIRY_DATE))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensures that a request with an null fileType returns a bad request status")
    void givenNullFileTypeWhenArtifactsUploadThenUploadFails() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, new byte[0]);
        mvc.perform(createArtifactRequest(emptyFile, TEST_FILE_NAME, null, DESCRIPTION, TEST_SHA256, TEST_EXPIRY_DATE))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensures that a request with an other fileType than DELTA or FULL returns a bad request status")
    void givenOtherFileTypeWhenArtifactsUploadThenUploadFails() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, new byte[0]);
        mvc.perform(createArtifactRequest(emptyFile, TEST_FILE_NAME, "OTHER", DESCRIPTION, TEST_SHA256, TEST_EXPIRY_DATE))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensures that a request with a null description returns a bad request status")
    void givenNullDescriptionWhenArtifactsUploadThenUploadFails() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, new byte[0]);
        mvc.perform(createArtifactRequest(emptyFile, null, FileType.DELTA.name(), null, TEST_SHA256, TEST_EXPIRY_DATE))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensures that a request with a null signature expiry date returns a bad request status")
    void givenNullSignatureExpiryDateWhenArtifactsUploadThenUploadFails() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, new byte[0]);
        mvc.perform(createArtifactRequest(emptyFile, null, FileType.DELTA.name(), DESCRIPTION, TEST_SHA256, null))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensures that a request with a null file name returns a bad request status")
    void givenNullFileNameWhenArtifactsUploadThenUploadFails() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, new byte[0]);
        mvc.perform(createArtifactRequest(emptyFile, null, FileType.FULL.name(), DESCRIPTION, TEST_SHA256, TEST_EXPIRY_DATE))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensures that a request with an expired signature date returns a bad request status")
    void givenExpiredSignatureDateWhenArtifactsUploadThenUploadFails() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, TEST_CONTENT.getBytes());
        mvc.perform(createArtifactRequest(file, TEST_FILE_NAME, FileType.DELTA.name(), DESCRIPTION, TEST_SHA256, "315532800")) // Past date
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensures that a request with an empty signature date returns a bad request status")
    void givenEmptySignatureDateWhenArtifactsUploadThenUploadFails() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, TEST_CONTENT.getBytes());
        mvc.perform(createArtifactRequest(file, TEST_FILE_NAME, FileType.DELTA.name(), DESCRIPTION, TEST_SHA256, "")) // Past date
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensures that a request with an invalid file type date returns a bad request status")
    void givenInvalidFileTypeWhenArtifactsUploadThenUploadFails() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, TEST_CONTENT.getBytes());
        mvc.perform(createArtifactRequest(file, TEST_FILE_NAME, "test", DESCRIPTION, TEST_SHA256, "315532800")) // Past date
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Attempts to create an artifact with an existing filename to test conflict handling")
    void givenFileWithExistingFilenameWhenArtifactsUploadThenUploadFails() throws Exception {
        testdataFactory.createArtifacts(TEST_FILE_NAME, FileType.valueOf(DELTA), "New file description", "123", TEST_SHA256);

        MockMultipartFile file = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, TEST_CONTENT.getBytes());
        mvc.perform(createArtifactRequest(file, TEST_FILE_NAME, FileType.DELTA.name(), DESCRIPTION, TEST_SHA256, TEST_EXPIRY_DATE))
                .andExpect(status().isConflict());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @Description("Ensures that a request for downloading an existing artifact returns a successful response")
    void givenFileAvailabilityShouldRespondAccordingly(boolean isFileAvailableOnS3) throws Exception {
        when(s3Service.generatePresignedUrl(anyString(), anyString(), anyLong())).thenCallRealMethod();
        when(s3Service.buildS3ObjectName(anyString(), anyString(), anyString())).thenCallRealMethod();
        Artifacts file = testdataFactory.createArtifacts(TEST_ARTIFACTS_FILE_NAME, FileType.valueOf(DELTA), TEST_FILE_DESCRIPTION, "1231", "1231");
        mockIsFileAvailableOnS3(isFileAvailableOnS3);
        var result = mvc.perform(get(ARTIFACTS_ENDPOINT_DOWNLOAD, TENANT_ID, file.getId()).contentType(MediaType.APPLICATION_JSON));
        if (isFileAvailableOnS3) {
            result.andExpect(status().is(302));
            var url = result.andReturn().getResponse().getHeader("Location");
            assertThat(url).isNotNull();
        } else {
            result.andExpect(status().isForbidden());
        }

    }

    private void mockIsFileAvailableOnS3(boolean isAvailable) {
        if (isAvailable) {

            when(s3Service.isValidGetUrl(any(URL.class))).thenReturn(isAvailable);
        } else {
            doThrow(new FileDownloadFailureException("File is unavailable for download")).when(s3Service).isValidGetUrl(any(URL.class));
        }

    }

    @Test
    @Description("Successfully modifies an artifact's description")
    void givenValidRequestWhenModifyingDescriptionThenSuccess() throws Exception {
        Long artifactId = testdataFactory.createArtifacts("artifact.txt", FileType.DELTA, "Old description", "123", "123").getId();
        MgmtArtifactsUpdateRequest  artifactsRequest = updateDescriptionArtifactsRequest();

        ObjectMapper objectMapper = new ObjectMapper();
        String requestJson = objectMapper.writeValueAsString(artifactsRequest);

        // Performing the POST request
        mvc.perform(put(MgmtRestConstants.ARTIFACTS_RESOURCE_V1_REQUEST_MAPPING, TENANT_ID, artifactId).contentType(MediaType.APPLICATION_JSON).content(requestJson))
                .andExpect(status().isOk());
    }

    @Test
    @Description("Fails to modify an artifact's description if the artifact is not found")
    void givenInvalidArtifactIdWhenModifyingDescriptionThenNotFound() throws Exception {
        long nonExistentArtifactId = 999L;  // Assume this ID does not exist
        MgmtArtifactsUpdateRequest  request = updateDescriptionArtifactsRequest();


        ObjectMapper objectMapper = new ObjectMapper();
        String requestJson = objectMapper.writeValueAsString(request);

        mvc.perform(put(MgmtRestConstants.ARTIFACTS_RESOURCE_V1_REQUEST_MAPPING, TENANT_ID, nonExistentArtifactId).contentType(MediaType.APPLICATION_JSON).content(requestJson))
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Returns Bad Request for missing required fields in the request")
    void givenMissingDescriptionWhenModifyingDescriptionThenBadRequest() throws Exception {
        Long artifactId = testdataFactory.createArtifacts("artifact.txt", FileType.DELTA, "Old description", "123", "123").getId();
        MgmtArtifactsRequest request = new MgmtArtifactsRequest(); // Empty description

        ObjectMapper objectMapper = new ObjectMapper();
        String requestJson = objectMapper.writeValueAsString(request);

        mvc.perform(put(MgmtRestConstants.ARTIFACTS_RESOURCE_V1_REQUEST_MAPPING, TENANT_ID, artifactId).contentType(MediaType.APPLICATION_JSON).content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensures retrieval of artifact information for an existing artifact")
    void givenArtifactIdThenGetArtifactThenReturnSuccess() throws Exception {

        // Create an artifact with no software modules and with links
        Artifacts file = testdataFactory.createArtifactsWithExpiryDate(
                TEST_ARTIFACTS_FILE_NAME_1,
                FileType.valueOf(DELTA),
                TEST_FILE_DESCRIPTION,
                "1231",
                "1231",
                Instant.now().getEpochSecond(),
                FileTransferStatus.UPLOADING_TO_STORAGE.toString()
        );
        final String expectedArtifactId = file.getId().toString();

        // Construct the URL by appending the artifactId to the ARTIFACTS_ENDPOINT
        String artifactUrl = ARTIFACTS_ENDPOINT + "/" + expectedArtifactId;

        // When (Act)
        mvc.perform(get(artifactUrl, TENANT_ID).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expectedArtifactId))
                .andExpect(jsonPath(JSON_PATH_FILE_NAME).value(file.getFileName()))
                .andExpect(jsonPath(JSON_PATH_FILE_TYPE).value(file.getFileType().name()))
                .andExpect(jsonPath(JSON_PATH_DESCRIPTION).value(file.getDescription()))
                .andExpect(jsonPath(JSON_PATH_EXPIRY_DATE).value(file.getExpiryDate()))
                .andExpect(jsonPath(JSON_PATH_CREATED_BY).exists())
                .andExpect(jsonPath(JSON_PATH_CREATED_AT).exists())
                .andExpect(jsonPath(JSON_PATH_LAST_MODIFIED_BY).exists())
                .andExpect(jsonPath(JSON_PATH_LAST_MODIFIED_AT).exists())
                .andExpect(jsonPath(JSON_PATH_SHA256).value(file.getSha256Hash()))
                .andExpect(jsonPath(JSON_PATH_MD5).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_SIZE).value(file.getFileSize()))
                .andExpect(jsonPath(JSON_PATH_SW_MODULES, hasSize(0)));
        assertThat(file).isNotNull();
    }

    @Test
    @Description("Ensures retrieval of artifact information for a non-existing artifact returns a not found status")
    void givenInvalidArtifactIdThenGetArtifactFails() throws Exception {

        final String invalidArtifactId = "90";

        // Construct the URL by appending the artifactId to the ARTIFACTS_ENDPOINT
        String artifactUrl = ARTIFACTS_ENDPOINT + "/" + invalidArtifactId;

        // When (Act)
        mvc.perform(get(artifactUrl, TENANT_ID).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()) // Expect 404 Not Found status
                .andExpect(jsonPath("$.name").value("ENTITY_NOT_EXISTS")) // Check for the name field
                .andExpect(jsonPath("$.debug").isNotEmpty()) // Check for the debug identifier
                .andExpect(jsonPath(JSON_PATH_MESSAGE).value("Artifacts with given identifier {90} does not exist.")); // Adjust the message as needed
    }

    @Test
    @Description("Given an existing artifact, when deleting it, then expect successful deletion and remove file from CDN and S3")
    void givenExistingArtifactWhenDeletingThenSuccess() throws Exception {
        reset(s3Client);
        Artifacts file = testdataFactory.createArtifacts(TEST_ARTIFACTS_FILE_NAME, FileType.valueOf(DELTA), TEST_FILE_DESCRIPTION, "1231", "1231");

        when(cdnDeleteSnsService.publishMessage(any(CdnDeleteRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(PublishResponse.builder().messageId(MOCK_MESSAGE_ID).build()));

        when(s3FileDeleteSnsService.publishMessage(any(S3FileDeletionRequest.class)))
                .thenThrow(new CompletionException(new RuntimeException(MESSAGE)));
        mvc.perform(delete(ARTIFACTS_ENDPOINT_DELETE, 1, file.getId()))
                .andExpect(status().isOk());
        Assertions.assertEquals(FileTransferStatus.DELETING_FROM_CDN, artifactsManagement.getArtifactsById(file.getId()).get().getFileStatus());

    }


    @Test
    @Description("Given an existing artifact without file in CDN, when deleting it, then expect successful deletion and remove file from S3 only")
    void givenExistingArtifactWithoutFileInCDNWhenDeletingThenSuccess() throws Exception {
        reset(s3Client);
        Artifacts file = testdataFactory.createArtifacts(TEST_ARTIFACTS_FILE_NAME, FileType.valueOf(DELTA), TEST_FILE_DESCRIPTION, "1231", "1231");

        final String path = artifactUrlHandlerProperties.getCdn().getDirectory()
                .replace(TENANT, tenantAware.getCurrentTenant().toUpperCase()).replace(SHA256, file.getSha256Hash());
        when(s3FileDeleteSnsService.publishMessage(any(S3FileDeletionRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(PublishResponse.builder().messageId(MOCK_MESSAGE_ID).build()));
        when(cdnDeleteSnsService.publishMessage(any(CdnDeleteRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(PublishResponse.builder().messageId(MOCK_MESSAGE_ID).build()));
        mvc.perform(delete(ARTIFACTS_ENDPOINT_DELETE, 1, file.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @Description("Given an existing artifact when deleting it causes SNS Exception, then throw Internal Server Error")
    void givenExistingArtifactOnSnsFailureWhenDeletingThenThrowInternalServerError() throws Exception {
        reset(s3Client);
        Artifacts file = testdataFactory.createArtifacts(TEST_ARTIFACTS_FILE_NAME, FileType.valueOf(DELTA), TEST_FILE_DESCRIPTION, "1231", "1231");
        final String path = artifactUrlHandlerProperties.getCdn().getDirectory()
                .replace(TENANT, tenantAware.getCurrentTenant().toUpperCase()).replace(SHA256, file.getSha256Hash());
        when(s3FileDeleteSnsService.publishMessage(any(S3FileDeletionRequest.class)))
                .thenThrow(new CompletionException(new RuntimeException(MESSAGE)));
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(CompletableFuture.failedFuture(new RuntimeException(MESSAGE)));
        mvc.perform(delete(ARTIFACTS_ENDPOINT_DELETE, 1, file.getId())).andExpect(status().isInternalServerError());
    }

    @Test
    @Description("Given a non-existing artifact ID, when attempting to delete it, then expect not found status")
    void givenNonExistingArtifactIdWhenDeletingThenNotFound() throws Exception {
        testdataFactory.createArtifacts(TEST_ARTIFACTS_FILE_NAME, FileType.valueOf(DELTA), TEST_FILE_DESCRIPTION, "1231", "1231");
        mvc.perform(delete(ARTIFACTS_ENDPOINT_DELETE, 1, 100L))
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Given an artifact linked to a software module, when attempting to delete it, then expect validation error")
    void givenArtifactLinkedToSoftwareModuleWhenDeletingThenValidationError() throws Exception {
        Path tempFile = tempDir.resolve("test_fileName_for_delete");
        Files.createFile(tempFile);

        Artifacts file = testdataFactory.createArtifacts(tempFile.toString(), FileType.valueOf(DELTA), TEST_FILE_DESCRIPTION, "1231", "1231");
        SoftwareModule softwareModule1 = testdataFactory.createSoftwareModule("testSM");

        Version version = testdataFactory.createVersion(softwareModule1.getId(), "testVersion");


        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder()
                .artifact((JpaArtifacts) file)
                .softwareModule((JpaSoftwareModule) softwareModule1)
                .sourceVersion((JpaVersion) version)
                .targetVersion((JpaVersion) version)
                .build();

        Set<ArtifactSoftwareModuleAssociation> iAssociationList = new HashSet<>();
        iAssociationList.add(association);

        testdataFactory.createArtifactsSoftwareModuleAssociation(iAssociationList);

        mvc.perform(delete(ARTIFACTS_ENDPOINT_DELETE, 1, file.getId()))
                .andExpect(status().isBadRequest());

        jdbcTemplate.update("DELETE FROM sp_artifact_software_module WHERE ARTIFACT_ID = ?", file.getId());
        jdbcTemplate.update("DELETE FROM sp_artifacts WHERE ID = ?", file.getId());


    }

    @Test
    @Description("Test successful unlinking of artifact and software module association")
    void testUnlinkArtifactSoftwareModuleAssociation() throws Exception {

        Path tempFile = tempDir.resolve(TEST_FILE_NAME);
        Files.createFile(tempFile);

        // Create an artifact with no software modules and with links
        Artifacts file = testdataFactory.createArtifacts(
                tempFile.toString(),
                FileType.valueOf(DELTA),
                TEST_FILE_DESCRIPTION,
                "1231",
                "1231"
        );

        SoftwareModule softwareModule = testdataFactory.createSoftwareModule("test");

        Version version = testdataFactory.createVersionForSoftwareModule(softwareModule);
        ArtifactSoftwareModuleAssociation association1 = MgmtArtifactSoftwareModuleAssociationMapper.toArtifactSoftwareModuleAssociationEntity(file, version, version, softwareModule);

        Set<ArtifactSoftwareModuleAssociation> associationsSet = new HashSet<>();
        associationsSet.add(association1);
        testdataFactory.saveArtifactSoftwareModuleAssociation(associationsSet);

        mvc.perform(delete(ARTIFACTS_V1_REQUEST_MAPPING + TEST_ARTIFACTS_URL, 1, file.getId(), softwareModule.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("Test unlinking of artifact and software module association with invalid artifact ID")
    void testUnlinkInvalidArtifactSoftwareModuleAssociation() throws Exception {

        // Create an artifact with no software modules and with links
        testdataFactory.createArtifacts(
                TEST_ARTIFACTS_FILE_NAME_1,
                FileType.valueOf(DELTA),
                TEST_FILE_DESCRIPTION,
                "1231",
                "1231"
        );

        mvc.perform(delete(ARTIFACTS_V1_REQUEST_MAPPING + TEST_ARTIFACTS_URL, 1, 100L, 1L))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Test unlinking of artifact and software module association with invalid software module ID")
    void testUnlinkInvalidSoftwareModuleArtifactSoftwareModuleAssociation() throws Exception {

        // Create an artifact with no software modules and with links
        Artifacts file = testdataFactory.createArtifacts(
                TEST_ARTIFACTS_FILE_NAME_1,
                FileType.valueOf(DELTA),
                TEST_FILE_DESCRIPTION,
                "1231",
                "1231"
        );
        testdataFactory.createSoftwareModule("test");
        mvc.perform(delete(ARTIFACTS_V1_REQUEST_MAPPING + TEST_ARTIFACTS_URL, 1, file.getId(), 100L))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Validating purging of an artifact linked to a software module has unlinked and removed from CDN in tenant configuration")
    void givenArtifactLinkedToSoftwareModuleAndCdnInTenantConfigWhenPurgingThenSuccess() throws Exception {
        Path tempFile = tempDir.resolve("test_fileName_for_purge_with_tenant_config_success");
        if (!Files.exists(tempFile)) {
            Files.createFile(tempFile);
        }

        Artifacts artifact = testdataFactory.createArtifacts(tempFile.toString(), FileType.valueOf(DELTA), TEST_FILE_DESCRIPTION, "1231", "1231");
        SoftwareModule softwareModule1 = testdataFactory.createSoftwareModule(TEST_PURGE_SM);

        Version sourceVersion = testdataFactory.createVersion(softwareModule1.getId(), TEST_PURGE_SOURCE_VERSION);
        Version targetVersion = testdataFactory.createVersion(softwareModule1.getId(), TEST_PURGE_TARGET_VERSION);


        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder()
                .artifact((JpaArtifacts) artifact)
                .softwareModule((JpaSoftwareModule) softwareModule1)
                .sourceVersion((JpaVersion) sourceVersion)
                .targetVersion((JpaVersion) targetVersion)
                .build();

        Set<ArtifactSoftwareModuleAssociation> iAssociationList = new HashSet<>();
        iAssociationList.add(association);

        testdataFactory.createArtifactsSoftwareModuleAssociation(iAssociationList);
        testdataFactory.addTenantConfigurations(TenantConfigurationProperties.TenantConfigurationKey.DEFAULT_CDN, TEST_CDN);

        Assertions.assertFalse(artifactsManagement.findAssociationByArtifactId(artifact.getId()).isEmpty());
        when(s3FileDeleteSnsService.publishMessage(any(S3FileDeletionRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(PublishResponse.builder().messageId(MOCK_MESSAGE_ID).build()));
        when(cdnDeleteSnsService.publishMessage(any(CdnDeleteRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(PublishResponse.builder().messageId(MOCK_MESSAGE_ID).build()));
        mvc.perform(delete(ARTIFACTS_ENDPOINT_PURGE, 1, artifact.getId()))
                .andExpect(status().isOk());

        Assertions.assertTrue(artifactsManagement.findAssociationByArtifactId(artifact.getId()).isEmpty());
        verify(snsAsyncClient, atLeastOnce()).publish(any(PublishRequest.class));
        Assertions.assertEquals(FileTransferStatus.DELETING_FROM_CDN, artifactsManagement.getArtifactsById(artifact.getId()).get().getFileStatus());

    }


    @Test
    @Description("Validating purging of an artifact if publishing to the SNS has an exception then error is logged")
    void givenArtifactLinkedToSoftwareModuleAndCdnInTenantConfigWhenSNSIsNotReachableThenErrorLogged(CapturedOutput output) throws Exception {
        Path tempFile = tempDir.resolve("test_fileName_for_purge_with_tenant_config_success");
        if (!Files.exists(tempFile)) {
            Files.createFile(tempFile);
        }

        Artifacts artifact = testdataFactory.createArtifacts(tempFile.toString(), FileType.valueOf(DELTA), TEST_FILE_DESCRIPTION, "1231", "1231");
        SoftwareModule softwareModule1 = testdataFactory.createSoftwareModule(TEST_PURGE_SM);

        Version sourceVersion = testdataFactory.createVersion(softwareModule1.getId(), TEST_PURGE_SOURCE_VERSION);
        Version targetVersion = testdataFactory.createVersion(softwareModule1.getId(), TEST_PURGE_TARGET_VERSION);


        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder()
                .artifact((JpaArtifacts) artifact)
                .softwareModule((JpaSoftwareModule) softwareModule1)
                .sourceVersion((JpaVersion) sourceVersion)
                .targetVersion((JpaVersion) targetVersion)
                .build();

        Set<ArtifactSoftwareModuleAssociation> iAssociationList = new HashSet<>();
        iAssociationList.add(association);

        testdataFactory.createArtifactsSoftwareModuleAssociation(iAssociationList);
        testdataFactory.addTenantConfigurations(TenantConfigurationProperties.TenantConfigurationKey.DEFAULT_CDN, TEST_CDN);

        Assertions.assertFalse(artifactsManagement.findAssociationByArtifactId(artifact.getId()).isEmpty());

        //Throw an exception while sending a message to SNS.
        when(cdnDeleteSnsService.publishMessage(any(CdnDeleteRequest.class)))
                .thenThrow(new CompletionException(new RuntimeException(MESSAGE)));

        when(snsAsyncClient.publish(any(PublishRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException(MESSAGE)));

        mvc.perform(delete(ARTIFACTS_ENDPOINT_PURGE, 1, artifact.getId()))
                .andExpect(status().isInternalServerError());
    }


    @Test
    @Description("Validating purging of an invalid artifact throws not found exception")
    void givenInvalidArtifactWhenPurgeThenNotFound() throws Exception {

        mvc.perform(delete(ARTIFACTS_ENDPOINT_PURGE, 1, 1000L))
                .andExpect(status().isNotFound());
    }


    @Test
    @Description("Validating purging of an artifact linked to a software module has unlinked and removed from App's default CDN")
    void givenArtifactLinkedToSoftwareModuleWhenPurgingThenSuccess() throws Exception {
        Path tempFile = tempDir.resolve("test_fileName_for_purge_success");
        if (!Files.exists(tempFile)) {
            Files.createFile(tempFile);
        }

        Artifacts artifact = testdataFactory.createArtifacts(tempFile.toString(), FileType.valueOf(DELTA), TEST_FILE_DESCRIPTION, "1231", "1231");
        SoftwareModule softwareModule1 = testdataFactory.createSoftwareModule(TEST_PURGE_SM);

        Version sourceVersion = testdataFactory.createVersion(softwareModule1.getId(), TEST_PURGE_SOURCE_VERSION);
        Version targetVersion = testdataFactory.createVersion(softwareModule1.getId(), TEST_PURGE_TARGET_VERSION);


        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder()
                .artifact((JpaArtifacts) artifact)
                .softwareModule((JpaSoftwareModule) softwareModule1)
                .sourceVersion((JpaVersion) sourceVersion)
                .targetVersion((JpaVersion) targetVersion)
                .build();

        Set<ArtifactSoftwareModuleAssociation> iAssociationList = new HashSet<>();
        iAssociationList.add(association);

        testdataFactory.createArtifactsSoftwareModuleAssociation(iAssociationList);

        final String defaultCcn = testdataFactory.getTenantConfigurations(TenantConfigurationProperties.TenantConfigurationKey.DEFAULT_CDN);


        Assertions.assertFalse(artifactsManagement.findAssociationByArtifactId(artifact.getId()).isEmpty());
        when(s3FileDeleteSnsService.publishMessage(any(S3FileDeletionRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(PublishResponse.builder().messageId(MOCK_MESSAGE_ID).build()));
        when(cdnDeleteSnsService.publishMessage(any(CdnDeleteRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(PublishResponse.builder().messageId(MOCK_MESSAGE_ID).build()));


        mvc.perform(delete(ARTIFACTS_ENDPOINT_PURGE, 1, artifact.getId()))
                .andExpect(status().isOk());

        Assertions.assertTrue(artifactsManagement.findAssociationByArtifactId(artifact.getId()).isEmpty());
        verify(snsAsyncClient, atLeastOnce()).publish(any(PublishRequest.class));
        Assertions.assertEquals(FileTransferStatus.DELETING_FROM_CDN, artifactsManagement.getArtifactsById(artifact.getId()).get().getFileStatus());


    }


    @Test
    @Description("Test successful linking of multiple software modules to an artifact")
    void givenMultipleSoftwareModulesWhenLinkingToAnArtifactThenAssociationsAreCreated() throws Exception {

        Path tempFile = tempDir.resolve("test_file1");

        boolean exists = Files.exists(tempFile);
        if (!exists) {
            Files.createFile(tempFile);
        }

        // Create an artifact with no software modules and with links
        Artifacts file = testdataFactory.createArtifacts(tempFile.toString(), FileType.valueOf(DELTA), TEST_FILE_DESCRIPTION, "1231", "1231");

        // link three software modules to an artifact
        SoftwareModule softwareModule1 = testdataFactory.createSoftwareModule("test1");
        SoftwareModule softwareModule2 = testdataFactory.createSoftwareModule("test2");
        SoftwareModule softwareModule3 = testdataFactory.createSoftwareModule("test3");

        Version version1 = testdataFactory.createVersionForSoftwareModule(softwareModule1);
        Version version2 = testdataFactory.createVersionForSoftwareModule(softwareModule2);
        Version version3 = testdataFactory.createVersionForSoftwareModule(softwareModule3);

        ArtifactSoftwareModuleAssociation association1 = MgmtArtifactSoftwareModuleAssociationMapper.toArtifactSoftwareModuleAssociationEntity(file, version1, version1, softwareModule1);
        ArtifactSoftwareModuleAssociation association2 = MgmtArtifactSoftwareModuleAssociationMapper.toArtifactSoftwareModuleAssociationEntity(file, version2, version2, softwareModule2);
        ArtifactSoftwareModuleAssociation association3 = MgmtArtifactSoftwareModuleAssociationMapper.toArtifactSoftwareModuleAssociationEntity(file, version3, version3, softwareModule3);

        Set<ArtifactSoftwareModuleAssociation> associationsSet = new HashSet<>();
        associationsSet.add(association1);
        associationsSet.add(association2);
        associationsSet.add(association3);

        testdataFactory.saveArtifactSoftwareModuleAssociation(associationsSet);

        String artifactUrl = ARTIFACTS_ENDPOINT + "/" + file.getId();
        MvcResult mvcResult = mvc.perform(get(artifactUrl, TENANT_ID).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode response = objectMapper.readTree(mvcResult.getResponse().getContentAsString());

        // Extract the softwareModules array
        JsonNode softwareModulesNode = response.path("softwareModules");

        // Assert that the size of software modules array is 3
        assertThat(softwareModulesNode.size()).isEqualTo(3);
    }

    @Test
    @Description("Ensures that a Add Artifact request with a future date return success status")
    void givenExpiryDateAsFutureDateArtifactsCreateThenUploadSuccess() throws Exception {

        MgmtArtifactsRequest artifactsRequest = createArtifactsRequestWithExpiryAndSHA256(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), SHA_256);
        ObjectMapper objectMapper = new ObjectMapper();
        String requestJson = objectMapper.writeValueAsString(artifactsRequest);

        // Create a mock PublishResponse
        PublishResponse mockPublishResponse = PublishResponse.builder()
                .messageId(MOCK_MESSAGE_ID)
                .build();

        // Mock the behavior of publishMessage to return a completed CompletableFuture with the mock response
        CompletableFuture<PublishResponse> completedFuture = CompletableFuture.completedFuture(mockPublishResponse);
        when(s3FileTransferSnsService.publishMessage(any(S3FileTransferRequest.class))).thenReturn(completedFuture);
        mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(requestJson)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
        // Verify the mock behavior
        verify(snsAsyncClient, times(1)).publish(any(PublishRequest.class));

    }

    @Test
    @Description("Ensures that a Add Artifact request with an invalid expiry date (current date) returns a bad request status")
    void givenExpiryDateAsCurrentDateArtifactsCreateThenUploadFails() throws Exception {

        MgmtArtifactsRequest artifactsRequest = createArtifactsRequestWithExpiryAndSHA256(Instant.now().getEpochSecond(), SHA_256);
        ObjectMapper objectMapper = new ObjectMapper();
        String requestJson = objectMapper.writeValueAsString(artifactsRequest);

        mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(requestJson)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensures that an Artifact Upload with a invalid date and file upload request to ensure success response")
    void givenInvalidExpiryDateUploadRequestWhenArtifactsUploadThenUploadFails() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, TEST_CONTENT.getBytes());
        mvc.perform(createArtifactRequestWithExpiry(file, TEST_FILE_NAME, FileType.DELTA.name(), DESCRIPTION, TEST_ARTIFACT_SHA256, String.valueOf(Instant.now().getEpochSecond())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Returns validation error when SHA256 is invalid")
    void givenRequestWithInvalidSHA256WhenArtifactsCreateThenReturnValidationError() throws Exception {

        // Create request with invalid SHA256
        MgmtArtifactsRequest artifactsRequest = createArtifactsRequestWithExpiryAndSHA256(
                Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(),
                "invalidSHA256Hash!@#$%");

        ObjectMapper objectMapper = new ObjectMapper();
        String requestJson = objectMapper.writeValueAsString(artifactsRequest);

        // Perform the request and expect validation error for invalid SHA256
        mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(requestJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE).value("sha256 is not valid"));
    }

    @Test
    @Description("Returns validation error when SHA256 is missing")
    void givenRequestWithMissingSHA256WhenArtifactsCreateThenReturnValidationError() throws Exception {

        // Create request without SHA256
        MgmtArtifactsRequest artifactsRequest = createArtifactsRequestWithExpiryAndSHA256(
                Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(),
                "");

        ObjectMapper objectMapper = new ObjectMapper();
        String requestJson = objectMapper.writeValueAsString(artifactsRequest);

        // Perform the request and expect validation error for missing SHA256
        mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(requestJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())  // or appropriate status for validation error
                .andExpect(jsonPath(JSON_PATH_MESSAGE).value("sha256 cannot be empty"));
    }

    @Test
    @Description("Allows uppercase SHA256")
    void givenRequestWithUpperCaseSHA256WhenArtifactsCreateThenReturnSuccess() throws Exception {

        // Create request with uppercase SHA256
        MgmtArtifactsRequest artifactsRequest = createArtifactsRequestWithExpiryAndSHA256(
                Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(),
                "45A24BA430132C2D54EB6AED2758CA78FCDA5A4E2866D2D04B5E80A70239D8C2");

        ObjectMapper objectMapper = new ObjectMapper();
        String requestJson = objectMapper.writeValueAsString(artifactsRequest);

        // Perform the request and expect success
        mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(requestJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated());
    }

    @Test
    @Description("Sha256 cannot be empty")
    void givenRequestWithoutSHA256WhenArtifactsCreateThenReturnError() throws Exception {

        // Create request with uppercase SHA256
        MgmtArtifactsRequest artifactsRequest = createArtifactsRequestWithExpiryAndSHA256(
                Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), null);

        ObjectMapper objectMapper = new ObjectMapper();
        String requestJson = objectMapper.writeValueAsString(artifactsRequest);

        // Perform the request and expect success
        mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(requestJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("when duplicate uppercase sha256 is found then return conflict")
    void givenRequestWhenSHA256UppercaseWhenArtifactsCreateThenReturnError() throws Exception {

        // Create request with uppercase SHA256
        MgmtArtifactsRequest artifactsRequest = createArtifactsRequestWithExpiryAndSHA256(
                Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), "45A24BA430132C2D54EB6AED2758CA78FCDA5A4E2866D2D04B5E80A70239D8C2");

        ObjectMapper objectMapper = new ObjectMapper();
        String requestJson = objectMapper.writeValueAsString(artifactsRequest);

        // Perform the request and expect success
        mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(requestJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated());

        // Create request with lowecase SHA256
        MgmtArtifactsRequest artifactsDuplicateRequest = createArtifactsRequestWithExpiryAndSHA256(
                Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), "45a24ba430132c2d54eb6aed2758ca78fcda5a4e2866d2d04b5e80a70239d8c2");

        String requestJsonwithDuplicateSha256 = objectMapper.writeValueAsString(artifactsDuplicateRequest);

        // Perform the request and expect success
        mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(requestJsonwithDuplicateSha256)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isConflict());

    }

    @Test
    @Description("Artifact with null sha256 is provided then fails")
    void givenRequestWithNullSha256ArtifactWhenUploadThenUploadFails() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, TEST_CONTENT.getBytes());
        final String TEST_SIGNATURE_EXPIRY_DATE = String.valueOf(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());

        doNothing().when(s3MultipartFileUpload).uploadFileToS3Multipart(any(S3FileUpload.class), any(InputStream.class));

        mvc.perform(createArtifactRequest(file, TEST_FILE_NAME, FileType.DELTA.name(), DESCRIPTION, null, TEST_SIGNATURE_EXPIRY_DATE))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Attempts to create an artifact with an existing filename but a different SHA256 to test created successfully")
    void givenRequestWithSameFilenameAndDifferentSHA256ThenReturnSuccess() throws Exception {

        // Create request with first SHA256
        MgmtArtifactsRequest artifactRequestUsingFileUrl = createArtifactRequestUsingFileUrl(
                "45A24BA430132C2D54EB6AED2758CA78FCDA5A4E2866D2D04B5E80A70239D8C2",
                "TestFile",
                FileType.DELTA
        );

        // Perform first request - expect 201 Created
        mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(new ObjectMapper().writeValueAsString(artifactRequestUsingFileUrl)).
                        contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated());

        // Modify SHA256 to a different value and perform again
        MgmtArtifactsRequest duplicateFileNameArtifactRequest = createArtifactRequestUsingFileUrl(
                "AA12BB34CC56DD78EE90FF11223344556677889900AABBCCDDEEFF0011223344",
                "TestFile",
                FileType.DELTA
        );

        // Perform second request - expect 201 created
        mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(new ObjectMapper().writeValueAsString(duplicateFileNameArtifactRequest)).
                        contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated());
    }


    @Test
    @Description("Ensures that an artifact is created with the status set to ACTIVE for CreateArtifactFileUrl")
    void givenValidArtifactRequestWhenSavedThenStatusIsSetToActive() throws Exception {
        MgmtArtifactsRequest artifactRequestUsingFileUrl = createArtifactRequestUsingFileUrl("45A24BA430132C2D54EB6AED2758CA78FCDA5A4E2866D2D04B5E80A70239D8C2",
                "TestFile", FileType.DELTA);
        MvcResult mvcResult = mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(new ObjectMapper().writeValueAsString(artifactRequestUsingFileUrl))
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultHandlers.print()).andExpect(status().isCreated()).andReturn();
        JsonNode response = new ObjectMapper().readTree(mvcResult.getResponse().getContentAsString());
        // Verify artifact status is set to ACTIVE
        long id = response.path("id").asLong();
        Artifacts artifacts = artifactsManagement.getArtifactsById(id).get();
        Assertions.assertEquals(ArtifactsStatus.ACTIVE.name(), artifacts.getArtifactStatus());
    }


    @Test
    @Description("Given the ArtifactsStatus enum, when retrieving all values, then ensure the correct statuses are returned")
    public void givenArtifactsStatusEnumWhenRetrievingAllValuesThenCorrectStatusesReturned() {
        ArtifactsStatus[] statuses = ArtifactsStatus.values();
        assertNotNull(statuses);
        Assertions.assertEquals(4, statuses.length);
        Assertions.assertEquals(ArtifactsStatus.ACTIVE, statuses[0]);
        Assertions.assertEquals(ArtifactsStatus.PURGED, statuses[1]);
        Assertions.assertEquals(ArtifactsStatus.DELETED, statuses[2]);
        Assertions.assertEquals(ArtifactsStatus.REPLACED, statuses[3]);
        Assertions.assertEquals(ArtifactsStatus.ACTIVE, ArtifactsStatus.valueOf("ACTIVE"));
        Assertions.assertEquals(ArtifactsStatus.PURGED, ArtifactsStatus.valueOf("PURGED"));
        Assertions.assertEquals(ArtifactsStatus.DELETED, ArtifactsStatus.valueOf("DELETED"));
    }

    @Test
    @Description("Ensures that when an artifact is Purged the status is set to PURGED")
    void givenValidArtifactRequestWhenPurgedThenStatusIsUpdatedToPurged() throws Exception {

        Path tempFile = tempDir.resolve("test_fileName_for_purge_with_tenant_config_success");
        if (!Files.exists(tempFile)) {
            Files.createFile(tempFile);
        }

        Artifacts artifact = testdataFactory.createArtifacts(tempFile.toString(), FileType.valueOf(DELTA), TEST_FILE_DESCRIPTION, "1231", "1231");
        SoftwareModule softwareModule1 = testdataFactory.createSoftwareModule(TEST_PURGE_SM);

        Version sourceVersion = testdataFactory.createVersion(softwareModule1.getId(), TEST_PURGE_SOURCE_VERSION);
        Version targetVersion = testdataFactory.createVersion(softwareModule1.getId(), TEST_PURGE_TARGET_VERSION);


        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder()
                .artifact((JpaArtifacts) artifact)
                .softwareModule((JpaSoftwareModule) softwareModule1)
                .sourceVersion((JpaVersion) sourceVersion)
                .targetVersion((JpaVersion) targetVersion)
                .build();

        Set<ArtifactSoftwareModuleAssociation> iAssociationList = new HashSet<>();
        iAssociationList.add(association);

        testdataFactory.createArtifactsSoftwareModuleAssociation(iAssociationList);

        mvc.perform(delete(ARTIFACTS_ENDPOINT_PURGE, 1, artifact.getId()))
                .andExpect(status().isOk());
        Assertions.assertEquals(ArtifactsStatus.PURGED.name(), artifactsManagement.getArtifactsById(artifact.getId()).get().getArtifactStatus());
        Assertions.assertEquals(FileTransferStatus.DELETING_FROM_CDN, artifactsManagement.getArtifactsById(artifact.getId()).get().getFileStatus());


    }

    @Test
    @Description("Ensures that when an artifact is Deleted the status is set to DELETED")
    void givenArtifactWhenDeletedThenStatusIsUpdatedToDeleted() throws Exception {
        Artifacts artifacts = testdataFactory.createArtifacts(TEST_ARTIFACTS_FILE_NAME, FileType.valueOf(DELTA), TEST_FILE_DESCRIPTION, "1231", "1231");
        mvc.perform(delete(ARTIFACTS_ENDPOINT_DELETE, 1, artifacts.getId()))
                .andExpect(status().isOk());
        Assertions.assertEquals(FileTransferStatus.DELETING_FROM_CDN, artifactsManagement.getArtifactsById(artifacts.getId()).get().getFileStatus());
        Assertions.assertEquals(ArtifactsStatus.DELETED.name(), artifactsManagement.getArtifactsById(artifacts.getId()).get().getArtifactStatus());

    }

    @Test
    @Description("Ensures that when an Artifact is in Purged, software modules association should return bad request")
    void givenArtifactWhenPurgedThenAllAssociationsAreDeleted() throws Exception {
        Artifacts artifact = testdataFactory.createArtifacts(
                TEST_ARTIFACTS_FILE_NAME, FileType.valueOf(DELTA), TEST_FILE_DESCRIPTION, "1231", "1231"
        );

        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(TEST_PURGE_SM);
        Version sourceVersion = testdataFactory.createVersion(softwareModule.getId(), TEST_PURGE_SOURCE_VERSION);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), TEST_PURGE_TARGET_VERSION);


        SoftwareModuleArtifactBindingRequest moduleArtifactBindingRequest = SoftwareModuleArtifactBindingRequest.builder()
                .softwareModuleId(softwareModule.getId().intValue())
                .sourceVersion(List.of(sourceVersion.getId().intValue()))
                .targetVersion(targetVersion.getId().intValue())
                .build();

        mvc.perform(delete(ARTIFACTS_ENDPOINT_PURGE, 1, artifact.getId()))
                .andExpect(status().isOk());

        mvc.perform(post(MgmtRestConstants.CREATE_ARTIFACTS_SM_ASSOCIATIONN_V1_REQUEST_MAPPING, 1L, artifact.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(moduleArtifactBindingRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Artifact is not in an active state"));

    }

    @Test
    @Description("Ensures that when an Artifact is in DELETE state, software modules association should return bad request")
    void givenArtifactNotActiveWhenCreatingAssociationThenBadRequest() throws Exception {
        Artifacts artifact = testdataFactory.createArtifacts(
                TEST_ARTIFACTS_FILE_NAME, FileType.valueOf(DELTA), TEST_FILE_DESCRIPTION, "1231", "1231"
        );

        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(TEST_PURGE_SM);
        Version sourceVersion = testdataFactory.createVersion(softwareModule.getId(), TEST_PURGE_SOURCE_VERSION);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), TEST_PURGE_TARGET_VERSION);

        SoftwareModuleArtifactBindingRequest moduleArtifactBindingRequest = SoftwareModuleArtifactBindingRequest.builder()
                .softwareModuleId(softwareModule.getId().intValue())
                .sourceVersion(List.of(sourceVersion.getId().intValue()))
                .targetVersion(targetVersion.getId().intValue())
                .build();

        mvc.perform(delete(ARTIFACTS_ENDPOINT_DELETE, 1, artifact.getId()))
                .andExpect(status().isOk());
        mvc.perform(post(MgmtRestConstants.CREATE_ARTIFACTS_SM_ASSOCIATIONN_V1_REQUEST_MAPPING, 1L, artifact.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(moduleArtifactBindingRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Artifact is not in an active state"));
    }

    @Test
    @Description("Given when an Artifact is in ACTIVE state, software modules association can be created")
    void givenArtifactActiveWhenAssociationSoftwareModuleThenSuccess() throws Exception {

        var result = invokeCreateArtifactViaUrlApi("https://en.xiaoai.me/pages/text-empty-pdf-generator");

        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(TEST_PURGE_SM);
        Version sourceVersion = testdataFactory.createVersion(softwareModule.getId(), TEST_PURGE_SOURCE_VERSION);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), TEST_PURGE_TARGET_VERSION);

        SoftwareModuleArtifactBindingRequest moduleArtifactBindingRequest = SoftwareModuleArtifactBindingRequest.builder()
                .softwareModuleId(softwareModule.getId().intValue())
                .sourceVersion(List.of(sourceVersion.getId().intValue()))
                .targetVersion(targetVersion.getId().intValue())
                .build();

        mvc.perform(post(MgmtRestConstants.CREATE_ARTIFACTS_SM_ASSOCIATIONN_V1_REQUEST_MAPPING, 1L, result.getArtifactId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(moduleArtifactBindingRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @Description("Given file name with multiple white spaces, when creating an artifact with file URL, then all the white spaces in filename are trimmed")
    void givenFileNameWithWhiteSpaceWhenCreatingArtifactWithFileURLThenSpaceTrimmed() throws Exception {

        Artifacts artifacts = testdataFactory.createArtifacts("test     file 01", FileType.valueOf(DELTA), "New file description", "123", "123");

        MgmtArtifactsRequest artifactsRequest = MgmtArtifactsRequest.builder()
                .filename(artifacts.getFileName())
                .fileType(artifacts.getFileType().name())
                .description(artifacts.getDescription())
                .signatureExpiryDate(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond())
                .build();

        artifactsRequest.setFileURL("https://sample-videos.com/video321/3gp/240/big_buck_bunny_240p_30mb.3gp");
        artifactsRequest.setSha256("4e3238d96120049df130dd1a9c5a44054ba02477c5d220949c4ee8fa942f33b2");

        ObjectMapper objectMapper = new ObjectMapper();
        String requestJson = objectMapper.writeValueAsString(artifactsRequest);

        MvcResult mvcResult = mvc.perform(post(MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING_FILEURL, TENANT_ID)
                        .content(requestJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(mvcResult.getResponse().getContentAsString());

        Long artifactId = response.path("id").asLong();

        Optional<Artifacts> updatedArtifact = artifactsManagement.getArtifactsById(artifactId);
        Assertions.assertEquals("testfile01", updatedArtifact.get().getFileName());
    }

    @Test
    @Description("Given purge artifact, when artifact is purges, then throw excpetion")
    void givenPurgeArtifactWhenArtifactPurgedThenThrowsException() throws Exception {
        Path tempFile = tempDir.resolve("test_fileName_for_purge_with_tenant_config_success");
        if (!Files.exists(tempFile)) {
            Files.createFile(tempFile);
        }

        Artifacts artifact = testdataFactory.createArtifacts(tempFile.toString(), FileType.valueOf(DELTA), TEST_FILE_DESCRIPTION, "1231", "1231");
        SoftwareModule softwareModule1 = testdataFactory.createSoftwareModule(TEST_PURGE_SM);

        Version sourceVersion = testdataFactory.createVersion(softwareModule1.getId(), TEST_PURGE_SOURCE_VERSION);
        Version targetVersion = testdataFactory.createVersion(softwareModule1.getId(), TEST_PURGE_TARGET_VERSION);

        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder()
                .artifact((JpaArtifacts) artifact)
                .softwareModule((JpaSoftwareModule) softwareModule1)
                .sourceVersion((JpaVersion) sourceVersion)
                .targetVersion((JpaVersion) targetVersion)
                .build();

        Set<ArtifactSoftwareModuleAssociation> iAssociationList = new HashSet<>();
        iAssociationList.add(association);

        testdataFactory.createArtifactsSoftwareModuleAssociation(iAssociationList);

        mvc.perform(delete(ARTIFACTS_ENDPOINT_PURGE, 1, artifact.getId()))
                .andExpect(status().isOk());
        Assertions.assertEquals(ArtifactsStatus.PURGED.name(), artifactsManagement.getArtifactsById(artifact.getId()).get().getArtifactStatus());
        Assertions.assertEquals(FileTransferStatus.DELETING_FROM_CDN, artifactsManagement.getArtifactsById(artifact.getId()).get().getFileStatus());

        mvc.perform(delete(ARTIFACTS_ENDPOINT_PURGE, 1, artifact.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Artifact is already purged with ID: " + artifact.getId()));
    }

}
