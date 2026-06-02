package org.eclipse.hawkbit.mgmt.rest.resource;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;

import jakarta.validation.constraints.NotNull;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.PagedList;
import org.cosmos.models.mgmt.vehicle.dto.EcuModelAssignmentRequest;
import org.cosmos.models.mgmt.vehicle.dto.MgmtVechicleCreateResponse;
import org.cosmos.models.mgmt.vehicle.dto.MgmtVehicleRequest;
import org.cosmos.models.mgmt.vehicle.dto.MgmtVehicleResponse;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtVehicleRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtVehicleMapper;
import org.eclipse.hawkbit.pagination.PagingUtility;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.VehicleManagement;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.VehicleRepository;
import org.eclipse.hawkbit.repository.model.Vehicle;
import org.eclipse.hawkbit.rest.swagger.SwaggerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = SwaggerConstants.VEHICLE)
public class MgmtVehicleResource implements MgmtVehicleRestApi {

    private static final Logger LOG = LoggerFactory.getLogger(MgmtVehicleResource.class);

    @Autowired
    private VehicleManagement vehicleManagement;
    @Autowired
    private VehicleRepository vehicleRepository;

    @Override
    public ResponseEntity<List<MgmtVechicleCreateResponse>> addVehicleModels(
            List<MgmtVehicleRequest> vehicleModelsRequest) {

        try {
            List<Vehicle> vehicleModels =
                    vehicleManagement.create(MgmtVehicleMapper.fromVehicleRequest(vehicleModelsRequest));

            LOG.debug("Vehicle Model/s added successfully");

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(MgmtVehicleMapper.toMgmtVehicleCreateResponse(vehicleModels));

        } catch (EntityAlreadyExistsException e) {

            Throwable cause = e;
            while (cause != null) {
                // Check the type of the cause
                if (cause instanceof DuplicateKeyException) {
                    throw new DuplicateKeyException(
                            "One or More Vehicle Model/s Name already exists"
                    );
                } else if (cause instanceof DataIntegrityViolationException) {
                    throw new DataIntegrityViolationException(
                            "Cannot add Vehicle Model: specified ERC type does not exist."
                    );
                }

                cause = cause.getCause(); // move to next cause
            }

            // fallback if no known cause found
            LOG.error("Unexpected EntityAlreadyExistsException: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();

        } catch (Exception e) {
            LOG.error("Unexpected error adding vehicle model(s): {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @Override
    public ResponseEntity<Void> updateVehicleModel(Long vehicleModelId, MgmtVehicleRequest vehicleModelRequest) {
        vehicleManagement.update(MgmtVehicleMapper.formVehicleRequest(vehicleModelRequest), vehicleModelId);
        LOG.debug("Vehicle Model with id: {} updated successfully", vehicleModelId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> deleteVehicleModel(Long vehicleModelId) {
        vehicleManagement.delete(vehicleModelId);
        LOG.debug("Vehicle Model with id: {} deleted successfully", vehicleModelId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<MgmtVehicleResponse>> getVehicleModel(Long vehicleModelId) {
        final Vehicle vehicle = vehicleManagement.get(vehicleModelId).orElseThrow(() ->
                new EntityNotFoundException(Vehicle.class, vehicleModelId));
        LOG.debug("Vehicle Model with id: {} fetched successfully", vehicleModelId);
        return ResponseEntity.ok(MgmtVehicleMapper.mapToMgmtVehicleResponse(List.of(vehicle)));
    }

    @Override
    public ResponseEntity<PagedList<MgmtVehicleResponse>> getAllVehicleModels(@RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
                                                                              @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
                                                                              @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
                                                                              @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeVehicleSortParam(sortParam);


        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final List<Vehicle> vehicleModels = vehicleManagement.findAll(pageable);
        LOG.debug("Fetching all Vehicle Models with offset: {}, limit: {}, sort: {}", sanitizedOffsetParam, sanitizedLimitParam, sorting);
        if (vehicleModels.isEmpty()) {
            LOG.debug("No Vehicle Models found");
            throw new ValidationException("No Vehicle Models found");
        }
        final Page<Vehicle> vehicleModelList;
        final long countModulesAll;


        if (rsqlParam != null) {
            vehicleModelList = vehicleManagement.findByRsql(pageable, rsqlParam);
            countModulesAll = vehicleModelList.getTotalElements();
        } else {
            vehicleModelList = new PageImpl<>(vehicleManagement.findAll(pageable));
            countModulesAll = vehicleManagement.count();
        }

        LOG.debug("All Vehicle Models fetched successfully");
        final List<MgmtVehicleResponse> vehicleResponseList = MgmtVehicleMapper.mapToMgmtVehicleResponse(vehicleModelList.getContent());
        return ResponseEntity.ok(new PagedList<>(vehicleResponseList, countModulesAll));
    }

    @Override
    public ResponseEntity<Void> assignEcuModelToVehicleModel(@NotNull @PathVariable("vehicleModelId") final Long vehicleModelId, @Valid @RequestBody List<EcuModelAssignmentRequest> ecuModelsList) {
        List<Long> ecuModelsListsLong = getEcuModelListLongOrThrowException(ecuModelsList);
        vehicleManagement.assignEcuModels(vehicleModelId, ecuModelsListsLong);
        LOG.debug("ECU Models {} assigned successfully to the Vehicle Model {}", ecuModelsList, vehicleModelId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> deleteVehicleEcuAssociation(Long vehicleModelId, List<EcuModelAssignmentRequest> ecuModelsList) {
        List<Long> ecuModelsListsLong = getEcuModelListLongOrThrowException(ecuModelsList);
        vehicleManagement.deleteEcuModels(vehicleModelId, ecuModelsListsLong);
        LOG.debug("ECU Models {} successfully removed for the Vehicle Model {}", ecuModelsList, vehicleModelId);
        return ResponseEntity.ok().build();
    }

    private List<Long> getEcuModelListLongOrThrowException(List<EcuModelAssignmentRequest> ecuModelAssignmentList) {
        List<Long> ecuModelsListsLong = ecuModelAssignmentList.stream().map(ecuModel -> Long.parseLong(ecuModel.getEcuModelId().toString())).toList();
        if (ecuModelsListsLong.isEmpty()) {
            throw new ValidationException("ECU Model List is empty");
        }
        return ecuModelsListsLong;
    }
}