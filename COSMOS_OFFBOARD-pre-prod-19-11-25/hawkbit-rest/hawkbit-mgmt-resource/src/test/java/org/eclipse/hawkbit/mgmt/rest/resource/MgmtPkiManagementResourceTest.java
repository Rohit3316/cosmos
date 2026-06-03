package org.eclipse.hawkbit.mgmt.rest.resource;

import org.cosmos.models.mgmt.pki.dto.SigningCertificateConfigurationInfo;
import org.cosmos.models.mgmt.pki.dto.SigningCertificateConfigurationUpdateInfo;
import org.eclipse.hawkbit.repository.PKIManagement;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaSigningCertificateConfiguration;
import org.eclipse.hawkbit.repository.model.SigningCertificateConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Description;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MgmtPkiManagementResource}.
 * Tests the PKI signing certificate configuration management REST resource.
 */
class MgmtPkiManagementResourceTest {

    private PKIManagement pkiManagement;
    private MgmtPkiManagementResource resource;

    /**
     * Sets up mocks and resource before each test.
     */
    @BeforeEach
    void setUp() {
        pkiManagement = mock(PKIManagement.class);
        resource = new MgmtPkiManagementResource(pkiManagement);
    }

    /**
     * Tests successful addition of a signing certificate configuration.
     * Verifies that the configuration is saved and the response is correct.
     */
    @Description("Tests successful addition of a signing certificate configuration.")
    @Test
    void addSigningConfiguration_success() {
        SigningCertificateConfigurationInfo config = mock(SigningCertificateConfigurationInfo.class);
        when(config.getEcuIdIssuer()).thenReturn("issuer1");
        when(pkiManagement.existsSigningCertificateConfigurationByEcuIdIssuer("issuer1")).thenReturn(false);

        SigningCertificateConfiguration savedConfig = mock(SigningCertificateConfiguration.class);
        when(pkiManagement.addSigningCertificateConfiguration(any(JpaSigningCertificateConfiguration.class)))
                .thenReturn(savedConfig);

        ResponseEntity<SigningCertificateConfiguration> response = resource.addSigningConfiguration(config);

        assertEquals(savedConfig, response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(pkiManagement).addSigningCertificateConfiguration(any(JpaSigningCertificateConfiguration.class));
    }

    /**
     * Tests that adding a signing certificate configuration throws
     * {@link EntityAlreadyExistsException} if the entity already exists.
     * Verifies that the configuration is not saved.
     */
    @Description("Tests that adding a signing certificate configuration throws EntityAlreadyExistsException if the entity already exists.")
    @Test
    void addSigningConfiguration_entityAlreadyExists() {
        SigningCertificateConfigurationInfo config = mock(SigningCertificateConfigurationInfo.class);
        when(config.getEcuIdIssuer()).thenReturn("issuer1");
        when(pkiManagement.existsSigningCertificateConfigurationByEcuIdIssuer("issuer1")).thenReturn(true);

        assertThrows(EntityAlreadyExistsException.class, () -> resource.addSigningConfiguration(config));
        verify(pkiManagement, never()).addSigningCertificateConfiguration(any());
    }

    @Description("Tests successful retrieval of a signing certificate configuration.")
    @Test
    void getSigningConfiguration_success() {
        SigningCertificateConfiguration config = mock(SigningCertificateConfiguration.class);
        when(pkiManagement.getSigningCertificateConfiguration("issuer1")).thenReturn(config);

        ResponseEntity<SigningCertificateConfiguration> response = resource.getSigningConfiguration("issuer1");

        assertEquals(config, response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Description("Tests that retrieving a non-existent signing certificate configuration throws EntityNotFoundException.")
    @Test
    void getSigningConfiguration_notFound() {
        when(pkiManagement.getSigningCertificateConfiguration("issuer1")).thenReturn(null);

        assertThrows(EntityNotFoundException.class, () -> resource.getSigningConfiguration("issuer1"));
    }

    @Description("Tests successful retrieval of all signing certificate configurations.")
    @Test
    void getAllSigningConfigurations_success() {
        List<SigningCertificateConfiguration> configs = List.of(mock(SigningCertificateConfiguration.class));
        when(pkiManagement.getAllSigningCertificateConfigurations()).thenReturn(configs);

        ResponseEntity<List<SigningCertificateConfiguration>> response = resource.getAllSigningConfigurations();

        assertEquals(configs, response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Description("Tests successful update of a signing certificate configuration.")
    @Test
    void updateSigningConfiguration_success() {
        JpaSigningCertificateConfiguration existingConfig = mock(JpaSigningCertificateConfiguration.class);
        SigningCertificateConfigurationUpdateInfo updateConfig = mock(SigningCertificateConfigurationUpdateInfo.class);

        when(pkiManagement.getSigningCertificateConfiguration("issuer1")).thenReturn(existingConfig);
        when(pkiManagement.updateSigningCertificateConfiguration(existingConfig)).thenReturn(existingConfig);

        ResponseEntity<SigningCertificateConfiguration> response = resource.updateSigningConfiguration("issuer1", updateConfig);

        assertEquals(existingConfig, response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(existingConfig).setPki(updateConfig.getPki());
        verify(existingConfig).setDdCertificatePath(updateConfig.getDdCertificatePath());
        verify(existingConfig).setDdPrivateKeyPath(updateConfig.getDdPrivateKeyPath());
        verify(existingConfig).setEspCertificatePath(updateConfig.getEspCertificatePath());
        verify(existingConfig).setEspPrivateKeyPath(updateConfig.getEspPrivateKeyPath());
        verify(existingConfig).setRspCertificatePath(updateConfig.getRspCertificatePath());
        verify(existingConfig).setRspPrivateKeyPath(updateConfig.getRspPrivateKeyPath());
        verify(existingConfig).setIntermediateCACertificatePath(updateConfig.getIntermediateCACertificatePath());
    }

    @Description("Tests that updating a non-existent signing certificate configuration throws EntityNotFoundException.")
    @Test
    void updateSigningConfiguration_notFound() {
        SigningCertificateConfigurationUpdateInfo updateConfig = mock(SigningCertificateConfigurationUpdateInfo.class);
        when(pkiManagement.getSigningCertificateConfiguration("issuer1")).thenReturn(null);

        assertThrows(EntityNotFoundException.class, () -> resource.updateSigningConfiguration("issuer1", updateConfig));
    }

    @Description("Tests successful deletion of a signing certificate configuration.")
    @Test
    void deleteSigningConfiguration_success() {
        ResponseEntity<Void> response = resource.deleteSigningConfiguration("issuer1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(pkiManagement).deleteSigningCertificateConfiguration("issuer1");
    }
}