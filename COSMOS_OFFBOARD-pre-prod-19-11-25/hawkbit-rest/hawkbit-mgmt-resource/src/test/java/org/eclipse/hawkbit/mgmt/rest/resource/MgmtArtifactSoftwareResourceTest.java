package org.eclipse.hawkbit.mgmt.rest.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Description;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.RandomStringUtils;
import org.cosmos.models.mgmt.FileType;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifactsRequest;
import org.cosmos.models.mgmt.artifacts.dto.ScomoArtifactBindingRequest;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.eclipse.hawkbit.api.ArtifactUrlHandlerProperties;
import org.eclipse.hawkbit.artifact.repository.ArtifactFilesystemProperties;
import org.cosmos.models.mgmt.artifacts.dto.SoftwareModuleArtifactBindingRequest;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtArtifactSoftwareModuleAssociationMapper;
import org.eclipse.hawkbit.repository.ArtifactsManagement;
import org.eclipse.hawkbit.repository.Constants;
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
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import static org.eclipse.hawkbit.repository.test.util.TestdataFactory.SM_INSTALLER_TYPE_0;
import static org.eclipse.hawkbit.repository.test.util.TestdataFactory.SM_TYPE_OS;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class MgmtArtifactSoftwareResourceTest extends AbstractManagementApiIntegrationTest {

    public static final String SOFTWARE_MODULE_NAME = "ResourceUtility.java";
    public static final String ARTIFACT_FILE_NAME = "ResourceUtility.java";
    public static final String FILE_DESCRIPTION = "New file description";
    public static final String FILE_SIZE = "1231";
    public static final String SHA_256 = "1231";
    public static final String SOURCE_VERSION = "SourceVersion";
    public static final String TARGET_VERSION = "TargetVersion";
    private static final String TEST_CDN = "Test-cdn";
    private static final String TEST_CDN_ROOT_DIRECTORY = "root";
    private static final String TEST_SCOMO = "testScomo";
    private static final String TEST_SOURCE_VERSION  = "testSourceVersion";
    private static final String TEST_TARGET_VERSION  = "testTargetVersion";

    @TempDir
    static Path tempDir;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Captor
    ArgumentCaptor<MessageHeaders> headersArgumentCaptor;
    @Autowired
    private ArtifactFilesystemProperties artifactFilesystemProperties;
    @Autowired
    private ArtifactUrlHandlerProperties artifactUrlHandlerProperties;
    @Autowired
    private ArtifactsManagement artifactsManagement;
    @MockBean
    private SnsAsyncClient snsAsyncClient;


    private static final String DELTA = "DELTA";
    private static final String ARTIFACTS_PATH = "/artifacts/";
    private static final String SW_MODULE_PATH = "/softwaremodules";
    private static final String FILE_TYPE = "$.fileType";
    private static final String JSON_PATH_SW_MODULES = "$.softwareModules";
    private static final String JSON_PATH_SW_MODULE_ID = "$.softwareModules[0].id";
    private static final String JSON_PATH_SOURCE_VERSION_FULL = "$.softwareModules[0].sourceVersionsForFull";
    private static final String JSON_PATH_SOURCE_VERSION_DELTA = "$.softwareModules[0].sourceVersionForDelta";
    private static final String JSON_PATH_TARGET_VERSION = "$.softwareModules[0].targetVersion";
    private static final String TEST_FILE_NAME = "test_fileName";
    private static final String TEST_FILE_NAME_TXT = "test_fileName.txt";
    private static final String DESCRIPTION = "description";
    private static final String TEST_EXPIRY_DATE = "1735689600";
    private static final String TEST_CONTENT = "Test content";
    private static final String TEST_SHA256 = "9d9595c5d94fb65b824f56e9999527dba9542481580d69feb89056aabaa0aa87";
    private static final String ARTIFACTS_ENDPOINT_FILEURL = MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING_FILEURL;
    private static final String ARTIFACTS_ENDPOINT = MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING;



    @BeforeEach
    void reset() throws IOException {
        // Create a mock PublishResponse
        PublishResponse mockPublishResponse = PublishResponse.builder()
                .messageId("mockMessageId")
                .build();
        // Create a completed CompletableFuture with the mock response
        CompletableFuture<PublishResponse> completedFuture = CompletableFuture.completedFuture(mockPublishResponse);

        // Mock the behavior of publish to return the completed future
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(completedFuture);
    }

    @AfterEach
    public void tearDown() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_artifact_software_module", "sp_software_versions", "sp_artifacts", "sp_base_software_module");
    }

    @Test
    @Description("Given a delta artifact and software module details, when creating the association, then it should return success. The test also verifies Cdn Upload message sending to SNS.")
    void givenDeltaArtifactAndSoftwareModuleDetailsWhenCreateThenReturnSuccess() throws Exception {

        MgmtArtifactsRequest artifactRequestUsingFileUrl = createArtifactRequestUsingFileUrl(TEST_SHA256, TEST_FILE_NAME, FileType.DELTA);

        MvcResult mvcResult = mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(new ObjectMapper().writeValueAsString(artifactRequestUsingFileUrl)).
                        contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated()).andReturn();
        JsonNode response = new ObjectMapper().readTree(mvcResult.getResponse().getContentAsString());
        long id = response.path("id").asLong();

        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SOFTWARE_MODULE_NAME);
        Version sourceVersion = testdataFactory.createVersion(softwareModule.getId(), SOURCE_VERSION);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), TARGET_VERSION);
        var association = getSoftwareModuleArtifactBindingRequest(Math.toIntExact(softwareModule.getId()), Collections.singletonList(Math.toIntExact(sourceVersion.getId())), Math.toIntExact(targetVersion.getId()));


        mvc.perform(post(MgmtRestConstants.BASE_V1_REQUEST_MAPPING_TENANT + ARTIFACTS_PATH + id + SW_MODULE_PATH, "1").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(association))).andExpect(status().isOk());
        // Verify the mock behavior
        verify(snsAsyncClient, times(2)).publish(any(PublishRequest.class));
    }

    @Test
    @Description("Given a full artifact and software module details, when creating the association, then it should return success.")
    void givenFullArtifactAndSoftwareModuleDetailsWhenCreateThenReturnSuccess() throws Exception {

        MgmtArtifactsRequest artifactRequestUsingFileUrl = createArtifactRequestUsingFileUrl(TEST_SHA256, TEST_FILE_NAME, FileType.FULL);

        MvcResult mvcResult = mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(new ObjectMapper().writeValueAsString(artifactRequestUsingFileUrl)).
                        contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated()).andReturn();
        JsonNode response = new ObjectMapper().readTree(mvcResult.getResponse().getContentAsString());
        long id = response.path("id").asLong();

        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SOFTWARE_MODULE_NAME);
        Version sourceVersion1 = testdataFactory.createVersion(softwareModule.getId(), SOURCE_VERSION + 1);
        Version sourceVersion2 = testdataFactory.createVersion(softwareModule.getId(), SOURCE_VERSION + 2);
        Version sourceVersion3 = testdataFactory.createVersion(softwareModule.getId(), SOURCE_VERSION + 3);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), TARGET_VERSION);

        List<Integer> sourceVersionsList = Arrays.asList(Math.toIntExact(sourceVersion1.getId()), Math.toIntExact(sourceVersion2.getId()), Math.toIntExact(sourceVersion3.getId()));
        var association = getSoftwareModuleArtifactBindingRequest(Math.toIntExact(softwareModule.getId()), sourceVersionsList, Math.toIntExact(targetVersion.getId()));

        testdataFactory.addTenantConfigurations(TenantConfigurationProperties.TenantConfigurationKey.DEFAULT_CDN, TEST_CDN);
        mvc.perform(post(MgmtRestConstants.BASE_V1_REQUEST_MAPPING_TENANT + ARTIFACTS_PATH+ id + SW_MODULE_PATH, "1").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(association))).andExpect(status().isOk());

        Assertions.assertEquals(3, artifactsManagement.artifactSoftwareModuleAssociationCount());
    }

    @Test
    @Description("Given a full artifact association, when the source and target version is same, then it should return success.")
    void givenFullArtifactAssociationWhenSameSourceAndTargetVersionThenReturnSuccess() throws Exception {


        MgmtArtifactsRequest artifactRequestUsingFileUrl = createArtifactRequestUsingFileUrl(TEST_SHA256, TEST_FILE_NAME, FileType.FULL);

        MvcResult mvcResult = mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(new ObjectMapper().writeValueAsString(artifactRequestUsingFileUrl)).
                        contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated()).andReturn();
        JsonNode response = new ObjectMapper().readTree(mvcResult.getResponse().getContentAsString());
        long id = response.path("id").asLong();

        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SOFTWARE_MODULE_NAME);
        Version sourceVersion1 = testdataFactory.createVersion(softwareModule.getId(), SOURCE_VERSION + 1);
        Version sourceVersion2 = testdataFactory.createVersion(softwareModule.getId(), SOURCE_VERSION + 2);
        Version sourceVersion3 = testdataFactory.createVersion(softwareModule.getId(), SOURCE_VERSION + 3);
        List<Integer> sourceVersionsList = Arrays.asList(Math.toIntExact(sourceVersion1.getId()), Math.toIntExact(sourceVersion2.getId()), Math.toIntExact(sourceVersion3.getId()));
        var association = getSoftwareModuleArtifactBindingRequest(Math.toIntExact(softwareModule.getId()), sourceVersionsList, Math.toIntExact(sourceVersion3.getId()));

        testdataFactory.addTenantConfigurations(TenantConfigurationProperties.TenantConfigurationKey.DEFAULT_CDN, TEST_CDN);

        mvc.perform(post(MgmtRestConstants.BASE_V1_REQUEST_MAPPING_TENANT + ARTIFACTS_PATH + id + SW_MODULE_PATH, "1").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(association))).andExpect(status().isOk());

        Optional<ArtifactSoftwareModuleAssociation> artifactSoftwareModuleAssociation = artifactsManagement.findArtifactSoftwareModuleAssociation(softwareModule.getId(), null, sourceVersion3.getId(), id);
        assertTrue(artifactSoftwareModuleAssociation.isPresent());
        Optional<ArtifactSoftwareModuleAssociation> artifactSoftwareModuleAssociation1 = artifactsManagement.findArtifactSoftwareModuleAssociation(softwareModule.getId(), sourceVersion2.getId(), sourceVersion3.getId(), id);
        assertTrue(artifactSoftwareModuleAssociation1.isPresent());
        Optional<ArtifactSoftwareModuleAssociation> artifactSoftwareModuleAssociation2 = artifactsManagement.findArtifactSoftwareModuleAssociation(softwareModule.getId(), sourceVersion1.getId(), sourceVersion3.getId(), id);
        assertTrue(artifactSoftwareModuleAssociation2.isPresent());
    }

    @Test
    @Description("Given a full artifact association, when an empty source version list is provided, then it should return success.")
    void givenFullArtifactAssociationWhenEmptySourceVersionListThenReturnSuccess() throws Exception {

        MgmtArtifactsRequest artifactRequestUsingFileUrl = createArtifactRequestUsingFileUrl(TEST_SHA256, TEST_FILE_NAME, FileType.FULL);

        MvcResult mvcResult = mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(new ObjectMapper().writeValueAsString(artifactRequestUsingFileUrl)).
                        contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated()).andReturn();
        JsonNode response = new ObjectMapper().readTree(mvcResult.getResponse().getContentAsString());
        long id = response.path("id").asLong();

        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SOFTWARE_MODULE_NAME);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), TARGET_VERSION);
        var association = getSoftwareModuleArtifactBindingRequest(Math.toIntExact(softwareModule.getId()), Collections.emptyList(), Math.toIntExact(targetVersion.getId()));

        testdataFactory.addTenantConfigurations(TenantConfigurationProperties.TenantConfigurationKey.DEFAULT_CDN, TEST_CDN);
        mvc.perform(post(MgmtRestConstants.BASE_V1_REQUEST_MAPPING_TENANT + ARTIFACTS_PATH + id + SW_MODULE_PATH, "1").contentType(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(association))).andExpect(status().isOk());

        Optional<ArtifactSoftwareModuleAssociation> artifactSoftwareModuleAssociation = artifactsManagement.findArtifactSoftwareModuleAssociation(softwareModule.getId(), null, targetVersion.getId(), id);
        assertTrue(artifactSoftwareModuleAssociation.isPresent());
    }

    @Test
    @Description("Given a full artifact association, when a source version list consists of duplicates, then it should return success.")
    void givenFullArtifactAssociationWhenDuplicateSourceVersionListThenReturnSuccess() throws Exception {
        MgmtArtifactsRequest artifactRequestUsingFileUrl = createArtifactRequestUsingFileUrl(TEST_SHA256, TEST_FILE_NAME, FileType.FULL);

        MvcResult mvcResult = mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(new ObjectMapper().writeValueAsString(artifactRequestUsingFileUrl)).
                        contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated()).andReturn();
        JsonNode response = new ObjectMapper().readTree(mvcResult.getResponse().getContentAsString());
        long id = response.path("id").asLong();

        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SOFTWARE_MODULE_NAME);
        Version sourceVersion = testdataFactory.createVersion(softwareModule.getId(), SOURCE_VERSION);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), TARGET_VERSION);
        var association = getSoftwareModuleArtifactBindingRequest(Math.toIntExact(softwareModule.getId()), List.of(Math.toIntExact(sourceVersion.getId()), Math.toIntExact(sourceVersion.getId()), Math.toIntExact(sourceVersion.getId())), Math.toIntExact(targetVersion.getId()));

        testdataFactory.addTenantConfigurations(TenantConfigurationProperties.TenantConfigurationKey.DEFAULT_CDN, TEST_CDN);

        mvc.perform(post(MgmtRestConstants.BASE_V1_REQUEST_MAPPING_TENANT + ARTIFACTS_PATH + id + SW_MODULE_PATH, "1").contentType(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(association))).andExpect(status().isOk());
        Assertions.assertEquals(1, artifactsManagement.artifactSoftwareModuleAssociationCount());
    }

    @Test
    @Description("Given a full artifact association, when an invalid source version is provided, then it should return not found.")
    void givenFullArtifactAssociationWhenInvalidSourceVersionThenReturnNotFound() throws Exception {
        MgmtArtifactsRequest artifactRequestUsingFileUrl = createArtifactRequestUsingFileUrl(TEST_SHA256, TEST_FILE_NAME, FileType.FULL);

        MvcResult mvcResult = mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(new ObjectMapper().writeValueAsString(artifactRequestUsingFileUrl)).
                        contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated()).andReturn();
        JsonNode response = new ObjectMapper().readTree(mvcResult.getResponse().getContentAsString());
        long id = response.path("id").asLong();

        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SOFTWARE_MODULE_NAME);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), TARGET_VERSION);
        var association = getSoftwareModuleArtifactBindingRequest(Math.toIntExact(softwareModule.getId()), List.of(Math.toIntExact(Integer.MAX_VALUE)), Math.toIntExact(targetVersion.getId()));

        mvc.perform(post(MgmtRestConstants.BASE_V1_REQUEST_MAPPING_TENANT + ARTIFACTS_PATH + id + SW_MODULE_PATH, "1").contentType(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(association))).andExpect(status().isNotFound());
    }

    @Test
    @Description("Given a delta artifact association, when the software module ID is null, then it should return bad request.")
    void givenDeltaArtifactAssociationWhenSoftwareModuleIDIsNullThenReturnBadRequest() throws Exception {
        var expiryDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate(ARTIFACT_FILE_NAME, FileType.DELTA, FILE_DESCRIPTION, FILE_SIZE, SHA_256, expiryDate, FileTransferStatus.UPLOADING_TO_STORAGE.toString());

        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SOFTWARE_MODULE_NAME);
        Version sourceVersion = testdataFactory.createVersion(softwareModule.getId(), SOURCE_VERSION);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), TARGET_VERSION);

        var association = getSoftwareModuleArtifactBindingRequest(null, Collections.singletonList(Math.toIntExact(sourceVersion.getId())), Math.toIntExact(targetVersion.getId()));

        mvc.perform(post(MgmtRestConstants.BASE_V1_REQUEST_MAPPING_TENANT + ARTIFACTS_PATH + artifact.getId() + SW_MODULE_PATH, "1").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(association))).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given a delta artifact association, when multiple source versions are provided, then it should return a bad request.")
    void givenDeltaArtifactAssociationWhenMultipleSourceVersionsThenReturnBadRequest() throws Exception {
        var expiryDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate(ARTIFACT_FILE_NAME, FileType.DELTA, FILE_DESCRIPTION, FILE_SIZE, SHA_256, expiryDate, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SOFTWARE_MODULE_NAME);
        Version sourceVersion1 = testdataFactory.createVersion(softwareModule.getId(), SOURCE_VERSION + 1);
        Version sourceVersion2 = testdataFactory.createVersion(softwareModule.getId(), SOURCE_VERSION + 2);
        Version version2 = testdataFactory.createVersion(softwareModule.getId(), TARGET_VERSION);
        List<Integer> sourceVersionList = List.of(Math.toIntExact(sourceVersion1.getId()), Math.toIntExact(sourceVersion2.getId()));
        var association = getSoftwareModuleArtifactBindingRequest(Math.toIntExact(softwareModule.getId()), sourceVersionList, Math.toIntExact(version2.getId()));

        mvc.perform(post(MgmtRestConstants.BASE_V1_REQUEST_MAPPING_TENANT + ARTIFACTS_PATH + artifact.getId() + SW_MODULE_PATH, "1").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(association))).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given a delta artifact association, when the same source and target version is provided, then it should return a bad request.")
    void givenDeltaArtifactAssociationWhenSameSourceAndTargetVersionThenReturnBadRequest() throws Exception {
        var expiryDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate(ARTIFACT_FILE_NAME, FileType.DELTA, FILE_DESCRIPTION, FILE_SIZE, SHA_256, expiryDate, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SOFTWARE_MODULE_NAME);
        Version version = testdataFactory.createVersion(softwareModule.getId(), SOURCE_VERSION);
        var association = getSoftwareModuleArtifactBindingRequest(Math.toIntExact(softwareModule.getId()), List.of(Math.toIntExact(version.getId())), Math.toIntExact(version.getId()));

        mvc.perform(post(MgmtRestConstants.BASE_V1_REQUEST_MAPPING_TENANT + ARTIFACTS_PATH + artifact.getId() + SW_MODULE_PATH, "1").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(association))).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given a delta artifact association, when the software module and versions are not linked, then it should return not found.")
    void givenDeltaArtifactAssociationWhenSoftwareModuleAndVersionsAreNotLinkedThenReturnNotFound() throws Exception {
        MgmtArtifactsRequest artifactRequestUsingFileUrl = createArtifactRequestUsingFileUrl(TEST_SHA256, TEST_FILE_NAME, FileType.DELTA);

        MvcResult mvcResult = mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(new ObjectMapper().writeValueAsString(artifactRequestUsingFileUrl)).
                        contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated()).andReturn();
        JsonNode response = new ObjectMapper().readTree(mvcResult.getResponse().getContentAsString());
        long id = response.path("id").asLong();

        SoftwareModule softwareModule1 = testdataFactory.createSoftwareModule(SOFTWARE_MODULE_NAME + 1);
        Version sourceVersion = testdataFactory.createVersion(softwareModule1.getId(), SOURCE_VERSION);
        Version targetVersion = testdataFactory.createVersion(softwareModule1.getId(), TARGET_VERSION);
        SoftwareModule softwareModule2 = testdataFactory.createSoftwareModule(SOFTWARE_MODULE_NAME + 2);
        var association = getSoftwareModuleArtifactBindingRequest(Math.toIntExact(softwareModule2.getId()), List.of(Math.toIntExact(sourceVersion.getId())), Math.toIntExact(targetVersion.getId()));

        mvc.perform(post(MgmtRestConstants.BASE_V1_REQUEST_MAPPING_TENANT + ARTIFACTS_PATH + id + SW_MODULE_PATH, "1").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(association))).andExpect(status().isNotFound());
    }

    @Test
    @Description("Given a delta artifact and software module details, when a duplicate association is created, then it should return a bad request.")
    void givenDeltaArtifactAndSoftwareModuleDetailsWhenDuplicateAssociationThenReturnBadRequest() throws Exception {
        var expiryDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate(ARTIFACT_FILE_NAME, FileType.DELTA, FILE_DESCRIPTION, FILE_SIZE, SHA_256, expiryDate, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SOFTWARE_MODULE_NAME);
        Version sourceVersion1 = testdataFactory.createVersion(softwareModule.getId(), SOURCE_VERSION + 1);
        Version targetVersion1 = testdataFactory.createVersion(softwareModule.getId(), TARGET_VERSION + 1);
        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact).softwareModule((JpaSoftwareModule) softwareModule).sourceVersion((JpaVersion) sourceVersion1).targetVersion((JpaVersion) targetVersion1).build();
        Set<ArtifactSoftwareModuleAssociation> associationList = new HashSet<>();
        associationList.add(association);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList);

        Version sourceVersion2 = testdataFactory.createVersion(softwareModule.getId(), SOURCE_VERSION + 2);
        Version targetVersion2 = testdataFactory.createVersion(softwareModule.getId(), TARGET_VERSION + 2);
        SoftwareModuleArtifactBindingRequest duplicateAssociation = getSoftwareModuleArtifactBindingRequest(Math.toIntExact(softwareModule.getId()), Collections.singletonList(Math.toIntExact(sourceVersion2.getId())), Math.toIntExact(targetVersion2.getId()));
        mvc.perform(post(MgmtRestConstants.BASE_V1_REQUEST_MAPPING_TENANT + ARTIFACTS_PATH + artifact.getId() + SW_MODULE_PATH, "1").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(duplicateAssociation))).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given a delta artifact association, when duplicate source and target version combination for same software module is provided, then it should return a bad request.")
    void givenDeltaArtifactAssociationWhenDuplicateSourceAndTargetVersionCombinationForSameSoftwareModuleThenReturnBadRequest() throws Exception {
        var expiryDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();
        Artifacts artifact1 = testdataFactory.createArtifactsWithExpiryDate(ARTIFACT_FILE_NAME + 1, FileType.DELTA, FILE_DESCRIPTION, FILE_SIZE, SHA_256, expiryDate, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SOFTWARE_MODULE_NAME);
        Version sourceVersion = testdataFactory.createVersion(softwareModule.getId(), SOURCE_VERSION);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), TARGET_VERSION);
        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact1).softwareModule((JpaSoftwareModule) softwareModule).sourceVersion((JpaVersion) sourceVersion).targetVersion((JpaVersion) targetVersion).build();
        Set<ArtifactSoftwareModuleAssociation> associationList = new HashSet<>();
        associationList.add(association);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList);

        Artifacts artifact2 = testdataFactory.createArtifactsWithExpiryDate(ARTIFACT_FILE_NAME + 2, FileType.DELTA, FILE_DESCRIPTION, FILE_SIZE, SHA_256, expiryDate, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        mvc.perform(post(MgmtRestConstants.BASE_V1_REQUEST_MAPPING_TENANT + ARTIFACTS_PATH + artifact2.getId() + SW_MODULE_PATH, "1").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(getSoftwareModuleArtifactBindingRequest(Math.toIntExact(softwareModule.getId()), Collections.singletonList(Math.toIntExact(sourceVersion.getId())), Math.toIntExact(targetVersion.getId()))))).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given a full artifact and software module details, when the source version is null, then it should return success.")
    void givenFullArtifactAndSoftwareModuleDetailsWhenNullSourceVersionThenReturnSuccess() throws Exception {
        MgmtArtifactsRequest artifactRequestUsingFileUrl = createArtifactRequestUsingFileUrl(TEST_SHA256, TEST_FILE_NAME, FileType.FULL);

        MvcResult mvcResult = mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(new ObjectMapper().writeValueAsString(artifactRequestUsingFileUrl)).
                        contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated()).andReturn();
        JsonNode response = new ObjectMapper().readTree(mvcResult.getResponse().getContentAsString());
        long id = response.path("id").asLong();

        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SOFTWARE_MODULE_NAME);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), TARGET_VERSION);

        mvc.perform(post(MgmtRestConstants.BASE_V1_REQUEST_MAPPING_TENANT + ARTIFACTS_PATH + id + SW_MODULE_PATH, "1").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(getSoftwareModuleArtifactBindingRequest(Math.toIntExact(softwareModule.getId()), null, Math.toIntExact(targetVersion.getId()))))).andExpect(status().isOk());
        Optional<ArtifactSoftwareModuleAssociation> artifactSoftwareModuleAssociation = artifactsManagement.findArtifactSoftwareModuleAssociation(softwareModule.getId(), null, targetVersion.getId(), id);
        assertTrue(artifactSoftwareModuleAssociation.isPresent());
    }

    @Test
    @Description("Given a full artifact and software module details, when duplicate source and target versions are provided, then it should return a bad request.")
    void givenOneFullArtifactAndSoftwareModuleDetailsWhenDuplicateSourceAndTargetVersionThenReturnBadRequest() throws Exception {
        var expiryDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate(ARTIFACT_FILE_NAME + 1, FileType.FULL, FILE_DESCRIPTION, FILE_SIZE, SHA_256, expiryDate, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SOFTWARE_MODULE_NAME);
        Version sourceVersion = testdataFactory.createVersion(softwareModule.getId(), SOURCE_VERSION);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), TARGET_VERSION);
        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact).softwareModule((JpaSoftwareModule) softwareModule).sourceVersion((JpaVersion) sourceVersion).targetVersion((JpaVersion) targetVersion).build();
        Set<ArtifactSoftwareModuleAssociation> associationList = new HashSet<>();
        associationList.add(association);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList);

        mvc.perform(post(MgmtRestConstants.BASE_V1_REQUEST_MAPPING_TENANT + ARTIFACTS_PATH + artifact.getId() + SW_MODULE_PATH, "1").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(getSoftwareModuleArtifactBindingRequest(Math.toIntExact(softwareModule.getId()), Collections.singletonList(Math.toIntExact(sourceVersion.getId())), Math.toIntExact(targetVersion.getId()))))).andExpect(status().isBadRequest());
    }

    @Description("Given a full artifact and software module details, when duplicate source and target versions are provided, then it should only insert unique data and returns success.")
    void givenFullArtifactAndSoftwareModuleDetailsWhenDuplicateSourceAndTargetVersionThenInsertUniqueDataAndReturnSuccess() throws Exception {
        var expiryDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate(ARTIFACT_FILE_NAME + 1, FileType.FULL, FILE_DESCRIPTION, FILE_SIZE, SHA_256, expiryDate, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        JpaArtifacts artifactsStatus = (JpaArtifacts) artifact;
        artifactsStatus.setArtifactStatus("ACTIVE");

        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SOFTWARE_MODULE_NAME);
        Version sourceVersion1 = testdataFactory.createVersion(softwareModule.getId(), SOURCE_VERSION + 1);
        Version sourceVersion2 = testdataFactory.createVersion(softwareModule.getId(), SOURCE_VERSION + 2);
        Version sourceVersion3 = testdataFactory.createVersion(softwareModule.getId(), SOURCE_VERSION + 3);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), TARGET_VERSION);
        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact).softwareModule((JpaSoftwareModule) softwareModule).sourceVersion((JpaVersion) sourceVersion1).targetVersion((JpaVersion) targetVersion).build();
        Set<ArtifactSoftwareModuleAssociation> associationList = new HashSet<>();
        associationList.add(association);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList);
        testdataFactory.addTenantConfigurations(TenantConfigurationProperties.TenantConfigurationKey.DEFAULT_CDN, TEST_CDN);

        mvc.perform(post(MgmtRestConstants.BASE_V1_REQUEST_MAPPING_TENANT + ARTIFACTS_PATH + artifact.getId() + SW_MODULE_PATH, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(getSoftwareModuleArtifactBindingRequest(Math.toIntExact(softwareModule.getId()), List.of(Math.toIntExact(sourceVersion1.getId()), Math.toIntExact(sourceVersion2.getId()), Math.toIntExact(sourceVersion3.getId())), Math.toIntExact(targetVersion.getId())))))
                .andExpect(status().isOk());
        Assertions.assertEquals(3, artifactsManagement.artifactSoftwareModuleAssociationCount());
    }

    @Test
    @Description("Given a delta type artifact association, when the source version is null, then it should return a bad request.")
    void givenDeltaTypeArtifactAssociationWhenSourceVersionIsNullThenReturnBadRequest() throws Exception {
        var expiryDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate(ARTIFACT_FILE_NAME, FileType.DELTA, FILE_DESCRIPTION, FILE_SIZE, SHA_256, expiryDate, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SOFTWARE_MODULE_NAME);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), TARGET_VERSION);
        var association = getSoftwareModuleArtifactBindingRequest(Math.toIntExact(softwareModule.getId()), null, Math.toIntExact(targetVersion.getId()));

        mvc.perform(post(MgmtRestConstants.BASE_V1_REQUEST_MAPPING_TENANT + ARTIFACTS_PATH + artifact.getId() + SW_MODULE_PATH, "1").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(association))).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given a delta artifact association, when the source version is empty, then it should return a bad request.")
    void givenDeltaArtifactAssociationWhenSourceVersionIsEmptyThenReturnBadRequest() throws Exception {
        var expiryDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate(ARTIFACT_FILE_NAME, FileType.DELTA, FILE_DESCRIPTION, FILE_SIZE, SHA_256, expiryDate, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SOFTWARE_MODULE_NAME);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), TARGET_VERSION);
        var association = getSoftwareModuleArtifactBindingRequest(Math.toIntExact(softwareModule.getId()), Collections.emptyList(), Math.toIntExact(targetVersion.getId()));

        mvc.perform(post(MgmtRestConstants.BASE_V1_REQUEST_MAPPING_TENANT + ARTIFACTS_PATH + artifact.getId() + SW_MODULE_PATH, "1").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(association))).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given a delta artifact association, when the target version is null, then it should return a bad request.")
    void givenDeltaArtifactAssociationWhenTargetVersionIsNullThenReturnBadRequest() throws Exception {
        var expiryDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate(ARTIFACT_FILE_NAME, FileType.DELTA, FILE_DESCRIPTION, FILE_SIZE, SHA_256, expiryDate, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SOFTWARE_MODULE_NAME);
        Version sourceVersion = testdataFactory.createVersion(softwareModule.getId(), SOURCE_VERSION);

        var association = getSoftwareModuleArtifactBindingRequest(Math.toIntExact(softwareModule.getId()), Collections.singletonList(Math.toIntExact(sourceVersion.getId())), null); // Missing targetVersion

        mvc.perform(post(MgmtRestConstants.BASE_V1_REQUEST_MAPPING_TENANT + ARTIFACTS_PATH + artifact.getId() + SW_MODULE_PATH, "1").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(association))).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given an invalid artifact ID, when creating an association, then it should return not found.")
    void shouldReturnNotFoundWhenArtifactIdIsInvalid() throws Exception {
        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SOFTWARE_MODULE_NAME);
        Version sourceVersion = testdataFactory.createVersion(softwareModule.getId(), SOURCE_VERSION);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), TARGET_VERSION);
        var association = getSoftwareModuleArtifactBindingRequest(Math.toIntExact(softwareModule.getId()), Collections.singletonList(Math.toIntExact(sourceVersion.getId())), Math.toIntExact(targetVersion.getId()));
        mvc.perform(post(MgmtRestConstants.BASE_V1_REQUEST_MAPPING_TENANT + ARTIFACTS_PATH + 9999999 + SW_MODULE_PATH, "1").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(association))).andExpect(status().isNotFound());
    }

    @Test
    @Description("Given a delta artifact association, when the software module ID is invalid, then it should return not found.")
    void givenDeltaTypeArtifactAssociationWhenSoftwareModuleIDIsInvalidThenReturnNotFound() throws Exception {
        MgmtArtifactsRequest artifactRequestUsingFileUrl = createArtifactRequestUsingFileUrl(TEST_SHA256, TEST_FILE_NAME, FileType.FULL);

        MvcResult mvcResult = mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(new ObjectMapper().writeValueAsString(artifactRequestUsingFileUrl)).
                        contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated()).andReturn();
        JsonNode response = new ObjectMapper().readTree(mvcResult.getResponse().getContentAsString());
        long id = response.path("id").asLong();

        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SOFTWARE_MODULE_NAME);
        Version sourceVersion = testdataFactory.createVersion(softwareModule.getId(), SOURCE_VERSION);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), TARGET_VERSION);
        var association = getSoftwareModuleArtifactBindingRequest(-1, Collections.singletonList(Math.toIntExact(sourceVersion.getId())), Math.toIntExact(targetVersion.getId()));    // Invalid softwareModuleId

        mvc.perform(post(MgmtRestConstants.BASE_V1_REQUEST_MAPPING_TENANT + ARTIFACTS_PATH + id + SW_MODULE_PATH, "1").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(association))).andExpect(status().isNotFound());
    }

    @Test
    @Description("Given a delta artifact association, when the source version ID is invalid, then it should return not found.")
    void givenDeltaArtifactAssociationWhenSourceVersionIsInvalidThenReturnNotFound() throws Exception {
        MgmtArtifactsRequest artifactRequestUsingFileUrl = createArtifactRequestUsingFileUrl(TEST_SHA256, TEST_FILE_NAME, FileType.FULL);

        MvcResult mvcResult = mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(new ObjectMapper().writeValueAsString(artifactRequestUsingFileUrl)).
                        contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated()).andReturn();
        JsonNode response = new ObjectMapper().readTree(mvcResult.getResponse().getContentAsString());
        long id = response.path("id").asLong();

        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SOFTWARE_MODULE_NAME);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), TARGET_VERSION);
        var association = getSoftwareModuleArtifactBindingRequest(Math.toIntExact(softwareModule.getId()), Collections.singletonList(-1), Math.toIntExact(targetVersion.getId())); // Invalid sourceVersionId

        mvc.perform(post(MgmtRestConstants.BASE_V1_REQUEST_MAPPING_TENANT + ARTIFACTS_PATH + id + SW_MODULE_PATH, "1").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(association))).andExpect(status().isNotFound());
    }

    @Test
    @Description("Given a delta artifact association, when the target version ID is invalid, then it should return not found.")
    void givenDeltaArtifactAssociationWhenTargetVersionIsInvalidThenReturnNotFound() throws Exception {
        MgmtArtifactsRequest artifactRequestUsingFileUrl = createArtifactRequestUsingFileUrl(TEST_SHA256, TEST_FILE_NAME, FileType.DELTA);

        MvcResult mvcResult = mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(new ObjectMapper().writeValueAsString(artifactRequestUsingFileUrl)).
                        contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated()).andReturn();
        JsonNode response = new ObjectMapper().readTree(mvcResult.getResponse().getContentAsString());
        long id = response.path("id").asLong();

        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SOFTWARE_MODULE_NAME);
        Version sourceVersion = testdataFactory.createVersion(softwareModule.getId(), SOURCE_VERSION);

        var association = getSoftwareModuleArtifactBindingRequest(Math.toIntExact(softwareModule.getId()), Collections.singletonList(Math.toIntExact(sourceVersion.getId())), -1);

        mvc.perform(post(MgmtRestConstants.BASE_V1_REQUEST_MAPPING_TENANT + ARTIFACTS_PATH + id + SW_MODULE_PATH, "1").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(association))).andExpect(status().isNotFound());
    }

    @Test
    @Description("Given a delta artifact association, when the artifact has an expired date, then it should return a bad request.")
    void givenDeltaArtifactAssociationWhenArtifactIsExpiredThenReturnBadRequest() throws Exception {

        // Create an artifact with an expired date
        var expiryDate = Instant.now().minus(10, ChronoUnit.DAYS).getEpochSecond();
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate(ARTIFACT_FILE_NAME, FileType.DELTA, FILE_DESCRIPTION, FILE_SIZE, SHA_256, expiryDate, FileTransferStatus.UPLOADING_TO_STORAGE.toString());

        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SOFTWARE_MODULE_NAME);
        Version sourceVersion = testdataFactory.createVersion(softwareModule.getId(), SOURCE_VERSION);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), TARGET_VERSION);

        var association = getSoftwareModuleArtifactBindingRequest(Math.toIntExact(softwareModule.getId()), Collections.singletonList(Math.toIntExact(sourceVersion.getId())), Math.toIntExact(targetVersion.getId()));

        mvc.perform(post(MgmtRestConstants.BASE_V1_REQUEST_MAPPING_TENANT + ARTIFACTS_PATH + artifact.getId() + SW_MODULE_PATH, "1").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(association))).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensures retrieval of artifact information contains softwareModules with full association for an existing full artifact")
    void givenFullArtifactIdThenGetArtifactThenReturnSuccessWithFullSoftwareModuleAssociation() throws Exception {

        Path tempFile = tempDir.resolve("test_fileName_for_full_association");
        Files.createFile(tempFile);

        Artifacts file = testdataFactory.createArtifacts(tempFile.toString(), FileType.FULL, FILE_DESCRIPTION, "1231", "1231");

        SoftwareModule softwareModuleFull = testdataFactory.createSoftwareModule("testSMForFullAssociation");

        Version targetVersion = testdataFactory.createVersion(softwareModuleFull.getId(), "testTargetVersionForFullAssociation1");

        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder()
                .artifact((JpaArtifacts) file)
                .softwareModule((JpaSoftwareModule) softwareModuleFull)
                .sourceVersion((JpaVersion) targetVersion)
                .targetVersion((JpaVersion) targetVersion)
                .build();

        Set<ArtifactSoftwareModuleAssociation> iAssociationList = new HashSet<>();
        iAssociationList.add(association);

        testdataFactory.createArtifactsSoftwareModuleAssociation(iAssociationList);

        // When (Act)
        mvc.perform(get(MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING + "/" + file.getId(), 1).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(file.getId()))
                .andExpect(jsonPath(FILE_TYPE).value("FULL"))
                .andExpect(jsonPath(JSON_PATH_SW_MODULES, hasSize(1)))
                .andExpect(jsonPath(JSON_PATH_SW_MODULE_ID).value(softwareModuleFull.getId()))
                .andExpect(jsonPath(JSON_PATH_SOURCE_VERSION_FULL, hasSize(0)))
                .andExpect(jsonPath(JSON_PATH_TARGET_VERSION).value(targetVersion.getName()))
                .andExpect(jsonPath(JSON_PATH_SOURCE_VERSION_DELTA).doesNotExist());
    }

    @Test
    @Description("Ensures retrieval of artifact information contains softwareModules with full association list for an existing full artifact")
    void givenFullArtifactIdThenGetArtifactThenReturnSuccessWithFullSoftwareModuleAssociationList() throws Exception {

        Path tempFile = tempDir.resolve("test_fileName_for_full_associations");
        Files.createFile(tempFile);

        Artifacts file = testdataFactory.createArtifacts(tempFile.toString(), FileType.FULL, FILE_DESCRIPTION, "1231", "1231");

        SoftwareModule softwareModuleFull = testdataFactory.createSoftwareModule("testSMForFullAssociations");

        Version targetVersion = testdataFactory.createVersion(softwareModuleFull.getId(), "testTargetVersionForFullAssociations");
        Version sourceVersion = testdataFactory.createVersion(softwareModuleFull.getId(), "testSourceVersionForFullAssociations");


        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder()
                .artifact((JpaArtifacts) file)
                .softwareModule((JpaSoftwareModule) softwareModuleFull)
                .sourceVersion((JpaVersion) sourceVersion)
                .targetVersion((JpaVersion) targetVersion)
                .build();

        Set<ArtifactSoftwareModuleAssociation> iAssociationList = new HashSet<>();
        iAssociationList.add(association);

        testdataFactory.createArtifactsSoftwareModuleAssociation(iAssociationList);

        // When (Act)
        mvc.perform(get(MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING + "/" + file.getId(), 1).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(file.getId()))
                .andExpect(jsonPath(FILE_TYPE).value("FULL"))
                .andExpect(jsonPath(JSON_PATH_SW_MODULES, hasSize(1)))
                .andExpect(jsonPath(JSON_PATH_SW_MODULE_ID).value(softwareModuleFull.getId()))
                .andExpect(jsonPath(JSON_PATH_SOURCE_VERSION_FULL, hasSize(1)))
                .andExpect(jsonPath("$.softwareModules[0].sourceVersionsForFull[0]").value(sourceVersion.getName()))
                .andExpect(jsonPath(JSON_PATH_TARGET_VERSION).value(targetVersion.getName()))
                .andExpect(jsonPath(JSON_PATH_SOURCE_VERSION_DELTA).doesNotExist());
    }

    @Test
    @Description("Ensures retrieval of artifact information contains softwareModules with delta association for an existing delta artifact")
    void givenDeltaArtifactIdThenGetArtifactThenReturnSuccessWithDeltaSoftwareModuleAssociation() throws Exception {

        Path tempFile = tempDir.resolve("test_fileName_for_delta_associations");
        Files.createFile(tempFile);

        Artifacts file = testdataFactory.createArtifacts(tempFile.toString(), FileType.DELTA, FILE_DESCRIPTION, "1231", "1231");

        SoftwareModule softwareModule1 = testdataFactory.createSoftwareModule("testSMForDeltaAssociations");

        Version targetVersion = testdataFactory.createVersion(softwareModule1.getId(), TARGET_VERSION);
        Version sourceVersion = testdataFactory.createVersion(softwareModule1.getId(), SOURCE_VERSION);


        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder()
                .artifact((JpaArtifacts) file)
                .softwareModule((JpaSoftwareModule) softwareModule1)
                .sourceVersion((JpaVersion) sourceVersion)
                .targetVersion((JpaVersion) targetVersion)
                .build();

        Set<ArtifactSoftwareModuleAssociation> iAssociationList = new HashSet<>();
        iAssociationList.add(association);

        testdataFactory.createArtifactsSoftwareModuleAssociation(iAssociationList);

        // When (Act)
        mvc.perform(get(MgmtRestConstants.BASE_V1_REQUEST_MAPPING_TENANT + ARTIFACTS_PATH + file.getId(), 1).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(file.getId()))
                .andExpect(jsonPath(FILE_TYPE).value(DELTA))
                .andExpect(jsonPath(JSON_PATH_SW_MODULES, hasSize(1)))
                .andExpect(jsonPath(JSON_PATH_SW_MODULE_ID).value(softwareModule1.getId()))
                .andExpect(jsonPath(JSON_PATH_SOURCE_VERSION_DELTA).value(sourceVersion.getName()))
                .andExpect(jsonPath(JSON_PATH_TARGET_VERSION).value(targetVersion.getName()))
                .andExpect(jsonPath(JSON_PATH_SOURCE_VERSION_FULL).doesNotExist());
    }

    @Test
    @Description("Given a full artifact association, when duplicate source and target version combination for same software module is provided, then it should return a bad request.")
    void givenOneFullArtifactAssociationWhenDuplicateSourceAndTargetVersionCombinationForSameSoftwareModuleThenReturnBadRequest() throws Exception {
        var expiryDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();
        Artifacts artifact1 = testdataFactory.createArtifactsWithExpiryDate(ARTIFACT_FILE_NAME + 21, FileType.FULL, FILE_DESCRIPTION, FILE_SIZE, SHA_256, expiryDate, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SOFTWARE_MODULE_NAME + 100);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), TARGET_VERSION + 100);
        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact1).softwareModule((JpaSoftwareModule) softwareModule).sourceVersion(null).targetVersion((JpaVersion) targetVersion).build();
        Set<ArtifactSoftwareModuleAssociation> associationList = new HashSet<>();
        associationList.add(association);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList);

        Artifacts artifact2 = testdataFactory.createArtifactsWithExpiryDate(ARTIFACT_FILE_NAME + 22, FileType.FULL, FILE_DESCRIPTION, FILE_SIZE, SHA_256, expiryDate, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        mvc.perform(post(MgmtRestConstants.BASE_V1_REQUEST_MAPPING_TENANT + ARTIFACTS_PATH + artifact2.getId() + SW_MODULE_PATH, "1").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(getSoftwareModuleArtifactBindingRequest(Math.toIntExact(softwareModule.getId()), Collections.singletonList(Math.toIntExact(targetVersion.getId())), Math.toIntExact(targetVersion.getId()))))).andExpect(status().isBadRequest());
    }



    @Test
    @Description("Test error when unlinking artifact and software module association with matching target versions")
    void givenSameTargetVersionUnlinkArtifactSoftwareModuleAssociationThenError() throws Exception {

        Path tempFile5 = tempDir.resolve("test_fileName9");
        Files.createFile(tempFile5);

        // Create an artifact
        Artifacts file = testdataFactory.createArtifacts(
                tempFile5.toString(),
                FileType.valueOf(DELTA),
                "New file description5",
                "12315",
                "12315"
        );

        // Create a software module
        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SM_TYPE_OS, "test_fileName9", false, Constants.SMF_DEFAULT_OS_KEY, SM_INSTALLER_TYPE_0);

        // Create a version
        Version version = testdataFactory.createVersionForSoftwareModule(softwareModule);
        Version differentVersion = testdataFactory.createVersion(softwareModule.getId(), RandomStringUtils.randomAlphanumeric(20), testdataFactory.getRandomInt());


        // Create a  association
        ArtifactSoftwareModuleAssociation association5 = MgmtArtifactSoftwareModuleAssociationMapper
                .toArtifactSoftwareModuleAssociationEntity(file, version, differentVersion, softwareModule);

        // Save the association
        Set<ArtifactSoftwareModuleAssociation> associationsSet = new HashSet<>();
        associationsSet.add(association5);
        testdataFactory.saveArtifactSoftwareModuleAssociation(associationsSet);

        Map<Long, Long> modules = new HashMap<>();
        modules.put(softwareModule.getId(), differentVersion.getId());
        // Create a distribution set with the software module and the matching target version
        testdataFactory.createDistributionSetWithSwModuleAndTargetVersion(modules, false, false);

        // Perform the unlink operation
        mvc.perform(delete(MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING + "/{artifactId}/softwaremodules/{softwareModuleId}", 1, file.getId(), softwareModule.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(IllegalArgumentException.class, result.getResolvedException()))
                .andExpect(result -> assertEquals("Software module is assigned to distribution set. Cannot be deleted.", result.getResolvedException().getMessage()));
    }

    @Test
    @Description("Test success when unlinking artifact and software module association with non-matching target versions")
    void givenDifferentTargetVersionUnlinkArtifactSoftwareModuleAssociation() throws Exception {

        Path tempFile5 = tempDir.resolve("test_fileName10");
        Files.createFile(tempFile5);

        // Create an artifact
        Artifacts file = testdataFactory.createArtifacts(
                tempFile5.toString(),
                FileType.valueOf(DELTA),
                "New file description6",
                "12316",
                "12316"
        );

        // Create a software module
        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SM_TYPE_OS, "test_fileName10", false, Constants.SMF_DEFAULT_OS_KEY, SM_INSTALLER_TYPE_0);

        // Create a version
        Version version = testdataFactory.createVersionForSoftwareModule(softwareModule);
        Version differentVersion = testdataFactory.createVersion(softwareModule.getId(), RandomStringUtils.randomAlphanumeric(20), testdataFactory.getRandomInt());


        // Create a association
        ArtifactSoftwareModuleAssociation association5 = MgmtArtifactSoftwareModuleAssociationMapper
                .toArtifactSoftwareModuleAssociationEntity(file, version, differentVersion, softwareModule);

        // Save the association
        Set<ArtifactSoftwareModuleAssociation> associationsSet = new HashSet<>();
        associationsSet.add(association5);
        testdataFactory.saveArtifactSoftwareModuleAssociation(associationsSet);

        Map<Long, Long> modules = new HashMap<>();
        modules.put(softwareModule.getId(), version.getId());
        // Create a distribution set with the software module and the non-matching target version
        // Perform the unlink operation
        mvc.perform(delete(MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING + "/{artifactId}/softwaremodules/{softwareModuleId}", 1, file.getId(), softwareModule.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    private SoftwareModuleArtifactBindingRequest getSoftwareModuleArtifactBindingRequest(Integer moduleId, List<Integer> sourceVersion, Integer targetVersion) {
        return SoftwareModuleArtifactBindingRequest.builder()
                .softwareModuleId(moduleId)
                .sourceVersion(sourceVersion)
                .targetVersion(targetVersion)
                .build();
    }

    @Test
    @Description("Given an artifact ID of type FULL, when the artifact is fetched, then it should be returned successfully.")
    void givenFullTypeArtifactsWhenGetArtifactsThenReturnSuccess() throws Exception {
        MgmtArtifactsRequest artifactRequestUsingFileUrl = createArtifactRequestUsingFileUrl(TEST_SHA256, TEST_FILE_NAME, FileType.FULL);

        MvcResult mvcResult = mvc.perform(post(ARTIFACTS_ENDPOINT_FILEURL, TENANT_ID).content(new ObjectMapper().writeValueAsString(artifactRequestUsingFileUrl)).
                        contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated()).andReturn();
        JsonNode response = new ObjectMapper().readTree(mvcResult.getResponse().getContentAsString());
        long id = response.path("id").asLong();

        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SOFTWARE_MODULE_NAME);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), TARGET_VERSION);
        var association = getSoftwareModuleArtifactBindingRequest(Math.toIntExact(softwareModule.getId()), null, Math.toIntExact(targetVersion.getId()));

        mvc.perform(post(MgmtRestConstants.BASE_V1_REQUEST_MAPPING_TENANT + ARTIFACTS_PATH + id + SW_MODULE_PATH, "1").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(association))).andExpect(status().isOk());

        mvc.perform(get(MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING + "/" + id, 1).contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath(FILE_TYPE).value("FULL"))
                .andExpect(jsonPath(JSON_PATH_SW_MODULES, hasSize(1)))
                .andExpect(jsonPath(JSON_PATH_SW_MODULE_ID).value(softwareModule.getId()))
                .andExpect(jsonPath(JSON_PATH_SOURCE_VERSION_FULL, hasSize(0)))
                .andExpect(jsonPath(JSON_PATH_TARGET_VERSION).value(targetVersion.getName()))
                .andExpect(jsonPath(JSON_PATH_SOURCE_VERSION_DELTA).doesNotExist());
    }

    @Test
    @Description("Given an artifact linked to a software module, when unlinking by scomo name, then the operation returns 200 OK")
    void givenArtifactLinkedToSoftwareModule_whenUnlinkingByScomoName_thenReturnsOk() throws Exception {

        Path tempFile5 = tempDir.resolve("test_fileName11");
        Files.createFile(tempFile5);

        // Create an artifact
        Artifacts file = testdataFactory.createArtifacts(
                tempFile5.toString(),
                FileType.valueOf(DELTA),
                "New file description6",
                "12316",
                "12316"
        );

        // Create a software module
        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(SM_TYPE_OS, "test_fileName11", false, Constants.SMF_DEFAULT_OS_KEY, SM_INSTALLER_TYPE_0);

        // Create a version
        Version version = testdataFactory.createVersionForSoftwareModule(softwareModule);


        // Create a association
        ArtifactSoftwareModuleAssociation association5 = MgmtArtifactSoftwareModuleAssociationMapper
                .toArtifactSoftwareModuleAssociationEntity(file, version, version, softwareModule);

        // Save the association
        Set<ArtifactSoftwareModuleAssociation> associationsSet = new HashSet<>();
        associationsSet.add(association5);
        testdataFactory.saveArtifactSoftwareModuleAssociation(associationsSet);

        Map<Long, Long> modules = new HashMap<>();
        modules.put(softwareModule.getId(), version.getId());
        // Perform the unlink operation
        mvc.perform(delete(MgmtRestConstants.UNLINK_ARTIFACT_SCOMO_V1_REQUEST_MAPPING, 1, file.getId(), softwareModule.getName()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("Given when an Artifact is in ACTIVE state, software modules with scomo name association can be created")
    void givenArtifactActiveWhenAssociateScomoThenSuccess() throws Exception {

        var result = invokeCreateArtifactViaUrlApi("https://en.xiaoai.me/pages/text-empty-pdf-generator");

        SoftwareModule softwareModule = testdataFactory.createSoftwareModule(TEST_SCOMO);
        Version sourceVersion = testdataFactory.createVersion(softwareModule.getId(), TEST_SOURCE_VERSION);
        Version targetVersion = testdataFactory.createVersion(softwareModule.getId(), TEST_TARGET_VERSION);

        ScomoArtifactBindingRequest moduleArtifactBindingRequest = ScomoArtifactBindingRequest.builder()
                .scomoId(softwareModule.getName())
                .sourceVersion(List.of(sourceVersion.getId().intValue()))
                .targetVersion(targetVersion.getId().intValue())
                .build();

        mvc.perform(post(MgmtRestConstants.CREATE_ARTIFACTS_SCOMOS_ASSOCIATION_V1_REQUEST_MAPPING, 1L, result.getArtifactId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(moduleArtifactBindingRequest)))
                .andExpect(status().isOk());
    }

}
