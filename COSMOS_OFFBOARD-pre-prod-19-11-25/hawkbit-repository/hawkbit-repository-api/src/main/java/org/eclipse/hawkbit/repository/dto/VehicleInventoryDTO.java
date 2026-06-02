package org.eclipse.hawkbit.repository.dto;

/**
 * Data Transfer Object representing vehicle inventory details.
 */
public class VehicleInventoryDTO {

    /**
     * Serial number of the vehicle ECU.
     */
    private String serialNumber;

    /**
     * Node ID of the ECU.
     */
    private String nodeId;

    /**
     * Type of the ECU model.
     */
    private String modelType;

    /**
     * Name of the ECU model.
     */
    private String modelName;

    /**
     * SCOMO ID associated with the ECU.
     */
    private String scomoId;

    /**
     * Reported software version of the ECU.
     */
    private String reportedVersion;

    /**
     * Default constructor.
     */
    public VehicleInventoryDTO() {}

    /**
     * Parameterized constructor to initialize all fields.
     *
     * @param serialNumber serial number of the ECU
     * @param nodeId node ID of the ECU
     * @param modelType type of the ECU model
     * @param modelName name of the ECU model
     * @param scomoId SCOMO ID associated with the ECU
     * @param reportedVersion reported software version of the ECU
     */
    public VehicleInventoryDTO(String serialNumber, String nodeId, String modelType, String modelName, String scomoId, String reportedVersion) {
        this.serialNumber = serialNumber;
        this.nodeId = nodeId;
        this.modelType = modelType;
        this.modelName = modelName;
        this.scomoId = scomoId;
        this.reportedVersion = reportedVersion;
    }

    /**
     * Gets the serial number.
     *
     * @return serial number
     */
    public String getSerialNumber() { return serialNumber; }

    /**
     * Sets the serial number.
     *
     * @param serialNumber serial number to set
     */
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    /**
     * Gets the node ID.
     *
     * @return node ID
     */
    public String getNodeId() { return nodeId; }

    /**
     * Sets the node ID.
     *
     * @param nodeId node ID to set
     */
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    /**
     * Gets the model type.
     *
     * @return model type
     */
    public String getModelType() { return modelType; }

    /**
     * Sets the model type.
     *
     * @param modelType model type to set
     */
    public void setModelType(String modelType) { this.modelType = modelType; }

    /**
     * Gets the model name.
     *
     * @return model name
     */
    public String getModelName() { return modelName; }

    /**
     * Sets the model name.
     *
     * @param modelName model name to set
     */
    public void setModelName(String modelName) { this.modelName = modelName; }

    /**
     * Gets the SCOMO ID.
     *
     * @return SCOMO ID
     */
    public String getScomoId() { return scomoId; }

    /**
     * Sets the SCOMO ID.
     *
     * @param scomoId SCOMO ID to set
     */
    public void setScomoId(String scomoId) { this.scomoId = scomoId; }

    /**
     * Gets the reported software version.
     *
     * @return reported software version
     */
    public String getReportedVersion() { return reportedVersion; }

    /**
     * Sets the reported software version.
     *
     * @param reportedVersion reported software version to set
     */
    public void setReportedVersion(String reportedVersion) { this.reportedVersion = reportedVersion; }
}
