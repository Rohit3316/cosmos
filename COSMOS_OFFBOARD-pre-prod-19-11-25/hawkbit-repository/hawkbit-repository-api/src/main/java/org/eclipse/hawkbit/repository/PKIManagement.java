package org.eclipse.hawkbit.repository;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.model.SigningCertificateConfiguration;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/**
 * Interface for managing Public Key Infrastructure (PKI) related operations.
 * Provides methods to interact with signing certificate configurations based on ECU ID issuers.
 * <p>
 * <b>Roles:</b>
 * <ul>
 *   <li>READ_REPOSITORY: Retrieve signing certificate configurations</li>
 *   <li>CREATE_REPOSITORY: Add signing certificate configurations</li>
 *   <li>UPDATE_REPOSITORY: Update signing certificate configurations</li>
 *   <li>DELETE_REPOSITORY: Delete signing certificate configurations</li>
 * </ul>
 * </p>
 */
public interface PKIManagement {

    /**
     * Retrieves the signing certificate configuration for a given ECU ID issuer.
     * <b>Role:</b> READ_REPOSITORY
     * This method checks if the ECU ID issuer exists in the EcuIdCertificatesRepository,
     * if so only then it fetches the signing certificate configuration.
     *
     * @param ecuIdIssuer The ECU ID issuer for which the signing certificate configuration is requested.
     * @return The signing certificate configuration associated with the specified ECU ID issuer.
     * @throws EntityNotFoundException if no signing certificate configuration is found for the given ECU ID issuer.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    SigningCertificateConfiguration getSigningCertificateConfiguration(String ecuIdIssuer);

    /**
     * Retrieves all signing certificate configurations available in the system.
     * <b>Role:</b> READ_REPOSITORY
     *
     * @return A list of all signing certificate configurations.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    List<SigningCertificateConfiguration> getAllSigningCertificateConfigurations();

    /**
     * Checks if a signing certificate configuration exists for the specified ECU ID issuer.
     * <b>Role:</b> READ_REPOSITORY
     *
     * @param ecuIdIssuer The ECU ID issuer to check for existence.
     * @return true if a signing certificate configuration exists for the given ECU ID issuer, false otherwise.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    boolean existsSigningCertificateConfigurationByEcuIdIssuer(String ecuIdIssuer);

    /**
     * Adds the provided signing certificate configuration.
     * <b>Role:</b> CREATE_REPOSITORY
     *
     * @param configuration The signing certificate configuration to save.
     * @return The saved signing certificate configuration.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    SigningCertificateConfiguration addSigningCertificateConfiguration(SigningCertificateConfiguration configuration);

    /**
     * Updates the provided signing certificate configuration.
     * <b>Role:</b> UPDATE_REPOSITORY
     *
     * @param configuration The signing certificate configuration to update.
     * @return The updated signing certificate configuration.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    SigningCertificateConfiguration updateSigningCertificateConfiguration(SigningCertificateConfiguration configuration);

    /**
     * Deletes the signing certificate configuration for the specified ECU ID issuer.
     * <b>Role:</b> DELETE_REPOSITORY
     *
     * @param ecuIdIssuer The ECU ID issuer whose signing certificate configuration should be deleted.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    void deleteSigningCertificateConfiguration(String ecuIdIssuer);
}
