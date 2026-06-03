/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;
import jakarta.validation.constraints.NotNull;

import static org.cosmos.models.ddi.DdiRestConstants.CONNECTIVITY_TYPE_BOTH;
import static org.cosmos.models.ddi.DdiRestConstants.CONNECTIVITY_TYPE_CELLULAR;
import static org.cosmos.models.ddi.DdiRestConstants.CONNECTIVITY_TYPE_WIFI;


/**
 * Detailed update action information.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiDeployment {

    private HandlingType download;

    private HandlingType update;

    @JsonProperty("chunks")
    @NotNull
    private List<DdiChunk> chunks;

    @JsonProperty("ddiMetadata")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DdiDsMetadata datasetMetadata;

    @JsonProperty("connectivityType")
    @NotNull
    private ConnectivityType connectivityType;

    private DdiMaintenanceWindowStatus maintenanceWindow;

    /**
     * Constructor.
     */
    public DdiDeployment() {
        // needed for json create.
    }

    /**
     * Constructor.
     *
     * @param download          handling type
     * @param update            handling type
     * @param chunks            to handle.
     * @param maintenanceWindow specifying whether there is a maintenance schedule associated.
     *                          If it is, the value is either 'available' (i.e. the
     *                          maintenance window is now available as per defined schedule
     *                          and the update can progress) or 'unavailable' (implying that
     *                          maintenance window is not available now and update should not
     *                          be attempted). If there is no maintenance schedule defined,
     *                          the parameter is null.
     */
    public DdiDeployment(final HandlingType download, final HandlingType update, final List<DdiChunk> chunks,
                         final DdiMaintenanceWindowStatus maintenanceWindow) {
        this.download = download;
        this.update = update;
        this.chunks = chunks;
        this.maintenanceWindow = maintenanceWindow;
    }

    /**
     * Constructor.
     *
     * @param download          handling type
     * @param update            handling type
     * @param chunks            to handle.
     * @param maintenanceWindow specifying whether there is a maintenance schedule associated.
     *                          If it is, the value is either 'available' (i.e. the
     *                          maintenance window is now available as per defined schedule
     *                          and the update can progress) or 'unavailable' (implying that
     *                          maintenance window is not available now and update should not
     *                          be attempted). If there is no maintenance schedule defined,
     *                          the parameter is null.
     * @param datasetMetadata   optional as distribution set additional information for the target/device
     */
    public DdiDeployment(final HandlingType download, final HandlingType update, final List<DdiChunk> chunks,
                         final DdiMaintenanceWindowStatus maintenanceWindow, final DdiDsMetadata datasetMetadata) {
        this.download = download;
        this.update = update;
        this.chunks = chunks;
        this.maintenanceWindow = maintenanceWindow;
        this.datasetMetadata = datasetMetadata;
    }

    /**
     * Constructor.
     *
     * @param download          handling type
     * @param update            handling type
     * @param chunks            to handle.
     * @param maintenanceWindow specifying whether there is a maintenance schedule associated.
     *                          If it is, the value is either 'available' (i.e. the
     *                          maintenance window is now available as per defined schedule
     *                          and the update can progress) or 'unavailable' (implying that
     *                          maintenance window is not available now and update should not
     *                          be attempted). If there is no maintenance schedule defined,
     *                          the parameter is null.
     * @param datasetMetadata   optional as distribution set additional information for the target/device
     */
    public DdiDeployment(final HandlingType download, final HandlingType update, final List<DdiChunk> chunks,
                         final DdiMaintenanceWindowStatus maintenanceWindow, final DdiDsMetadata datasetMetadata,
                         final ConnectivityType connectivityType) {
        this.download = download;
        this.update = update;
        this.chunks = chunks;
        this.maintenanceWindow = maintenanceWindow;
        this.datasetMetadata = datasetMetadata;
        this.connectivityType = connectivityType;
    }

    public HandlingType getDownload() {
        return download;
    }

    public HandlingType getUpdate() {
        return update;
    }

    public List<DdiChunk> getChunks() {
        if (chunks == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(chunks);
    }

    public DdiDsMetadata getDatasetMetadata() {
        return datasetMetadata;
    }

    public DdiMaintenanceWindowStatus getMaintenanceWindow() {
        return this.maintenanceWindow;
    }

    public ConnectivityType getConnectivityType() {
        return connectivityType;
    }

    /**
     * @return string
     * @author T7437JK
     * @Modified on 03/08/2023
     * added datasetMetadata field
     */
    @Override
    public String toString() {
        return "Deployment [download=" + download + ", update=" + update + ", chunks=" + chunks
                + (datasetMetadata == null ? "" : (", ddiMetadata=" + datasetMetadata))
                + ", connectivityType=" + connectivityType
                + (maintenanceWindow == null ? "]" : (", maintenanceWindow=" + maintenanceWindow + "]"));
    }

    /**
     * The handling type for the update action.
     */
    public enum HandlingType {

        /**
         * Not necessary for the command.
         */
        SKIP("skip"),

        /**
         * Try to execute (local applications may intervene by SP control API).
         */
        ATTEMPT("attempt"),

        /**
         * Execution independent of local intervention attempts.
         */
        FORCED("forced");

        @Schema(example = "xyz")
        private final String name;

        HandlingType(final String name) {
            this.name = name;
        }

        @JsonValue
        public String getName() {
            return name;
        }
    }

    /**
     * Status of the maintenance window for action.
     */
    public enum DdiMaintenanceWindowStatus {
        /**
         * A window is currently available, target can go ahead with
         * installation.
         */
        AVAILABLE("available"),

        /**
         * A window is not available, target should wait and skip the
         * installation.
         */
        UNAVAILABLE("unavailable");

        private final String status;

        DdiMaintenanceWindowStatus(final String status) {
            this.status = status;
        }

        /**
         * @return status of maintenance window.
         */
        @JsonValue
        public String getStatus() {
            return this.status;
        }
    }

    /**
     * The connectivity type for the update action.
     */
    public enum ConnectivityType {

        /**
         * Wi-Fi connectivity.
         */
        WIFI(CONNECTIVITY_TYPE_WIFI),

        /**
         * Cellular connectivity.
         */
        CELLULAR(CONNECTIVITY_TYPE_CELLULAR),

        /**
         * Wi-Fi/Cellular connectivity.
         */
        BOTH(CONNECTIVITY_TYPE_BOTH);

        private final String name;

        ConnectivityType(final String name) {
            this.name = name;
        }

        @JsonValue
        public String getName() {
            return name;
        }
    }

}