package org.cosmos.models.mgmt.systemmanagement.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * 
 * Model representation of a response for the get all tenants
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtAllTenantResponse {

	private List<MgmtTenant> tenants;

	public List<MgmtTenant> getTenants() {
		return tenants;
	}

	public void setTenants(List<MgmtTenant> tenants) {
		this.tenants = tenants;
	}
}
