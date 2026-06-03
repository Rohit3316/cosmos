package org.eclipse.hawkbit.repository.jpa;


import java.util.List;
import java.util.Set;
import org.eclipse.hawkbit.repository.jpa.model.JpaEcuModel;
import org.eclipse.hawkbit.repository.jpa.model.JpaVehicle;
import org.eclipse.hawkbit.repository.model.EcuModel;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link org.eclipse.hawkbit.repository.model.EcuModel} repository.
 */
@Repository
@Transactional
public interface EcuModelRepository extends JpaRepository<JpaEcuModel, Long>, JpaSpecificationExecutor<JpaEcuModel> {

    @Query("SELECT e FROM JpaEcuModel e WHERE e.vehicleModel = :vehicleModelId AND e.id IN :ecuModels")
    List<EcuModel> findByVehicleModelAndIds(@Param("vehicleModelId") JpaVehicle vehicleModelId, @Param("ecuModels") List<Long> ecuModels);

    @Query("SELECT e FROM JpaEcuModel e WHERE e.ecuNodeId IN :ecuNodeId")
    List<EcuModel> findByEcuNodeId(@Param("ecuNodeId") Set<String> ecuNodeId);

    boolean existsByEcuNodeId(String ecuNodeId);

    @Override
    default long count() {
        return this.count(Specification.where(null));
    }
}
