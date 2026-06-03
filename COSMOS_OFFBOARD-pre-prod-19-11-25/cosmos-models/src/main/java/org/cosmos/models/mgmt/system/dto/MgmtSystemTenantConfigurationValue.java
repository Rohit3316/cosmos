/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.cosmos.models.mgmt.system.dto;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A json annotated rest model for a tenant configuration value to RESTful API
 * representation.
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtSystemTenantConfigurationValue extends RepresentationModel<MgmtSystemTenantConfigurationValue> {

    @JsonInclude(Include.ALWAYS)
    @Schema(example = "true")
    private Object value;

    @JsonInclude(Include.ALWAYS)
    @Schema(example = "true")
    private boolean isGlobal = true;

    @Schema(example = "1623085150")
    private Long lastModifiedAt;
    
    @Schema(example = "example user")
    private String lastModifiedBy;
    
    @Schema(example = "1523085150")
    private Long createdAt;
    
    @Schema(example = "example user")
    private String createdBy;

    public Object getValue() {
        return value;
    }

    public void setValue(final Object value) {
        this.value = value;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    public void setGlobal(final boolean isGlobal) {
        this.isGlobal = isGlobal;
    }

    public Long getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(final Long lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(final String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Long createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

}
