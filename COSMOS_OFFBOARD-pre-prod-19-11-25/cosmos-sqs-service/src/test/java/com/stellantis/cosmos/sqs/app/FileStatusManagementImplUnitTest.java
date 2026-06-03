package com.stellantis.cosmos.sqs.app;

import com.stellantis.cosmos.sqs.app.service.ArtifactFileStatusManager;
import com.stellantis.cosmos.sqs.app.service.EspFileStatusManager;
import com.stellantis.cosmos.sqs.app.service.RspFileStatusManager;
import io.qameta.allure.Description;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.cosmos.models.sqs.FileType;
import org.cosmos.s3.ChecksumCalculator;
import org.cosmos.s3.S3Repository;
import org.eclipse.hawkbit.api.ArtifactUrlHandlerProperties;
import org.eclipse.hawkbit.repository.SupportPackageManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.file.supportpackage.configuration.SupportPackageUrlHandlerProperties;
import org.eclipse.hawkbit.repository.jpa.ArtifactsRepository;
import org.eclipse.hawkbit.repository.jpa.EspRepository;
import org.eclipse.hawkbit.repository.jpa.RspRepository;
import org.eclipse.hawkbit.repository.jpa.RspRolloutRepository;
import org.eclipse.hawkbit.repository.jpa.EspEcuRolloutRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.jpa.model.JpaEsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaRsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaEspEcuRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRspRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileStatusManagementImplUnitTest {

    private final ChecksumCalculator checksumCalculator = new ChecksumCalculator();
    private final SupportPackageUrlHandlerProperties supportPackageUrlHandlerProperties = new SupportPackageUrlHandlerProperties();
    private final ArtifactUrlHandlerProperties artifactUrlHandlerProperties = new ArtifactUrlHandlerProperties();
    private static final Long DEFAULT_FILE_SIZE = 10L;
    private static final String DEFAULT_TENANT = "default";

    @Mock
    private EspRepository espRepository;
    @Mock
    private RspRepository rspRepository;
    @Mock
    private ArtifactsRepository artifactsRepository;
    @Mock
    private S3Repository s3Repository;
    @Mock
    private SupportPackageManagement supportPackageManagement;

    public EspFileStatusManager getEspFileStatusManager() {
        return new EspFileStatusManager(espRepository, checksumCalculator, supportPackageUrlHandlerProperties, s3Repository, supportPackageManagement);
    }

    public ArtifactFileStatusManager getArtifactFileStatusManager() {
        return new ArtifactFileStatusManager(artifactsRepository, checksumCalculator, artifactUrlHandlerProperties, s3Repository);
    }

    public RspFileStatusManager getRspFileStatusManager() {
        return new RspFileStatusManager(rspRepository, checksumCalculator, supportPackageUrlHandlerProperties, s3Repository, supportPackageManagement);
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(s3Repository.getFileSizeFromS3(any())).thenReturn(DEFAULT_FILE_SIZE);
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(espRepository, rspRepository, artifactsRepository, s3Repository);
    }

    private JpaEsp createEsp(String fileStatus) {
        JpaEsp esp = new JpaEsp();
        esp.setId((long) (Math.random() * 1000));
        esp.setSha256Hash(UUID.randomUUID().toString().replace("-", ""));
        esp.setFileName("file_" + UUID.randomUUID() + ".esp");
        esp.setMd5Hash(UUID.randomUUID().toString().replace("-", ""));
        esp.setFileStatus(fileStatus);
        esp.setFileType(MgmtSupportPackageFileType.ADA_CERTIFICATE);
        esp.setFileSize(DEFAULT_FILE_SIZE);
        esp.setCreatedAt((long) (Math.random() * 10000));
        esp.setTenant(DEFAULT_TENANT);
        return esp;
    }

    private JpaRsp createRsp(String fileStatus) {
        JpaRsp rsp = new JpaRsp();
        rsp.setId((long) (Math.random() * 1000));
        rsp.setSha256Hash(UUID.randomUUID().toString().replace("-", ""));
        rsp.setFileName("file_" + UUID.randomUUID() + ".rsp");
        rsp.setMd5Hash(UUID.randomUUID().toString().replace("-", ""));
        rsp.setFileStatus(fileStatus);
        rsp.setFileType(MgmtSupportPackageFileType.WHATS_NEW);
        rsp.setFileSize(DEFAULT_FILE_SIZE);
        rsp.setCreatedAt((long) (Math.random() * 10000));
        rsp.setTenant(DEFAULT_TENANT);
        return rsp;
    }

    private JpaArtifacts createArtifact(String fileStatus) {
        JpaArtifacts artifact = new JpaArtifacts();
        artifact.setId((long) (Math.random() * 1000));
        artifact.setSha256Hash(UUID.randomUUID().toString().replace("-", ""));
        artifact.setFileName("file_" + UUID.randomUUID() + ".artifact");
        artifact.setMd5Hash(UUID.randomUUID().toString().replace("-", ""));
        artifact.setFileStatus(fileStatus);
        artifact.setFileType(org.cosmos.models.mgmt.FileType.FULL);
        artifact.setFileSize(DEFAULT_FILE_SIZE);
        artifact.setCreatedAt((long) (Math.random() * 10000));
        artifact.setTenant(DEFAULT_TENANT);
        return artifact;
    }

    @Test
    @Description("Given an ESP file, when the updateFileStatus method is called, then the file status should be successfully updated in the ESP repository.")
    void givenEspFile_whenUpdateFileStatus_thenUpdateSuccessfully() {

        JpaEsp esp = createEsp(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.name());
        when(espRepository.findById(esp.getId())).thenReturn(Optional.of(esp));
        EspFileStatusManager manager = getEspFileStatusManager();
        manager.updateFileStatus(esp.getId(), esp.getFileStatus(), esp.getMd5Hash(), esp.getTenant());

        verify(espRepository, times(1)).save(esp);
        verify(espRepository).save(argThat(a ->
                Objects.equals(a.getFileSize(), DEFAULT_FILE_SIZE) &&
                        esp.getFileStatus().equals(a.getSupportPackageFileStatus().name()) &&
                        esp.getMd5Hash().equals(a.getMd5Hash()) &&
                        esp.getId().equals(a.getId()) &&
                        esp.getSha256Hash().equals(a.getSha256Hash()) &&
                        esp.getFileName().equals(a.getFileName()) &&
                        esp.getTenant().equals(a.getTenant()) &&
                        esp.getFileType() == a.getFileType()
        ));

    }

    @Test
    @Description("Given an ESP file that does not exist, when the updateFileStatus method is called, then an EntityNotFoundException should be thrown.")
    void givenEspFileNotFound_whenUpdateFileStatus_thenThrowEntityNotFoundException() {

        when(espRepository.findById(1L)).thenReturn(Optional.empty());

        EspFileStatusManager manager = getEspFileStatusManager();

        JpaEsp esp = createEsp(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.name());
        assertThrows(EntityNotFoundException.class, () -> manager.updateFileStatus(esp.getId(), esp.getFileStatus(), esp.getMd5Hash(), esp.getTenant()));
        verify(espRepository, never()).save(any());
    }

    @Test
    @Description("Given an artifact file, when the updateFileStatus method is called, then the file status should be successfully updated in the artifact repository.")
    void givenArtifactFile_whenUpdateFileStatus_thenUpdateSuccessfully() {

        JpaArtifacts artifact = createArtifact(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.name());
        when(artifactsRepository.findById(artifact.getId())).thenReturn(Optional.of(artifact));

        ArtifactFileStatusManager manager = getArtifactFileStatusManager();
        manager.updateFileStatus(artifact.getId(), artifact.getFileStatus().name(), artifact.getMd5Hash(), artifact.getTenant());

        verify(artifactsRepository, times(1)).save(artifact);
        verify(artifactsRepository).save(argThat(a ->
                Objects.equals(a.getFileSize(), DEFAULT_FILE_SIZE) &&
                        artifact.getFileStatus().name().equals(a.getFileStatus().name()) &&
                        artifact.getMd5Hash().equals(a.getMd5Hash()) &&
                        artifact.getId().equals(a.getId()) &&
                        artifact.getSha256Hash().equals(a.getSha256Hash()) &&
                        artifact.getFileName().equals(a.getFileName()) &&
                        artifact.getTenant().equals(a.getTenant()) &&
                        artifact.getFileType() == a.getFileType()
        ));
    }

    @Test
    @Description("Given an artifact file that does not exist, when the updateFileStatus method is called, then an EntityNotFoundException should be thrown.")
    void givenArtifactFileNotFound_whenUpdateFileStatus_thenThrowEntityNotFoundException() {

        when(artifactsRepository.findById(1L)).thenReturn(Optional.empty());

        ArtifactFileStatusManager manager = getArtifactFileStatusManager();

        JpaArtifacts artifact = createArtifact(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.name());
        assertThrows(EntityNotFoundException.class, () -> manager.updateFileStatus(artifact.getId(), artifact.getFileStatus().name(), artifact.getMd5Hash(), artifact.getTenant()));
        verify(artifactsRepository, never()).save(any());
    }

    @Test
    @Description("Given an RSP file, when the updateFileStatus method is called, then the file status should be successfully updated in the RSP repository.")
    void givenRspFile_whenUpdateFileStatus_thenUpdateSuccessfully() {

        JpaRsp rsp = createRsp(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.name());
        when(rspRepository.findById(rsp.getId())).thenReturn(Optional.of(rsp));

        RspFileStatusManager manager = getRspFileStatusManager();
        manager.updateFileStatus(rsp.getId(), rsp.getFileStatus(), rsp.getMd5Hash(), rsp.getTenant());

        verify(rspRepository, times(1)).save(rsp);
        verify(rspRepository).save(argThat(a ->
                Objects.equals(a.getFileSize(), DEFAULT_FILE_SIZE) &&
                        rsp.getFileStatus().equals(a.getSupportPackageFileStatus().name()) &&
                        rsp.getMd5Hash().equals(a.getMd5Hash()) &&
                        rsp.getId().equals(a.getId()) &&
                        rsp.getSha256Hash().equals(a.getSha256Hash()) &&
                        rsp.getFileName().equals(a.getFileName()) &&
                        rsp.getTenant().equals(a.getTenant()) &&
                        rsp.getFileType() == a.getFileType()
        ));
    }

    @Test
    @Description("Given an RSP file that does not exist, when the updateFileStatus method is called, then an EntityNotFoundException should be thrown.")
    void givenRspFileNotFound_whenUpdateFileStatus_thenThrowEntityNotFoundException() {

        when(rspRepository.findById(1L)).thenReturn(Optional.empty());

        RspFileStatusManager manager = getRspFileStatusManager();

        JpaRsp rsp = createRsp(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.name());
        assertThrows(EntityNotFoundException.class, () -> manager.updateFileStatus(rsp.getId(), rsp.getFileStatus(), rsp.getMd5Hash(), rsp.getTenant()));
        verify(rspRepository, never()).save(any());
    }

    @Test
    @Description("Given an ESP file, when the updateFileStatus method is called, then the file status should be successfully updated in the ESP repository.")
    void givenEspFile_whenUpdateFileStatusFailureStatus_thenUpdateSuccessfully() {

        JpaEsp esp = createEsp(FileTransferStatus.CDN_UPLOAD_ERROR.name());
        when(espRepository.findById(esp.getId())).thenReturn(Optional.of(esp));

        EspFileStatusManager manager = getEspFileStatusManager();
        manager.updateFileStatus(esp.getId(), esp.getFileStatus());

        verify(espRepository, times(1)).save(esp);
        verify(espRepository).save(argThat(a ->
                Objects.equals(a.getFileSize(), DEFAULT_FILE_SIZE) &&
                        esp.getFileStatus().equals(a.getFileStatus()) &&
                        esp.getMd5Hash().equals(a.getMd5Hash()) &&
                        esp.getId().equals(a.getId()) &&
                        esp.getSha256Hash().equals(a.getSha256Hash()) &&
                        esp.getFileName().equals(a.getFileName()) &&
                        esp.getTenant().equals(a.getTenant()) &&
                        esp.getFileType() == a.getFileType()
        ));
    }

    @Test
    @Description("Given an ESP file that does not exist, when the updateFileStatus method is called, then an EntityNotFoundException should be thrown.")
    void givenEspFileNotFound_whenUpdateFileStatusFailureStatus_thenThrowEntityNotFoundException() {

        JpaEsp esp = createEsp(FileTransferStatus.CDN_UPLOAD_ERROR.name());
        when(espRepository.findById(1L)).thenReturn(Optional.empty());

        EspFileStatusManager manager = getEspFileStatusManager();

        assertThrows(EntityNotFoundException.class, () -> manager.updateFileStatus(esp.getId(), esp.getFileStatus()));
        verify(espRepository, never()).save(any());
    }

    @Test
    @Description("Given an RSP file, when the updateFileStatus method is called, then the file status should be successfully updated in the RSP repository.")
    void givenRspFile_whenUpdateFileStatusFailureStatus_thenUpdateSuccessfully() {

        JpaRsp rsp = createRsp(FileTransferStatus.CDN_UPLOAD_ERROR.name());
        when(rspRepository.findById(rsp.getId())).thenReturn(Optional.of(rsp));

        RspFileStatusManager manager = getRspFileStatusManager();
        manager.updateFileStatus(rsp.getId(), rsp.getFileStatus());
        verify(rspRepository, times(1)).save(rsp);
        verify(rspRepository).save(argThat(a ->
                Objects.equals(a.getFileSize(), DEFAULT_FILE_SIZE) &&
                        rsp.getFileStatus().equals(a.getFileStatus()) &&
                        rsp.getMd5Hash().equals(a.getMd5Hash()) &&
                        rsp.getId().equals(a.getId()) &&
                        rsp.getSha256Hash().equals(a.getSha256Hash()) &&
                        rsp.getFileName().equals(a.getFileName()) &&
                        rsp.getTenant().equals(a.getTenant()) &&
                        rsp.getFileType() == a.getFileType()
        ));
    }

    @Test
    @Description("Given an RSP file that does not exist, when the updateFileStatus method is called, then an EntityNotFoundException should be thrown.")
    void givenRspFileNotFound_whenUpdateFileStatusFailureStatus_thenThrowEntityNotFoundException() {

        when(rspRepository.findById(1L)).thenReturn(Optional.empty());

        RspFileStatusManager manager = getRspFileStatusManager();

        JpaRsp rsp = createRsp(FileTransferStatus.CDN_UPLOAD_ERROR.name());
        assertThrows(EntityNotFoundException.class, () -> manager.updateFileStatus(rsp.getId(), rsp.getFileStatus()));
        verify(rspRepository, never()).save(any());
    }

    @Test
    @Description("Given an artifact file, when the updateFileStatus method is called, then the file status should be successfully updated in the artifact repository.")
    void givenArtifactFile_whenUpdateFileStatusFailureStatus_thenUpdateSuccessfully() {

        JpaArtifacts artifact = createArtifact(FileTransferStatus.CDN_UPLOAD_ERROR.name());
        when(artifactsRepository.findById(artifact.getId())).thenReturn(Optional.of(artifact));

        ArtifactFileStatusManager manager = getArtifactFileStatusManager();
        manager.updateFileStatus(artifact.getId(), artifact.getFileStatus().name());

        verify(artifactsRepository, times(1)).save(artifact);
        verify(artifactsRepository).save(argThat(a ->
                Objects.equals(a.getFileSize(), DEFAULT_FILE_SIZE) &&
                        artifact.getFileStatus().name().equals(a.getFileStatus().name()) &&
                        artifact.getMd5Hash().equals(a.getMd5Hash()) &&
                        artifact.getId().equals(a.getId()) &&
                        artifact.getSha256Hash().equals(a.getSha256Hash()) &&
                        artifact.getFileName().equals(a.getFileName()) &&
                        artifact.getTenant().equals(a.getTenant()) &&
                        artifact.getFileType() == a.getFileType()
        ));
    }

    @Test
    @Description("Given an artifact file that does not exist, when the updateFileStatus method is called, then an EntityNotFoundException should be thrown.")
    void givenArtifactFileNotFound_whenUpdateFileStatusFailureStatus_thenThrowEntityNotFoundException() {

        when(artifactsRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        ArtifactFileStatusManager manager = getArtifactFileStatusManager();

        JpaArtifacts artifact = createArtifact(FileTransferStatus.CDN_UPLOAD_ERROR.name());
        assertThrows(EntityNotFoundException.class, () -> manager.updateFileStatus(artifact.getId(), artifact.getArtifactStatus()));
        verify(artifactsRepository, never()).save(any());
    }
}
