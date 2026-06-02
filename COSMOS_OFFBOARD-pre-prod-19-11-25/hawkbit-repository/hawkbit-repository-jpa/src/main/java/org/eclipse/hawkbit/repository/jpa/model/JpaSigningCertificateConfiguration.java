package org.eclipse.hawkbit.repository.jpa.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.hawkbit.repository.model.SigningCertificateConfiguration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;


/**
 * Represents the configuration for signing certificates used in the system.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "sp_signing_certificate_configuration")
@Entity
public class JpaSigningCertificateConfiguration extends AbstractJpaBaseEntity implements SigningCertificateConfiguration {

    @Column(name = "pki")
    private String pki;

    @Column(name = "ecu_id_issuer", nullable = false, unique = true)
    @NotNull
    private String ecuIdIssuer;

    @Column(name = "dd_certificate_path", nullable = false)
    @NotNull
    private String ddCertificatePath;

    @Column(name = "esp_certificate_path", nullable = false)
    @NotNull
    private String espCertificatePath;

    @Column(name = "rsp_certificate_path", nullable = false)
    @NotNull
    private String rspCertificatePath;

    @Column(name = "intermediate_ca_certificate_path", nullable = false)
    @NotNull
    private String intermediateCACertificatePath;

    @Column(name = "dd_private_key_path", nullable = false)
    @NotNull
    private String ddPrivateKeyPath;

    @Column(name = "esp_private_key_path", nullable = false)
    @NotNull
    private String espPrivateKeyPath;

    @Column(name = "rsp_private_key_path", nullable = false)
    @NotNull
    private String rspPrivateKeyPath;
}
