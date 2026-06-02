package org.eclipse.hawkbit.repository.jpa;

import org.cosmos.models.sqs.FileType;
import org.eclipse.hawkbit.repository.jpa.service.EspFileRemovalService;
import org.eclipse.hawkbit.repository.jpa.service.FileRemovalService;
import org.eclipse.hawkbit.repository.jpa.service.FileRemovalServiceFactory;
import org.eclipse.hawkbit.repository.jpa.service.RspFileRemovalService;
import org.eclipse.hawkbit.repository.jpa.service.ArtifactsFileRemovalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

 class FileRemovalServiceFactoryTest {

    private RspFileRemovalService rspFileRemovalService;
    private EspFileRemovalService espFileRemovalService;
    private ArtifactsFileRemovalService artifactsFileRemovalService;
    private FileRemovalServiceFactory factory;

    @BeforeEach
     void setUp() {
        rspFileRemovalService = mock(RspFileRemovalService.class);
        espFileRemovalService = mock(EspFileRemovalService.class);
        artifactsFileRemovalService = mock(ArtifactsFileRemovalService.class);

        when(rspFileRemovalService.getFileType()).thenReturn(FileType.RSP);
        when(espFileRemovalService.getFileType()).thenReturn(FileType.ESP);
        when(artifactsFileRemovalService.getFileType()).thenReturn(FileType.ARTIFACT);

        factory = new FileRemovalServiceFactory(List.of(rspFileRemovalService, espFileRemovalService, artifactsFileRemovalService));
        factory.init();
    }

    @Test
     void testGetInstanceForRsp() {
        FileRemovalService service = FileRemovalServiceFactory.getInstance(FileType.RSP);
        assertNotNull(service);
        assertEquals(rspFileRemovalService, service);
    }

    @Test
     void testGetInstanceForEsp() {
        FileRemovalService service = FileRemovalServiceFactory.getInstance(FileType.ESP);
        assertNotNull(service);
        assertEquals(espFileRemovalService, service);
    }

    @Test
    void testGetInstanceForArtifact() {
        FileRemovalService service = FileRemovalServiceFactory.getInstance(FileType.ARTIFACT);
        assertNotNull(service);
        assertEquals(artifactsFileRemovalService, service);
    }


} 
