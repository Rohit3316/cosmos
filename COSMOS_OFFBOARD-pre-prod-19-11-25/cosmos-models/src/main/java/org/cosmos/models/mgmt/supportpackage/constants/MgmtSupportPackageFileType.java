package org.cosmos.models.mgmt.supportpackage.constants;

/**
 * Enum representing the different types of management support package files.
 * Each enum constant represents a specific file type and its associated category.
 */
public enum MgmtSupportPackageFileType {
    /**
     * Variant coding file type.
     * Category: ESP (ECU Specific Package)
     */
    VARIANT_CODING("ESP"),

    /**
     * License file type.
     * Category: ESP (ECU Specific Package)
     */
    LICENSE("ESP"),

    /**
     * UDS flow file type.
     * Category: ESP (ECU Specific Package)
     */
    UDS_FLOW("ESP"),

    /**
     * ECU script file type.
     * Category: ESP (ECU Specific Package)
     */
    ECU_SCRIPT("ESP"),

    /**
     * Baseline inventory file type.
     * Category: RSP (Rollout Specific Package)
     */
    BASELINE_INVENTORY("RSP"),

    /**
     * Installation rollback plan file type.
     * Category: RSP (Rollout Specific Package)
     */
    INSTALLATION_ROLLBACK_PLAN("RSP"),

    /**
     * DTC blacklist file type.
     * Category: RSP (Rollout Specific Package)
     */
    DTC_BLACKLIST("RSP"),

    /**
     * Rule engine configuration file type.
     * Category: RSP (Rollout Specific Package)
     */
    RULE_ENGINE_CONFIG("RSP"),

    /**
     * PROXI file type.
     * Category: RSP (Rollout Specific Package)
     */
    PROXI("RSP"),

    /**
     * PROXI signature file type.
     * Category: RSP (Rollout Specific Package)
     */
    PROXI_SIGNATURE("RSP"),

    /**
     * ADA certificate file type.
     * Category: RSP (Rollout Specific Package)
     */
    ADA_CERTIFICATE("ESP"),

    /**
     * ADA license file type.
     * Category: RSP (Rollout Specific Package)
     */
    ADA_LICENSE("ESP"),

    WHATS_NEW("RSP"),

    UDS_GLOBAL_PRE_INSTALL("RSP"),

    UDS_GLOBAL_POST_INSTALL("RSP");

    private final String category;

    /**
     * Constructor for MgmtSupportPackageFileType enum.
     *
     * @param category The category of the management support package file type.
     */
    MgmtSupportPackageFileType(String category) {
        this.category = category;
    }

    /**
     * Returns the category of the management support package file type.
     *
     * @return The category of the management support package file type.
     */
    public String getCategory() {
        return category;
    }

    /**
     * Returns the name of the enum constant.
     *
     * @return The name of the enum constant.
     */
    public String getName() {
        return name();
    }
}