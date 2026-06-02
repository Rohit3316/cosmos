package com.stellantis.cosmos.sqs.app;

import com.stellantis.cosmos.sqs.app.service.ArtifactFileStatusManager;
import com.stellantis.cosmos.sqs.app.service.FileStatusManagement;
import com.stellantis.cosmos.sqs.app.service.core.FileStatusManagerFactory;
import io.qameta.allure.Description;
import org.cosmos.models.sqs.FileType;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileStatusManagerFactoryTest {

    private FileStatusManagerFactory factory;

    @BeforeEach
    void setUp() {
        FileStatusManagement mockManagement = mock(ArtifactFileStatusManager.class);
        when(mockManagement.getFileType()).thenReturn(FileType.ARTIFACT);
        factory = new FileStatusManagerFactorySimulator(List.of(mockManagement));
    }

    @Test
    @Description("Given a valid FileType (ARTIFACT), when getInstance is called, then it should return a non-null FileStatusManagement instance.")
    void givenValidFileType_whenGetInstance_thenReturnNonNullInstance() {
        FileStatusManagement management = factory.getInstance(FileType.ARTIFACT);
        assertNotNull(management);
    }

    @Test
    @Description("Given an invalid FileType (ESP), when getInstance is called, then it should throw an EntityNotFoundException.")
    void givenInvalidFileType_whenGetInstance_thenThrowEntityNotFoundException() {
        assertThrows(EntityNotFoundException.class, () -> factory.getInstance(FileType.ESP));
    }

    private static class FileStatusManagerFactorySimulator extends FileStatusManagerFactory{

        public FileStatusManagerFactorySimulator(List<FileStatusManagement> fileStatusManagementList) {
            super(fileStatusManagementList);
            initializer();
        }

        @Override
        protected void initializer() {
            super.initializer();
        }
    }
}
