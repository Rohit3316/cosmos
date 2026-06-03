package org.cosmos.models.mgmt.supportpackage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtBaseSupportPackageCreateRequest;

/**
 * Represents support package upload object using file url
 *
 */
@Schema(description = "Details about the uploaded  ESP or RSP file")
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public final class MgmtFileUrlSupportPackageCreateRequest extends MgmtBaseSupportPackageCreateRequest {

    @NotBlank
    @Schema(description = "Information for uploading the actual binary using an endpoint", required = true)
    @Pattern(regexp = "^https://[a-zA-Z0-9.-]+(\\.[a-zA-Z]{2,6})(/[^\\s]*)?$", message = "Invalid file URL")
    private String fileUrl;

}
