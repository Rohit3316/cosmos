package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.jpa.model.JpaSigningCertificateConfiguration;
import org.eclipse.hawkbit.repository.model.SigningCertificateConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SigningCertificateConfigurationRepository extends JpaRepository<JpaSigningCertificateConfiguration, Long> {

    /**
     * Finds a signing certificate configuration by its ECU ID issuer.
     *
     * @param ecuIdIssuer The ECU ID issuer to search for.
     * @return An Optional containing the JpaSigningCertificateConfiguration if found, or empty if not found.
     */
    Optional<SigningCertificateConfiguration> findByEcuIdIssuer(String ecuIdIssuer);

    /**
     * Checks if a signing certificate configuration exists for the given ECU ID issuer.
     *
     * @param ecuIdIssuer The ECU ID issuer to check.
     * @return true if a configuration exists, false otherwise.
     */
    boolean existsByEcuIdIssuer(String ecuIdIssuer);

    /**
     * Deletes a signing certificate configuration by its ECU ID issuer.
     * @param ecuIdIssuer The ECU ID issuer of the configuration to delete.
     */
    void deleteByEcuIdIssuer(String ecuIdIssuer);

}
