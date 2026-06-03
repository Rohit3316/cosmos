package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareInstallerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * {@link org.eclipse.hawkbit.repository.model.SoftwareInstallerType} repository.
 */
@Transactional(readOnly = true)
public interface SoftwareInstallerTypeRepository extends
        JpaRepository<JpaSoftwareInstallerType, Long>, JpaSpecificationExecutor<JpaSoftwareInstallerType> {
    /**
     * @param name
     *          to search for
     * @return JpaSoftwareInstallerType for the given SoftwareInstallerType Name
     * @throws org.eclipse.hawkbit.repository.exception.EntityNotFoundException if given SoftwareInstallerType Name not exists
     */
    Optional<JpaSoftwareInstallerType> findByName(String name);

    /**
     * @param isDeleted
     *            to <code>true</code> if only marked as deleted have to be
     *            count or all undeleted.
     * @return number of SoftwareInstallerType in the repository.
     */
    Long countByDeleted(boolean isDeleted);
}
