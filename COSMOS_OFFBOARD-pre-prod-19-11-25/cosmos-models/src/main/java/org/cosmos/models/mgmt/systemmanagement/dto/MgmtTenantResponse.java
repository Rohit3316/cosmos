/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.systemmanagement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.cosmos.models.mgmt.MgmtBaseEntity;
import org.cosmos.models.mgmt.system.dto.MgmtSystemTenantConfigurationValue;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * Model representation of a response for the tenant obj
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtTenantResponse  extends MgmtBaseEntity {

    @JsonProperty(required = true)
    @NotNull
    @Schema(example = "7")
    private int optLockRevision;

    @JsonProperty(required = true)
    @NotNull
    @Schema(example = "default")
    private String tenant;

    @JsonProperty(required = true)
    @NotNull
    @Schema(example = "5")
    private long defaultDistributionType;

    @JsonProperty(required = true)
    @NotNull
    private Map<String, MgmtSystemTenantConfigurationValue> configurations;

    public MgmtTenantResponse() {
    }

    public MgmtTenantResponse(int optLockRevision, String tenant, long defaultDistributionType) {
        this.optLockRevision = optLockRevision;
        this.tenant = tenant;
        this.defaultDistributionType = defaultDistributionType;
    }

    public int getOptLockRevision() {
        return optLockRevision;
    }

    public void setOptLockRevision(int optLockRevision) {
        this.optLockRevision = optLockRevision;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public long getDefaultDistributionType() {
        return defaultDistributionType;
    }

    public Map<String, MgmtSystemTenantConfigurationValue> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(Map<String, MgmtSystemTenantConfigurationValue> configurations) {
        this.configurations = configurations;
    }

    public void setDefaultDistributionType(long defaultDistributionType) {
        this.defaultDistributionType = defaultDistributionType;


    }

}
