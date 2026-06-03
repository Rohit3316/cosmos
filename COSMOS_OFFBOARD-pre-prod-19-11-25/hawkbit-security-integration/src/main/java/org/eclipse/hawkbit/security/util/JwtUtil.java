package org.eclipse.hawkbit.security.util;

import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.Assert;

import java.util.Collections;

/**
 * Util class for Jwt Decoder and validator
 */
public class JwtUtil {

    private JwtUtil() {
    }

    /**
     * Create a decoder with timestamp and issuer validator
     *
     * @param jwkSetUrl jek set url
     * @param issuer    issuer url
     * @return NimbusJwtDecoder decoder
     */
    public static NimbusJwtDecoder decoder(String jwkSetUrl, String issuer) {
        Assert.hasText(jwkSetUrl, "Jwk set url is empty");
        Assert.hasText(issuer, "Issuer url is empty");
        NimbusJwtDecoder delegate = NimbusJwtDecoder
                .withJwkSetUri(jwkSetUrl)
                .jwsAlgorithm(SignatureAlgorithm.from(JwsAlgorithms.RS256))
                .build();
        delegate.setClaimSetConverter(MappedJwtClaimSetConverter.withDefaults(Collections.emptyMap()));
        delegate.setJwtValidator(validator(issuer));
        return delegate;
    }

    /**
     * Create a validator with timestamp and issuer validation
     *
     * @param issuer issuer url
     * @return OAuth2TokenValidator validator
     */
    public static OAuth2TokenValidator<Jwt> validator(String issuer) {
        Assert.hasText(issuer, "Issuer url is empty");
        return JwtValidators.createDefaultWithIssuer(issuer);
    }
}
