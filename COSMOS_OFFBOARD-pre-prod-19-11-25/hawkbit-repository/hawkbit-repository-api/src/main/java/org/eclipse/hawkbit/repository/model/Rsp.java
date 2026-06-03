package org.eclipse.hawkbit.repository.model;

import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;

import java.util.List;

/**
 * This interface represents a Rollout Specific Package (RSP) in the Cosmos OTA  management system.
 * It extends the {@link BaseSupportPackage} interface, which provides common attributes and methods for support packages.
 *
 */
public interface Rsp extends BaseSupportPackage {

    /**
     * Retrieves a list of {@link RspRollout} objects associated with this RSP.
     *
     * @return A list of {@link RspRollout}.
     */
    List<RspRollout> getRspRollouts();

    MgmtSupportPackageFileType getRollout();


}