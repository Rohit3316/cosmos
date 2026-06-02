package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;

/**
 * Request Deployment Logs.
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DdiFeedbackResponse extends RepresentationModel<DdiFeedbackResponse> {
    private Boolean canceled;

    public void setCanceledBasedOnExecutionStatus(DdiStatus.ExecutionStatus executionStatus) {
        if (executionStatus == DdiStatus.ExecutionStatus.CANCELED_ACCEPT) {
            this.canceled = true;
        } else if (executionStatus == DdiStatus.ExecutionStatus.CANCELED_REJECT) {
            this.canceled = false;
        } else {
            this.canceled = null; // Do not include the field in the response
        }
    }




}
