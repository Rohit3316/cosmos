package org.cosmos.models.mgmt.targetfilter.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for Vin list target Filter POST commands.
 *
 */
public class MgmtTargetFilterVinListRequestBody {

	    @JsonProperty(required = true)
	    @Schema(example = "['4F2YZ04153KM18431_1FU15645454', '1C3BF66P9HX758540_1FU15645469']")
	    private List<String> controllerIds;

		public List<String> getControllerIds() {
			return controllerIds;
		}

		public void setControllerIds(List<String> controllerIds) {
			this.controllerIds = controllerIds;
		}

}
