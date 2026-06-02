package org.cosmos.models.mgmt.supportpackage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Map;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;

@Schema(description = "Details about the uploaded  ESP or RSP file")
@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public sealed class MgmtBaseSupportPackageCreateRequest permits MgmtFileUrlSupportPackageCreateRequest, MgmtFileSupportPackageCreateRequest {

    @NotBlank
    @Schema(description = "Name for the uploaded file", required = true)
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Invalid file name")
    private String fileName;

    @NotNull
    @Schema(description = "ESP, RSP file types", required = true)
    private MgmtSupportPackageFileType fileType;

    @NotNull
    @Schema(description = "sha256 of the uploaded file", required = true, example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
    @Pattern(regexp = "^[a-fA-F0-9]{64}$", message = "Invalid SHA-256 hash")
    private String sha256;

    @Schema(description = "Version of the file", example = "68541061AX00000000000000000008*670181252*133156002JY0000*")
    private String fileVersion;

    @Schema(description = "List of the controllerId’s for which the file is uploaded (if the same file is uploaded for multiple vehicles in the campaign). Required for ESP fileTypes. Not required for RSP fileTypes", required = true)
    private List<String> controllerIds;

    @Schema(description = "Specific for ESP, this will help assign ecu of the controllerId to this esp package. Required for ESP fileTypes . Not required for RSP fileType", required = true, example = "0242")
    private String ecuNodeAddress;

    @Schema(description = "Description of the file")
    private String fileContentDescription;

    @Schema(description = "URL to a release notes page")
    @Pattern(regexp = "^https://[a-zA-Z0-9.-]+(\\.[a-zA-Z]{2,6})(/[^\\s]*)?$", message = "Invalid file info URL")
    private String fileInfoUrl;

    @Schema(description = "Metadata to support the onboard with extra information about the file", example = "{   \n" +
            "  \"swName\": \"HPC10ROWDEV01********************\",  \n" +
            " \"swTargetVersion\": \"68541061AA000000000000000000007670181252*133156002JY0000*\",  \n" +
            "  \"ecuNodeId\": \"CAN00xC1\" \n" +
            "} ")
    private Map<String, String> fileMetadata;


}
