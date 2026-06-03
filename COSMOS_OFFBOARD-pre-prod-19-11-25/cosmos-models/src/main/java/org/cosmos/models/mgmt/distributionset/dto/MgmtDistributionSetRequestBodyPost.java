/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.distributionset.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModuleAssignments;

import java.util.List;

/**
 * A json annotated rest model for DistributionSet for POST.
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtDistributionSetRequestBodyPost extends MgmtDistributionSetRequestBodyPut {

    // deprecated format from the time where os, application and runtime where
    // statically defined
    @JsonProperty
    @Schema(hidden = true)
    private MgmtSoftwareModuleAssignments os;

    @JsonProperty
    @Schema(hidden = true)
    private MgmtSoftwareModuleAssignments runtime;

    @JsonProperty
    @Schema(hidden = true)
    private MgmtSoftwareModuleAssignments application;
    // deprecated format - END

    @JsonProperty
    private List<MgmtSoftwareModuleAssignments> modules;

    @JsonProperty
    @Schema(example = "test_default_ds_type")
    private String type;
    
    @JsonProperty
    @Schema(example = "true")
    private boolean softwareDowngradeEnabled;

	public boolean isSoftwareDowngradeEnabled() {
		return softwareDowngradeEnabled;
	}

	public void setSoftwareDowngradeEnabled(boolean softwareDowngradeEnabled) {
		this.softwareDowngradeEnabled = softwareDowngradeEnabled;
	}

	/**
     * @return the os
     */
    public MgmtSoftwareModuleAssignments getOs() {
        return os;
    }

    /**
     * @param os
     *            the os to set
     *
     * @return updated body
     */
    public MgmtDistributionSetRequestBodyPost setOs(final MgmtSoftwareModuleAssignments os) {
        this.os = os;
        return this;
    }

    /**
     * @return the runtime
     */
    public MgmtSoftwareModuleAssignments getRuntime() {
        return runtime;
    }

    /**
     * @param runtime
     *            the runtime to set
     *
     * @return updated body
     */
    public MgmtDistributionSetRequestBodyPost setRuntime(final MgmtSoftwareModuleAssignments runtime) {
        this.runtime = runtime;

        return this;
    }

    /**
     * @return the application
     */
    public MgmtSoftwareModuleAssignments getApplication() {
        return application;
    }

    /**
     * @param application
     *            the application to set
     *
     * @return updated body
     */
    public MgmtDistributionSetRequestBodyPost setApplication(final MgmtSoftwareModuleAssignments application) {
        this.application = application;

        return this;
    }

    /**
     * @return the modules
     */
    public List<MgmtSoftwareModuleAssignments> getModules() {
        return modules;
    }

    /**
     * @param modules
     *            the modules to set
     *
     * @return updated body
     */
    public MgmtDistributionSetRequestBodyPost setModules(final List<MgmtSoftwareModuleAssignments> modules) {
        this.modules = modules;

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
    public MgmtDistributionSetRequestBodyPost setType(final String type) {
        this.type = type;

        return this;
    }

}
