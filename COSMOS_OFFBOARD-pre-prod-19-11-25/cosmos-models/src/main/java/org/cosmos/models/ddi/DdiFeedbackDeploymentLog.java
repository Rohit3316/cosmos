package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.hateoas.RepresentationModel;

/**
 * Request Deployment Logs.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiFeedbackDeploymentLog extends RepresentationModel<DdiFeedbackDeploymentLog> {

}

