package org.cosmos.models.mgmt.rollout.constants;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

import static org.cosmos.models.mgmt.MgmtRestConstants.CONNECTIVITY_TYPE_CELLULAR;
import static org.cosmos.models.mgmt.MgmtRestConstants.CONNECTIVITY_TYPE_WIFI;
import static org.cosmos.models.mgmt.MgmtRestConstants.CONNECTIVITY_TYPE_WIFI_PREFERRED;

/**
 * Definition of the Connectivity type for the REST management API.
 *
 */
public enum MgmtRolloutConnectivityType {
    /**
     * The Wi-Fi type.
     */
    WIFI_ONLY(CONNECTIVITY_TYPE_WIFI),

    /**
     * The cellular type.
     */
    CELLULAR(CONNECTIVITY_TYPE_CELLULAR),

    /**
     * Default Type WIFI.
     */
    WIFI_PREFERRED(CONNECTIVITY_TYPE_WIFI_PREFERRED);

    @Schema(example = "xyz")
    private final String name;

    MgmtRolloutConnectivityType(final String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }

}