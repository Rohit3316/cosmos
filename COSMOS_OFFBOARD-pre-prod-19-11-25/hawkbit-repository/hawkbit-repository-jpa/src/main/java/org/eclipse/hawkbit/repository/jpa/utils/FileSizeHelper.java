package org.eclipse.hawkbit.repository.jpa.utils;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.validation.ValidationException;

import static org.cosmos.models.mgmt.MgmtRestConstants.EMPTY_FILE;
import static org.cosmos.models.mgmt.MgmtRestConstants.MAX_SIZE_ARTIFACTS;

/**
 * Utility class for checking file sizes and retrieving file size from headers.
 * This class provides methods to validate file sizes and retrieve file sizes
 * from HTTP headers using a HEAD request.
 */
@Component
public final class FileSizeHelper {

    private FileSizeHelper() {
        // Utility class, no instantiation allowed
    }

    /**
     * Checks if the given file size is acceptable.
     *
     * @param fileSize the file size of the file
     */
    public static void isFileSizeAcceptable(Long fileSize) {
        if (!(fileSize > EMPTY_FILE && fileSize < MAX_SIZE_ARTIFACTS)) {

            throw new ValidationException("Total file size in bytes which cannot exceed " + MAX_SIZE_ARTIFACTS);
        }
    }

    /**
     * Checks if the file size of the file at the given URL is acceptable
     * and return the file size
     *
     * @param url the URL of the file
     */
    public static Long isFileSizeAcceptable(String url) {

        Long fileSize = getFileSizeFromHeader(url);
        isFileSizeAcceptable(fileSize);
        return fileSize;
    }

    /**
     * Retrieves the file size from a HEAD request to the given URL.
     *
     * @param url the URL of the file
     * @return the file size
     */
    public static Long getFileSizeFromHeader(String url) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    url, HttpMethod.HEAD, null, Void.class);

            return response.getHeaders().getContentLength();
        } catch (Exception e) {
            throw new ValidationException("Error while checking the file size: " + e.getMessage());
        }
    }






}
