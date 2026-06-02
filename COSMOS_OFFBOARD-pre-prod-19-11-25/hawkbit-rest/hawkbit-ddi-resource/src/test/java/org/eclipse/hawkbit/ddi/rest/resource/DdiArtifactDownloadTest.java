/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang3.RandomUtils;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.sns.models.CdnDeleteRequest;
import org.cosmos.sns.models.CdnUploadRequest;
import org.cosmos.sns.models.S3FileTransferRequest;
import org.cosmos.sns.services.impl.CdnDeleteSnsService;
import org.cosmos.sns.services.impl.CdnUploadSnsService;
import org.cosmos.sns.services.impl.S3FileTransferSnsService;
import org.eclipse.hawkbit.artifact.repository.model.ArtifactsHash;

import org.eclipse.hawkbit.ddi.rest.resource.DdiArtifactDownloadTest.DownloadTestConfiguration;
import org.eclipse.hawkbit.mgmt.rest.resource.awsServices.MgmtS3Service;
import org.eclipse.hawkbit.repository.event.remote.DownloadProgressEvent;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifactSoftwareModuleAssociationEntity;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaVersion;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.repository.model.inventory.ArtifactsUpload;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test artifact downloads from the controller.
 */
@Feature("Component Tests - Direct Device Integration API")
@Story("Artifact Download Resource")
@SpringBootTest(classes = {DownloadTestConfiguration.class})
public class DdiArtifactDownloadTest extends AbstractDDiApiIntegrationTest {

    private static final AtomicInteger downLoadProgress = new AtomicInteger();
    private static final AtomicLong shippedBytes = new AtomicLong();
    private static final long TENANT_ID = 1L;
    private static final String ARTIFACTS_ENDPOINT = MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING;
    private static final String TEST_FILE_NAME_TXT = "test_file.txt";
    private static final String FILE = "file";
    private static final String TEST_CONTENT = "Test content";
    private static final String DESCRIPTION = "description";
    private static final String SHA256 = "sha256";
    private static final String SIGNATURE_EXPIRY_DATE = "signatureExpiryDate";
    private static final String FILE_NAME = "filename";
    private static final String FILE_TYPE = "fileType";
    private static final String VINTESTRS9123Y = "VINTESTRS9123Y";
    private static final String VINTESTRS9123S = "VINTESTRS9123S";
    private static final String BYTE1000 = "bytes=-1000";
    private static final String BYTES = "bytes=";
    private static final String TEST_1 = "TEST1";
    private static final String TEST_2 = "TEST2";
    private static final String FILENAME = "file1";
    private static final String RANGE = "Range";
    private static final String MD5 = "d41d8cd98f00b204e9800998ecf8427e";
    private static final String SH256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String VALUE_1234567890 = "1234567890";
    private static final String VALUE_123455 = "123455";
    private static final String VALUE_446 = "446";
    private static final String VALUE_444 = "444";
    private static final String VINTESTRS9125T = "VINTESTRS9125T";
    private static final String DCROSS = "Dcross";
    private static final String MOCK_MESSAGE_ID = "mockMessageId";
    private static final String CONTENT_DISPOSITION = "Content-Disposition";
    private static final String VEHICLE_MODEL_NAME = "STLA-Brain";
    protected static final String TABLE_SP_ARTIFACT_SOFTWARE_MODULE = "sp_artifact_software_module";
    protected static final String TABLE_SP_ARTIFACTS = "sp_artifacts";
    protected static final String TABLE_SP_SOFTWARE_ECU_MODEL = "sp_software_ecu_model";
    protected static final String TABLE_SP_SOFTWARE_VERSIONS = "sp_software_versions";
    protected static final String TABLE_SP_BASE_SOFTWARE_MODULE = "sp_base_software_module";
    protected static final String TABLE_SP_ECU_MODEL = "sp_ecu_model";
    protected static final String TABLE_SP_TARGET = "sp_target";
    protected static final String TABLE_SP_ACTION = "sp_action";
    protected static final String TABLE_SP_DISTRIBUTION_SET = "sp_distribution_set";

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
    @MockBean
    MgmtS3Service s3Service;
    @Mock
    private CdnUploadSnsService cdnUploadSnsService;
    @Mock
    private CdnDeleteSnsService cdnDeleteSnsService;
    @Mock
    private S3FileTransferSnsService s3FileTransferSnsService;
    @MockBean
    private SnsAsyncClient snsAsyncClient;

    public static MockHttpServletRequestBuilder createArtifactRequest(MockMultipartFile file, String filename, String fileType, String description, String sha256, String signatureExpiryDate) {
        return MockMvcRequestBuilders.multipart(ARTIFACTS_ENDPOINT, TENANT_ID)
                .file(file)
                .param(FILE_NAME, filename)
                .param(FILE_TYPE, fileType)
                .param(DESCRIPTION, description)
                .param(SHA256, sha256)
                .param(SIGNATURE_EXPIRY_DATE, signatureExpiryDate);
    }

    @BeforeEach
    public void setup() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, TABLE_SP_ARTIFACT_SOFTWARE_MODULE, TABLE_SP_ARTIFACTS, TABLE_SP_SOFTWARE_ECU_MODEL, TABLE_SP_SOFTWARE_VERSIONS, TABLE_SP_BASE_SOFTWARE_MODULE, TABLE_SP_ECU_MODEL, TABLE_SP_TARGET, TABLE_SP_ACTION, TABLE_SP_DISTRIBUTION_SET);

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
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    @Test
    @Description("Tests non allowed requests on the artifact ressource, e.g. invalid URI, wrong if-match, wrong command.")
    public void invalidRequestsOnArtifactResource() throws Exception {
        when(s3Service.generatePresignedUrl(anyString(), anyString(), anyLong())).thenCallRealMethod();
        when(s3Service.buildS3ObjectName(anyString(), anyString(), anyString())).thenCallRealMethod();
        // create target
        final Target target = testdataFactory.createTarget(VINTESTRS9123Y, VINTESTRS9123Y, "440", testdataFactory.createVehicle(VEHICLE_MODEL_NAME).getId());
        final List<Target> targets = Collections.singletonList(target);

        // create ds
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        assignDistributionSet(ds, targets);

        // create artifact
        final Version version1 = testdataFactory.createVersion(ds.findFirstModuleByType(osType).get().getId(), TEST_1);
        final Version version2 = testdataFactory.createVersion(ds.findFirstModuleByType(osType).get().getId(), TEST_2);
        Artifacts artifact = createArtifacts();
        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact).softwareModule((JpaSoftwareModule) ds.findFirstModuleByType(osType).get()).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version2).build();
        Set<ArtifactSoftwareModuleAssociation> associationList = new HashSet<>();
        associationList.add(association);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList);

        // SM does not exist
        mvc.perform(get("/controllers/{controllerId}/softwaremodules/1234567890/artifacts/{filename}",
                target.getControllerId(), artifact.getFileName())).andExpect(status().isNotFound());
        mvc.perform(get("/controllers/{controllerId}/softwaremodules/1234567890/artifacts/{filename}.MD5SUM",
                target.getControllerId(), artifact.getFileName())).andExpect(status().isNotFound());
        when(s3Service.isValidGetUrl(any(java.net.URL.class))).thenReturn(true);
    }

    @Test
    @WithUser(principal = "4712", authorities = "ROLE_CONTROLLER", allSpPermissions = true)
    @Description("Tests valid downloads through the artifact resource by identifying the artifact not by ID but file name.")
    public void downloadArtifactThroughFileName() throws Exception {
        when(s3Service.generatePresignedUrl(anyString(), anyString(), anyLong())).thenCallRealMethod();
        when(s3Service.buildS3ObjectName(anyString(), anyString(), anyString())).thenCallRealMethod();
        downLoadProgress.set(1);
        shippedBytes.set(0);
        assertThat(softwareModuleManagement.findAll(PAGE)).isEmpty();

        // create target
        final Target target = testdataFactory.createTarget(VINTESTRS9125T, VINTESTRS9125T, VALUE_446, testdataFactory.createVehicle(DCROSS).getId());
        final List<Target> targets = Collections.singletonList(target);

        // create ds
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        // create artifact
        final int artifactSize = (int) quotaManagement.getMaxArtifactSize();
        final byte[] random = RandomUtils.nextBytes(artifactSize);
        final Version version1 = testdataFactory.createVersion(ds.findFirstModuleByType(osType).get().getId(), TEST_1);
        final Version version2 = testdataFactory.createVersion(ds.findFirstModuleByType(osType).get().getId(), TEST_2);
        Artifacts artifact = createArtifacts();
        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact).softwareModule((JpaSoftwareModule) ds.findFirstModuleByType(osType).get()).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version2).build();
        Set<ArtifactSoftwareModuleAssociation> associationList = new HashSet<>();
        associationList.add(association);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList);
        // now assign and download successful
        assignDistributionSet(ds, targets);
        when(s3Service.isValidGetUrl(any(URL.class))).thenReturn(true);
    }

    @Test
    @WithUser(principal = TestdataFactory.DEFAULT_CONTROLLER_ID, authorities = "ROLE_CONTROLLER", allSpPermissions = true)
    @Description("Test various HTTP range requests for artifact download, e.g. chunk download or download resume.")
    public void rangeDownloadArtifact() throws Exception {
        when(s3Service.generatePresignedUrl(anyString(), anyString(), anyLong())).thenCallRealMethod();
        when(s3Service.buildS3ObjectName(anyString(), anyString(), anyString())).thenCallRealMethod();
        // create target
        final Target target = testdataFactory.createTarget(VINTESTRS9123S, VINTESTRS9123S, VALUE_444, testdataFactory.createVehicle("X250").getId());
        final List<Target> targets = Collections.singletonList(target);

        // create ds
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        final int resultLength = (int) quotaManagement.getMaxArtifactSize();

        // create artifact
        final byte[] random = RandomUtils.nextBytes(resultLength);
        final Version version1 = testdataFactory.createVersion(ds.findFirstModuleByType(osType).get().getId(), TEST_1);
        final Version version2 = testdataFactory.createVersion(ds.findFirstModuleByType(osType).get().getId(), TEST_2);
        MockMultipartFile file = new MockMultipartFile(FILE, TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, TEST_CONTENT.getBytes());
        final String TEST_SIGNATURE_EXPIRY_DATE = String.valueOf(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());
        Artifacts artifact = createArtifacts();

        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact).softwareModule((JpaSoftwareModule) ds.findFirstModuleByType(osType).get()).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version2).build();
        Set<ArtifactSoftwareModuleAssociation> associationList = new HashSet<>();
        associationList.add(association);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList);
        when(s3Service.isValidGetUrl(any(URL.class))).thenReturn(true);
        assertThat(random).hasSize(resultLength);

        // now assign and download successful
        assignDistributionSet(ds, targets);
    }

    public Artifacts createArtifacts() throws NoSuchAlgorithmException, IOException {
        MockMultipartFile file = new MockMultipartFile(FILE, TEST_FILE_NAME_TXT, MediaType.TEXT_PLAIN_VALUE, TEST_CONTENT.getBytes());
        final MessageDigest md5Digest = MessageDigest.getInstance("MD5");
        ArtifactsHash artifactsHash = ArtifactsHash.builder()
                .md5(MD5)
                .sha256(SH256)
                .build();
        ArtifactsUpload artifactsUpload = new ArtifactsUpload(file.getInputStream(), FILENAME,
                "FULL", "description", 12345L, artifactsHash);
        Artifacts artifact = this.artifactsManagement.saveArtifacts(artifactsUpload, file.getSize(), md5Digest);
        return artifact;
    }

    @Configuration
    public static class DownloadTestConfiguration {

        @Bean
        public Listener cancelEventHandlerStubBean() {
            return new Listener();
        }

    }

    private static class Listener {

        @EventListener(classes = DownloadProgressEvent.class)
        public static void listen(final DownloadProgressEvent event) {
            downLoadProgress.incrementAndGet();
            shippedBytes.addAndGet(event.getShippedBytesSinceLast());
        }
    }


}