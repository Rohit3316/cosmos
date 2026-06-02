package org.eclipse.hawkbit.security.oidc.authentication;

import org.eclipse.hawkbit.security.OidcSecurityProperties;
import org.eclipse.hawkbit.security.oidc.authentication.model.UserTenantAuthorities;
import org.eclipse.hawkbit.security.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

public class JwtAuthoritiesValidator {

    private static final Logger LOG = LoggerFactory.getLogger(JwtAuthoritiesValidator.class);

    private static final OAuth2Error INVALID_REQUEST = new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST);

    private static final String CLIENT_ID = "client_id";

    private final OidcSecurityProperties oidcSecurityProperties;

    public JwtAuthoritiesValidator(final OidcSecurityProperties oidcSecurityProperties) {
        this.oidcSecurityProperties = oidcSecurityProperties;
    }

    /**
     * Method to validate Client ID, PROD scope and JWT token
     *
     * @param jwt token
     */
    public void accessTokenValidator(Jwt jwt) {
        try {

            //Validate JWT token with timestamp and issuer url
            //Cosmos-376, 386
            OAuth2TokenValidatorResult result = JwtUtil.validator(oidcSecurityProperties.getProvider().getOidc().getIssuerUri())
                    .validate(jwt);
            if (result.hasErrors()) {
                LOG.error("Error during validating access token for timestamp and issuer url : {}", result.getErrors());
                throw new JwtException("Error during access token validation");
            }

            //Validate client_id of access token
            //Cosmos-376
            if (!jwt.getClaim(CLIENT_ID).equals(oidcSecurityProperties.getRegistration().getOidc().getClientId())
                    && !oidcSecurityProperties.getProvider().getOidc().getAuthorizedClients().contains(jwt.getClaim(CLIENT_ID))) {
                LOG.error("Error during validating access token for client_id : {}", (String) jwt.getClaim(CLIENT_ID));
                throw new JwtException("Invalid clientId in access token");
            }

            //Validate aud claim
            //Cosmos-376
            String[] scopes = oidcSecurityProperties.getRegistration().getOidc().getScope().split(",");
            Optional<String> prdScope = Arrays.stream(scopes).filter(scope -> scope.contains("prd:")).findFirst();
            ArrayList<String> aud = jwt.getClaim("aud");

            /*
             * The value of the aud claim (audience) in the token is equal to the PRD code in the case an AS is used,
             * otherwise it will be equal to the client id
             */
            if (prdScope.isPresent()) {
                String audApplication = prdScope.get().split(":")[1];
                if (!aud.contains(audApplication.toUpperCase())) {
                    LOG.error("Error during validating access token for prdScope : {}", audApplication.toUpperCase());
                    throw new JwtException("Invalid aud in access token");
                }
            } else {
                //aud === clientId
                if (!aud.contains(oidcSecurityProperties.getRegistration().getOidc().getClientId())
                        && !CollectionUtils.containsAny(aud, oidcSecurityProperties.getProvider().getOidc().getAuthorizedClients())) {
                    LOG.error("Error during validating access token for aud : {}", aud);
                    throw new JwtException("Invalid aud in access token");
                }
            }

        } catch (final Exception e) {
            throw new OAuth2AuthenticationException(INVALID_REQUEST, e);
        }
    }

    /**
     * Validate whether user has authorities and/or tenants
     *
     * @param userTenantAuthorities user authorities/tenants
     * @return boolean
     */
    public boolean isValidUserTenantAuthorities(UserTenantAuthorities userTenantAuthorities) {
        return userTenantAuthorities != null
                && userTenantAuthorities.getUserAuthorities() != null
                && !userTenantAuthorities.getUserAuthorities().isEmpty();
    }
}
