package org.eclipse.hawkbit.mgmt.rest.resource;

/**
 * This enum represents the different types of ECU (Electronic Control Unit) models.
 * Each enum value represents a specific type of ECU model and is associated with a type name.
 * This class is used only for testing purpose to cover test cases
 */
public enum EcuModelTypeEnum {

    /**
     * Small Target ECU model type.
     */
    ST("Small Target"),

    /**
     * Big Target ECU model type.
     */
    BT("Big Target"),

    /**
     * Big Target without Downloader ECU model type.
     */
    BTND("Big Target without Downloader"),

    /**
     * Big Target Multi Hardware without Downloader ECU model type.
     */
    BTMHND("Big Target Multi Hardware without Downloader"),

    /**
     * Big Target with Downloader ECU model type.
     */
    BTD("Big Target with Downloader"),

    /**
     * Big Target Multi Software with Downloader ECU model type.
     */
    BTMSD("Big Target Multi Software with Downloader"),

    /**
     * Big Target Multi Hardware with Downloader ECU model type.
     */
    BTMHD("Big Target Multi Hardware with Downloader"),

    /**
     * OTA Master ECU model type.
     */
    OM("OTA Master"),

    /**
     * Self Updater ECU model type.
     */
    SU("Self Updater");

    /**
     * The name of the ECU model type.
     */
    private final String typeName;

    /**
     * Constructor for the EcuModelType enum.
     * @param typeName The name of the ECU model type.
     */
    EcuModelTypeEnum(String typeName){
        this.typeName = typeName;
    }

    /**
     * Getter for the name of the ECU model type.
     * @return The name of the ECU model type.
     */
    public String getTypeName() {
        return typeName;
    }
}