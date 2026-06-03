/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.softwaremodule.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.cosmos.models.mgmt.MgmtNamedEntity;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifacts;
import org.cosmos.models.mgmt.ecu.dto.MgmtEcuModelResponse;

import java.util.List;
import java.util.Objects;

/**
 * A json annotated rest model for SoftwareModule to RESTful API representation.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtSoftwareModule extends MgmtNamedEntity {
    @JsonProperty(value = "id", required = true)
    @Schema(example = "6")
    private Long moduleId;

    @JsonProperty(required = true)
    private String format;

    @JsonProperty(required = true)
    @Schema(example = "os")
    private String type;

    @Schema(example = "OS")
    private String typeName;

    @JsonProperty
    @Schema(example = "Vendor Limited, California")
    private String vendor;

    @JsonProperty
    @Schema(example = "false")
    private boolean deleted;

    @JsonProperty
    @Schema(example = "false")
    private boolean encrypted;

    @JsonProperty("ecuModels")
    private List<MgmtEcuModelResponse> ecuModels;

    @JsonProperty(required = true)
    @Schema(example = "1330")
    private String swInstallerType;

    @JsonProperty("artifacts")
    private List<MgmtArtifacts> artifacts;
    

	public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    public Long getModuleId() {
        return moduleId;
    }

    @JsonIgnore
    public void setModuleId(final Long moduleId) {
        this.moduleId = moduleId;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(final String typeName) {
        this.typeName = typeName;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(final String vendor) {
        this.vendor = vendor;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(final boolean encrypted) {
        this.encrypted = encrypted;
    }

    public List<MgmtEcuModelResponse> getEcuModels() {
        return ecuModels;
    }

    public void setEcuModels(final List<MgmtEcuModelResponse> ecuModels) {
        this.ecuModels = ecuModels;
    }

    public String getSwInstallerType() {
        return swInstallerType;
    }

    public void setSwInstallerType(String swInstallerType) {
        this.swInstallerType = swInstallerType;
    }

    public List<MgmtArtifacts> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(final List<MgmtArtifacts> artifacts) {
        this.artifacts = artifacts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MgmtSoftwareModule that = (MgmtSoftwareModule) o;
        return deleted == that.deleted && encrypted == that.encrypted && Objects.equals(moduleId, that.moduleId) && Objects.equals(format, that.format) && Objects.equals(type, that.type) && Objects.equals(typeName, that.typeName) && Objects.equals(vendor, that.vendor) && Objects.equals(swInstallerType, that.swInstallerType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), moduleId, format, type, typeName, vendor, deleted, encrypted, swInstallerType);
    }
}
