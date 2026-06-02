package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.PKIManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaSigningCertificateConfiguration;
import org.eclipse.hawkbit.repository.model.SigningCertificateConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * JPA implementation of {@link PKIManagement}.
 * This class provides methods to manage Public Key Infrastructure (PKI) related operations,
 * specifically retrieving signing certificate configurations based on ECU ID issuers.
 */
@Transactional(readOnly = true)
@Validated
public class JpaPKIManagement implements PKIManagement {

    private final SigningCertificateConfigurationRepository signingCertificateConfigurationRepository;

    public JpaPKIManagement(SigningCertificateConfigurationRepository signingCertificateConfigurationRepository) {
        this.signingCertificateConfigurationRepository = signingCertificateConfigurationRepository;
    }

    @Override
    public SigningCertificateConfiguration getSigningCertificateConfiguration(String ecuIdIssuer) {

        return signingCertificateConfigurationRepository.findByEcuIdIssuer(ecuIdIssuer)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Signing certificate configuration not found for ECU ID issuer: " + ecuIdIssuer));
    }

    @Override
    public List<SigningCertificateConfiguration> getAllSigningCertificateConfigurations() {
        return signingCertificateConfigurationRepository.findAll()
                .stream()
                .map(config -> (SigningCertificateConfiguration) config)
                .toList();
    }

    @Override
    public boolean existsSigningCertificateConfigurationByEcuIdIssuer(String ecuIdIssuer) {
        return signingCertificateConfigurationRepository.existsByEcuIdIssuer(ecuIdIssuer);
    }

    @Override
    public SigningCertificateConfiguration addSigningCertificateConfiguration(SigningCertificateConfiguration configuration) {
        return signingCertificateConfigurationRepository.save((JpaSigningCertificateConfiguration) configuration);
    }

    @Override
    public SigningCertificateConfiguration updateSigningCertificateConfiguration(SigningCertificateConfiguration configuration) {
        String ecuIdIssuer = configuration.getEcuIdIssuer();
        if (!signingCertificateConfigurationRepository.existsByEcuIdIssuer(ecuIdIssuer)) {
            throw new EntityNotFoundException("Signing certificate configuration not found for ECU ID issuer: " + ecuIdIssuer);
        }
        return signingCertificateConfigurationRepository.save((JpaSigningCertificateConfiguration) configuration);
    }

    @Override
    public void deleteSigningCertificateConfiguration(String ecuIdIssuer) {
        if (!signingCertificateConfigurationRepository.existsByEcuIdIssuer(ecuIdIssuer)) {
            throw new EntityNotFoundException("Signing certificate configuration not found for ECU ID Issuer: " + ecuIdIssuer);
        }

        signingCertificateConfigurationRepository.deleteByEcuIdIssuer(ecuIdIssuer);
    }


}
