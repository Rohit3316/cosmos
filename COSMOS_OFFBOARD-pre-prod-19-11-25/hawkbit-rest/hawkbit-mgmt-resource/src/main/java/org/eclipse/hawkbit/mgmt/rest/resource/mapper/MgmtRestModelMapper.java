/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource.mapper;

import org.cosmos.models.mgmt.MgmtBaseEntity;
import org.cosmos.models.mgmt.MgmtNamedEntity;
import org.cosmos.models.mgmt.distributionset.constants.MgmtCancelationType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutConnectivityType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutDowngradeAllowed;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredMedia;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation.CancelationType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 */
public final class MgmtRestModelMapper {

    // private constructor, utility class
    private MgmtRestModelMapper() {

    }

    public static void mapBaseToBase(final MgmtBaseEntity response, final TenantAwareBaseEntity base) {
        response.setCreatedBy(base.getCreatedBy());
        response.setLastModifiedBy(base.getLastModifiedBy());
        if (base.getCreatedAt() > 0) {
            response.setCreatedAt(base.getCreatedAt());
        }
        if (base.getLastModifiedAt() > 0) {
            response.setLastModifiedAt(base.getLastModifiedAt());
        }
    }

    public static void mapNamedToNamed(final MgmtNamedEntity response, final NamedEntity base) {
        mapBaseToBase(response, base);

        response.setName(base.getName());
        response.setDescription(base.getDescription());
    }

    /**
     * Convert the given Source {@link MgmtRolloutConnectivityType} into a corresponding destination {@link MgmtRolloutConnectivityType}.
     * @param source ConnectivityType / MgmtConnectivityType
     * @param destination ConnectivityType / MgmtConnectivityType
     * @return <D> destination type
     * @param <S> source type
     * @param <D> destination type
     */
    public static <S extends Enum<S>, D extends Enum<D>> D mapConnectivityType(final S source, Class<D> destination) {
        return source != null ? Enum.valueOf(destination, source.name()) : null;
    }

    /**
     * Convert the given Source {@link MgmtRolloutRequiredMedia} into a corresponding destination {@link MgmtRolloutRequiredMedia}.
     * @param source RequiredMedia / MgmtRequiredMedia
     * @param destination RequiredMedia / MgmtRequiredMedia
     * @return <D> destination type
     * @param <S> source type
     * @param <D> destination type
     */
    public static <S extends Enum<S>, D extends Enum<D>> D mapRequiredMedia(final S source, Class<D> destination) {
        return source != null ? Enum.valueOf(destination, source.name()) : null;
    }

    /**
     * Convert the given Source {@link MgmtRolloutDowngradeAllowed} into a corresponding destination {@link MgmtRolloutDowngradeAllowed}.
     * @param source DowngradeAllowed / MgmtDowngradeAllowed
     * @param destination DowngradeAllowed / MgmtDowngradeAllowed
     * @return <D> destination type
     * @param <S> source type
     * @param <D> destination type
     */
    public static <S extends Enum<S>, D extends Enum<D>> D mapDowngradeAllowed(final S source, Class<D> destination) {
        return source != null ? Enum.valueOf(destination, source.name()) : null;
    }

    /**
     * Converts the given repository {@link CancelationType} into a
     * corresponding {@link MgmtCancelationType}.
     *
     * @param cancelationType
     *            the repository representation of the cancellation type
     *
     * @return <null> or the REST cancellation type
     */
    public static CancelationType convertCancelationType(final MgmtCancelationType cancelationType) {
        if (cancelationType == null) {
            return null;
        }

        switch (cancelationType) {
            case SOFT:
                return CancelationType.SOFT;
            case FORCE:
                return CancelationType.FORCE;
            case NONE:
                return CancelationType.NONE;
            default:
                throw new IllegalStateException("Action Cancelation Type is not supported");
        }
    }
}
