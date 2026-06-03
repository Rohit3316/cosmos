package org.cosmos.models.mgmt.systemmanagement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

import org.cosmos.models.mgmt.MgmtNamedEntity;
import org.springframework.hateoas.Link;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtTenant extends MgmtNamedEntity {

    @JsonProperty
    @Schema(example = "6")
    private Long id;

    @JsonProperty
    @Schema(example = "xyz")
    private String value;

    public MgmtTenant(Long id, String value, Link link) {
    	this.id = id;
        this.value = value;
        this.add(link);
        
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MgmtTenant that = (MgmtTenant) o;
        return Objects.equals(id, that.id) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, value);
    }
}
