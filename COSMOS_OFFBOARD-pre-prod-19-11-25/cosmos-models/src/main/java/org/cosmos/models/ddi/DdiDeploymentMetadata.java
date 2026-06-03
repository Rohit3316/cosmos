package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutType;

import static org.cosmos.models.ddi.DdiRestConstants.CONNECTIVITY_TYPE_CELLULAR_CAPS;
import static org.cosmos.models.ddi.DdiRestConstants.CONNECTIVITY_TYPE_WIFI_ONLY;
import static org.cosmos.models.ddi.DdiRestConstants.CONNECTIVITY_TYPE_WIFI_PREFERRED;

/**
 * This class represents a Deployment Descriptor's Deployment Metadata in the DDI model.
 * It includes properties like requiredStateOfCharge.
 * It is annotated with JsonIgnoreProperties to ignore unknown properties during deserialization.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class DdiDeploymentMetadata {


    /**
     * This attribute denotes Required State of Charge (Battery)
     * Conditions for Update
     */
    @JsonProperty("requiredStateOfCharge")
    @Valid
    private DdiRequiredStateOfCharge requiredStateOfCharge;


    /**
     * This attribute
     denotes whether the download
     * needs to happen from CDN or external USB
     */
    @JsonProperty(value = "requiredMedia", defaultValue = "0")
    @Schema(example = "0")
    private RequiredMedia requiredMedia;

    @Schema(description = "The estimated time  for the deployment update, derived from the rollout configuration")
    private Integer estimatedUpdateTime;
    /**
     * This attribute denotes Connectivity Preference for download
     */
    @JsonProperty(value = "connectivityType", defaultValue = "WIFI_PREFERRED")
    @NotNull
    @Schema(example = "WIFI_PREFERRED")
    private ConnectivityType connectivityType;

    /**
     * This attribute denotes Deployment / Rollout End Date
     */
    @JsonProperty("endDate")
    @Schema(example = "1718095596287")
    private Long endDate;

    /**
     * This attribute denotes Number of retires for the
     * onboard before downloaded stops to download and report failure
     */
    @JsonProperty("downloadRetryCount")
    @Schema(example = "3")
    private Integer downloadRetryCount;

    /**
     * This attribute denotes Number of day(s) from the start
     * of the download that the download has to complete else the
     * download needs to be discarded and failure needs to be reported by onboard
     */
    @JsonProperty("maxDownloadDurationTimer")
    @Schema(example = "5")
    private Integer maxDownloadDurationTimer;

    /**
     * This attribute denotes Number of day(s) from the start
     * of the download over wifi that the download has to complete else the
     * download needs to be discarded and failure needs to be reported by onboard
     */
    @JsonProperty("maxWifiDownloadDurationTimer")
    @Schema(example = "3")
    private Integer maxDownloadWifiDurationTimer;

    /**
     * This attribute denotes Number of day(s) from the start
     * of the download over cellular that the download has to complete else the
     * download needs to be discarded and failure needs to be reported by onboard
     */
    @JsonProperty("maxCellularDownloadDurationTimer")
    @Schema(example = "3")
    private Integer maxDownloadCellularDurationTimer;

    /**
     * This attribute denotes Maximum allowed time for the
     * update to complete including install and rollback time
     */
    @JsonProperty("maxUpdateTime")
    @Schema(example = "1800")
    private Long maxUpdateTime;

    /**
     * This attribute denotes To specify if the onboard is allowed
     * to perform a downgrade if the artifacts provided are lower
     * in version than the existing version on the ECU
     */
    @JsonProperty(value = "downgradeAllowed", defaultValue = "0")
    @Schema(example = "0")
    private DowngradeAllowed downgradeAllowed;

    /**
     * This attribute denotes configuration to deployment logs
     */
    @JsonProperty(value = "logs")
    private DdiDeploymentMetadataLogs logs;

    /**
     * This attribute denotes the expiry date of the certificate
     * used to sign the Deployment Descriptor (DD)
     */
    @JsonProperty("ddExpiryDate")
    @Schema(example = "1760008118344")
    private Long ddExpiryDate;

    /**
     * This attribute denotes the type of deployment, such as FOTA
     * (Firmware Over-The-Air) or AOTA (Application Over-The-Air).
     */
    @JsonProperty("type")
    @Schema(example = "FOTA")
    private MgmtRolloutType type;

    /**
     * The connectivity type for the rollout update action.
     */
    public enum ConnectivityType {

        /**
         * Wi-Fi as preferred connectivity.
         */
        WIFI_PREFERRED(CONNECTIVITY_TYPE_WIFI_PREFERRED),

        /**
         * Cellular Only connectivity.
         */
        CELLULAR(CONNECTIVITY_TYPE_CELLULAR_CAPS),

        /**
         * Wi-Fi Only connectivity.
         */
        WIFI_ONLY(CONNECTIVITY_TYPE_WIFI_ONLY);

        private final String name;

        ConnectivityType(final String name) {
            this.name = name;
        }

        @JsonValue
        public String getName() {
            return name;
        }
    }

    /**
     * Enum to specify If the download needs to happen from CDN or external USB
     */
    public enum RequiredMedia {
        FROM_CDN("0"),
        FROM_USB("1");

        private final String value;

        RequiredMedia(String value) {
            this.value = value;
        }

        @JsonCreator
        public static RequiredMedia fromValue(String value) {
            for (RequiredMedia media : RequiredMedia.values()) {
                if (media.value.equals(value)) {
                    return media;
                }
            }
            throw new IllegalArgumentException("Value not supported: " + value);
        }

        @JsonValue
        public String getValue() {
            return value;
        }
    }

    /**
     * Enum to specify whether DowngradeAllowed
     */
    public enum DowngradeAllowed {
        NO("0"),
        YES("1");

        private final String value;

        DowngradeAllowed(String value) {
            this.value = value;
        }

        @JsonCreator
        public static DowngradeAllowed fromValue(String value) {
            for (DowngradeAllowed allowed : DowngradeAllowed.values()) {
                if (allowed.value.equals(value)) {
                    return allowed;
                }
            }
            throw new IllegalArgumentException("Value not supported: " + value);
        }

        @JsonValue
        public String getValue() {
            return value;
        }
    }


}
