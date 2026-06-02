package org.eclipse.hawkbit.mgmt.rest.resource;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDeploymentLogRestApi;
import org.eclipse.hawkbit.repository.DeploymentLogManagement;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.rest.util.RequestResponseContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;

@RestController
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
@Tag(name = "Deployment Log")
public class MgmtDeploymentLogResource implements MgmtDeploymentLogRestApi {

    private static final Logger LOG = LoggerFactory.getLogger(MgmtDeploymentLogResource.class);

    private final DeploymentManagement deploymentManagement;

    private final DeploymentLogManagement deploymentLogManagement;

    private final RequestResponseContextHolder requestResponseContextHolder;

    public MgmtDeploymentLogResource(final DeploymentManagement deploymentManagement,
                                     final DeploymentLogManagement deploymentLogManagement,
                                     final RequestResponseContextHolder requestResponseContextHolder) {
        this.deploymentManagement = deploymentManagement;
        this.deploymentLogManagement = deploymentLogManagement;
        this.requestResponseContextHolder = requestResponseContextHolder;
    }

    /**
     * Handles the GET request for downloading an artifact.
     *
     * @param controllerId
     * @param actionId
     * @param deploymentLogId
     * @return responseEntity with status ok if successful
     */
    @Override
    public ResponseEntity<InputStream> downloadDeploymentLog(@PathVariable("controllerId") final String controllerId, @PathVariable("actionId") final Long actionId,
                                                             @PathVariable("deploymentLogId") final Long deploymentLogId) {

        validateRequestParams(actionId, deploymentLogId);

        //Removed the implementation of this api to read and return the file from file system and returning a 404 error temporarily.
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This API is temporarily unavailable due to future changes.");
        //TODO: Implement to return a presigned url of the deployment log file similar to artifacts download api in future.

    }


    /**
     * Method to validate input params in Db
     *
     * @param actionId
     * @param deploymentLogId
     */
    private void validateRequestParams(final Long actionId, final Long deploymentLogId) {

        try {
            deploymentManagement.findAction(actionId);
        } catch (Exception ex) {
            throw new EntityNotFoundException(Action.class, actionId);
        }

        deploymentLogManagement.findDeploymentLogById(deploymentLogId);
    }
}
