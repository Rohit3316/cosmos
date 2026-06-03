package org.cosmos.s3;

import org.cosmos.s3.exception.S3Exception;
import org.cosmos.s3.model.S3FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.Assert;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Paths;

public class S3Repository {
    private static final Logger LOG = LoggerFactory.getLogger(S3Repository.class);
    private final S3Client s3Client;

    public S3Repository() {
        this.s3Client = S3Client.create();
    }

    public S3Repository(S3Client s3Client) {
        Assert.isTrue(s3Client != null, S3Constants.S3_CLIENT_ERROR_MSG);
        this.s3Client = s3Client;
    }

    @Async
    public void uploadFileToS3(S3FileUpload fileUpload) {
        Assert.isTrue(fileUpload != null, S3Constants.FILE_UPLOAD_ERROR_MSG);
        Assert.hasText(fileUpload.getBucketName(), S3Constants.BUCKET_NAME_ERROR_MSG);
        Assert.hasText(fileUpload.getKeyPath(), S3Constants.KEY_PATH_ERROR_MSG);
        Assert.hasText(fileUpload.getFilename(), S3Constants.FILE_NAME_ERROR_MSG);
        Assert.hasText(fileUpload.getFilePath(), S3Constants.FILE_PATH_ERROR_MSG);
        try {
            LOG.debug("S3 putObject {}", fileUpload);
            s3Client.putObject(PutObjectRequest.builder().bucket(fileUpload.getBucketName())
                    .key(fileUpload.getKeyPath() + fileUpload.getFilename())
                    .build(), Paths.get(fileUpload.getFilePath()));
        } catch (Exception e) {
            throw new S3Exception("Failed to upload file into S3", e);
        }
    }

    @Async
    public void downloadFileFromS3(S3FileUpload fileUpload) {
        Assert.isTrue(fileUpload != null, S3Constants.FILE_UPLOAD_ERROR_MSG);
        Assert.hasText(fileUpload.getBucketName(), S3Constants.BUCKET_NAME_ERROR_MSG);
        Assert.hasText(fileUpload.getKeyPath(), S3Constants.KEY_PATH_ERROR_MSG);
        Assert.hasText(fileUpload.getFilename(), S3Constants.FILE_NAME_ERROR_MSG);
        try {
            LOG.debug("S3 getObject {}", fileUpload);
            if (!(fileUpload.getKeyPath() + fileUpload.getFilename()).isEmpty()) {
                LOG.debug("S3 getObject file {}", fileUpload.getKeyPath() + fileUpload.getFilename());
                s3Client.getObject(GetObjectRequest.builder()
                        .bucket(fileUpload.getBucketName())
                        .key(fileUpload.getKeyPath() + fileUpload.getFilename()).build());
            }
        } catch (Exception e) {
            throw new S3Exception("Failed to download file from S3", e);
        }
    }

    @Async
    public void deleteFileFromS3(S3FileUpload fileUpload) {
        Assert.notNull(fileUpload, S3Constants.FILE_UPLOAD_ERROR_MSG);
        Assert.hasText(fileUpload.getBucketName(), S3Constants.BUCKET_NAME_ERROR_MSG);
        Assert.hasText(fileUpload.getKeyPath(), S3Constants.KEY_PATH_ERROR_MSG);
        Assert.hasText(fileUpload.getFilename(), S3Constants.FILE_NAME_ERROR_MSG);

        String fileKey = fileUpload.getKeyPath() + fileUpload.getFilename();

        LOG.debug("Attempting to delete S3 object: Bucket={}, Key={}", fileUpload.getBucketName(), fileKey);
        try {
            DeleteObjectResponse deleteResponse = s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(fileUpload.getBucketName())
                    .key(fileKey)
                    .build());

            if (deleteResponse.sdkHttpResponse().isSuccessful()) {
                LOG.info("Successfully deleted file from S3: Bucket={}, Key={}", fileUpload.getBucketName(), fileKey);
            } else {
                LOG.error("Failed to delete file from S3. Bucket={}, Key={}, HTTP Status={}", fileUpload.getBucketName(), fileKey, deleteResponse.sdkHttpResponse().statusCode());
                throw new S3Exception("Failed to delete file from S3");
            }
        } catch (AwsServiceException | SdkClientException | S3Exception e) {
            LOG.error("Error occurred while deleting file from S3: {}", e.getMessage(), e);
            throw new S3Exception("Error occurred while deleting file from S3");
        }
    }

    /**
     * Retrieves the size of a file in bytes from S3 storage.
     *
     * @param fileUpload the descriptor containing S3 bucket name, key path, and filename
     * @return the file size in bytes if the file exists
     * @throws S3Exception if the file does not exist or an error occurs during the request
     */
    public Long getFileSizeFromS3(S3FileUpload fileUpload) {
        Assert.notNull(fileUpload, S3Constants.FILE_UPLOAD_ERROR_MSG);
        Assert.hasText(fileUpload.getBucketName(), S3Constants.BUCKET_NAME_ERROR_MSG);
        Assert.hasText(fileUpload.getKeyPath(), S3Constants.KEY_PATH_ERROR_MSG);
        Assert.hasText(fileUpload.getFilename(), S3Constants.FILE_NAME_ERROR_MSG);

        String fileKey = fileUpload.getKeyPath() + fileUpload.getFilename();

        LOG.debug("Attempting to perform head request on S3 object: Bucket={}, Key={}", fileUpload.getBucketName(), fileKey);
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(fileUpload.getBucketName())
                    .key(fileKey)
                    .build();
            HeadObjectResponse response = s3Client.headObject(request);

            if (response.sdkHttpResponse().isSuccessful()) {
                LOG.info("Successfully performed head request on S3: Bucket={}, Key={}", fileUpload.getBucketName(), fileKey);
                // Get the content length (size in bytes)
                return response.contentLength();
            } else {
                LOG.error("Failed to perform head request on S3. Bucket={}, Key={}, HTTP Status={}",
                        fileUpload.getBucketName(), fileKey, response.sdkHttpResponse().statusCode());
                throw new S3Exception("Failed to perform head request on S3");
            }
        } catch (AwsServiceException | SdkClientException | S3Exception e) {
            LOG.error("Error occurred while performing head request on S3: {}", e.getMessage(), e);
            throw new S3Exception("Error occurred while performing head request on S3", e);
        }
    }

}
