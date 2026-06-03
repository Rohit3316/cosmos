package org.cosmos.sns.services.impl;

import org.cosmos.sns.models.CdnUploadRequest;
import org.cosmos.sns.services.SnsService;
import org.cosmos.sns.services.SnsServiceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsAsyncClient;

/**
 * Service for publishing messages to the CDN upload SNS topic.
 */
@Service
public class CdnUploadSnsService extends SnsService<CdnUploadRequest> {

    @Value("${cosmos.server.sns.cdn-upload.arn}")
    private String cdnUploadSns;

    public CdnUploadSnsService(SnsAsyncClient snsAsyncClient) {
        super(snsAsyncClient);
    }

    @Override
    protected String getTopicArn(CdnUploadRequest message) {
        return cdnUploadSns;
    }

    @Override
    public SnsServiceType getSnsServiceType(){
        return SnsServiceType.CDN_UPLOAD;
    }
}