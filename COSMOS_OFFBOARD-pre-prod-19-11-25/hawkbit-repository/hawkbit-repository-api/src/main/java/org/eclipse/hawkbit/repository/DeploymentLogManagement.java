package org.eclipse.hawkbit.repository;

import org.cosmos.s3.exception.S3Exception;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.model.DeploymentLog;
import org.eclipse.hawkbit.repository.model.DeploymentLogUpload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Service for {@link DeploymentLog} management operations.
 */
public interface DeploymentLogManagement {


    /**
     * Creates a new DeploymentLog entry in the database and uploads the log file to S3.
     *
     * @param deploymentLogUpload details of the deployment log to create
     * @param file the log file to upload to S3
     * @return the created DeploymentLog
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    DeploymentLog create(@NotNull @Valid DeploymentLogUpload deploymentLogUpload, MultipartFile file) throws S3Exception;

    /**
     * @return the total amount of local DeploymentLog stored in the DeploymentLog
     * management
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    long count();

    /**
     * @param actionId
     * @param fileName
     * @param bytesize
     * @param range
     * @return boolean
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    boolean checkDeploymentLogExists(Long actionId, String fileName, Integer sequence);
    
    
    /**
     * @param id
     * @return the DeploymentLog for given deploymentLog id
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    DeploymentLog findDeploymentLogById(Long id);

}
