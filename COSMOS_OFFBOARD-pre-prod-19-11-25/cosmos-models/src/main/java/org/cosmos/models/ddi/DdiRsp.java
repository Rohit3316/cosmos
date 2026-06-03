package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.springframework.hateoas.RepresentationModel;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * This class represents the RSP file in the DDI model.
 * It includes properties like fileType, fileName, fileSize, rspMetadata, rspSignature, hashes and links.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class DdiRsp extends RepresentationModel<DdiRsp> {

    /**
     * The type of the RSP file.
     */
    @JsonProperty("fileType")
    MgmtSupportPackageFileType fileType;

    /**
     * The name of the RSP file.
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
     * The RSP metadata.
     */
    @JsonProperty("rspMetadata")
    @NotNull
    @Size(max = 256, message = "Field cannot exceed 256 characters")
    String rspMetadata;

    /**
     * The RSP signature.
     */
    @JsonProperty("rspSignature")
    @NotNull
    @Size(max = 256, message = "Field cannot exceed 256 characters")
    String rspSignature;

    /**
     * The hashes of the RSP file.
     */
    @JsonProperty("hashes")
    @NotNull
    DdiSupportPackageHash hashes;

    /**
     * Constructor for DdiRsp.
     *
     * @param fileType The type of the RSP file.
     * @param fileName The name of the RSP file.
     * @param fileSize The size of the RSP file.
     * @param rspMetadata The RSP metadata.
     * @param rspSignature The RSP signature.
     * @param hashes The hashes of the RSP file.
     */
    public DdiRsp(MgmtSupportPackageFileType fileType, String fileName, Long fileSize, String rspMetadata, String rspSignature, DdiSupportPackageHash hashes) {
        this.fileType = fileType;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.rspMetadata = rspMetadata;
        this.rspSignature = rspSignature;
        this.hashes = hashes;
    }
        
}
