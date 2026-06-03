package org.cosmos.models.mgmt;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON model for Management API to define the maintenance window.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtMaintenanceWindow extends MgmtMaintenanceWindowRequestBody {

    /**
     * Time in {@link TimeUnit#SECONDS} of the next maintenance window
     * start
     */
    @JsonProperty
    @Schema(example = "1691065905")
    private long nextStartAt;

    public long getNextStartAt() {
        return nextStartAt;
    }

    public void setNextStartAt(final long nextStartAt) {
        this.nextStartAt = nextStartAt;
    }
}
