/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.api;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.eclipse.hawkbit.api.ArtifactUrlHandlerProperties.UrlProtocol;
import org.eclipse.hawkbit.api.URLPlaceholder.SoftwareData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for creating urls to download artifacts.
 */
@Feature("Unit Tests - Artifact URL Handler")
@Story("Test to generate the artifact download URL")
@ExtendWith(MockitoExtension.class)
class PropertyBasedArtifactUrlHandlerTest {

    private static final String TEST_PROTO = "coap";
    private static final String TEST_REL_UDP = "download-udp";
    private static final String TEST_REL_HTTP = "download-http";
    private static final String TEST_IP = "127.0.0.1";
    private static final int TEST_PORT = 5683;
    private static final long TENANT_ID = 456789L;
    private static final String CONTROLLER_ID = "Test";
    private static final String FILENAME_DECODE = "test123!§$%&";
    private static final String FILENAME_ENCODE = "test123%21%C2%A7%24%25%26";
    private static final long SOFTWAREMODULEID = 87654L;
    private static final long TARGETID = 3474366L;
    private static final String TARGETID_BASE62 = "EZqA";
    private static final String SHA1HASH = "test12345";
    private static final long ARTIFACTID = 1345678L;
    private static final String ARTIFACTID_BASE62 = "5e4U";
    private static final String TENANT = "TEST_TENANT";
    private static final String CONTROLLERS = "/controllers/";
    private static final String SOFTWARE_MODULES = "/softwaremodules/";
    private static final String ARTIFACTS = "/artifacts/";

    private static final String HTTP_LOCALHOST = "http://localhost:8080/";

    private static final String DEVICE_V1_TENANTS = "device/v1";
    private static final URLPlaceholder placeholder = new URLPlaceholder(TENANT, TENANT_ID, CONTROLLER_ID, TARGETID,
            new SoftwareData(SOFTWAREMODULEID, FILENAME_DECODE, ARTIFACTID, SHA1HASH));
    private ArtifactUrlHandler urlHandlerUnderTest;
    private ArtifactUrlHandlerProperties properties;

    @BeforeEach
    public void setup() {
        properties = new ArtifactUrlHandlerProperties();
        urlHandlerUnderTest = new PropertyBasedArtifactUrlHandler(properties);

    }

    @Test
    @Description("Tests the generation of http download url.")
    void urlGenerationWithDefaultConfiguration() {
        properties.getProtocols().put(TEST_REL_HTTP, new UrlProtocol());

        final List<ArtifactUrl> ddiUrls = urlHandlerUnderTest.getUrls(placeholder, ApiType.DDI);
        assertThat(ddiUrls).containsExactly(
                new ArtifactUrl("http".toUpperCase(), TEST_REL_HTTP, HTTP_LOCALHOST + DEVICE_V1_TENANTS  + CONTROLLERS
                        + CONTROLLER_ID + SOFTWARE_MODULES + SOFTWAREMODULEID + ARTIFACTS + FILENAME_ENCODE));

        final List<ArtifactUrl> dmfUrls = urlHandlerUnderTest.getUrls(placeholder, ApiType.DMF);
        assertThat(ddiUrls).isEqualTo(dmfUrls);
    }

    @Test
    @Description("Tests the generation of custom download url with a CoAP example that supports DMF only.")
    void urlGenerationWithCustomConfiguration() {
        final UrlProtocol proto = new UrlProtocol();
        proto.setIp(TEST_IP);
        proto.setPort(TEST_PORT);
        proto.setProtocol(TEST_PROTO);
        proto.setRel(TEST_REL_UDP);
        proto.setSupports(List.of(ApiType.DMF));
        proto.setRef("{protocol}://{ip}:{port}/fw/{tenant}/{controllerId}/sha1/{artifactSHA1}");
        properties.getProtocols().put(TEST_PROTO, proto);

        List<ArtifactUrl> urls = urlHandlerUnderTest.getUrls(placeholder, ApiType.DDI);

        assertThat(urls).isEmpty();
        urls = urlHandlerUnderTest.getUrls(placeholder, ApiType.DMF);

        assertThat(urls).containsExactly(new ArtifactUrl(TEST_PROTO.toUpperCase(), TEST_REL_UDP,
                "coap://127.0.0.1:5683/fw/" + TENANT + "/" + CONTROLLER_ID + "/sha1/" + SHA1HASH));
    }

    @Test
    @Description("Tests the generation of custom download url using Base62 references with a CoAP example that supports DMF only.")
    void urlGenerationWithCustomShortConfiguration() {
        final UrlProtocol proto = new UrlProtocol();
        proto.setIp(TEST_IP);
        proto.setPort(TEST_PORT);
        proto.setProtocol(TEST_PROTO);
        proto.setRel(TEST_REL_UDP);
        proto.setSupports(List.of(ApiType.DMF));
        proto.setRef("{protocol}://{ip}:{port}/fws/{tenant}/{targetIdBase62}/{artifactIdBase62}");
        properties.getProtocols().put("ftp", proto);

        List<ArtifactUrl> urls = urlHandlerUnderTest.getUrls(placeholder, ApiType.DDI);

        assertThat(urls).isEmpty();
        urls = urlHandlerUnderTest.getUrls(placeholder, ApiType.DMF);

        assertThat(urls).containsExactly(new ArtifactUrl(TEST_PROTO.toUpperCase(), TEST_REL_UDP,
                TEST_PROTO + "://127.0.0.1:5683/fws/" + TENANT + "/" + TARGETID_BASE62 + "/" + ARTIFACTID_BASE62));
    }
    @Test
    @Description("Verfies that the full qualified host of the statically defined hostname is replaced with the host of the request.")
    void urlGenerationWithHostFromRequest() throws URISyntaxException {
        final String testHost = "ddi.host.com";

        final UrlProtocol proto = new UrlProtocol();
        proto.setIp(TEST_IP);
        proto.setPort(TEST_PORT);
        proto.setProtocol(TEST_PROTO);
        proto.setRel(TEST_REL_UDP);
        proto.setSupports(List.of(ApiType.DDI));
        proto.setRef("{protocol}://{hostnameRequest}:{port}/fws/{tenant}/{targetIdBase62}/{artifactIdBase62}");
        properties.getProtocols().put("ftp", proto);

        final List<ArtifactUrl> urls = urlHandlerUnderTest.getUrls(placeholder, ApiType.DDI,
                new URI(TEST_PROTO + "://" + testHost));

        assertThat(urls).containsExactly(new ArtifactUrl(TEST_PROTO.toUpperCase(), TEST_REL_UDP, TEST_PROTO + "://"
                + testHost + ":5683/fws/" + TENANT + "/" + TARGETID_BASE62 + "/" + ARTIFACTID_BASE62));
    }

}
