package org.cosmos.sns.services.impl;

import org.cosmos.sns.models.CdnDeleteRequest;
import org.cosmos.sns.services.SnsService;
import org.cosmos.sns.services.SnsServiceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsAsyncClient;

/**
 * Service for publishing messages to the CDN delete SNS topic.
 */
@Service
public class CdnDeleteSnsService extends SnsService<CdnDeleteRequest> {

    @Value("${cosmos.server.sns.cdn-delete.arn}")
    private String cdnDeleteSns;

    public CdnDeleteSnsService(SnsAsyncClient snsAsyncClient) {
        super(snsAsyncClient);
    }

    @Override
    protected String getTopicArn(CdnDeleteRequest message) {
        return cdnDeleteSns;
    }

    @Override
    public SnsServiceType getSnsServiceType(){
        return SnsServiceType.CDN_DELETE;
    }

}