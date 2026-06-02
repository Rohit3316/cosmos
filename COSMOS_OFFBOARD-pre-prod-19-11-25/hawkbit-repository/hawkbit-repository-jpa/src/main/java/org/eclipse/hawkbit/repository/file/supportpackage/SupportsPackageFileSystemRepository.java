package org.eclipse.hawkbit.repository.file.supportpackage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.hawkbit.artifact.repository.ArtifactFilesystemProperties;
import org.eclipse.hawkbit.repository.exception.FileDownloadFailureException;
import org.eclipse.hawkbit.repository.file.supportpackage.configuration.SupportPackageFileSystemProperties;
import org.eclipse.hawkbit.repository.file.utils.FileHelper;
import org.eclipse.hawkbit.repository.jpa.utils.FileSizeAndStorageQuotaCheckingInputStream;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import jakarta.validation.ValidationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * This class implements the {@link SupportPackageFileSystem} interface and provides functionality for managing
 * support package files in a file system.
 */
public class SupportsPackageFileSystemRepository implements SupportPackageFileSystem {
    private final SupportPackageFileSystemProperties properties;
    private final RestTemplate restTemplate;
    public static final String SP_TYPE = "{spType}";
    public static final String SHA_256 = "{sha256}";

    @Getter
    @Setter
    public static class S3bucket {
        /**
         * The S3 bucket name.
         */
        private String name = "cosmos-us-east-1-dev-artifacts";
        /**
         * The expiry time of presigned url in minutes.
         */
        private String presignedUrlExpiry = "10m";

        public Long getPreSignedUrlExpiryTime() {
            /**
             * pattern for presignedUrlExpiry
             * eg presignedUrlExpiry: 60s, 10m, 4h, 5d for 60seconds, 10 mins, 4hours and 5days
             */
            Pattern pattern = Pattern.compile("(\\d+)([smhd])");
            Matcher matcher = pattern.matcher(getPresignedUrlExpiry());

            if (matcher.matches()) {
                int value = Integer.parseInt(matcher.group(1));
                String unit = matcher.group(2);

                return switch (unit) {
                    case "s" -> (long) value;
                    case "m" -> value * 60L;
                    case "h" -> value * 3600L;
                    case "d" -> value * 86400L;
                    default -> throw new ValidationException("Invalid time unit in preSignedUrlExpiry");
                };
            } else {
                throw new ValidationException("Invalid format for preSignedUrlExpiry");
            }
        }
    }
    /**
     * Constructor for the SupportsPackageFileSystemRepository class.
     *
     * @param properties The properties for configuring the file system repository.
     * @param restTemplate The RestTemplate for making HTTP requests.
     */
    public SupportsPackageFileSystemRepository(SupportPackageFileSystemProperties properties,
                                               @Qualifier("fileDownloadRestTemplate") RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    /**
     * Private method to generate the base path for a support package file based on its attributes.
     *
     * @param attributes The attributes of the support package file.
     * @return The base path for the support package file.
     */
    private String getBasePath(SupportPackageFilePathAttributes attributes) {
        String path = properties.getPath();
        for (Map.Entry<String, String> entry : getReplacementMap(attributes).entrySet()) {
            path = path.replace(entry.getKey(), entry.getValue());
        }
        return path;
    }

    /**
     * Method to download a file from a given URL.
     *
     * @param fileUrl The URL of the file to download.
     * @return An InputStream for reading the downloaded file.
     * @throws IOException If an error occurs while downloading the file.
     */
    public InputStream downloadFile(String fileUrl) throws IOException, URISyntaxException {
        URI encodedFileUrl = new URI(fileUrl);
        ResponseEntity<Resource> response;
        try{
            response = restTemplate.exchange(
                    encodedFileUrl,
                    HttpMethod.GET,
                    null,
                    Resource.class
            );
        }catch (HttpClientErrorException | HttpServerErrorException ex){
            throw  new FileDownloadFailureException("File download Failed with error code"+ ex.getStatusCode()+ "and message"+ex.getResponseBodyAsString());
        }catch (ResourceAccessException e){
            throw new FileDownloadFailureException("Unable to connect to the File url");
        }


        if (response.getStatusCode().is2xxSuccessful() && response.getBody() == null) {
            throw new IOException("Failed to download the file from " + fileUrl);
        } else {
            return Objects.requireNonNull(response.getBody()).getInputStream();
        }
    }

    /**
     * Method to download a file from a given URL, calculate its SHA-256 hash, and store it in the file system.
     *
     * @param attributes The attributes of the support package file.
     * @param fileUrl The URL of the file to download.
     * @param fileName The name of the file to store in the file system.
     * @return The SHA-256 hash of the downloaded file.
     * @throws IOException If an error occurs while downloading or storing the file.
     * @throws NoSuchAlgorithmException If the SHA-256 algorithm is not available.
     */
    @Override
    @Async
    public String downloadFileWithHash(SupportPackageFilePathAttributes attributes, String fileUrl, String fileName) throws IOException, NoSuchAlgorithmException {
        String efsFileLocation = getBasePath(attributes);
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

        try (
                InputStream inputStream = downloadFile(fileUrl);
                InputStream quotaInputStream = wrapInQuotaStream(inputStream);
                InputStream digestInputStream = wrapInDigestInputStream(quotaInputStream, messageDigest)) {
            FileHelper.createFileFromInputStream(digestInputStream, efsFileLocation, fileName);
        } catch (IOException e) {
            throw new IOException("Unable to read the file");
        }catch (URISyntaxException e){
            throw new ValidationException("Invalid File Url");
        }

        return getSha256(messageDigest.digest());
    }

    /**
     * Private method to generate a map of replacements for placeholders in the file path.
     *
     * @param attributes The attributes of the support package file.
     * @return A map of placeholders and their replacements.
     */
    private static Map<String, String> getReplacementMap(SupportPackageFilePathAttributes attributes) {
        Map<String, String> replacementMap = new HashMap<>();
        replacementMap.put(SP_TYPE, attributes.spType());
        replacementMap.put(SHA_256, attributes.sha256());
        return replacementMap;
    }

    /**
     * Private method to wrap an input stream with a FileSizeAndStorageQuotaCheckingInputStream.
     *
     * @param inputStream The input stream to wrap.
     * @return The wrapped input stream.
     */
    private InputStream wrapInQuotaStream(final InputStream inputStream) {
        final long maxSupportPackageSize = properties.getSizeInBytes();
        final long currentlyUsedFileStorageSize = 0L; //TODO: should get the storage size left in efs or s3
        return new FileSizeAndStorageQuotaCheckingInputStream(inputStream, maxSupportPackageSize,
                maxSupportPackageSize - currentlyUsedFileStorageSize);
    }

    /**
     * Private method to wrap an input stream with a DigestInputStream.
     *
     * @param inputStream The input stream to wrap.
     * @param digest The MessageDigest to use for calculating the hash.
     * @return The wrapped input stream.
     */
    private InputStream wrapInDigestInputStream(final InputStream inputStream, MessageDigest digest) {
        return new DigestInputStream(inputStream, digest);
    }

    /**
     * Private method to convert a byte array to a hexadecimal string.
     *
     * @param bytes The byte array to convert.
     * @return The hexadecimal string representation of the byte array.
     */
    private String getSha256(byte[] bytes){
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }
}
