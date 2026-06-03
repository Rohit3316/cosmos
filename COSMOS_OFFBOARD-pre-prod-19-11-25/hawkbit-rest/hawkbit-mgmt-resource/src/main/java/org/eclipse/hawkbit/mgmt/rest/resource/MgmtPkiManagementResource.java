package org.eclipse.hawkbit.mgmt.rest.resource;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.mgmt.pki.dto.SigningCertificateConfigurationInfo;
import org.cosmos.models.mgmt.pki.dto.SigningCertificateConfigurationUpdateInfo;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtPkiManagementRestApi;
import org.eclipse.hawkbit.repository.PKIManagement;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaSigningCertificateConfiguration;
import org.eclipse.hawkbit.repository.model.SigningCertificateConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * REST resource for managing PKI signing certificate configurations.
 * Implements {@link MgmtPkiManagementRestApi} to provide endpoints for PKI management.
 */
@RestController
@Validated
@Slf4j
@Tag(name = "PKI Management")
public class MgmtPkiManagementResource implements MgmtPkiManagementRestApi {

    private final PKIManagement pkiManagement;

    /**
     * Constructs a new {@code MgmtPkiManagementResource} with the given PKI management service.
     *
     * @param pkiManagement the PKI management service
     */
    public MgmtPkiManagementResource(PKIManagement pkiManagement) {
        this.pkiManagement = pkiManagement;
    }

    /**
     * Adds a new signing certificate configuration.
     *
     * @param config the signing certificate configuration info
     * @return the saved signing certificate configuration
     * @throws EntityAlreadyExistsException if a configuration with the given ECU ID Issuer already exists
     */
    @Override
    public ResponseEntity<SigningCertificateConfiguration> addSigningConfiguration(
            @RequestBody @Valid SigningCertificateConfigurationInfo config) {

        log.info("Received request to add signing configuration: {}", config);

        if (pkiManagement.existsSigningCertificateConfigurationByEcuIdIssuer(config.getEcuIdIssuer())) {
            throw new EntityAlreadyExistsException("A signing configuration with the given ECU ID Issuer already exists.");
        }

        JpaSigningCertificateConfiguration jpaConfiguration = JpaSigningCertificateConfiguration.builder()
                .pki(config.getPki())
                .ecuIdIssuer(config.getEcuIdIssuer())
                .ddCertificatePath(config.getDdCertificatePath())
                .ddPrivateKeyPath(config.getDdPrivateKeyPath())
                .espCertificatePath(config.getEspCertificatePath())
                .espPrivateKeyPath(config.getEspPrivateKeyPath())
                .rspCertificatePath(config.getRspCertificatePath())
                .rspPrivateKeyPath(config.getRspPrivateKeyPath())
                .intermediateCACertificatePath(config.getIntermediateCACertificatePath())
                .build();

        return ResponseEntity.ok(pkiManagement.addSigningCertificateConfiguration(jpaConfiguration));
    }

    @Override
    public ResponseEntity<SigningCertificateConfiguration> getSigningConfiguration(String ecuIdIssuer) {
        log.info("Received request to get signing configuration for ECU ID Issuer: {}", ecuIdIssuer);
        SigningCertificateConfiguration config = pkiManagement.getSigningCertificateConfiguration(ecuIdIssuer);
        if (config == null) {
            throw new EntityNotFoundException("Signing configuration not found for ECU ID Issuer: " + ecuIdIssuer);
        }
        return ResponseEntity.ok(config);
    }


    @Override
    public ResponseEntity<List<SigningCertificateConfiguration>> getAllSigningConfigurations() {
        log.info("Received request to get all signing configurations");
        List<SigningCertificateConfiguration> configs = pkiManagement.getAllSigningCertificateConfigurations();
        return ResponseEntity.ok(configs);
    }

    @Override
    public ResponseEntity<SigningCertificateConfiguration> updateSigningConfiguration(String ecuIdIssuer, SigningCertificateConfigurationUpdateInfo updateConfig) {
        log.info("Received request to update signing configuration for ECU ID Issuer: {}", ecuIdIssuer);

        JpaSigningCertificateConfiguration existingConfig = (JpaSigningCertificateConfiguration) pkiManagement.getSigningCertificateConfiguration(ecuIdIssuer);
        if (existingConfig == null) {
            throw new EntityNotFoundException("Signing configuration not found for ECU ID Issuer: " + ecuIdIssuer);
        }

        existingConfig.setPki(updateConfig.getPki());
        existingConfig.setDdCertificatePath(updateConfig.getDdCertificatePath());
        existingConfig.setDdPrivateKeyPath(updateConfig.getDdPrivateKeyPath());
        existingConfig.setEspCertificatePath(updateConfig.getEspCertificatePath());
        existingConfig.setEspPrivateKeyPath(updateConfig.getEspPrivateKeyPath());
        existingConfig.setRspCertificatePath(updateConfig.getRspCertificatePath());
        existingConfig.setRspPrivateKeyPath(updateConfig.getRspPrivateKeyPath());
        existingConfig.setIntermediateCACertificatePath(updateConfig.getIntermediateCACertificatePath());

        return ResponseEntity.ok(pkiManagement.updateSigningCertificateConfiguration(existingConfig));
    }

    @Override
    public ResponseEntity<Void> deleteSigningConfiguration(String ecuIdIssuer) {
        log.info("Received request to delete signing configuration for ECU ID Issuer: {}", ecuIdIssuer);
        pkiManagement.deleteSigningCertificateConfiguration(ecuIdIssuer);
        return ResponseEntity.noContent().build();
    }
}
