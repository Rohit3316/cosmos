package org.cosmos.models.mgmt.softwaremodule.dto;

import java.util.List;

import org.cosmos.models.mgmt.ecu.dto.EcuModels;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MgmtAssignEcuModelRequestBody {

	@JsonProperty(required = true)
	List<EcuModels> ecuModels;

	public List<EcuModels> getEcuModels() {
		return ecuModels;
	}

	public void setEcuModels(List<EcuModels> ecuModels) {
		this.ecuModels = ecuModels;
	}

}
