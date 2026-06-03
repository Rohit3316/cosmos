/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.rest.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.lang.annotation.Target;
import java.security.SignatureException;
import org.cosmos.models.ddi.DdiActionFeedback;
import org.cosmos.models.ddi.DdiActionFeedbacks;
import org.cosmos.models.ddi.DdiActivateAutoConfirmation;
import org.cosmos.models.ddi.DdiCancel;
import org.cosmos.models.ddi.DdiConfigData;
import org.cosmos.models.ddi.DdiConfirmationBase;
import org.cosmos.models.ddi.DdiConfirmationBaseAction;
import org.cosmos.models.ddi.DdiConfirmationFeedback;
import org.cosmos.models.ddi.DdiControllerBase;
import org.cosmos.models.ddi.DdiDeploymentBase;
import org.cosmos.models.ddi.DdiDeviceInventory;
import org.cosmos.models.ddi.DdiFeedbackRequestBody;
import org.cosmos.models.ddi.DdiFeedbackResponse;
import org.cosmos.models.ddi.DdiRestConstants;
import org.cosmos.models.ddi.InventoryWithAction;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.PagedList;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

/**
 * REST resource handling for root controller CRUD operations.
 */
@Tag(name = "DDI Root Controller", description = "REST resource handling for root controller CRUD operations")
public interface DdiRootControllerRestApi {


    /**
     * Resource for software module.
     *
     * @param controllerId              of the target
     * @param actionId                  of the {@link DdiDeploymentBase} that matches to active
     *                                  actions.
     * @param resource                  an hashcode of the resource which indicates if the action has
     *                                  been changed, e.g. from 'soft' to 'force' and the eTag needs
     *                                  to be re-generated
     * @param actionHistoryMessageCount specifies the number of messages to be returned from action
     *                                  history. Regardless of the passed value, in order to restrict
     *                                  resource utilization by controllers, maximum number of
     *                                  messages that are retrieved from database is limited by
     *                                  actionHistoryMessageCount less than zero: retrieves the
     *                                  maximum allowed number of action status messages from history;
     *                                  actionHistoryMessageCount equal to zero: does not retrieve any
     *                                  message;
     *                                  actionHistoryMessageCount greater than zero: retrieves the
     *                                  specified number of messages, limited by maximum allowed
     *                                  number.
     * @return the response
     */
    @Operation(summary = "Resource for software module (Deployment Base)", description = """
            Core resource for deployment operations. Contains all information necessary in order to execute the operation.

            Keep in mind that the provided download links for the artifacts are generated dynamically by the update server. Host, port and path and not guaranteed to be similar to the provided examples below but will be defined at runtime.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = DdiRestConstants.CONTROLLER_BASE_DEPLOYMENT_ACTION_PATH, produces = {MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE, DdiRestConstants.MEDIA_TYPE_CBOR})
    @Deprecated
    //TODO:This APi can be removed once the flow is completed
    ResponseEntity<DdiDeploymentBase> getControllerBasedeploymentAction(@PathVariable("controllerId") @NotEmpty final String controllerId,
                                                                        @PathVariable("actionId") @NotNull final Long actionId,
                                                                        @RequestParam(value = DdiRestConstants.DEVICE_V1_NO_TENANTS_REQUEST_MAPPING
                                                                                + "c", required = false, defaultValue = "-1") final int resource,
                                                                        @RequestParam(value = DdiRestConstants.DEVICE_V1_NO_TENANTS_REQUEST_MAPPING
                                                                                + "actionHistory", defaultValue = DdiRestConstants.NO_ACTION_HISTORY) final Integer actionHistoryMessageCount);

    /**
     * This is the feedback channel for the {@link DdiDeploymentBase} action.
     *
     * @param feedback     to provide
     * @param controllerId of the target that matches to controller id
     * @param actionId     of the action we have feedback for
     * @return the response contains links
     */
    @Operation(summary = "Feedback channel for the DeploymentBase action", description = """
            Feedback channel. It is up to the device how much intermediate feedback is provided.
            However, the action will be kept open until the controller on the device reports a finished (either successful or error).
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully added feedback"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "410", description = "Action is not active anymore.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = DdiRestConstants.BASE_DEPLOYMENT_ACTION_FEEDBACK_PATH,
            consumes = {MediaType.APPLICATION_JSON_VALUE, DdiRestConstants.MEDIA_TYPE_CBOR},
            produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE, DdiRestConstants.MEDIA_TYPE_CBOR})
    @Deprecated
    ResponseEntity<DdiFeedbackResponse> postBaseDeploymentActionFeedback(@Valid final DdiActionFeedback feedback,
                                                                         @PathVariable("controllerId") final String controllerId,
                                                                         @PathVariable("actionId") @NotNull final Long actionId);

    /**
     * Root resource for an individual {@link Target}.
     * To verify inventoryHash present for the target
     *
     * @param controllerId of the target that matches to controller id
     * @param hash         polling inventoryHash of the target
     * @return the response
     */
    @Operation(summary = "Poll for Updates", description = "This base resource can be regularly polled by the controller on the provisioning target or device in order to retrieve actions that need to be executed.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = DdiRestConstants.GET_INVENTORY_HASH_PATH, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE, DdiRestConstants.MEDIA_TYPE_CBOR})
    ResponseEntity<DdiControllerBase> getInventoryHash(@PathVariable("controllerId") final String controllerId,
                                                       @RequestParam(value = DdiRestConstants.HASH, required = true) final String hash,
                                                       @RequestHeader(required = false) HttpHeaders headers);


    /**
     * Root resource for an individual {@link Target}.
     * To upload feedback present for the target
     *
     * @param feedbackRequestBody of the request
     * @param controllerId of the target that matches to controller id
     * @return the response
     */
    @Operation(summary = "Add General Feedback", description = "Add General Feedback")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = DdiRestConstants.GENERAL_FEEDBACK_PATH, consumes = {
            MediaType.APPLICATION_JSON_VALUE, DdiRestConstants.MEDIA_TYPE_CBOR})
    ResponseEntity<Void> feedback(@PathVariable("controllerId") @NotEmpty final String controllerId,
                                  @Valid @RequestBody(required = false) final DdiFeedbackRequestBody feedbackRequestBody);

    /**
     * This is the feedback channel for the config data action.
     *
     * @param configData   as body
     * @param controllerId to provide data for
     * @return status of the request
     * deprecated since we are removing sp_polling and its references
     */
    @Deprecated
    @Operation
    @PutMapping(value = DdiRestConstants.PUT_POLLING_DATA_PATH)
    ResponseEntity<String> putPollingData(@Valid final DdiConfigData configData, @PathVariable("controllerId") final String controllerId, @PathVariable("pollingId") final String pollingId);


    /**
     * RequestMethod.GET method for the {@link DdiCancel} action.
     *
     * @param controllerId ID of the calling target
     * @param actionId     of the action
     * @return the {@link DdiCancel} response
     */
    @Operation(summary = "Cancel an action", description = """
            The Hawkbit server might cancel an operation, e.g. an unfinished update has a successor. It is up to the provisioning target to decide to accept the cancellation or reject it.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = DdiRestConstants.CONTROLLER_CANCEL_ACTION_PATH, produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE,
            DdiRestConstants.MEDIA_TYPE_CBOR})
    ResponseEntity<DdiCancel> getControllerCancelAction(@PathVariable("controllerId") @NotEmpty final String controllerId,
                                                        @PathVariable("actionId") @NotNull final Long actionId);

    /**
     * RequestMethod.POST method receiving the {@link DdiActionFeedback} from
     * the target.
     *
     * @param feedback     the {@link DdiActionFeedback} from the target.
     * @param controllerId the ID of the calling target
     * @param actionId     of the action we have feedback for
     * @return the {@link DdiActionFeedback} response
     */
    @Operation(summary = "Feedback channel for cancel actions", description = """
            It is up to the device how much intermediate feedback is provided. However, the action will be kept open until the controller on the device reports a finished (either successful or error) or rejects the action, e.g. the canceled actions have been started already.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully cancelled"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = DdiRestConstants.POST_CANCEL_ACTION_FEEDBACK_PATH, consumes = {MediaType.APPLICATION_JSON_VALUE,
            DdiRestConstants.MEDIA_TYPE_CBOR})
    ResponseEntity<Void> postCancelActionFeedback(@Valid final DdiActionFeedback feedback,
                                                  @PathVariable("controllerId") @NotEmpty final String controllerId,
                                                  @PathVariable("actionId") @NotNull final Long actionId);

    /**
     * Resource for installed distribution set to retrieve the last successfully
     * finished action.
     *
     * @param controllerId              of the target
     * @param actionId                  of the {@link DdiDeploymentBase} that matches to installed
     *                                  action.
     * @param actionHistoryMessageCount specifies the number of messages to be returned from action
     *                                  history. Regardless of the passed value, in order to restrict
     *                                  resource utilization by controllers, maximum number of
     *                                  messages that are retrieved from database is limited by
     *                                  actionHistoryMessageCount less than zero: retrieves the
     *                                  maximum allowed number of action status messages from history;
     *                                  actionHistoryMessageCount equal to zero: does not retrieve any
     *                                  message;
     *                                  actionHistoryMessageCount greater than zero: retrieves the
     *                                  specified number of messages, limited by maximum allowed
     *                                  number.
     * @return the {@link DdiDeploymentBase}. The response is of same format as
     * for the /deploymentBase resource.
     */
    @Operation(summary = "Previously installed action", description = """
            Resource to receive information of the previous installation. Can be used to re-retrieve artifacts of the already finished action, for example in case a re-installation is necessary. The response will be of the same format as the deploymentBase operation, providing the previous action that has been finished successfully. As the action is already finished, no further feedback is expected.                    
            Keep in mind that the provided download links for the artifacts are generated dynamically by the update server. Host, port and path are not guaranteed to be similar to the provided examples below but will be defined at runtime.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = """
                    The response body includes the detailed operation for the already finished action in the same format as for the deploymentBase operation.
                                
                    In this case the (optional) query for the last 10 messages, previously provided by the device, are included."""),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = DdiRestConstants.GET_INSTALLED_ACTION_PATH, produces = {MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE, DdiRestConstants.MEDIA_TYPE_CBOR})
    ResponseEntity<DdiDeploymentBase> getControllerInstalledAction(@PathVariable("controllerId") @NotEmpty final String controllerId,
                                                                   @PathVariable("actionId") @NotNull final Long actionId,
                                                                   @RequestParam(value = DdiRestConstants.DEVICE_V1_NO_TENANTS_REQUEST_MAPPING
                                                                           + "actionHistory", defaultValue = DdiRestConstants.NO_ACTION_HISTORY) final Integer actionHistoryMessageCount);

    /**
     * Returns the confirmation base with the current auto-confirmation state
     * for a given controllerId and toggle links. In case there are actions
     * present where the confirmation is required, a reference to it will be
     * returned as well.
     *
     * @param controllerId to check the state for
     * @return the state as {@link DdiAutoConfirmationState}
     */
    @Operation(summary = "Resource to request confirmation specific information for the controller", description = """
            Core resource for confirmation related operations. While active actions awaiting confirmation will be referenced, the current auto-confirmation status will be shown. In case auto-confirmation is active, details like the initiator, remark and date of activation (as unix timestamp) will be provided.
            Reference links to switch the auto-confirmation state are exposed as well.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(oneOf = {}))),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = DdiRestConstants.GET_CONFIRMATION_BASE_PATH, produces = {MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE, DdiRestConstants.MEDIA_TYPE_CBOR})
    ResponseEntity<DdiConfirmationBase> getConfirmationBase(@PathVariable("controllerId") @NotEmpty final String controllerId);

    /**
     * Resource for confirmation of an action.
     *
     * @param controllerId              of the target
     * @param actionId                  of the {@link DdiConfirmationBaseAction} that matches to
     *                                  active actions in WAITING_FOR_CONFIRMATION status.
     * @param resource                  a hashcode of the resource which indicates if the action has
     *                                  been changed, e.g. from 'soft' to 'force' and the eTag needs
     *                                  to be re-generated
     * @param actionHistoryMessageCount specifies the number of messages to be returned from action
     *                                  history. Regardless of the passed value, in order to restrict
     *                                  resource utilization by controllers, maximum number of
     *                                  messages that are retrieved from database is limited by
     *                                  actionHistoryMessageCount less than zero: retrieves the
     *                                  maximum allowed number of action status messages from history;
     *                                  actionHistoryMessageCount equal to zero: does not retrieve any
     *                                  message;
     *                                  actionHistoryMessageCount greater than zero: retrieves the
     *                                  specified number of messages, limited by maximum allowed
     *                                  number.
     * @return the response
     */
    @Operation(summary = "Confirmation status of an action", description = """
            Resource to receive information about a pending confirmation. The response will be of the same format as the deploymentBase operation. The controller should provide feedback about the confirmation first, before processing the deployment.
                    
            Keep in mind that the provided download links for the artifacts are generated dynamically by the update server. Host, port and path are not guaranteed to be similar to the provided examples below but will be defined at runtime.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The response body includes the detailed information about the action awaiting confirmation in the same format as for the deploymentBase operation."),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = DdiRestConstants.GET_CONFIRMATION_BASE_ACTION_PATH, produces = {MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE, DdiRestConstants.MEDIA_TYPE_CBOR})
    ResponseEntity<DdiConfirmationBaseAction> getConfirmationBaseAction(@PathVariable("controllerId") @NotEmpty final String controllerId,
                                                                        @PathVariable("actionId") @NotNull final Long actionId,
                                                                        @RequestParam(value = DdiRestConstants.DEVICE_V1_NO_TENANTS_REQUEST_MAPPING
                                                                                + "c", required = false, defaultValue = "-1") final int resource,
                                                                        @RequestParam(value = DdiRestConstants.DEVICE_V1_NO_TENANTS_REQUEST_MAPPING
                                                                                + "actionHistory", defaultValue = DdiRestConstants.NO_ACTION_HISTORY) final Integer actionHistoryMessageCount);

    /**
     * This is the feedback channel for the {@link DdiConfirmationBaseAction}
     * action.
     *
     * @param feedback     to provide
     * @param controllerId of the target that matches to controller id
     * @param actionId     of the action we have feedback for
     * @return the response
     */
    @Operation(summary = "Feedback channel for actions waiting for confirmation", description = """
            The device will use this resource to either confirm or deny an action which is waiting for confirmation. The action will be transferred into the RUNNING state in case the device is confirming it. Afterwards it will be exposed by the deploymentBase.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully added"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target or Action not found", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "410", description = "Action is not active anymore.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = DdiRestConstants.POST_CONFIRMATION_ACTION_FEEDBACK_PATH, consumes = {
            MediaType.APPLICATION_JSON_VALUE, DdiRestConstants.MEDIA_TYPE_CBOR})

    ResponseEntity<Void> postConfirmationActionFeedback(@Valid final DdiConfirmationFeedback feedback,
                                                        @PathVariable("controllerId") final String controllerId,
                                                        @PathVariable("actionId") @NotNull final Long actionId);

    /**
     * Activate auto confirmation for a given controllerId. Will use the
     * provided initiator and remark field from the provided
     * {@link DdiActivateAutoConfirmation}. If not present, the values will be
     * prefilled with a default remark and the CONTROLLER as initiator.
     *
     * @param controllerId to activate auto-confirmation for
     * @param body         as {@link DdiActivateAutoConfirmation}
     * @return {@link org.springframework.http.HttpStatus#OK} if successful or
     * {@link org.springframework.http.HttpStatus#CONFLICT} in case
     * auto-confirmation was active already.
     */
    @Operation(summary = "Interface to activate auto-confirmation for a specific device", description = """
            The device can use this resource to activate auto-confirmation. As a result all current active as well as future actions will automatically be confirmed by mentioning the initiator as triggered person. Actions will be automatically confirmed, as long as auto-confirmation is active.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully activated"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = DdiRestConstants.POST_ACTIVATE_AUTO_CONFIRMATION_PATH, consumes = {
            MediaType.APPLICATION_JSON_VALUE, DdiRestConstants.MEDIA_TYPE_CBOR})
    ResponseEntity<Void> activateAutoConfirmation(@PathVariable("controllerId") @NotEmpty final String controllerId,
                                                  @Valid @RequestBody(required = false) final DdiActivateAutoConfirmation body);

    /**
     * Deactivate auto confirmation for a given controller id.
     *
     * @param controllerId to disable auto-confirmation for
     * @return {@link org.springframework.http.HttpStatus#OK} if successfully
     * executed
     */
    @Operation(summary = "Interface to deactivate auto-confirmation for a specific controller", description = """
            The device can use this resource to deactivate auto-confirmation. All active actions will remain unchanged while all future actions need to be confirmed, before processing with the deployment.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully deactivated"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = DdiRestConstants.POST_DEACTIVATE_AUTO_CONFIRMATION_PATH)
    ResponseEntity<Void> deactivateAutoConfirmation(@PathVariable("controllerId") @NotEmpty final String controllerId);

    /**
     * This is the feedback channel for all the {@link DdiDeploymentBase} action status.
     *
     * @param feedback     to provide
     * @param controllerId of the target that matches to controller id
     * @param actionId     of the action we have feedback for
     * @return the response contains links
     */
    @Operation(summary = "Feedback channel for the DeploymentBase action", description = """
            Feedback channel. The device will shared a list of feedbacks.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully added feedbacks"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "410", description = "Action is not active anymore.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = DdiRestConstants.POST_DEPLOYED_ACTION_FEEDBACK_LIST_PATH,
            consumes = {MediaType.APPLICATION_JSON_VALUE, DdiRestConstants.MEDIA_TYPE_CBOR},
            produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE, DdiRestConstants.MEDIA_TYPE_CBOR})
    ResponseEntity<DdiFeedbackResponse> postBaseDeploymentActionFeedbackList(@Valid final DdiActionFeedbacks feedback,
                                                                             @PathVariable("controllerId") final String controllerId,
                                                                             @PathVariable("actionId") @NotNull final Long actionId);


    /**
     * Push Inventory with Signature API
     *
     * @param inventory    as body
     * @param controllerId to provide data for
     * @return status of the request
     */
    @Operation(summary = "Allow new inventory to be provided to the device on hardware level", description = """
            The usual behavior is that when a new device registers at the server it is requested to provide the meta information that will allow the server to identify the device on a hardware level (e.g. hardware revision, mac address, serial number etc.).
             """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = DdiRestConstants.PUT_DEVICE_INVENTORY_PATH, consumes = {MediaType.APPLICATION_JSON_VALUE,
            DdiRestConstants.MEDIA_TYPE_CBOR})
    ResponseEntity<Void> putDeviceInventory(
            @Valid final DdiDeviceInventory inventory,
            @PathVariable("controllerId") final String controllerId) throws SignatureException, JsonProcessingException, IllegalAccessException;


    // TODO: Edit Operaion field description, @APIResponses,

    /**
     * Endpoint to publish deployment logs for a specific action on a controller.
     * <p>
     * This endpoint allows the upload of deployment logs related to a specific action
     * performed on a controller. It supports multipart file uploads, including the ability
     * to handle chunked uploads. The request may include metadata such as filename,
     * sequence, bytesize, and indicators for the last chunk or file.
     *
     * @param isLastFile   Required flag indicating if this is the last file in a multipart upload.
     * @param controllerId Identifier for the target controller.
     * @param actionId     Identifier for the action associated with the logs.
     * @param file         The log file to be uploaded.
     * @param fileName     Required filename for the log file.
     * @param sequence     Optional sequence number for chunked file uploads.
     * @param bytesize     Required size of the log file in bytes.
     * @param range        Required byte range for chunked file uploads.
     * @param isLastChunk  Required flag indicating if this is the last chunk of a multipart upload.
     * @return ResponseEntity indicating the result of the operation.
     */
    @Operation(
            summary = "Publish deployment logs",
            description = """
                        Uploads deployment logs for a specific action related to a controller.
                        The endpoint supports multipart file uploads and handles chunked uploads.
                        Parameters:
                        - controllerId (Required): The ID of the target controller.
                        - actionId (Required): The ID of the action for which logs are provided.
                        - file (Required): The log file to be uploaded. Chunked uploads are supported.
                        - fileName (Required): The filename for the uploaded log file.
                        - sequence: Optional sequence number for chunked uploads.
                        - bytesize (Required): Size of the file in bytes.
                        - range (Required): Range for chunked uploads.
                        - isLastChunk (Required): Flag indicating if this is the last chunk of a multipart upload.
                        - isLastFile (Required): Flag indicating if this is the last file in a multipart upload.
                    """
    )
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully added"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "410", description = "Action is not active anymore.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))})
    @PostMapping(value = DdiRestConstants.POST_DEPLOYMENT_LOGS_PATH, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE, DdiRestConstants.MEDIA_TYPE_CBOR})
    ResponseEntity<Void> publishDeploymentLogs(@PathVariable("controllerId") @NotEmpty final String controllerId,
                                               @PathVariable("actionId") @NotNull final Long actionId,
                                               @RequestPart(value = "file") @NotNull final MultipartFile file,
                                               @RequestParam(value = "filename") @NotEmpty String fileName,
                                               @RequestParam(value = "sequence", required = false) final Integer sequence,
                                               @RequestParam(value = "isLastFile") @NotNull final Boolean isLastFile);

    /**
     * Resource for software module.
     *
     * @param controllerId              of the target
     * @param actionId                  of the {@link DdiDeploymentBase} that matches to active
     *                                  actions.
     * @param resource                  an hashcode of the resource which indicates if the action has
     *                                  been changed, e.g. from 'soft' to 'force' and the eTag needs
     *                                  to be re-generated
     * @param actionHistoryMessageCount specifies the number of messages to be returned from action
     *                                  history. Regardless of the passed value, in order to restrict
     *                                  resource utilization by controllers, maximum number of
     *                                  messages that are retrieved from database is limited by
     *                                  actionHistoryMessageCount less than zero: retrieves the
     *                                  maximum allowed number of action status messages from history;
     *                                  actionHistoryMessageCount equal to zero: does not retrieve any
     *                                  message;
     *                                  actionHistoryMessageCount greater than zero: retrieves the
     *                                  specified number of messages, limited by maximum allowed
     *                                  number.
     * @return the response
     */
    @Operation(summary = "Resource for software module (Deployment Base)", description = """
            Core resource for deployment operations. Contains all information necessary in order to execute the operation.
                    
            Keep in mind that the provided download links for the artifacts are generated dynamically by the update server. Host, port and path and not guaranteed to be similar to the provided examples below but will be defined at runtime.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = DdiRestConstants.GET_TARGET_BASED_DEPLOYMENT_ACTION_PATH, produces = {MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE, DdiRestConstants.MEDIA_TYPE_CBOR})
    ResponseEntity<?> getTargetBasedeploymentAction(@PathVariable("controllerId") @NotEmpty final String controllerId,
                                                                              @PathVariable("actionId") final Long actionId,
                                                                              @RequestParam(value = DdiRestConstants.DEVICE_V1_NO_TENANTS_REQUEST_MAPPING+ "c", required = false, defaultValue = "-1") final int resource,
                                                                              @RequestParam(value = DdiRestConstants.DEVICE_V1_NO_TENANTS_REQUEST_MAPPING
                                                                                      + "actionHistory", defaultValue = DdiRestConstants.NO_ACTION_HISTORY) final Integer actionHistoryMessageCount);

    /**
     * Retrieves the inventory details based on the Controller ID.
     * <p>
     * This method fetches the details of a specific resource by its ID, including pagination and sorting options.
     * </p>
     *
     * @param controllerId      The unique identifier of the controller.
     * @param pagingOffsetParam The offset for pagination, defaulting to the configured value.
     * @param pagingLimitParam  The limit for pagination, defaulting to the configured value.
     * @param sortParam         The sorting criteria, optional.
     * @return A `ResponseEntity` containing a paginated list of `InventoryWithAction` objects.
     */

    @Operation(summary = "Retrieves the Inventory Details based on the ControllerId", description = "Fetches the details of a specific resource by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = DdiRestConstants.GET_INVENTORY_DETAILS, produces = {MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE, DdiRestConstants.MEDIA_TYPE_CBOR})
    ResponseEntity<PagedList<InventoryWithAction>> fetchInventoryDetails(@PathVariable("controllerId") String controllerId,
                                                                         @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
                                                                         @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
                                                                         @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam);


}