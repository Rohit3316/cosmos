package org.eclipse.hawkbit.feignclient.configuration;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.feignclient.exception.BadGatewayException;
import org.eclipse.hawkbit.feignclient.exception.BadRequestException;
import org.eclipse.hawkbit.feignclient.exception.ConflictException;
import org.eclipse.hawkbit.feignclient.exception.ForbiddenException;
import org.eclipse.hawkbit.feignclient.exception.GatewayTimeoutException;
import org.eclipse.hawkbit.feignclient.exception.InternalServerErrorException;
import org.eclipse.hawkbit.feignclient.exception.MethodNotAllowedException;
import org.eclipse.hawkbit.feignclient.exception.NotFoundException;
import org.eclipse.hawkbit.feignclient.exception.NotImplementedException;
import org.eclipse.hawkbit.feignclient.exception.ServiceUnavailableException;
import org.eclipse.hawkbit.feignclient.exception.TooManyRequestsException;
import org.eclipse.hawkbit.feignclient.exception.UnAuthorizedException;
import org.eclipse.hawkbit.feignclient.exception.UnsupportedMediaTypeException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * {@link GeneralClientErrorDecoder} is a custom {@link ErrorDecoder} for Feign clients.
 * <p>
 * It maps specific HTTP status codes to meaningful custom exceptions to improve error handling
 * and traceability during remote service invocation via Feign.
 */
@Component
@Slf4j
public class GeneralClientErrorDecoder implements ErrorDecoder {
    private final ErrorDecoder defaultErrorDecoder = new Default();

    /**
     * Decodes the HTTP response and maps it to a specific exception based on the status code.
     *
     * @param methodKey the Feign client method key.
     * @param response  the HTTP response.
     * @return the appropriate exception based on the HTTP status code.
     */
    @Override
    public Exception decode(String methodKey, Response response) {
        HttpStatus status = HttpStatus.valueOf(response.status());

        String url = response.request().url();
        log.error("Feign client error - method: {}, status: {}, URL: {}", methodKey, status, url);

        return switch (status) {
            case BAD_REQUEST -> // 400
                    new BadRequestException("Bad request: " + response.request().url());
            case UNAUTHORIZED -> // 401
                    new UnAuthorizedException("Unauthorized access to: " + response.request().url());
            case FORBIDDEN -> // 403
                    new ForbiddenException("Forbidden access to: " + response.request().url());
            case NOT_FOUND -> // 404
                    new NotFoundException("Resource not found at: " + response.request().url());
            case METHOD_NOT_ALLOWED -> // 405
                    new MethodNotAllowedException("Method not allowed at: " + response.request().url());
            case CONFLICT -> // 409
                    new ConflictException("Conflict occurred at: " + response.request().url());
            case UNSUPPORTED_MEDIA_TYPE -> // 415
                    new UnsupportedMediaTypeException("Unsupported media type at: " + response.request().url());
            case TOO_MANY_REQUESTS -> // 429
                    new TooManyRequestsException("Too many requests sent to: " + response.request().url());
            case INTERNAL_SERVER_ERROR -> // 500
                    new InternalServerErrorException("Server error occurred at: " + response.request().url());
            case NOT_IMPLEMENTED -> // 501
                    new NotImplementedException("Feature not implemented at: " + response.request().url());
            case BAD_GATEWAY -> // 502
                    new BadGatewayException("Bad gateway at: " + response.request().url());
            case SERVICE_UNAVAILABLE -> // 503
                    new ServiceUnavailableException("Service unavailable at: " + response.request().url());
            case GATEWAY_TIMEOUT -> // 504
                    new GatewayTimeoutException("Gateway timeout at: " + response.request().url());
            default -> defaultErrorDecoder.decode(methodKey, response);
        };
    }

}
