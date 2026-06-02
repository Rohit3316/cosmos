package org.eclipse.hawkbit.mgmt.rest.resource.mapper;

import org.cosmos.models.mgmt.ecu.dto.MgmtEcuVehicleModelResponse;
import org.cosmos.models.mgmt.vehicle.dto.MgmtVechicleCreateResponse;
import org.cosmos.models.mgmt.vehicle.dto.MgmtVehicleRequest;
import org.cosmos.models.mgmt.vehicle.dto.MgmtVehicleResponse;
import org.eclipse.hawkbit.repository.jpa.model.JpaVehicle;
import org.eclipse.hawkbit.repository.model.Vehicle;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 */
public final class MgmtVehicleMapper {
    private MgmtVehicleMapper() {
    }

    public static List<Vehicle> fromVehicleRequest(List<MgmtVehicleRequest> vehicleModelRequest) {
        return vehicleModelRequest.stream().map(MgmtVehicleMapper::formVehicleRequest).toList();
    }

    public static Vehicle formVehicleRequest(MgmtVehicleRequest vehicleRequest) {
        return new JpaVehicle(vehicleRequest.getName(), vehicleRequest.getErcType());
    }

    public static List<MgmtVehicleResponse> mapToMgmtVehicleResponse(List<Vehicle> vehicleModels) {
        //"appendEcuModels" parameter is set to "true" so that by default we receive the list of associated "ecuModels" for "vehicleModel"
        return toMgmtVehicleResponse(vehicleModels, true);
    }

    public static List<MgmtVechicleCreateResponse> toMgmtVehicleCreateResponse(List<Vehicle> vehicleModels) {
        if (vehicleModels.isEmpty()) {
            return Collections.emptyList();
        }
        return vehicleModels.stream()
                .map(response -> {
                    MgmtVechicleCreateResponse.MgmtVechicleCreateResponseBuilder responseBuilder = MgmtVechicleCreateResponse.builder()
                            .id(response.getId())
                            .name(response.getName())
                            .ercType(response.getErcType());
                    return responseBuilder.build();
                }).collect(Collectors.toList());
    }


    /**
     * This method is used to map to List of  vehicleModels along with List of ecuModels in the response
     * based on the parameter of appendEcuModels
     *
     * @param vehicleModels
     * @param appendEcuModels
     * @return List of VehicleResponse
     */
    // Updated method for mapping Vehicle to MgmtVehicleResponse

    public static List<MgmtVehicleResponse> toMgmtVehicleResponse(List<Vehicle> vehicleModels, boolean appendEcuModels) {
        if (vehicleModels.isEmpty()) {
            return Collections.emptyList();
        }
        return vehicleModels.stream()
                .map(vehicle -> {
                    MgmtVehicleResponse.MgmtVehicleResponseBuilder mgmtVehicleResponseBuilder = MgmtVehicleResponse.builder()
                            .id(vehicle.getId())
                            .name(vehicle.getName())
                            .ercType(vehicle.getErcType());

                    if (appendEcuModels) {

                        List<MgmtEcuVehicleModelResponse> ecuModels = MgmtEcuModelMapper.toMgmtEcuModelResponse(vehicle.getVehicleEcu());
                        mgmtVehicleResponseBuilder.ecuModels(ecuModels);
                    }

                    return mgmtVehicleResponseBuilder.build();
                }).toList();
    }

    /**
     * Converts a list of Vehicle models to a list of MgmtEcuVehicleModelResponse objects.
     *
     * @param vehicleModels the list of Vehicle models to be converted
     * @return a list of MgmtEcuVehicleModelResponse objects; if the input list is empty, returns an empty list
     */
    public static List<org.cosmos.models.mgmt.vehicle.dto.MgmtEcuVehicleModelResponse> toMgmtVehicleResponse(List<Vehicle> vehicleModels) {
        if (vehicleModels.isEmpty()) {
            return Collections.emptyList();
        }
        return vehicleModels.stream()
                .map(response -> org.cosmos.models.mgmt.vehicle.dto.MgmtEcuVehicleModelResponse.builder()
                        .id(response.getId())
                        .name(response.getName())
                        .build())
                .toList();
    }

}