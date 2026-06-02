/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.google.common.collect.Iterables;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.cosmos.models.annotations.InvalidEpochTimeException;
import org.cosmos.sns.exception.SnsException;
import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.ServerError;
import org.eclipse.hawkbit.repository.exception.EcuCertificateException;
import org.eclipse.hawkbit.rest.json.model.ExceptionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * General controller advice for exception handling.
 */
@RestControllerAdvice
public class ResponseExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ResponseExceptionHandler.class);
    private static final Map<ServerError, HttpStatus> ERROR_TO_HTTP_STATUS = new EnumMap<>(ServerError.class);
    private static final HttpStatus DEFAULT_RESPONSE_STATUS = HttpStatus.INTERNAL_SERVER_ERROR;
    private static final String CORRELATION_ID = "correlationId";

    private static final String MESSAGE_FORMATTER_SEPARATOR = " ";

    static {
        ERROR_TO_HTTP_STATUS.put(ServerError.FIELD_ERROR, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.ENTITY_NOT_EXISTS, HttpStatus.NOT_FOUND);
        ERROR_TO_HTTP_STATUS.put(ServerError.ENTITY_ALREADY_EXISTS, HttpStatus.CONFLICT);
        ERROR_TO_HTTP_STATUS.put(ServerError.ENTITY_READ_ONLY, HttpStatus.FORBIDDEN);
        ERROR_TO_HTTP_STATUS.put(ServerError.REST_SORT_PARAM_INVALID_DIRECTION, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.REST_SORT_PARAM_INVALID_FIELD, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.REST_SORT_PARAM_SYNTAX, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.REST_RSQL_PARAM_INVALID_FIELD, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.REST_RSQL_SEARCH_PARAM_SYNTAX, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.INSUFFICIENT_PERMISSION, HttpStatus.FORBIDDEN);
        ERROR_TO_HTTP_STATUS.put(ServerError.ARTIFACT_UPLOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        ERROR_TO_HTTP_STATUS.put(ServerError.ARTIFACT_ENCRYPTION_NOT_SUPPORTED, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.ARTIFACT_ENCRYPTION_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        ERROR_TO_HTTP_STATUS.put(ServerError.ARTIFACT_UPLOAD_FAILED_SHA1_MATCH, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.ARTIFACT_UPLOAD_FAILED_SHA256_MATCH, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.ARTIFACT_UPLOAD_FAILED_MD5_MATCH, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.ARTIFACT_DELETE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        ERROR_TO_HTTP_STATUS.put(ServerError.ARTIFACT_BINARY_DELETED, HttpStatus.GONE);
        ERROR_TO_HTTP_STATUS.put(ServerError.ARTIFACT_LOAD_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
        ERROR_TO_HTTP_STATUS.put(ServerError.QUOTA_EXCEEDED, HttpStatus.FORBIDDEN);
        ERROR_TO_HTTP_STATUS.put(ServerError.FILE_SIZE_QUOTA_EXCEEDED, HttpStatus.FORBIDDEN);
        ERROR_TO_HTTP_STATUS.put(ServerError.STORAGE_QUOTA_EXCEEDED, HttpStatus.FORBIDDEN);
        ERROR_TO_HTTP_STATUS.put(ServerError.ACTION_NOT_CANCELABLE, HttpStatus.METHOD_NOT_ALLOWED);
        ERROR_TO_HTTP_STATUS.put(ServerError.ACTION_NOT_FORCE_QUITABLE, HttpStatus.METHOD_NOT_ALLOWED);
        ERROR_TO_HTTP_STATUS.put(ServerError.DS_CREATION_FAILED_MISSING_MODULE, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.DS_MODULE_UNSUPPORTED, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.DS_TYPE_UNDEFINED, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.TENANT_NOT_EXISTS, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.ENTITY_LOCKED, HttpStatus.LOCKED);
        ERROR_TO_HTTP_STATUS.put(ServerError.ROLLOUT_ILLEGAL_STATE, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.CONFIGURATION_VALUE_INVALID, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.CONFIGURATION_KEY_INVALID, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.INVALID_TARGET_ADDRESS, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.CONSTRAINT_VIOLATION, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.OPERATION_NOT_SUPPORTED, HttpStatus.GONE);
        ERROR_TO_HTTP_STATUS.put(ServerError.CONCURRENT_MODIFICATION, HttpStatus.CONFLICT);
        ERROR_TO_HTTP_STATUS.put(ServerError.MAINTENANCE_SCHEDULE_INVALID, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.TARGET_ATTRIBUTES_INVALID, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.AUTO_CONFIRMATION_ALREADY_ACTIVE, HttpStatus.CONFLICT);
        ERROR_TO_HTTP_STATUS.put(ServerError.AUTO_ASSIGN_USER_ACCEPTANCE_REQUIRED_INVALID, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.CONFIGURATION_VALUE_CHANGE_NOT_ALLOWED, HttpStatus.FORBIDDEN);
        ERROR_TO_HTTP_STATUS.put(ServerError.MULTIASSIGNMENT_NOT_ENABLED, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.NO_WEIGHT_PROVIDED_IN_MULTIASSIGNMENT_MODE, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.TARGET_TYPE_IN_USE, HttpStatus.CONFLICT);
        ERROR_TO_HTTP_STATUS.put(ServerError.TARGET_TYPE_INCOMPATIBLE, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.DS_INVALID, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.DS_INCOMPLETE, HttpStatus.BAD_REQUEST);
        ERROR_TO_HTTP_STATUS.put(ServerError.STOP_ROLLOUT_FAILED, HttpStatus.LOCKED);
        ERROR_TO_HTTP_STATUS.put(ServerError.FILE_DOWNLOAD_FAILED, HttpStatus.FORBIDDEN);
        ERROR_TO_HTTP_STATUS.put(ServerError.SERVER_BAD_GATEWAY, HttpStatus.BAD_GATEWAY);
    }

    private static HttpStatus getStatusOrDefault(final ServerError error) {
        return ERROR_TO_HTTP_STATUS.getOrDefault(error, DEFAULT_RESPONSE_STATUS);
    }


    /**
     * method for handling exception of type AbstractServerRtException. Called
     * by the Spring-Framework for exception handling.
     *
     * @param request the Http request
     * @param ex      the exception which occurred
     * @return the entity to be responded containing the exception information
     * as entity.
     */
    @ExceptionHandler(AbstractServerRtException.class)
    public ResponseEntity<ExceptionInfo> handleSpServerRtExceptions(final HttpServletRequest request,
                                                                    final Exception ex) {
        logRequest(request, ex);
        final ExceptionInfo response = createExceptionInfo(ex);
        final HttpStatus responseStatus;
        if (ex instanceof final AbstractServerRtException abstractServerRtException) {
            responseStatus = getStatusOrDefault(abstractServerRtException.getError());
        } else {
            responseStatus = DEFAULT_RESPONSE_STATUS;
        }
        return new ResponseEntity<>(response, responseStatus);
    }

    /**
     * method for handling exception of type DataIntegrityViolationException. Called
     * by the Spring-Framework for exception handling.
     *
     * @param request the Http request
     * @param ex      the exception which occurred
     * @return the entity to be responded containing the exception information
     * as entity.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ExceptionInfo> handleDataIntegrityViolationException(final HttpServletRequest request,
                                                                               final Exception ex) {
        logRequest(request, ex);
        final ExceptionInfo response = new ExceptionInfo();
        response.setMessage(ex.getMessage() != null ? ex.getMessage() : ServerError.CONSTRAINT_VIOLATION.name());
        response.setName(ex.getClass().getName());
        response.setDebug(MDC.get(CORRELATION_ID));
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    /**
     * method for handling exception of type OAuth2AuthorizationException. Called
     * by the Spring-Framework for exception handling.
     *
     * @param request the Http request
     * @param ex      the exception which occurred
     * @return the entity to be responded containing the exception information
     * as entity.
     */
    @ExceptionHandler(OAuth2AuthorizationException.class)
    public ResponseEntity<ExceptionInfo> handleOAuth2AuthorizationException(final HttpServletRequest request,
                                                                            final Exception ex) {
        logRequest(request, ex);
        final ExceptionInfo response = new ExceptionInfo();
        response.setMessage(ex.getMessage());
        response.setName(ex.getClass().getSimpleName());
        response.setDebug(MDC.get(CORRELATION_ID));
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    /**
     * Handles exceptions of type {@link SnsException}.
     * This method is called by the Spring-Framework for exception handling.
     *
     * @param request the Http request
     * @param ex      the exception which occurred
     * @return the entity to be responded containing the exception information
     *         as an entity.
     */
    @ExceptionHandler(SnsException.class)
    public ResponseEntity<ExceptionInfo> handleSnsException(final HttpServletRequest request,
                                                            final Exception ex) {
        logRequest(request, ex);
        final ExceptionInfo response = new ExceptionInfo();
        response.setMessage(ex.getMessage());
        response.setName(ex.getClass().getName());
        response.setDebug(MDC.get(CORRELATION_ID));
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(NoSuchAlgorithmException.class)
    public ResponseEntity<Object> handleNoSuchAlgorithmException(final HttpServletRequest request, final Exception ex) {
        logRequest(request, ex);
        LOG.warn("Unable to create hash: {}", ex.getMessage());
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * Method for handling exception of type HttpMessageNotReadableException,
     * MethodArgumentNotValidException and MissingServletRequestParameterException
     * HttpMessageNotReadableException is thrown in case the request body is not
     * well formed (e.g. syntax failures, missing/invalid parameters) and cannot be
     * deserialized. Called by the Spring-Framework for exception handling.
     * MissingServletRequestParameterException is thrown when request param is
     * missing.
     *
     * @param request the Http request
     * @param ex      the exception which occurred
     * @return the entity to be responded containing the exception information as
     * entity.
     */
    @ExceptionHandler({HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ExceptionInfo> handleExceptionCausedByIncorrectRequestBody(final HttpServletRequest request,
                                                                                     final Exception ex) {
        logRequest(request, ex);
        Throwable cause = ex.getCause();

        if (cause instanceof InvalidFormatException) {
            return handleInvalidFormatException((InvalidFormatException) cause, ex);
        }

        if (ex instanceof MethodArgumentTypeMismatchException || ex instanceof MissingServletRequestParameterException) {
            return handleParameterException(ex);
        }

        if (((HttpMessageNotReadableException) ex).getRootCause() instanceof InvalidEpochTimeException) {
            return handleInvalidEpochException(request, ex);
        }

        return handleDefaultException();
    }

    /**
     * Method for handling exception of type
     * which is thrown in case any of the mandatory request fields are missing
     *
     * @param request the Http request
     * @param ex      the exception which occurred
     * @return the entity to be responded containing the exception information as
     * entity.
     */
    @ExceptionHandler({MethodArgumentNotValidException.class})
    public ResponseEntity<ExceptionInfo> handleMethodArgumentNotValidException(final HttpServletRequest request,
                                                                               final Exception ex) {
        logRequest(request, ex);
        final ExceptionInfo response = new ExceptionInfo();
        BindingResult result = ((MethodArgumentNotValidException) ex).getBindingResult();
        List<FieldError> fieldErrors = result.getFieldErrors().stream()
                .map(f -> new FieldError(f.getObjectName(), f.getField(), Objects.requireNonNull(f.getCode())))
                .toList();
        List<String> parameterList = new ArrayList<>();
        fieldErrors.forEach(fieldError ->
                parameterList.add(fieldError.getField() + " cannot be null"));
        response.setName(ex.getClass().getName());
        response.setDebug(MDC.get(CORRELATION_ID));
        response.setMessage("Some Parameters are missing");
        response.setParameters(parameterList);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Method for handling exception of type IllegalArgumentException, JsonProcessingExeption and JsonMappingException
     * which is thrown in case the http method type is not supported.
     *
     * @param request the Http request
     * @param ex      the exception which occurred
     * @return the entity to be responded containing the exception information as
     * entity.
     */

    @ExceptionHandler({IllegalArgumentException.class, NullPointerException.class, JsonProcessingException.class, JsonMappingException.class})
    public ResponseEntity<ExceptionInfo> handleJsonMappingException(
            final HttpServletRequest request, final Exception ex) {
        logRequest(request, ex);

        final ExceptionInfo response = new ExceptionInfo();
        response.setName(ex.getClass().getName());
        response.setDebug(MDC.get(CORRELATION_ID));

        if (ex instanceof JsonMappingException) {
            response.setMessage("Malformed request structure");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (ex instanceof NullPointerException && ex.getMessage() != null &&
                ex.getMessage().contains("org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation.getArtifact()")) {
            response.setMessage("Software Module not found for the given tenant.");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        response.setMessage(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }


    /**
     * Method for handling exception of type HttpRequestMethodNotSupportedException
     * which is thrown in case the http method type is not supported.
     *
     * @param request the Http request
     * @param ex      the exception which occurred
     * @return the entity to be responded containing the exception information as
     * entity.
     */
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<ExceptionInfo> handleExceptionCausedByRequestMethodNotSupported(
            final HttpServletRequest request, final Exception ex) {
        logRequest(request, ex);
        final ExceptionInfo response = new ExceptionInfo();
        response.setName(ex.getClass().getName());
        response.setDebug(MDC.get(CORRELATION_ID));
        response.setMessage(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * Method for handling exception of type MethodArgumentTypeMismatchException
     * which is thrown when the argument type in the URL is not same as defined for
     * the mapping (example int provided for string)
     *
     * @param request the Http request
     * @param ex      the exception which occurred
     * @return the entity to be responded containing the exception information as
     * entity.
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ExceptionInfo> handleExceptionCausedByIncorrectRequestParms(final HttpServletRequest request,
                                                                                      final Exception ex) {
        logRequest(request, ex);
        final ExceptionInfo response = new ExceptionInfo();
        response.setName(ex.getClass().getName());
        response.setDebug(MDC.get(CORRELATION_ID));
        String message = generateErrorMessageBasedOnLocalizedMessage(ex.getLocalizedMessage());
        response.setMessage(message);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Generates a dynamic error message based on the localized exception message.
     *
     * @param localizedMessage The localized exception message that contains details about the type mismatch.
     * @return A custom error message indicating the expected type for the path variable.
     */
    private static String generateErrorMessageBasedOnLocalizedMessage(String localizedMessage) {
        if (localizedMessage != null) {
            int rawStartIndex = localizedMessage.indexOf("to required type '");
            if (rawStartIndex == -1) {
                return "Invalid type for path variable. Please provide the correct value.";
            }
            int startIndex = rawStartIndex + "to required type '".length();
            int endIndex = localizedMessage.indexOf("'", startIndex);
            if (endIndex == -1) {
                return "Invalid type for path variable. Please provide the correct value.";
            }
            String expectedType = localizedMessage.substring(startIndex, endIndex);
            String simpleType = expectedType.contains(".") ? expectedType.substring(expectedType.lastIndexOf('.') + 1) : expectedType;
            return String.format("This API accepts a %s value for the path variable.", simpleType);
        }
        return "Invalid type for path variable. Please provide the correct value.";
    }

    /**
     * Method for handling exception of type ConstraintViolationException which
     * is thrown in case the request is rejected due to a constraint violation.
     * Called by the Spring-Framework for exception handling.
     *
     * @param request the Http request
     * @param ex      the exception which occurred
     * @return the entity to be responded containing the exception information
     * as entity.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ExceptionInfo> handleConstraintViolationException(final HttpServletRequest request,
                                                                            final ConstraintViolationException ex) {
        logRequest(request, ex);
        final ExceptionInfo response = new ExceptionInfo();
        response.setMessage(ex.getConstraintViolations().stream().map(
                        violation -> violation.getPropertyPath() + MESSAGE_FORMATTER_SEPARATOR + violation.getMessage() + ".")
                .collect(Collectors.joining(MESSAGE_FORMATTER_SEPARATOR)));
        response.setName(ServerError.CONSTRAINT_VIOLATION.name());
        response.setDebug(MDC.get(CORRELATION_ID));

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles exceptions of type {@link HandlerMethodValidationException}.
     * This method is called by the Spring Framework for exception handling when
     * method parameter validation fails.
     *
     * @param request the HTTP request that caused the exception
     * @param ex the HandlerMethodValidationException thrown during validation
     * @return ResponseEntity containing the exception information with validation messages
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ExceptionInfo> handleHandlerMethodValidationException(final HttpServletRequest request,
                                                                                final HandlerMethodValidationException ex) {
        logRequest(request, ex);
        final ExceptionInfo response = new ExceptionInfo();

        final String validationMessages = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(MessageSourceResolvable::getDefaultMessage)
                )
                .collect(Collectors.joining(MESSAGE_FORMATTER_SEPARATOR));

        response.setMessage(validationMessages);
        response.setName(ServerError.CONSTRAINT_VIOLATION.name());
        response.setDebug(MDC.get(CORRELATION_ID));

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }


    /**
     * Method for handling exception of type ValidationException which is thrown
     * in case the request is rejected due to invalid requests. Called by the
     * Spring-Framework for exception handling.
     *
     * @param request the Http request
     * @param ex      the exception which occurred
     * @return the entity to be responded containing the exception information
     * as entity.
     */
    @ExceptionHandler({ValidationException.class})
    public ResponseEntity<ExceptionInfo> handleValidationException(final HttpServletRequest request,
                                                                   final ValidationException ex) {
        logRequest(request, ex);
        final ExceptionInfo response = new ExceptionInfo();
        response.setMessage(ex.getMessage());
        response.setName(ex.getClass().getName());
        response.setDebug(MDC.get(CORRELATION_ID));
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Method for handling exception of type {@link MultipartException} which is
     * thrown in case the request body is not well formed and cannot be
     * deserialized. Called by the Spring-Framework for exception handling.
     *
     * @param request the Http request
     * @param ex      the exception which occurred
     * @return the entity to be responded containing the exception information
     * as entity.
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ExceptionInfo> handleMultipartException(final HttpServletRequest request,
                                                                  final Exception ex) {

        logRequest(request, ex);

        final List<Throwable> throwables = ExceptionUtils.getThrowableList(ex);
        final Throwable responseCause = Iterables.getLast(throwables);

        if (responseCause.getMessage().isEmpty()) {
            LOG.warn("Request {} lead to MultipartException without root cause message:\n{}", request.getRequestURL(),
                    ex.getStackTrace());
        }

        final ExceptionInfo response = createExceptionInfo(new MultiPartFileUploadException(responseCause));
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles exceptions of type {@link InvalidEpochTimeException}.
     * This method is called by the Spring-Framework for exception handling.
     *
     * @param request the Http request
     * @param ex      the exception which occurred
     * @return the entity to be responded containing the exception information
     *         as an entity.
     */
    @ExceptionHandler(InvalidEpochTimeException.class)
    public ResponseEntity<ExceptionInfo> handleInvalidEpochException(final HttpServletRequest request,
                                                            final Exception ex) {
        logRequest(request, ex);
        final ExceptionInfo response = new ExceptionInfo();
        response.setMessage(((HttpMessageNotReadableException) ex).getRootCause().getMessage());
        response.setName(((HttpMessageNotReadableException) ex).getRootCause().getClass().getSimpleName());
        response.setDebug(MDC.get(CORRELATION_ID));
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }


    /**
     * Method for handling exception of type {@link EcuCertificateException}
     * @param ex the exception which occurred
     * @return the entity to be responded containing the exception information
     */
    @ExceptionHandler(EcuCertificateException.class)
    public ResponseEntity<Map<String, String>> handleEcuCertificateException(EcuCertificateException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }

    private void logRequest(final HttpServletRequest request, final Exception ex) {
        LOG.info("Handling exception {} of request {}", ex.getClass().getName(), request.getRequestURL());
    }

    private ExceptionInfo createExceptionInfo(final Exception ex) {
        final ExceptionInfo response = new ExceptionInfo();
        response.setMessage(ex.getMessage());
        response.setName(((AbstractServerRtException) ex).getError().name());
        if (ex instanceof AbstractServerRtException) {
            response.setDebug(MDC.get(CORRELATION_ID));
        }

        return response;
    }


    /**
     * Extracts the name of the field that caused the deserialization issue from the InvalidFormatException.
     *
     * @param ex the InvalidFormatException thrown during deserialization
     * @return the name of the field causing the error, or "unknown" if the field name cannot be determined
     *
     * This method navigates the exception's path to find the last field in the JSON hierarchy
     * that was being processed when the error occurred. If the path is empty or unavailable,
     * it defaults to returning "unknown".
     */
    private String extractFieldName(InvalidFormatException ex) {
        return ex.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .reduce((first, second) -> second) // Get the last field in the path
                .orElse("unknown");
    }


    /**
     * Retrieves the valid values for the enum type specified in the deserialization error.
     *
     * @param enumType the target type of the enum class
     * @return a formatted string containing the list of valid enum values, enclosed in square brackets
     *
     * This method ensures that the provided class is an enum type. If it is, it collects all
     * the constant names defined in the enum and returns them as a comma-separated list within brackets.
     * If the class is not an enum, it returns an empty list representation ("[]").
     */
    private String getValidEnumValues(Class<?> enumType) {
        if (!enumType.isEnum()) {
            return "[]";
        }
        Object[] enumConstants = enumType.getEnumConstants();
        return Arrays.stream(enumConstants)
                .map(Object::toString)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    /**
     * Handles InvalidFormatException specifically for enum deserialization errors.
     *
     * @param invalidFormatException the exception caused by invalid input
     * @param ex the original exception
     * @return ResponseEntity containing the error details
     */
    @SuppressWarnings("unchecked")
    private ResponseEntity<ExceptionInfo> handleInvalidFormatException(InvalidFormatException invalidFormatException, Exception ex) {
        return (ResponseEntity<ExceptionInfo>) handleInvalidFormatExceptionWithFieldName(invalidFormatException, ex, extractFieldName(invalidFormatException));
    }

    /**
     * Handles InvalidFormatException specifically for enum deserialization errors with field name.
     *
     * @param invalidFormatException the exception caused by invalid input
     * @param ex the original exception
     * @param fieldName the name of the field causing the error
     * @return ResponseEntity containing the error details
     */
    public ResponseEntity<?> handleInvalidFormatExceptionWithFieldName(InvalidFormatException invalidFormatException, Exception ex, String fieldName) {
        if (invalidFormatException.getTargetType().isEnum()) {
            String invalidValue = invalidFormatException.getValue().toString();
            String validValues = getValidEnumValues(invalidFormatException.getTargetType());

            ExceptionInfo response = createExceptionInfo(
                    ex.getClass().getName(),
                    "Invalid value for field '" + fieldName + "': '" + invalidValue
                            + "'. Accepted values are: " + validValues + "."
            );

            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        return handleDefaultException();
    }

    /**
     * Handles parameter-related exceptions.
     *
     * @param ex the exception caused by invalid or missing parameters
     * @return ResponseEntity containing the error details
     */
    private ResponseEntity<ExceptionInfo> handleParameterException(Exception ex) {
        ExceptionInfo response = createExceptionInfo(
                ex.getClass().getName(),
                ex.getMessage()
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles default exception response.
     *
     * @return ResponseEntity containing the generic error details
     */
    private ResponseEntity<ExceptionInfo> handleDefaultException() {
        ExceptionInfo response = createExceptionInfo(new MessageNotReadableException());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Creates a standardized ExceptionInfo object.
     *
     * @param name the name of the exception
     * @param message the error message to be displayed
     * @return ExceptionInfo instance
     */
    private ExceptionInfo createExceptionInfo(String name, String message) {
        ExceptionInfo response = new ExceptionInfo();
        response.setName(name);
        response.setDebug(MDC.get(CORRELATION_ID));
        response.setMessage(message);
        return response;
    }

    /**
     * Handles DuplicateKeyException thrown by the application.
     * This is typically triggered when trying to insert a record that already exists
     * in a database table with a unique constraint.
     *
     * @param request the HttpServletRequest that caused the exception
     * @param ex      the DuplicateKeyException thrown
     * @return a ResponseEntity containing structured exception info and HTTP 409 status
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ExceptionInfo> handleDuplicateKeyException(
            final HttpServletRequest request, final DuplicateKeyException ex) {

        logRequest(request, ex);
        final ExceptionInfo response = new ExceptionInfo();
        response.setMessage(ex.getMessage() != null ? ex.getMessage() : "Duplicate key violation");
        response.setName(ex.getClass().getName());
        response.setDebug(MDC.get(CORRELATION_ID));
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }
}