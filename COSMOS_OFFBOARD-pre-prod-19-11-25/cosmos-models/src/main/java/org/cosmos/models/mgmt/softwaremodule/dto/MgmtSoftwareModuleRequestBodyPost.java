/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.softwaremodule.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cosmos.models.mgmt.MgmtRestConstants;

/**
 * Request Body for SoftwareModule POST.
 *
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MgmtSoftwareModuleRequestBodyPost {

    @JsonProperty(required = true)
    @Schema(example = "HPC10ROWDEV01********************")
    private String name;

    @JsonProperty(defaultValue = MgmtRestConstants.DEFAULT_REQUEST_BODY_SM_VERSION)
    @Schema(example = "1.0.0")
    private String version = MgmtRestConstants.DEFAULT_REQUEST_BODY_SM_VERSION;

    @JsonProperty(required = true)
    @Schema(example = "os")
    private String type;

    @JsonProperty(required = true)
    @Schema(example = "QNX")
    private String format;

    @JsonProperty
    @Schema(example = "SM Description")
    private String description;

    @JsonProperty
    @Schema(example = "Magneti Marelli")
    private String vendor;

    @JsonProperty
    @Schema(example = "false")
    private boolean encrypted;

    @JsonProperty(required = true)
    @Schema(example = "HPC")
    private String swInstallerType;

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     *
     * @return updated body
     */
    public MgmtSoftwareModuleRequestBodyPost setName(final String name) {
        this.name = name;
        return this;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version
     *            the version to set
     *
     * @return updated body
     */
    public MgmtSoftwareModuleRequestBodyPost setVersion(final String version) {
        this.version = version;
        return this;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type
     *            the type to set
     *
     * @return updated body
     */
    public MgmtSoftwareModuleRequestBodyPost setType(final String type) {
        this.type = type;
        return this;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description
     *            the description to set
     *
     * @return updated body
     */
    public MgmtSoftwareModuleRequestBodyPost setDescription(final String description) {
        this.description = description;
        return this;
    }

    /**
     * @return the vendor
     */
    public String getVendor() {
        return vendor;
    }

    /**
     * @param vendor
     *            the vendor to set
     *
     * @return updated body
     */
    public MgmtSoftwareModuleRequestBodyPost setVendor(final String vendor) {
        this.vendor = vendor;
        return this;
    }

    /**
     * @return if encrypted
     */
    public boolean isEncrypted() {
        return encrypted;
    }

    /**
     * @param encrypted
     *            if should be encrypted
     *
     * @return updated body
     */
    public MgmtSoftwareModuleRequestBodyPost setEncrypted(final boolean encrypted) {
        this.encrypted = encrypted;
        return this;
    }

    /**
     * @return the swInstallerType
     */
    public String getSwInstallerType() {
        return swInstallerType;
    }

    /**
     * @param swInstallerType
     *            the swInstallerType to set
     *
     * @return updated body
     */
    public MgmtSoftwareModuleRequestBodyPost setSwInstallerType(final String swInstallerType) {
        this.swInstallerType = swInstallerType;
        return this;
    }

}
