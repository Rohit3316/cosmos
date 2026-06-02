package org.eclipse.hawkbit.repository;

import java.util.List;
import java.util.Set;

import org.cosmos.models.mgmt.supportpackage.dto.MgmtFileUrlSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtBaseSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.model.Esp;
import org.eclipse.hawkbit.repository.model.EspEcuRollout;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rsp;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.prepost.PreAuthorize;

import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;


public interface SupportPackageManagement {


    /**
     * Retrieves all ESP support packages for a specific rollout and controller ID.
     *
     * @param rolloutId     The ID of the rollout.
     * @param controllerIds The List of controller IDs of the support package.
     * @return A list of SupportPackage objects associated with the given rollout ID and controller ID.
     */
    List<Esp> getESPSupportPackages(Long rolloutId, List<String> controllerIds);

    /**
     * Creates ESP support package as per the request of DOCG.
     *
     * @param mgmtSupportPackageCreateRequest the request object containing details for creating the support package
     * @param rollout                         the rollout for which the support package is being created
     * @return ESP created or replaced
     */
    Esp handleEspSupportPackage(Esp esp,MgmtBaseSupportPackageCreateRequest supportPackageCreateRequest, Long rolloutId, Long tenantId);
    /**
     * Creates RSP support package as per the request of DOCG.
     *
     * @param mgmtSupportPackageCreateRequest the request object containing details for creating the support package
     * @param rollout                         the rollout for which the support package is being created
     * @return RSP created or replaced
     */
    Rsp handleRspSupportPackage(Rsp Rsp,MgmtBaseSupportPackageCreateRequest mgmtSupportPackageCreateRequest, Long rolloutId, Long tenantId);

    /**
     * Returns a list of ESP ECU Rollout entities by rollout ID and list of ECU node addresses.
     *
     * @param rolloutId
     * @param ecuNodeAddress
     * @return
     */
    List<EspEcuRollout> getByRolloutIdAndEcuNodeAddressList(Long rolloutId, Set<String> ecuNodeAddress);


    /**
     * Generates and caches signatures for a support package file with all the ecu issuer configurations present in the system.
     *
     * @param fileId   The ID of the file for which signatures are to be generated.
     * @param sha256   The SHA-256 hash of the file.
     * @param fileType The type of the support package file.
     */
    void generateAndCacheSignaturesForSupportPackage(Long fileId, String sha256, MgmtSupportPackageFileType fileType, Rollout rollout);

    /**
     * Retrieves all ESP support packages for a specific rollout.
     *
     * @param rolloutId The ID of the rollout.
     * @return A list of SupportPackage objects associated with the given rollout ID.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    List<Esp> getESPSupportPackages(Long rolloutId);

    /**
     * Retrieves all RSP support packages for a specific rollout.
     *
     * @param rolloutId The ID of the rollout.
     * @return A list of SupportPackage objects associated with the given rollout ID.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ + SpPermission.SpringEvalExpressions.HAS_AUTH_OR + SpPermission.SpringEvalExpressions.IS_CONTROLLER)
    List<Rsp> getRSPSupportPackages(Long rolloutId);

    /**
     * Retrieves all ESP support packages for a specific rollout and support package ID.
     *
     * @param rolloutId The ID of the rollout.
     * @param packageId The ID of the support package.
     * @return A list of SupportPackage objects associated with the given rollout ID and support package ID.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    List<Esp> getESPSupportPackageById(Long rolloutId, Long packageId);

    /**
     * Retrieves all RSP support packages for a specific rollout and support package ID.
     *
     * @param rolloutId The ID of the rollout.
     * @param packageId The ID of the support package.
     * @return A list of SupportPackage objects associated with the given rollout ID and support package ID.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    List<Rsp> getRSPSupportPackageById(Long rolloutId, Long packageId);

    /**
     * @param rolloutId
     * @param supportPackageIds
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_DELETE)
    void unLinkEspEcuRollout(Long rolloutId, List<Long> supportPackageIds);

    /**
     * @param rolloutId
     * @param supportPackageIds
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_DELETE)
    void unLinkRspRollout(Long rolloutId, List<Long> supportPackageIds);

    /**
     * Checks if there are no associations for the given list of {@link Esp} and then delete.
     *
     * @param esp
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_DELETE)
    void deleteEsp(List<Esp> esp);

    /**
     * Checks if there are no associations for the given list of {@link Rsp} and then delete.
     *
     * @param rsp
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_DELETE)
    void deleteRsp(List<Rsp> rsp);

    /**
     * Deletes ESP/RSP packages by its {@link Rollout#getId().
     *
     * @param rolloutId The ID of the ESP/RSP packages {@link Rollout} to delete.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_DELETE)
    void deleteSupportPackage(final Long rolloutId, final List<Esp> esp, final List<Rsp> rsp);

    /**
     * Initiates a file download from an external location to an S3 bucket.
     *
     * @param mgmtSupportPackageCreateRequest The request object containing details for creating the support package.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_CREATE)
    void initiateFileDownloadToS3(MgmtFileUrlSupportPackageCreateRequest mgmtSupportPackageCreateRequest, Long tenantId);

    /**
     * fetched the Esp by rollout Id and Controller Id
     */
    List<Esp> getEspByRolloutIdAndControllerId(String controllerId, Long rolloutId);

    /**
     * Checks if there is a conflict between the file type in the package request and the existing rollout.
     *
     * @param packageRequest the package creation request containing the file type
     * @param rollout        the rollout to check against for file type conflicts
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    void checkForESPSupportingPackageType(Esp esp, Rollout rollout);

    /**
     * Counts the total number of support packages associated with a specific rollout.
     *
     * @param rolloutId The ID of the rollout for which the support packages are to be counted.
     * @return The total number of support packages associated with the given rollout ID.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    long countSupportPackages(long rolloutId);

    /**
     * Retrieves a paginated slice of RSP support packages for a specific rollout.
     *
     * @param rolloutId The ID of the rollout for which the RSP support packages are to be retrieved.
     * @param pageable  The pagination information, including page number and size.
     * @return A Slice containing the RSP support packages associated with the given rollout ID.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Slice<Rsp> getRSPSupportPackages(Long rolloutId, Pageable pageable);

    /**
     * Retrieves a paginated slice of ESP support packages for a specific rollout.
     *
     * @param rolloutId The ID of the rollout for which the ESP support packages are to be retrieved.
     * @param pageable  The pagination information, including page number and size.
     * @return A Slice containing the ESP support packages associated with the given rollout ID.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Slice<Esp> getESPSupportPackages(Long rolloutId, Pageable pageable);

    /**
     * Retrieves a paginated slice of RSP support packages for a specific rollout using an RSQL query.
     *
     * @param rsqlParam The RSQL query parameter to filter the RSP support packages.
     * @param rolloutId The ID of the rollout for which the RSP support packages are to be retrieved.
     * @param pageable  The pagination information, including page number and size.
     * @return A Slice containing the RSP support packages associated with the given rollout ID and matching the RSQL query.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Slice<Rsp> findRspsByRolloutRsql(@NotNull String rsqlParam, @NotEmpty Long rolloutId,
                                     @NotNull Pageable pageable);

    /**
     * Retrieves a paginated slice of ESP support packages for a specific rollout using an RSQL query.
     *
     * @param rsqlParam The RSQL query parameter to filter the ESP support packages.
     * @param rolloutId The ID of the rollout for which the ESP support packages are to be retrieved.
     * @param pageable  The pagination information, including page number and size.
     * @return A Slice containing the ESP support packages associated with the given rollout ID and matching the RSQL query.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Slice<Esp> findEspsByRolloutRsql(@NotNull String rsqlParam, @NotEmpty Long rolloutId,
                                     @NotNull Pageable pageable);

    /**
     * Validates that the provided controller IDs are not already associated with the given rollout
     * using the same ECU node address and file type.
     * <p>
     * If a conflict is found, a {@link ValidationException} is thrown indicating that the device
     * of the specified file type already exists for the given rollout ID and controller IDs.
     *
     * @param rollout               The rollout entity to validate against.
     * @param supportPackageRequest The support package containing the controller IDs and file type.
     * @throws ValidationException If a controller ID is already associated with the same file type
     *                             for the given rollout ID and ECU node address.
     */
    void validateTargetsForRollout(Rollout rollout,  Esp esp);

}
