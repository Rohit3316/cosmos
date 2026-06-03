package org.cosmos.models.mgmt;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * A json annotated rest model for NamedEntity to RESTful API representation.
 */
public abstract class MgmtNamedEntity extends MgmtBaseEntity {

    @JsonProperty(required = true)
    @Schema(example = "Name of entity")
    private String name;

    @JsonProperty
    @Schema(example = "Description of entity")
    private String description;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MgmtNamedEntity that = (MgmtNamedEntity) o;
        return Objects.equals(name, that.name) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, description);
    }
}
