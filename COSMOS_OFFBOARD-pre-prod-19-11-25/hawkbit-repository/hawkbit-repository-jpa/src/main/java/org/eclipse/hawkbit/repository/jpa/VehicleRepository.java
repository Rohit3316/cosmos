package org.eclipse.hawkbit.repository.jpa;

import java.util.List;

import org.eclipse.hawkbit.repository.jpa.model.JpaTenantMetaData;
import org.eclipse.hawkbit.repository.jpa.model.JpaVehicle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link org.eclipse.hawkbit.repository.model.Vehicle} repository.
 */
@Repository
@Transactional
public interface VehicleRepository extends JpaRepository<JpaVehicle, Long> , JpaSpecificationExecutor<JpaVehicle> {
    List<JpaVehicle> findByNameContaining(final String name, Pageable pageable);

    /**
     * Get list of {@link JpaVehicle} Containing case ignored {@link JpaVehicle#getName()}
     *
     * @param name     the name
     * @param pageable page request
     * @return JpaVehicle list
     */
    List<JpaVehicle> findByNameContainingIgnoreCase(final String name, Pageable pageable);

    boolean existsByVehicleEcuId(Long ecuModelId);

    /**
     * Get list of {@link JpaVehicle} for the given list of vehicle ids
     *
     * @param vehicleIds list of vehicle ids
     * @return JpaVehicle list found in the repository
     */
    List<JpaVehicle> findAllById(Iterable<Long> vehicleIds);

    @Override
    default long count() {
        return this.count(Specification.where(null));
    }

}
