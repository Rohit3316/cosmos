package org.cosmos.sns.services.impl;

import org.cosmos.sns.models.S3FileTransferRequest;
import org.cosmos.sns.services.SnsService;
import org.cosmos.sns.services.SnsServiceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsAsyncClient;

/**
 * Service for publishing messages to the S3 file transfer SNS topic.
 */
@Service
public class S3FileTransferSnsService extends SnsService<S3FileTransferRequest> {

    @Value("${cosmos.server.sns.url-to-s3-transfer.arn}")
    private String urlToS3TransferSns;

    public S3FileTransferSnsService(SnsAsyncClient snsAsyncClient) {
        super(snsAsyncClient);
    }

    @Override
    protected String getTopicArn(S3FileTransferRequest message) {
        return urlToS3TransferSns;
    }

    @Override
    public SnsServiceType getSnsServiceType(){
        return SnsServiceType.S3_FILE_TRANSFER;
    }
}