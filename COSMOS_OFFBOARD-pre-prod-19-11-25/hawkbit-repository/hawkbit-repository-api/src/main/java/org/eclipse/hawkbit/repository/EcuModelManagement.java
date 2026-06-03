package org.eclipse.hawkbit.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.model.EcuModel;
import org.eclipse.hawkbit.repository.model.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Management service for {@link EcuModel}s.
 */
public interface EcuModelManagement {

    /**
     * @param ecuModelModels
     * @return Created List<EcuModel>
     * will add new ECU model/s details into cosmos
     * @throws org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException in case the {@link EcuModel} is already present
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    List<EcuModel> create(@NotNull @Valid List<EcuModel> ecuModelModels);

    /**
     * @param ecuModel
     * @param ecuModelId will update the existing ecu model id with latest details
     * @throws org.eclipse.hawkbit.repository.exception.EntityNotFoundException in case @ecuModelId does not exist.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    void update(EcuModel ecuModel, Long ecuModelId);

    /**
     * @param id
     * @return Optional of {@link EcuModel} for the given ecu model id
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Optional<EcuModel> get(long id);

    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    List<EcuModel> getEcuModelByIdsAndThrowIfNotFound(@NotEmpty Collection<Long> ecuModelIds);

    /**
     * @return List of All {@link EcuModel}
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    List<EcuModel> findAll(Pageable pageable);

    /**
     * @param id will delete the ecu model for given ecu model id
     * @throws org.eclipse.hawkbit.repository.exception.EntityNotFoundException in case @ecuModelId does not exist.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_DELETE_TARGET)
    void delete(@NotNull Long id);

    /**
     * @param ids list of ecu model ids
     *            For List of ECU Model Ids list
     * @return List of All {@link EcuModel}
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    List<EcuModel> getByIds(List<Long> ids);

    /**
     * will delete all the ecu models
     *
     * @throws org.eclipse.hawkbit.repository.exception.EntityNotFoundException in case if no ECU models exists.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_DELETE_TARGET)
    void deleteAll();


    /**
     * get existing assoication of vehicle Model Id with ecu models
     *
     * @throws org.eclipse.hawkbit.repository.exception.EntityNotFoundException in case if no ECU models exists.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    List<EcuModel> getAllEcuForVehicleModelId(Vehicle vehicleModelId, List<Long> ids);

    /*  get ECU model by node ids
     * @param ecuNodeIds
     * @return List of EcuModel
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.IS_CONTROLLER)
    Set<EcuModel> getEcuModelByNodeIds(Collection<String> ecuNodeIds);

    /**
     * Check if the ecu node address exists
     *
     * @param ecuNodeAddress - ecu node address to look for
     * @return true if found, false otherwise
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    boolean isEcuNodeAddressExists(String ecuNodeAddress);

    /**
     * Find all {@link EcuModel}s by RSQL query.
     *
     * @param pageable the pagination information
     * @param rsqlParam the RSQL query string
     * @return a page of ecus matching the RSQL query
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<EcuModel> findByRsql(String rsqlParam, Pageable pageable);

    long count();
}
