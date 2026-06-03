package org.cosmos.models.mgmt.targetfilter.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.cosmos.models.mgmt.targetfilter.dto.MgmtTargetFilterQuery;

/**
 * A json annotated rest model for Vin List Target Filter to RESTful API
 * representation.
 *
 */
@JsonInclude(Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtTargetFilterVinListQuery {

	@JsonProperty
	private MgmtTargetFilterQuery targetFilter;
	
	@JsonProperty
	private List<String> notPresent;

	public List<String> getNotPresent() {
		return notPresent;
	}

	public void setNotPresent(List<String> notPresent) {
		this.notPresent = notPresent;
	}

	public MgmtTargetFilterQuery getTargetFilter() {
		return targetFilter;
	}

	public void setTargetFilter(MgmtTargetFilterQuery targetFilter) {
		this.targetFilter = targetFilter;
	}
	
}
