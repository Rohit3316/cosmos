package org.eclipse.hawkbit.repository.jpa;

import java.util.Optional;
import org.eclipse.hawkbit.repository.jpa.model.JpaEcuModelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link org.eclipse.hawkbit.repository.model.EcuModelType} repository.
 */
@Transactional(readOnly = true)
public interface EcuModelTypeRepository extends JpaRepository<JpaEcuModelType, Long> {
    /**
     * @param name
     * @return JpaEcuModelType for the given EcuModelType Name
     * @throws org.eclipse.hawkbit.repository.exception.EntityNotFoundException if given EcuModelType Name not exists
     */
    Optional<JpaEcuModelType> findByName(String name);
}
