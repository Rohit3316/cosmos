/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.api;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Artifact handler properties class for holding all supported protocols with
 * host, ip, port and download pattern.
 *
 * @see PropertyBasedArtifactUrlHandler
 */
@ConfigurationProperties("hawkbit.artifact.url")
@Data
public class ArtifactUrlHandlerProperties {

    /**
     * Rel as key and complete protocol as value.
     */
    private final Map<String, UrlProtocol> protocols = new HashMap<>();

    /**
     * Artifacts cdn properties
     */
    private final Cdn cdn = new Cdn();

    /**
     * Artifacts s3 properties
     */
    private final S3 s3 = new S3();

    /**
     * Artifacts custom properties
     */
    private final Custom custom = new Custom();

    /**
     * Protocol specific properties to generate URLs accordingly.
     */
    @Data
    public static class UrlProtocol {

        private static final int DEFAULT_HTTP_PORT = 8080;

        /**
         * Set to true if enabled.
         */
        private boolean enabled = true;

        /**
         * Hypermedia rel value for this protocol.
         */
        private String rel = "download-http";

        /**
         * Hypermedia ref pattern for this protocol. Supported place holders are
         * protocol,controllerId,targetId,targetIdBase62,ip,port,hostname,
         * artifactFileName,artifactSHA1,
         * artifactIdBase62,artifactId,tenant,softwareModuleId,
         * softwareModuleIdBase62.
         * <p>
         * The update server itself supports
         */
        private String ref = "{protocol}://{hostname}:{port}/device/v1/controllers/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/{artifactFileName}";

        /**
         * Protocol name placeholder that can be used in ref pattern.
         */
        private String protocol = "http";

        /**
         * Hostname placeholder that can be used in ref pattern.
         */
        private String hostname = "localhost";

        /**
         * IP address placeholder that can be used in ref pattern.
         */
        // Exception squid:S1313 - default only, can be configured
        @SuppressWarnings("squid:S1313")
        private String ip = "127.0.0.1";

        /**
         * Port placeholder that can be used in ref pattern.
         */
        private Integer port = DEFAULT_HTTP_PORT;

        /**
         * Support for the following hawkBit API.
         */
        private List<ApiType> supports = Arrays.asList(ApiType.DDI, ApiType.DMF, ApiType.MGMT);

        /**
         * The protocol name placeholder.
         */
        private String httpsProtocol = "https";

        /**
         * The port for https protocol.
         */
        private Integer httpsPort = 443;

        /**
         * The default port for http protocol.
         */
        private int httpPort = 80;


    }

    @Data
    public static class Cdn {

        private String host;

        private String rootDirectory;
        /**
         * Artifacts cdn directory where files are uploaded.
         */
        private String directory = "/{tenant}/{type}/{SHA256}";

    }

    @Data
    public static class S3 {
        /**
         * S3 directory where files are uploaded.
         */
        private String directory = "{tenant}/{type}/{SHA256}/";

        /**
         * Upload file type
         */
        @Getter
        public enum Type {
            ARTIFACT("artifact");

            private final String fileType;

            Type(final String type) {
                this.fileType = type;
            }
        }

    }


    @Data
    public static class Custom {
        /**
         * Represents custom download links required to download an artifact.
         * This includes both CDN and HTTP download links.
         */
        private String download = "http://testcdn.cosmos.stellantis.edgekey.net/tenant/{tenant}/hash/{SHA256}/artifacts/{artifactFileName}";

        public String getDownload() {
            return download;
        }

        public void setDownload(String download) {
            this.download = download;
        }
    }

}
