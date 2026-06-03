package org.eclipse.hawkbit.repository.jpa;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.validation.ValidationException;

import org.eclipse.hawkbit.repository.EcuModelManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.VehicleFields;
import org.eclipse.hawkbit.repository.VehicleManagement;
import org.eclipse.hawkbit.repository.builder.VehicleCreate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaVehicleCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaVehicle;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.model.EcuModel;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.Vehicle;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link VehicleManagement}.
 */
@Transactional
@Validated
public class JpaVehicleManagement implements VehicleManagement  {

    private static final Logger LOG = LoggerFactory.getLogger(JpaVehicleManagement.class);
    private final EcuModelManagement ecuModelManagement;
    private final VehicleRepository vehicleRepository;

    private final TargetManagement targetManagement;

    private final RolloutManagement rolloutManagement;

    private final VirtualPropertyReplacer virtualPropertyReplacer;

    private final Database database;



    public JpaVehicleManagement(EcuModelManagement ecuModelManagement, final VehicleRepository vehicleRepository, TargetManagement targetManagement, RolloutManagement rolloutManagement, VirtualPropertyReplacer virtualPropertyReplacer,final Database database) {
        this.ecuModelManagement = ecuModelManagement;
        this.vehicleRepository = vehicleRepository;
        this.targetManagement = targetManagement;
        this.rolloutManagement = rolloutManagement;
        this.virtualPropertyReplacer=virtualPropertyReplacer;
        this.database=database;

    }

    @Override
    public List<Vehicle> create(List<Vehicle> vehicles) {
        List<JpaVehicle> jpaVehicles = vehicles.stream().map(JpaVehicle.class::cast).toList();
        return Collections.unmodifiableList(vehicleRepository.saveAll(jpaVehicles));
    }

    @Override
    public void update(Vehicle vehicle, Long vehicleModelId) {
        final JpaVehicle jpaVehicle = (JpaVehicle) getVehicleModelByIdAndThrowIfNotFound(vehicleModelId);
        checkVehicleTargetsAssociationOrThrowException(vehicleModelId);
        jpaVehicle.setName(vehicle.getName());
        jpaVehicle.setErcType(vehicle.getErcType());
        vehicleRepository.save(jpaVehicle);
    }

    @Override
    public List<Vehicle> findAll(Pageable pageable) {
        return Collections.unmodifiableList(vehicleRepository.findAll(pageable).toList());
    }

    @Override
    public List<Vehicle> findByNameContaining(final String name, Pageable pageable) {
        return Collections.unmodifiableList(vehicleRepository.findByNameContainingIgnoreCase(name, pageable));
    }

    /**
     * Get list of {@link Vehicle} for provided list of vehicle ids
     *
     * @param ids list of vehicle ids
     * @return list of vehicles
     */
    @Override
    public List<Vehicle> findAllById(List<Long> ids) {
        return Collections.unmodifiableList(vehicleRepository.findAllById(ids));
    }

    @Override
    public Optional<Vehicle> get(long id) {
        return vehicleRepository.findById(id).map(v -> v);
    }

    @Override
    public void delete(Long id) {
        getVehicleModelByIdAndThrowIfNotFound(id);
        getTargetsByVehicleModelIdAndThrowIfFound(id);
        vehicleRepository.deleteById(id);
    }

    @Override
    public void deleteAll() {
        vehicleRepository.deleteAll();
    }

    @Override
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = org.eclipse.hawkbit.repository.jpa.configuration.Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Vehicle create(VehicleCreate c) {
        final JpaVehicleCreate create = (JpaVehicleCreate) c;
        JpaVehicle v = create.build();
        return vehicleRepository.save(v);
    }

    @Override
    public void assignEcuModels(Long vehicleModelId, List<Long> ecuModelsListInRequest) {
        final JpaVehicle jpaVehicle = (JpaVehicle) getVehicleModelByIdAndThrowIfNotFound(vehicleModelId);
        List<EcuModel> ecuModels = checkReturnEcuModelsExistOrThrowException(ecuModelsListInRequest, null);
        Set<EcuModel> eculModelSet = new HashSet<>(ecuModels);
        eculModelSet.forEach(jpaVehicle::addVehicleEcu);
        vehicleRepository.save(jpaVehicle);
    }

    @Override
    public void deleteEcuModels(Long vehicleModelId, List<Long> ecuModelIdsInRequest) {
        final JpaVehicle jpaVehicle = (JpaVehicle) getVehicleModelByIdAndThrowIfNotFound(vehicleModelId);
        checkReturnEcuModelsExistOrThrowException(ecuModelIdsInRequest, vehicleModelId);
        checkRolloutsForTargetListOrThrowException(vehicleModelId);
        Set<EcuModel> eculModelSet = jpaVehicle.getVehicleEcu().stream().filter(e -> !ecuModelIdsInRequest.contains(e.getId())).collect(Collectors.toSet());
        jpaVehicle.setVehicleEcu(eculModelSet);
        vehicleRepository.save(jpaVehicle);
    }

    @Override
    public Page<Vehicle> findByRsql(final Pageable pageable, final String rsqlParam) {

        final List<Specification<JpaVehicle>> specList = List.of(RSQLUtility.buildRsqlSpecification(rsqlParam, VehicleFields.class,
                virtualPropertyReplacer, database));
        return JpaManagementHelper.findAllWithCountBySpec(vehicleRepository, pageable, specList);
    }
    @Override
    public long count() {
        return vehicleRepository.count();
    }

    private List<EcuModel> checkReturnEcuModelsExistOrThrowException(List<Long> ecuModelIdsInRequest, Long vehicleModelId) {
        List<EcuModel> ecuModels;
        if (vehicleModelId == null) {
            ecuModels = ecuModelManagement.getByIds(ecuModelIdsInRequest);
        } else {
            JpaVehicle vehicle = new JpaVehicle();
            vehicle.setId(vehicleModelId);
            ecuModels = ecuModelManagement.getAllEcuForVehicleModelId(vehicle, ecuModelIdsInRequest);
        }
        if (ecuModels.size() < ecuModelIdsInRequest.size()) {
            LOG.error("Few or all of the ECU Models in the list does not exist");
            throw new EntityNotFoundException("Few or all of the ECU Models in the list does not exist");
        }
        return ecuModels;
    }

    private void checkRolloutsForTargetListOrThrowException(Long vehicleModelId) {
        List<Target> targets = targetManagement.findByVehicleModelId(vehicleModelId);
        if (!targets.isEmpty()) {
            List<Rollout> rollouts = rolloutManagement.findAllRolloutByTargetIds(targets);
            if (!rollouts.isEmpty()) {
                throw new ValidationException("ECU Model cannot be deleted because Vehicle Model is attached to target(s) which is part of an any rollouts");
            }
        }
    }

    /**
     * Check for association of vehicle model ID with targets
     *
     * @param vehicleModelId
     */
    private void checkVehicleTargetsAssociationOrThrowException(Long vehicleModelId) {
        List<Target> targets = targetManagement.findByVehicleModelId(vehicleModelId);
        if (!targets.isEmpty()) {
            throw new ValidationException(String.format("Vehicle Model with id %s is associated with target(s) hence update" +
                    " not allowed", vehicleModelId));
        }
    }

    private Vehicle getVehicleModelByIdAndThrowIfNotFound(Long id) {
        return vehicleRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(Vehicle.class, id));
    }

    private List<Target> getTargetsByVehicleModelIdAndThrowIfFound(Long id) {
        List<Target> targets = targetManagement.findByVehicleModelId(id);
        if (!targets.isEmpty()) {
            throw new ValidationException(String.format("Target found for VehicleModel with ID: %s. Cannot delete it", id));
        }
        return targets;
    }
}
