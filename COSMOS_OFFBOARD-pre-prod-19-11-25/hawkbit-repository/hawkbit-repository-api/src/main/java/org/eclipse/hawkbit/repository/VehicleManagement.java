package org.eclipse.hawkbit.repository;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.builder.VehicleCreate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Vehicle;
import org.springframework.cloud.context.named.NamedContextFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

/**
 * Management service for {@link org.eclipse.hawkbit.repository.model.Vehicle}s.
 */
public interface VehicleManagement {
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    List<Vehicle> create(@NotNull @Valid List<Vehicle> vehicles);

    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    void update(Vehicle vehicle, Long vehicleModelId);

    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    List<Vehicle> findAll(Pageable pageable);

    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    List<Vehicle> findByNameContaining(String name, Pageable pageable);

    /**
     * Get list of {@link Vehicle} for provided list of vehicle ids
     *
     * @param ids list of vehicle ids
     * @return list of vehicles
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    List<Vehicle> findAllById(List<Long> ids);

    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET + SpPermission.SpringEvalExpressions.HAS_AUTH_OR
            + SpPermission.SpringEvalExpressions.IS_CONTROLLER)
    Optional<Vehicle> get(long id);

    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_DELETE_TARGET)
    void delete(@NotNull Long id);

    /*
     * Delete all the vehicle models
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_DELETE_TARGET)
    void deleteAll();

    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    Vehicle create(@NotNull @Valid VehicleCreate create);

    /**
     * Assigns List of ECU models to a given vehicle model id
     *
     * @param vehicleModelId
     *            {@link org.eclipse.hawkbit.repository.model.Vehicle} to associate with ecu models
     *
     * @param ecuModelIds list of ecu model ids
     *
     * @throws EntityNotFoundException
     *             if the vehicle Model id or ecu models does not exist
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    void assignEcuModels(Long vehicleModelId, List<Long> ecuModelIds);

    /**
     * Deletes List of ECU models for a given vehicle model id
     *
     * @param vehicleModelId
     *            {@link org.eclipse.hawkbit.repository.model.Vehicle} associated with ecu models
     *
     * @param ecuModelIds list of ecu model ids
     *
     * @throws EntityNotFoundException
     *             if the vehicle Model id or ecu models does not exist
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_DELETE_TARGET)
    void deleteEcuModels(Long vehicleModelId, List<Long> ecuModelIds);


    /**
     * Find all {@link Vehicle}s by RSQL query.
     *
     * @param pageable the pagination information
     * @param rsqlParam the RSQL query string
     * @return a page of vehicles matching the RSQL query
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<Vehicle> findByRsql(@NotNull Pageable pageable, @NotNull String rsqlParam);

    long count();




}
