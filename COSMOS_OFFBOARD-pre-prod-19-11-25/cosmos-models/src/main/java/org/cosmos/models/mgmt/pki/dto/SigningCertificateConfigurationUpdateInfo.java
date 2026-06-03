package org.cosmos.models.mgmt.pki.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO representing update signing certificate configuration info for PKI API request body.
 */
@Data
@Schema(description = "Update Signing certificate configuration info request body")
public class SigningCertificateConfigurationUpdateInfo {

    /**
     * PKI type.
     */
    @Schema(description = "PKI type")
    @NotNull
    private String pki;

    /**
     * DD certificate path.
     */
    @Schema(description = "DD certificate path")
    @NotNull
    private String ddCertificatePath;

    /**
     * ESP certificate path.
     */
    @Schema(description = "ESP certificate path")
    @NotNull
    private String espCertificatePath;

    /**
     * RSP certificate path.
     */
    @Schema(description = "RSP certificate path")
    @NotNull
    private String rspCertificatePath;

    /**
     * Intermediate CA certificate path.
     */
    @Schema(description = "Intermediate CA certificate path")
    @NotNull
    private String intermediateCACertificatePath;

    /**
     * DD private key path.
     */
    @Schema(description = "DD private key path")
    @NotNull
    private String ddPrivateKeyPath;

    /**
     * ESP private key path.
     */
    @Schema(description = "ESP private key path")
    @NotNull
    private String espPrivateKeyPath;

    /**
     * RSP private key path.
     */
    @Schema(description = "RSP private key path")
    @NotNull
    private String rspPrivateKeyPath;
}