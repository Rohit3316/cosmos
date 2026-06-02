package org.eclipse.hawkbit.mgmt.rest.resource;

import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ValidationException;

import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.PagedList;
import org.cosmos.models.mgmt.ecu.dto.MgmtCreateEcuModelResponse;
import org.cosmos.models.mgmt.ecu.dto.MgmtEcuModelRequest;
import org.cosmos.models.mgmt.ecu.dto.MgmtEcuModelResponse;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtEcuRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtEcuModelMapper;
import org.eclipse.hawkbit.pagination.PagingUtility;
import org.eclipse.hawkbit.repository.EcuModelManagement;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.EcuModel;
import org.eclipse.hawkbit.rest.swagger.SwaggerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = SwaggerConstants.ECU)
public class MgmtEcuResource implements MgmtEcuRestApi {

    private static final Logger LOG = LoggerFactory.getLogger(MgmtEcuResource.class);

    @Autowired
    private EcuModelManagement ecuModelManagement;

    @Override
    @ResponseStatus(HttpStatus.CREATED)
    public List<MgmtCreateEcuModelResponse> addEcuModels(List<MgmtEcuModelRequest> ecuModelsRequests) {
        ecuModelsRequests.forEach(ecuModelRequest -> {
            if (ecuModelRequest.getEcuModelType() == null) {
                throw new ValidationException("Ecu Model Type is Mandatory");
            }
        });
        try {
            List<EcuModel> ecuModelResponses = ecuModelManagement.create(MgmtEcuModelMapper.fromEcuModelRequests(ecuModelsRequests));
            LOG.debug("ECU Model/s added successfully");
            return MgmtEcuModelMapper.toMgmtCreateEcuModelResponse(ecuModelResponses);
        } catch (EntityAlreadyExistsException e) {
            LOG.error("One or More ECU Model/s already exists ");
            throw new EntityAlreadyExistsException("One or More ECU Model/s already exists ");
        }

    }


    @Override
    public ResponseEntity<Void> updateEcuModel(Long ecuModelId, MgmtEcuModelRequest ecuModelsRequest) {

        ecuModelManagement.update(MgmtEcuModelMapper.toEcuModelRequest(ecuModelsRequest), ecuModelId);
        LOG.debug("ECU Model with id: {} updated successfully", ecuModelId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<MgmtEcuModelResponse>> getEcuModel(Long ecuModelId) {
        EcuModel ecuModel = ecuModelManagement.get(ecuModelId).orElseThrow(() ->
                new EntityNotFoundException(EcuModel.class, ecuModelId));
        LOG.debug("ECU Model with id: {} fetched successfully", ecuModelId);
        //Parameter "appendVehicles" is added to indicate whether Vehicles has to be added to the EcuModelResponse or not.
        return ResponseEntity.ok(MgmtEcuModelMapper.toMgmtEcuModelResponse(List.of(ecuModel), true));
    }

    @Override
    public ResponseEntity<PagedList<MgmtEcuModelResponse>> getAllEcuModels(@RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
                                                                           @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
                                                                           @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
                                                                           @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeECUModuleSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        Page<EcuModel> ecuModels;
        final long countModulesAll;

        if (rsqlParam != null) {
            ecuModels = ecuModelManagement.findByRsql(rsqlParam, pageable);
            countModulesAll = ecuModels.getTotalElements();
        } else {
            ecuModels = new PageImpl<>(ecuModelManagement.findAll(pageable));
            countModulesAll = ecuModelManagement.count();
        }

        if (ecuModels.isEmpty()) {
            throw new EntityNotFoundException("No ECU Models found");
        }
        LOG.debug("All ECU Models fetched successfully");
        return ResponseEntity.ok(new PagedList<>(MgmtEcuModelMapper.toMgmtEcuModelResponse(ecuModels.getContent(), true), countModulesAll));
    }

    @Override
    public ResponseEntity<Void> deleteEcuModel(Long ecuModelId) {
        ecuModelManagement.delete(ecuModelId);
        LOG.debug("ECU Model with id: {} deleted successfully", ecuModelId);
        return ResponseEntity.ok().build();
    }
}
