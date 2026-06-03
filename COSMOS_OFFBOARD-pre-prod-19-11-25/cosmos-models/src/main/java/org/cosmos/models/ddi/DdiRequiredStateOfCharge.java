package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import jakarta.validation.constraints.Size;

/**
 * This class represents a Deployment Metadata in the DDI model.
 * It denotes Required State of Charge (Battery) Conditions for Update
 * It is annotated with JsonIgnoreProperties to ignore unknown properties during deserialization.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class DdiRequiredStateOfCharge {

    /**
     * This attribute denotes Required State of Charge (Battery) Conditions for Update
     * Note: Once the key name is finalised, need to change key and also remove or modify defaultValue
     */
    @JsonProperty(value = "dummyKey", defaultValue = "dummyValue")
    @Size(max = 256, message = "Field cannot exceed 256 characters")
    private String dummyKey;


    public DdiRequiredStateOfCharge(String dummyKey) {
        this.dummyKey = dummyKey;
    }

    public DdiRequiredStateOfCharge() {
    }

}

