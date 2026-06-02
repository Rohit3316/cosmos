/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.api;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.eclipse.hawkbit.api.ArtifactUrlHandlerProperties.UrlProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Implementation for ArtifactUrlHandler for creating urls to download resource
 * based on patterns configured by {@link ArtifactUrlHandlerProperties}.
 * <p>
 * This mechanism can be used to generate links to arbitrary file hosting
 * infrastructure. However, the hawkBit update server supports hosting files as
 * well in the following {@link UrlProtocol#getRef()} patterns:
 * <p>
 * Default:
 * {protocol}://{hostname}:{port}/device/v1/controllers/{controllerId}/
 * softwaremodules/{softwareModuleId}/artifacts/{artifactFileName}
 * <p>
 * Default (MD5SUM files):
 * {protocol}://{hostname}:{port}/device/v1/controllers/{controllerId}/
 * softwaremodules/{softwareModuleId}/artifacts/{artifactFileName}.MD5SUM
 */
public class PropertyBasedArtifactUrlHandler implements ArtifactUrlHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PropertyBasedArtifactUrlHandler.class);

    private static final String PROTOCOL_PLACEHOLDER = "protocol";
    private static final String CONTROLLER_ID_PLACEHOLDER = "controllerId";
    private static final String TARGET_ID_BASE10_PLACEHOLDER = "targetId";
    private static final String TARGET_ID_BASE62_PLACEHOLDER = "targetIdBase62";
    private static final String IP_PLACEHOLDER = "ip";
    private static final String PORT_PLACEHOLDER = "port";
    private static final String HOSTNAME_PLACEHOLDER = "hostname";
    private static final String HOSTNAME_REQUEST_PLACEHOLDER = "hostnameRequest";
    private static final String PORT_REQUEST_PLACEHOLDER = "portRequest";
    private static final String HOSTNAME_WITH_DOMAIN_REQUEST_PLACEHOLDER = "domainRequest";
    private static final String ARTIFACT_FILENAME_PLACEHOLDER = "artifactFileName";
    private static final String ARTIFACT_SHA1_PLACEHOLDER = "artifactSHA1";
    private static final String ARTIFACT_ID_BASE10_PLACEHOLDER = "artifactId";
    private static final String ARTIFACT_ID_BASE62_PLACEHOLDER = "artifactIdBase62";
    private static final String TENANT_PLACEHOLDER = "tenant";
    private static final String TYPE_PLACEHOLDER = "type";
    private static final String TENANT_ID_BASE10_PLACEHOLDER = "tenantId";
    private static final String TENANT_ID_BASE62_PLACEHOLDER = "tenantIdBase62";
    private static final String SOFTWARE_MODULE_ID_BASE10_PLACDEHOLDER = "softwareModuleId";
    private static final String SOFTWARE_MODULE_ID_BASE62_PLACDEHOLDER = "softwareModuleIdBase62";
    public static final String ARTIFACT_SHA_256_HASH_PLACEHOLDER = "SHA256";
    public static final String CDN_DIRECTORY_PLACE_HOLDER = "hawkbit.artifact.url.cdn.directory";
    public static final String CDN_HOST_PLACE_HOLDER = "hawkbit.artifact.url.cdn.host";
    public static final String CDN_ROOT_DIRECTORY_PLACE_HOLDER = "hawkbit.artifact.url.cdn.rootDirectory";
    private final ArtifactUrlHandlerProperties urlHandlerProperties;

    /**
     * @param urlHandlerProperties for URL generation configuration
     */
    public PropertyBasedArtifactUrlHandler(final ArtifactUrlHandlerProperties urlHandlerProperties) {
        this.urlHandlerProperties = urlHandlerProperties;
    }

    private static String generateUrl(final UrlProtocol protocol,
                                      final URLPlaceholder placeholder,
                                      final URI requestUri,
                                      final ArtifactUrlHandlerProperties.Cdn cdn) {
        final Set<Entry<String, String>> entrySet = getReplaceMap(protocol, placeholder, requestUri, cdn).entrySet();

        String urlPattern = protocol.getRef();

        for (final Entry<String, String> entry : entrySet) {
            if (entry.getKey().equals(PORT_PLACEHOLDER)) {
                urlPattern = urlPattern.replace(":{" + entry.getKey() + "}",
                        !StringUtils.hasText(entry.getValue()) ? "" : (":" + entry.getValue()));
            } else {
                if (entry.getValue() != null) {
                    urlPattern = urlPattern.replace("{" + entry.getKey() + "}", entry.getValue());
                }
            }
        }
        return urlPattern;
    }

    private static Map<String, String> getReplaceMap(final UrlProtocol protocol,
                                                     final URLPlaceholder placeholder,
                                                     final URI requestUri,
                                                     final ArtifactUrlHandlerProperties.Cdn cdn) {
        final Map<String, String> replaceMap = new LinkedHashMap<>();

        replaceMap.put(CDN_HOST_PLACE_HOLDER, cdn.getHost());
        replaceMap.put(CDN_ROOT_DIRECTORY_PLACE_HOLDER, cdn.getRootDirectory());
        replaceMap.put(CDN_DIRECTORY_PLACE_HOLDER, cdn.getDirectory());
        replaceMap.put(IP_PLACEHOLDER, protocol.getIp());
        replaceMap.put(HOSTNAME_PLACEHOLDER, protocol.getHostname());

        replaceMap.put(HOSTNAME_REQUEST_PLACEHOLDER, getRequestHost(protocol, requestUri));
        replaceMap.put(PORT_REQUEST_PLACEHOLDER, getRequestPort(protocol, requestUri));
        replaceMap.put(HOSTNAME_WITH_DOMAIN_REQUEST_PLACEHOLDER, computeHostWithRequestDomain(protocol, requestUri));

        try {
            if(placeholder.getSoftwareData() != null) {
                replaceMap.put(ARTIFACT_FILENAME_PLACEHOLDER,
                        URLEncoder.encode(placeholder.getSoftwareData().getFilename(), StandardCharsets.UTF_8.toString()));
            }
            if(placeholder.getEspRspData() != null) {
                replaceMap.put(ARTIFACT_FILENAME_PLACEHOLDER,
                        URLEncoder.encode(placeholder.getEspRspData().getFilename(), StandardCharsets.UTF_8.toString()));
            }
        } catch (final UnsupportedEncodingException e) {
            LOG.error("Could not encode {}", placeholder.getSoftwareData() != null ? placeholder.getSoftwareData().getFilename():placeholder.getEspRspData().getFilename(), e);
        }
        if (placeholder.getSoftwareData() !=null && placeholder.getSoftwareData().getSha256Hash() != null) {
            replaceMap.put(ARTIFACT_SHA_256_HASH_PLACEHOLDER, placeholder.getSoftwareData().getSha256Hash());
        }
        if (placeholder.getEspRspData() !=null && placeholder.getEspRspData().getSha256Hash() != null) {
            replaceMap.put(ARTIFACT_SHA_256_HASH_PLACEHOLDER, placeholder.getEspRspData().getSha256Hash());
        }
        replaceMap.put(PROTOCOL_PLACEHOLDER, getRequestProtocol(protocol, requestUri));
        replaceMap.put(PORT_PLACEHOLDER, getPort(protocol));
        replaceMap.put(TENANT_PLACEHOLDER, placeholder.getTenant());
        replaceMap.put(TYPE_PLACEHOLDER, placeholder.getType());
        replaceMap.put(TENANT_ID_BASE10_PLACEHOLDER, String.valueOf(placeholder.getTenantId()));
        replaceMap.put(TENANT_ID_BASE62_PLACEHOLDER, Base62Util.fromBase10(placeholder.getTenantId()));
        replaceMap.put(CONTROLLER_ID_PLACEHOLDER, placeholder.getControllerId());
        replaceMap.put(TARGET_ID_BASE10_PLACEHOLDER, String.valueOf(placeholder.getTargetId()));
        if (placeholder.getTargetId() != null) {
            replaceMap.put(TARGET_ID_BASE62_PLACEHOLDER, Base62Util.fromBase10(placeholder.getTargetId()));
        }
        if(placeholder.getSoftwareData() != null) {
            replaceMap.put(ARTIFACT_SHA1_PLACEHOLDER, placeholder.getSoftwareData().getSha1Hash());
            replaceMap.put(ARTIFACT_ID_BASE62_PLACEHOLDER,
                    Base62Util.fromBase10(placeholder.getSoftwareData().getArtifactId()));
            replaceMap.put(ARTIFACT_ID_BASE10_PLACEHOLDER, String.valueOf(placeholder.getSoftwareData().getArtifactId()));
            replaceMap.put(SOFTWARE_MODULE_ID_BASE10_PLACDEHOLDER,
                    String.valueOf(placeholder.getSoftwareData().getSoftwareModuleId()));
            replaceMap.put(SOFTWARE_MODULE_ID_BASE62_PLACDEHOLDER,
                    Base62Util.fromBase10(placeholder.getSoftwareData().getSoftwareModuleId()));
        }
        return replaceMap;
    }

    private static String getRequestPort(final UrlProtocol protocol, final URI requestUri) {
        if (requestUri == null) {
            return getPort(protocol);
        }
        return requestUri.getPort() > 0 ? String.valueOf(requestUri.getPort()) : getPort(protocol, requestUri);
    }

    /**
     * Returns the port number based on the given URL protocol and request URI.
     *
     * @param protocol   the URL protocol to consider
     * @param requestUri the request URI to consider
     * @return the port number as a string
     */
    private static String getPort(final UrlProtocol protocol, final URI requestUri) {
        if (requestUri.getScheme() == null) {
            return String.valueOf(protocol.getPort());
        }
        LOG.debug("requestUri port: {}", requestUri.getPort());
        return requestUri.getScheme().equalsIgnoreCase(protocol.getHttpsProtocol()) ? String.valueOf(protocol.getHttpsPort()) : String.valueOf(protocol.getHttpPort());
    }

    /**
     * Returns the protocol from the given request URI or the protocol of the given UrlProtocol if the request URI is null.
     *
     * @param protocol   the UrlProtocol to consider
     * @param requestUri the request URI to consider
     * @return the protocol as a string
     */
    private static String getRequestProtocol(final UrlProtocol protocol, final URI requestUri) {
        if (requestUri == null || requestUri.getScheme() == null) {
            return protocol.getProtocol();
        }
        LOG.debug("requestUri protocol: {}", requestUri.getScheme());
        return requestUri.getScheme();
    }

    private static String getRequestHost(final UrlProtocol protocol, final URI requestUri) {
        if (requestUri == null) {
            return protocol.getHostname();
        }

        return Optional.ofNullable(requestUri.getHost()).orElse(protocol.getHostname());
    }

    private static String getPort(final UrlProtocol protocol) {
        return protocol.getPort() == null ? null : String.valueOf(protocol.getPort());
    }

    private static String computeHostWithRequestDomain(final UrlProtocol protocol, final URI requestUri) {

        if (requestUri == null) {
            return protocol.getHostname();
        }

        if (!protocol.getHostname().contains(".")) {
            return protocol.getHostname();
        }

        final String host = StringUtils.delimitedListToStringArray(protocol.getHostname(), ".")[0].trim();

        final List<String> domainElements = Arrays
                .asList(StringUtils.delimitedListToStringArray(requestUri.getHost(), "."));
        final String domain = StringUtils.collectionToDelimitedString(domainElements.subList(1, domainElements.size()),
                ".");

        if (!StringUtils.hasText(domain)) {
            return protocol.getHostname();
        }

        return host + "." + domain;
    }

    @Override
    public List<ArtifactUrl> getUrls(final URLPlaceholder placeholder, final ApiType api) {
        return getUrls(placeholder, api, null);
    }

    @Override
    public List<ArtifactUrl> getUrls(final URLPlaceholder placeholder, final ApiType api, final URI requestUri) {
        ArtifactUrlHandlerProperties.Cdn cdn = urlHandlerProperties.getCdn();
        return urlHandlerProperties.getProtocols().values().stream()
                .filter(urlProtocol -> urlProtocol.getSupports().contains(api) && urlProtocol.isEnabled())
                .map(urlProtocol -> new ArtifactUrl(urlProtocol.getProtocol().toUpperCase(), urlProtocol.getRel(),
                        generateUrl(urlProtocol, placeholder, requestUri, cdn)))
                .toList();

    }

}
