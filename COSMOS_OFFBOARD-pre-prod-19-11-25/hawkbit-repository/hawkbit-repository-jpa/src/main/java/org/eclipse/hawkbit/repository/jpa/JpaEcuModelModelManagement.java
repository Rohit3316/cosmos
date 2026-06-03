package org.eclipse.hawkbit.repository.jpa;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.validation.ValidationException;

import org.eclipse.hawkbit.repository.EcuModelFields;
import org.eclipse.hawkbit.repository.EcuModelManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaEcuModel;
import org.eclipse.hawkbit.repository.jpa.model.JpaEcuModelType;
import org.eclipse.hawkbit.repository.jpa.model.JpaVehicle;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.model.EcuModel;
import org.eclipse.hawkbit.repository.model.EcuModelType;
import org.eclipse.hawkbit.repository.model.Vehicle;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link EcuModelManagement}.
 */
@Transactional
@Validated
public class JpaEcuModelModelManagement implements EcuModelManagement {


    private final EcuModelRepository ecuModelRepository;

    private final EcuModelTypeRepository ecuModelTypeRepository;

    private final VehicleRepository vehicleRepository;

    private final SoftwareModuleRepository softwareModuleRepository;

    private final VirtualPropertyReplacer virtualPropertyReplacer;

    private final Database database;

    public JpaEcuModelModelManagement(final EcuModelRepository ecuModelRepository, final EcuModelTypeRepository ecuModelTypeRepository, VehicleRepository vehicleRepository, SoftwareModuleRepository softwareModuleRepository, VirtualPropertyReplacer virtualPropertyReplacer,final Database database) {
        this.ecuModelRepository = ecuModelRepository;
        this.ecuModelTypeRepository = ecuModelTypeRepository;
        this.vehicleRepository = vehicleRepository;
        this.softwareModuleRepository = softwareModuleRepository;
        this.virtualPropertyReplacer=virtualPropertyReplacer;
        this.database=database;
    }

    @Override
    public List<EcuModel> create(List<EcuModel> ecuModelModels) {
        List<JpaEcuModel> jpaEcuModels = ecuModelModels.stream().map(JpaEcuModel.class::cast).toList();
        jpaEcuModels.forEach(ecuModelRequest -> ecuModelRequest.setEcuModelType(
                (JpaEcuModelType) getEcuModelTypeByNameAndThrowIfNotFound(ecuModelRequest.getEcuModelType())));
        return Collections.unmodifiableList(ecuModelRepository.saveAll(jpaEcuModels));
    }

    @Override
    public void update(EcuModel ecuModel, Long ecuModelId) {
        long softwareModuleCount = softwareModuleRepository.countBySoftwareEcuModels(ecuModelId);
        if (vehicleRepository.existsByVehicleEcuId(ecuModelId) || softwareModuleCount > 0) {
            throw new ValidationException("EcuModel cannot be updated as it has associations with VehicleModel or SoftwareModel");
        }
        final JpaEcuModel jpaEcuModel = (JpaEcuModel) getEcuModelByIdAndThrowIfNotFound(ecuModelId);
        JpaEcuModelType ecuModelType = (JpaEcuModelType) getEcuModelTypeByNameAndThrowIfNotFound(ecuModel.getEcuModelType());
        jpaEcuModel.setEcuModelType(ecuModelType);
        jpaEcuModel.setEcuModelName(ecuModel.getEcuModelName());
        jpaEcuModel.setEcuNodeId(ecuModel.getEcuNodeId());
        ecuModelRepository.save(jpaEcuModel);
    }

    @Override
    public Optional<EcuModel> get(long id) {
        return ecuModelRepository.findById(id).map(v -> v);
    }

    public List<EcuModel> getEcuModelByIdsAndThrowIfNotFound(Collection<Long> ids) {
        return ids.stream()
                .map(id -> ecuModelRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException(EcuModel.class, id)))
                .collect(Collectors.toList());
    }

    @Override
    public List<EcuModel> findAll(Pageable pageable) {
        List<JpaEcuModel> ecuModels = ecuModelRepository.findAll(pageable).toList();
        return Collections.unmodifiableList(ecuModels);
    }

    @Override
    public void delete(Long id) {
        getEcuModelByIdAndThrowIfNotFound(id);
        if (vehicleRepository.existsByVehicleEcuId(id)) {
            throw new ValidationException("EcuModel cannot be deleted as it has associations with VehicleModel");
        }
        if (softwareModuleRepository.countBySoftwareEcuModels(id) > 0) {
            throw new ValidationException("EcuModel cannot be deleted as it has associations with SoftwareModule");
        }
        ecuModelRepository.deleteById(id);
    }

    @Override
    public List<EcuModel> getByIds(List<Long> ids) {
        return Collections.unmodifiableList(ecuModelRepository.findAllById(ids));
    }

    @Override
    public void deleteAll() {
        ecuModelRepository.deleteAll();
    }

    @Override
    public List<EcuModel> getAllEcuForVehicleModelId(Vehicle vehicleModelId, List<Long> ids) {
        return Collections.unmodifiableList(ecuModelRepository.findByVehicleModelAndIds((JpaVehicle) vehicleModelId, ids));
    }

    @Override
    public Set<EcuModel> getEcuModelByNodeIds(Collection<String> ecuNodeIds) {
        return new HashSet<>(ecuModelRepository.findByEcuNodeId(new HashSet<>(ecuNodeIds)));
    }

    @Override
    public boolean isEcuNodeAddressExists(String ecuNodeAddress) {
        return ecuModelRepository.existsByEcuNodeId(ecuNodeAddress);
    }

    private EcuModel getEcuModelByIdAndThrowIfNotFound(Long id) {
        return ecuModelRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(EcuModel.class, id));
    }

    /**
     * @param name is ecuModelType
     * @return the object of {@link EcuModelType} which has mapped id of given name
     * @throws ValidationException if the parameter name doesn't support or exists in the system
     */
    private EcuModelType getEcuModelTypeByNameAndThrowIfNotFound(String name) {
        return ecuModelTypeRepository.findByName(name)
                .orElseThrow(() -> new ValidationException("Invalid Ecu Model Type: " + name));
    }

    @Override
    public Page<EcuModel> findByRsql(String rsqlParam, Pageable pageable) {
        final List<Specification<JpaEcuModel>> specList = List.of(RSQLUtility.buildRsqlSpecification(rsqlParam, EcuModelFields.class,
                virtualPropertyReplacer, database));
        return JpaManagementHelper.findAllWithCountBySpec(ecuModelRepository, pageable, specList);
    }

    @Override
    public long count() {
        return ecuModelRepository.count();
    }
}
