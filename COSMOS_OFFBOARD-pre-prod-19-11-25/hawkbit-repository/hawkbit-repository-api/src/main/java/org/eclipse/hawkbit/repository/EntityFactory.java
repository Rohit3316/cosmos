/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.eclipse.hawkbit.repository.builder.ActionStatusBuilder;
import org.eclipse.hawkbit.repository.builder.ArtifactsBuilder;
import org.eclipse.hawkbit.repository.builder.DistributionSetBuilder;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeBuilder;
import org.eclipse.hawkbit.repository.builder.PollingFeedbackBuilder;
import org.eclipse.hawkbit.repository.builder.RolloutBuilder;
import org.eclipse.hawkbit.repository.builder.RolloutGroupBuilder;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleBuilder;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleFormatBuilder;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleMetadataBuilder;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeBuilder;
import org.eclipse.hawkbit.repository.builder.TagBuilder;
import org.eclipse.hawkbit.repository.builder.TargetBuilder;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryBuilder;
import org.eclipse.hawkbit.repository.builder.TargetTypeBuilder;
import org.eclipse.hawkbit.repository.builder.UserBuilder;
import org.eclipse.hawkbit.repository.builder.UserElementBuilder;
import org.eclipse.hawkbit.repository.builder.VehicleBuilder;
import org.eclipse.hawkbit.repository.builder.VersionBuilder;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.SoftwareOfTarget;

/**
 * central {@link BaseEntity} generation service. Objects are created but not
 * persisted.
 */
public interface EntityFactory {

    /**
     * @return {@link ActionStatusBuilder} object
     */
    ActionStatusBuilder actionStatus();

    /**
     * @return {@link DistributionSetBuilder} object
     */
    DistributionSetBuilder distributionSet();

    /**
     * Generates an {@link MetaData} element for distribution set without
     * persisting it.
     *
     * @param key   {@link MetaData#getKey()}
     * @param value {@link MetaData#getValue()}
     * @return {@link MetaData} object
     */
    MetaData generateDsMetadata(@Size(min = 1, max = MetaData.KEY_MAX_SIZE) @NotNull String key,
                                @Size(max = MetaData.VALUE_MAX_SIZE) String value);

    /**
     * Generates an {@link MetaData} element for target without persisting it.
     *
     * @param key   {@link MetaData#getKey()}
     * @param value {@link MetaData#getValue()}
     * @return {@link MetaData} object
     */
    MetaData generateTargetMetadata(@Size(min = 1, max = MetaData.KEY_MAX_SIZE) @NotNull String key,
                                    @Size(max = MetaData.VALUE_MAX_SIZE) String value);


    SoftwareOfTarget generateTargetSoftware(@Size(min = 1, max = SoftwareOfTarget.KEY_MAX_SIZE) @NotNull String componentId,
                                            @Size(max = SoftwareOfTarget.VALUE_MAX_SIZE) String node, @Size(max = SoftwareOfTarget.VALUE_MAX_SIZE) String version);


    /**
     * @return {@link SoftwareModuleMetadataBuilder} object
     */
    SoftwareModuleMetadataBuilder softwareModuleMetadata();

    /**
     * @return {@link TagBuilder} object
     */
    TagBuilder tag();

    /**
     * @return {@link RolloutGroupBuilder} object
     */
    RolloutGroupBuilder rolloutGroup();

    /**
     * @return {@link DistributionSetTypeBuilder} object
     */
    DistributionSetTypeBuilder distributionSetType();

    /**
     * @return {@link RolloutBuilder} object
     */
    RolloutBuilder rollout();

    /**
     * @return {@link SoftwareModuleBuilder} object
     */
    SoftwareModuleBuilder softwareModule();

    /**
     * @return {@link SoftwareModuleTypeBuilder} object
     */
    SoftwareModuleTypeBuilder softwareModuleType();

    /**
     * @return {@link SoftwareModuleTypeBuilder} object
     */
    SoftwareModuleFormatBuilder softwareModuleFormat();

    /**
     * @return {@link TargetBuilder} object
     */
    TargetBuilder target();

    /**
     * @return {@link TargetTypeBuilder} object
     */
    TargetTypeBuilder targetType();

    /**
     * @return {@link TargetFilterQueryBuilder} object
     */
    TargetFilterQueryBuilder targetFilterQuery();

    UserBuilder user();

    UserElementBuilder userElement();

    VersionBuilder version();

    VehicleBuilder vehicle();

    ArtifactsBuilder artifacts();


}
