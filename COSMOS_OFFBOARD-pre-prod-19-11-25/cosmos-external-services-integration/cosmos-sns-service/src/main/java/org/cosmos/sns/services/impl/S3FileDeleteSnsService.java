package org.cosmos.sns.services.impl;

import org.cosmos.sns.models.S3FileDeletionRequest;
import org.cosmos.sns.services.SnsService;
import org.cosmos.sns.services.SnsServiceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsAsyncClient;

/**
 * Service for publishing messages to the S3 delete SNS topic.
 */
@Service
public class S3FileDeleteSnsService extends SnsService<S3FileDeletionRequest> {

    @Value("${cosmos.server.sns.s3-delete.arn}")
    private String s3FileDeleteSnsArn;

    public S3FileDeleteSnsService(SnsAsyncClient snsAsyncClient) {
        super(snsAsyncClient);
    }

    /**
     * Service for publishing messages to the S3 delete SNS topic.
     *
     * This service extends the {@link SnsService} class and provides the implementation
     * for publishing messages related to S3 file deletions to the configured SNS topic.
     */
    @Override
    protected String getTopicArn(S3FileDeletionRequest message) {
        return s3FileDeleteSnsArn;
    }

    @Override
    public SnsServiceType getSnsServiceType() {
        return SnsServiceType.S3_FILE_DELETE;
    }
}
