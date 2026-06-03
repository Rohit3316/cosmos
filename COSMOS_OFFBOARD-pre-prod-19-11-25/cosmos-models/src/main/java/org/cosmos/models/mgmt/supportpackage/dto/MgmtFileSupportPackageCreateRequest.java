package org.cosmos.models.mgmt.supportpackage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtBaseSupportPackageCreateRequest;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;

/**
 * Represents support package upload object using file
 *
 */
@Schema(description = "Details about the uploaded ESP or RSP file")
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public final class MgmtFileSupportPackageCreateRequest extends MgmtBaseSupportPackageCreateRequest {

    @NotNull
    @Schema(description = "The actual file to be uploaded")
    private MultipartFile file;

}
