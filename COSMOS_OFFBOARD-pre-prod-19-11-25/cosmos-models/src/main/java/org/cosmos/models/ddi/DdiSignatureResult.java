package org.cosmos.models.ddi;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * Result of a DDI signature generation operation.
 */
@Data
@Builder
public class DdiSignatureResult {
    /**
     * The generated signature
     */
    public String signature;
    /**
     * The expiry date of the signing certificate.
     */
    public Date signingCertificateExpirationDate;
}

