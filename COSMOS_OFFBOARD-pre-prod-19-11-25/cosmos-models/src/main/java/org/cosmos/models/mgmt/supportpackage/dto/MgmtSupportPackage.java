package org.cosmos.models.mgmt.supportpackage.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jdk.jfr.Description;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.cosmos.models.mgmt.MgmtBaseEntity;

import java.util.List;
import java.util.Map;


/**
 * Represents a support package in the management system, containing details such as the file name, URL, and metadata.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
@Description("Represents a support package in the management system.")
public class MgmtSupportPackage extends MgmtBaseEntity {


    private String type;
    /**
     * The ID of the support package.
     */
    @JsonProperty("supportPackageId")
    @Schema(example = "1")
    private Long supportPackageId;

    /**
     * The name of the support package file.
     */
    @JsonProperty
    @Schema(example = "file.cdp")
    private String filename;

    /**
     * The type of the support package file.
     */
    @JsonProperty
    @Schema(example = "delta")
    private String fileType;

    /**
     * The URL where the support package file can be downloaded.
     */
    @JsonProperty
    @Schema(example = "http://fileUrl")
    private String fileUrl;

    /**
     * The MD5 hash of the support package file for integrity verification.
     */
    @JsonProperty("md5_hash")
    @Schema(example = "5d41402abc4b2a76b9719d911017c592")
    private String md5;

    /**
     * The expiry date of the file signature, if applicable.
     */
    @JsonProperty
    @Schema(example = "1709481600")
    private Long signatureExpiryDate;

    /**
     * The SHA-256 hash of the support package file for integrity verification.
     */
    @JsonProperty("sha_256")
    @Schema(example = "0232j32309adskalsdmksdjf9i343k4as0d90232j32309adskalsd")
    private String sha_256;

    /**
     * The version of the support package file.
     */
    @JsonProperty("file_version")
    @Schema(example = "1")
    private String file_version;

    /**
     * A list of VINs (Vehicle Identification Numbers) associated with the support package.
     */
    @JsonProperty("controllerIds")
    @Schema(example = "[\"19UYA31581L000167\", \"19UYA31581L000760\"]")
    private List<String> controllerIds;

    /**
     * The ECU node address associated with the support package.
     */
    @JsonProperty
    @Schema(example = "10A2")//NOSONAR
    private String ecuNodeAddr;

    /**
     * A description of the content of the support package file.
     */
    @JsonProperty
    @Schema(example = "This file contains the latest firmware update.")
    private String fileContentDescription;

    /**
     * The URL where additional information about the support package file can be found.
     */
    @JsonProperty
    @Schema(example = "http://example.com/release-notes")
    private String fileInfoUrl;

    /**
     * A map containing metadata about the support package file, such as size, creator, and release date.
     */
    @JsonProperty
    @Schema(example = "{   \n" +
            "  \"swName\": \"HPC10ROWDEV01********************\",  \n" +
            " \"swTargetVersion\": \"68541061AA000000000000000000007670181252*133156002JY0000*\",  \n" +
            "  \"ecuNodeId\": \"CAN00xC1\" \n" +
            "} ")
    private Map<String, String> fileMetadata;
}