package org.cosmos.models.mgmt.softwaremodule.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtAssignedSoftwareModule extends MgmtSoftwareModule {

	
	 	@JsonProperty
	    @Schema(example = "68541061AA000000000000000000007670181252*1")
	    private String softwareVersionTargetName;
	    
	    @JsonProperty
	    @Schema(example = "['68541061000000000008*670181252*1','00007670181252*133136002JY0000*']")
	    private List<String> softwareVersionSourceName;
	    
	    public String getSoftwareVersionTargetName() {
			return softwareVersionTargetName;
		}

		public void setSoftwareVersionTargetName(String softwareVersionTargetName) {
			this.softwareVersionTargetName = softwareVersionTargetName;
		}

		public List<String> getSoftwareVersionSourceName() {
			return softwareVersionSourceName;
		}

		public void setSoftwareVersionSourceName(List<String> softwareVersionSourceName) {
			this.softwareVersionSourceName = softwareVersionSourceName;
		}
}
