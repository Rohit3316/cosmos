/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.api;

import java.util.Objects;

/**
 * Container for variables available to the {@link ArtifactUrlHandler}.
 *
 */
public class URLPlaceholder {
    private final String tenant;
    private final String type;
    private final Long tenantId;
    private String controllerId;
    private Long targetId;
    private SoftwareData softwareData;
    private EspRspData espRspData;

    /**
     *
     * @param tenant of the client
     * @param type of the file
     * @param tenantId of the tenant
     * @param espRspData of the esp or rsp file
     */
    public URLPlaceholder(final String tenant, final String type, final Long tenantId,
                          final EspRspData espRspData) {
        this.tenant = tenant;
        this.type = type;
        this.tenantId = tenantId;
        this.espRspData = espRspData;
    }

    /**
     *
     * @param tenant of the client
     * @param type of the file
     * @param tenantId of the tenant
     * @param controllerId of the target
     * @param targetId of the target
     * @param softwareData information about the artifact and software module that can be
     *      accessed by the URL.
     */
    public URLPlaceholder(final String tenant, final String type, final Long tenantId, final String controllerId, final Long targetId,
                          final SoftwareData softwareData) {
        this.tenant = tenant;
        this.type = type;
        this.tenantId = tenantId;
        this.controllerId = controllerId;
        this.targetId = targetId;
        this.softwareData = softwareData;
    }

    /**
     * Constructor.
     *
     * @param tenant
     *            of the client
     * @param tenantId
     *            of teh tenant
     * @param controllerId
     *            of the target
     * @param targetId
     *            of the target
     * @param softwareData
     *            information about the artifact and software module that can be
     *            accessed by the URL.
     */
    public URLPlaceholder(final String tenant, final Long tenantId, final String controllerId, final Long targetId,
            final SoftwareData softwareData) {
        this.tenant = tenant;
        this.type = "";
        this.tenantId = tenantId;
        this.controllerId = controllerId;
        this.targetId = targetId;
        this.softwareData = softwareData;
    }

    public static class EspRspData {
        private String filename;
        private String sha256Hash;

        public EspRspData(final String filename, final String sha256Hash) {
            this.filename = filename;
            this.sha256Hash = sha256Hash;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getSha256Hash() {
            return sha256Hash;
        }

        public void setSha256Hash(String sha256Hash) {
            this.sha256Hash = sha256Hash;
        }
    }

    /**
     * Information about the artifact and software module that can be accessed
     * by the URL.
     *
     */
    public static class SoftwareData {
        private Long softwareModuleId;
        private String filename;
        private Long artifactId;
        private String sha1Hash;
        private String sha256Hash;

        /**
         * Constructor.
         * 
         * @param softwareModuleId
         *            of the module the artifact belongs to
         * @param filename
         *            of the artifact
         * @param artifactId
         *            of the artifact
         * @param sha1Hash
         *            of the artifact
         */
        public SoftwareData(final Long softwareModuleId, final String filename, final Long artifactId,
                final String sha1Hash) {
            this.softwareModuleId = softwareModuleId;
            this.filename = filename;
            this.artifactId = artifactId;
            this.sha1Hash = sha1Hash;
        }
        public SoftwareData(final Long softwareModuleId, final String filename, final Long artifactId,
                            final String sha1Hash, final String sha256Hash) {
            this.softwareModuleId = softwareModuleId;
            this.filename = filename;
            this.artifactId = artifactId;
            this.sha1Hash = sha1Hash;
            this.sha256Hash=sha256Hash;
        }

        public Long getSoftwareModuleId() {
            return softwareModuleId;
        }

        public void setSoftwareModuleId(final Long softwareModuleId) {
            this.softwareModuleId = softwareModuleId;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(final String filename) {
            this.filename = filename;
        }

        public Long getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(final Long artifactId) {
            this.artifactId = artifactId;
        }

        public String getSha1Hash() {
            return sha1Hash;
        }

        public void setSha1Hash(final String sha1Hash) {
            this.sha1Hash = sha1Hash;
        }

        public String getSha256Hash() {
            return sha256Hash;
        }


        @Override
        public boolean equals(final Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            final SoftwareData that = (SoftwareData) o;
            return Objects.equals(softwareModuleId, that.softwareModuleId)
                    && Objects.equals(filename, that.filename)
                    && Objects.equals(artifactId, that.artifactId)
                    && Objects.equals(sha1Hash, that.sha1Hash);
        }

        @Override
        public int hashCode() {
            return Objects.hash(softwareModuleId, filename, artifactId, sha1Hash);
        }
    }

    public String getTenant() {
        return tenant;
    }

    public String getType() {
        return type;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getControllerId() {
        return controllerId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public EspRspData getEspRspData() {
        return espRspData;
    }

    public SoftwareData getSoftwareData() {
        return softwareData;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        final URLPlaceholder that = (URLPlaceholder) o;
        return tenantId.equals(that.tenantId) && Objects.equals(controllerId, that.controllerId) && Objects.equals(
                targetId, that.targetId) && Objects.equals(softwareData, that.softwareData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, controllerId, targetId, softwareData);
    }
}
