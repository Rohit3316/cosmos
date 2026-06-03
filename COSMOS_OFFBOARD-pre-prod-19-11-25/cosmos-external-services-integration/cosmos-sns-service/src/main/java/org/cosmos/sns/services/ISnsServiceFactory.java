package org.cosmos.sns.services;

import org.cosmos.models.sns.SnsPublishable;

public interface ISnsServiceFactory {

    /**
     * Creates an SNS service based on the given type.
     *
     * @param type The type of the SNS service to create.
     * @return An SNS service instance.
     */
    <T extends SnsPublishable> ISnsService<T> getInstance(SnsServiceType type);
}
