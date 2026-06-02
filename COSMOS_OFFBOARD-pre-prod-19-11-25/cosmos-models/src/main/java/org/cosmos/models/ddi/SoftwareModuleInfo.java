package org.cosmos.models.ddi;
import org.springframework.context.annotation.Description;

@Description("Represents metadata information about a software module, including details such as name, version, and associated artifacts.")
public class SoftwareModuleInfo {

    /**
     * The unique identifier for the software module.
     */
    private Long softwareModuleId;

    /**
     * Indicates whether the software module is visible to the target.
     */
    private boolean targetVisible;

    /**
     * Constructs a new SoftwareModuleInfo instance with the specified ID and visibility status.
     *
     * @param softwareModuleId the unique identifier for the software module
     * @param targetVisible whether the software module is visible to the target
     */
    public SoftwareModuleInfo(Long softwareModuleId, boolean targetVisible) {
        this.softwareModuleId = softwareModuleId;
        this.targetVisible = targetVisible;
    }

    /**
     * Gets the unique identifier for the software module.
     *
     * @return the software module ID
     */
    public Long getSoftwareModuleId() {
        return softwareModuleId;
    }

    /**
     * Sets the unique identifier for the software module.
     *
     * @param softwareModuleId the software module ID to set
     */
    public void setSoftwareModuleId(Long softwareModuleId) {
        this.softwareModuleId = softwareModuleId;
    }

    /**
     * Checks whether the software module is visible to the target.
     *
     * @return true if the software module is visible, false otherwise
     */
    public boolean isTargetVisible() {
        return targetVisible;
    }

    /**
     * Sets the visibility status of the software module for the target.
     *
     * @param targetVisible true to make the module visible, false otherwise
     */
    public void setTargetVisible(boolean targetVisible) {
        this.targetVisible = targetVisible;
    }
}
