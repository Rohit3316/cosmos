package org.eclipse.hawkbit.repository.jpa;

import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.sns.SnsPublishable;
import org.cosmos.sns.services.SnsService;
import org.eclipse.hawkbit.api.ArtifactUrlHandlerProperties;
import org.eclipse.hawkbit.artifact.repository.ArtifactFilesystemProperties;
import org.eclipse.hawkbit.repository.jpa.service.ArtifactsFileRemovalService;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.validation.ValidationException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArtifactsFileRemovalServiceTest {

    @Mock
    private ArtifactFilesystemProperties artifactFilesystemProperties;

    @Mock
    private ArtifactUrlHandlerProperties artifactUrlHandlerProperties;


    @Mock
    private ArtifactsRepository artifactsRepository;

    @Mock
    private SnsService<SnsPublishable> snsService;

    @InjectMocks
    private ArtifactsFileRemovalService artifactsFileRemovalService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);




        ArtifactUrlHandlerProperties.S3 s3 = mock(ArtifactUrlHandlerProperties.S3.class);
        ArtifactUrlHandlerProperties.Cdn cdn = mock(ArtifactUrlHandlerProperties.Cdn.class);
        when(artifactUrlHandlerProperties.getS3()).thenReturn(s3);
        when(artifactUrlHandlerProperties.getCdn()).thenReturn(cdn);
        ArtifactFilesystemProperties.S3bucket s3bucket = mock(ArtifactFilesystemProperties.S3bucket.class);
        when(artifactFilesystemProperties.getS3bucket()).thenReturn(s3bucket);
        when(s3bucket.getName()).thenReturn("dummyBucketName");

     }

    @Test
    void testRemoveFileFromStorage_FileAlreadyDeleted() {
        Long fileId = 1L;
        Artifacts artifact = mock(Artifacts.class);
        when(artifact.getFileStatus()).thenReturn(FileTransferStatus.STORAGE_DELETE_SUCCESSFUL);
        when(artifactsRepository.getArtifactsById(fileId)).thenReturn(Optional.of(artifact));

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            artifactsFileRemovalService.removeFileFromStorage(fileId);
        });

        assertEquals("File is already deleted from storage", exception.getMessage());
    }

}