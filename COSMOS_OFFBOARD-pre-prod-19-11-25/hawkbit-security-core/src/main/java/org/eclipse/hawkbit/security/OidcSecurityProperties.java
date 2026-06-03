/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * The common properties for OIDC security.
 */
@ConfigurationProperties("spring.security.oauth2.client")
@Getter
@NoArgsConstructor
public class OidcSecurityProperties {

    private final Registration registration = new Registration();
    private final Provider provider = new Provider();

    @Getter
    @NoArgsConstructor
    public static class Registration {
        private final Oidc oidc = new Oidc();

        @Data
        @NoArgsConstructor
        public static class Oidc {
            private String clientId = "";
            private String scope = "";
            private String env = "";
        }
    }

    @Getter
    @NoArgsConstructor
    public static class Provider {
        private final Oidc oidc = new Oidc();

        @Data
        @NoArgsConstructor
        public static class Oidc {
            private String type = "";
            private List<String> authorizedClients = new ArrayList<>();
            private String issuerUri = "";
            private String jwkSetUri = "";
        }
    }

}
