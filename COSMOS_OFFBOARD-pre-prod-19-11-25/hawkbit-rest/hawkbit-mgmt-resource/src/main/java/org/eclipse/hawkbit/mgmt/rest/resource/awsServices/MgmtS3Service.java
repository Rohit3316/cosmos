package org.eclipse.hawkbit.mgmt.rest.resource.awsServices;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.exception.FileDownloadFailureException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class MgmtS3Service implements S3Service {

    private static final String ARTIFACT = "artifact";
    private static final String SUPPORT_PACKAGE = "support-package";
    private final RestTemplate restTemplate;

    public MgmtS3Service(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public URL generatePresignedUrl(String bucketName, String objectKey, Long preSignedUrlExpiryTime) {
        S3Presigner preSigner = S3Presigner.builder()
                .build();

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                .getObjectRequest(getObjectRequest)
                .signatureDuration(Duration.ofSeconds(preSignedUrlExpiryTime))
                .build();

        PresignedGetObjectRequest preSignedRequest = preSigner.presignGetObject(getObjectPresignRequest);

        return preSignedRequest.url();
    }

    @Override
    public String buildS3ObjectName(String tenant, String checksum, String fileName) {

        return String.join("/", List.of(tenant, ARTIFACT, checksum, fileName));

    }

    @Override
    public String buildS3SupportPkgObjectName(String tenant, String checksum, String fileName, String fileType) {
        return String.join("/", List.of(tenant, fileType, checksum, fileName));
    }

    @Override
    public boolean isValidGetUrl(URL url) {

        ResponseEntity<byte[]> response;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Range", "bytes=0-0");

        // Create the request entity with headers
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        try {
            response = restTemplate.exchange(url.toURI(), HttpMethod.GET, requestEntity, byte[].class);
        } catch (HttpServerErrorException | HttpClientErrorException | ResourceAccessException |
                 URISyntaxException ex) {
            log.error("Failed to download file with cause: {}", ex.getMessage());
            throw new FileDownloadFailureException("File is unavailable for download");
        }
        return response.getStatusCode().equals(HttpStatus.PARTIAL_CONTENT);
    }

}
