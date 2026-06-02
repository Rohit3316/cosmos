package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.springframework.hateoas.RepresentationModel;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


/**
 * DdiEsp is a class that represents the ESP file that is used to sign the DDI file.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Getter
public class DdiEsp extends RepresentationModel<DdiEsp> {

    /**
     * The type of the ESP file.
     */
    @JsonProperty("fileType")
    MgmtSupportPackageFileType fileType;

    /**
     * The name of the ESP file.
     */
    @JsonProperty("fileName")
    @NotNull
    @Size(max = 256, message = "Field cannot exceed 256 characters")
    String fileName;

    /**
     * The size of the ESP file.
     */
    @JsonProperty("size")
    @NotNull
    Long fileSize;

    /**
     * The ESP metadata.
     */
    @JsonProperty("espMetadata")
    @NotNull
    @Size(max = 256, message = "Field cannot exceed 256 characters")
    String espMetadata;

    /**
     * The ESP signature.
     */
    @JsonProperty("espSignature")
    @NotNull
    @Size(max = 256, message = "Field cannot exceed 256 characters")
    String espSignature;



    @JsonProperty("hashes")
    @NotNull
    DdiSupportPackageHash hashes;

    public DdiEsp(MgmtSupportPackageFileType fileType, String fileName, Long fileSize, String espMetadata, String espSignature,
                  DdiSupportPackageHash hashes) {
        this.fileType = fileType;
        this.fileName = fileName;
        this.fileSize= fileSize;
        this.espMetadata = espMetadata;
        this.espSignature = espSignature;
        this.hashes= hashes;
    }
}
