package org.eclipse.hawkbit.mgmt.rest.resource.mapper;

import org.cosmos.models.mgmt.ecu.dto.MgmtCreateEcuModelResponse;
import org.cosmos.models.mgmt.ecu.dto.MgmtEcuModelRequest;
import org.cosmos.models.mgmt.ecu.dto.MgmtEcuModelResponse;
import org.cosmos.models.mgmt.ecu.dto.MgmtEcuVehicleModelResponse;
import org.eclipse.hawkbit.repository.jpa.model.JpaEcuModel;
import org.eclipse.hawkbit.repository.jpa.model.JpaEcuModelType;
import org.eclipse.hawkbit.repository.model.EcuModel;

import java.util.List;
import java.util.Set;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 */
public final class MgmtEcuModelMapper {
    private MgmtEcuModelMapper() {
    }

    public static List<EcuModel> fromEcuModelRequests(List<MgmtEcuModelRequest> ecuModelRequests) {
        return ecuModelRequests.stream().map(MgmtEcuModelMapper::toEcuModelRequest).toList();
    }

    public static EcuModel toEcuModelRequest(MgmtEcuModelRequest ecuModelRequest) {
        return new JpaEcuModel(new JpaEcuModelType(ecuModelRequest.getEcuModelType()), ecuModelRequest.getEcuModelName(), ecuModelRequest.getEcuNodeId());
    }

    public static List<MgmtCreateEcuModelResponse> toMgmtCreateEcuModelResponse(List<EcuModel> ecuModelResponses) {
        return ecuModelResponses.stream().map(response -> {
            MgmtCreateEcuModelResponse.MgmtCreateEcuModelResponseBuilder mgmtEcuModelResponseBuilder = MgmtCreateEcuModelResponse.builder().id(response.getId())
                    .ecuModelName(response.getEcuModelName()).ecuNodeId(response.getEcuNodeId()).ecuModelType(response.getEcuModelType());

            return mgmtEcuModelResponseBuilder.build();
        }).toList();
    }

    public static List<MgmtEcuVehicleModelResponse> toMgmtEcuModelResponse(Set<EcuModel> ecuModelResponses) {
        return ecuModelResponses.stream()
                .map(response -> MgmtEcuVehicleModelResponse.builder()
                        .id(response.getId())
                        .ecuModelType(response.getEcuModelType())
                        .ecuModelName(response.getEcuModelName())
                        .ecuNodeId(response.getEcuNodeId())
                        .build())
                .toList();
    }

    /**
     * This method is used  to map list of ecuModels along with list of associated vehicleModels in the response
     * based on the parameter of appendVehicles
     *
     * @param ecuModelResponses
     * @param appendVehicles
     * @return List of ecuModelResponse
     */
    public static List<MgmtEcuModelResponse> toMgmtEcuModelResponse(List<EcuModel> ecuModelResponses, boolean appendVehicles) {
        return ecuModelResponses.stream().map(response -> {
            MgmtEcuModelResponse.MgmtEcuModelResponseBuilder mgmtEcuModelResponseBuilder = MgmtEcuModelResponse.builder().id(response.getId())
                    .ecuModelName(response.getEcuModelName()).ecuNodeId(response.getEcuNodeId()).ecuModelType(response.getEcuModelType());

            if (appendVehicles) {
                mgmtEcuModelResponseBuilder.vehicleModels(MgmtVehicleMapper.toMgmtVehicleResponse(response.getVehicleModel()));
            }
            return mgmtEcuModelResponseBuilder.build();
        }).toList();
    }

    public static List<MgmtEcuModelResponse> toMgmtSoftwareEcuModelResponse(Set<EcuModel> ecuModelResponses) {
        return ecuModelResponses.stream().map(response -> MgmtEcuModelResponse.builder().id(response.getId())
                .ecuModelName(response.getEcuModelName()).ecuModelType(response.getEcuModelType())
                .ecuNodeId(response.getEcuNodeId()).build()).toList();
    }

}
