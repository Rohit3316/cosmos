package org.eclipse.hawkbit.repository.jpa.utils;

import org.eclipse.hawkbit.repository.jpa.BaseEntityRepository;
import org.eclipse.hawkbit.repository.jpa.model.helper.JpaRolloutMetaData;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
/**
 * Repository interface for managing {@link JpaRolloutMetaData} entities.
 * Extends base JPA repository and specification executor for advanced queries.
 */
public interface RolloutMetaDataRepository
        extends BaseEntityRepository<JpaRolloutMetaData, Long>, JpaSpecificationExecutor<JpaRolloutMetaData> {
}
