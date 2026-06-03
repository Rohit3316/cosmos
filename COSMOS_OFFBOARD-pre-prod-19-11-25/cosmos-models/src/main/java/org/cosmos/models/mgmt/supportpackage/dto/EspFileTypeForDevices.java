package org.cosmos.models.mgmt.supportpackage.dto;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;

import jakarta.validation.constraints.NotNull;

/**
 * A data transfer object (DTO) representing the association between a controller ID
 * and the type of support package file ({@link MgmtSupportPackageFileType}) assigned to it.
 * <p>
 * This record is commonly used when retrieving or transferring minimal information
 * about which fileType is associated with a particular device/controller during rollout validation,
 * package creation, or conflict checks.
 * </p>
 *
 * @param controllerId the unique identifier of the target/controller
 * @param fileType the type of the support package file assigned to the controller
 */
@Jacksonized
@Builder(toBuilder = true)
public record EspFileTypeForDevices(
        String controllerId,
        MgmtSupportPackageFileType fileType)
{
}
