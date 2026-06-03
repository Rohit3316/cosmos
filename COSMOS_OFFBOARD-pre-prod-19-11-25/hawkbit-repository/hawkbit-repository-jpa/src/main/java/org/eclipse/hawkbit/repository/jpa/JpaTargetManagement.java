/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import com.google.common.collect.Lists;
import org.cosmos.models.ddi.InventoryWithAction;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.FilterParams;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetMetadataFields;
import org.eclipse.hawkbit.repository.TimestampCalculator;
import org.eclipse.hawkbit.repository.VehicleManagement;
import org.eclipse.hawkbit.repository.builder.TargetCreate;
import org.eclipse.hawkbit.repository.builder.TargetUpdate;
import org.eclipse.hawkbit.repository.event.remote.TargetAttributesRequestedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTargetCreate;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTargetUpdate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaTargetSoftware_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetMetadata_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetSoftware;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.jpa.model.TargetMetadataCompositeKey;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.SpecificationsBuilder;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetInventory;
import org.eclipse.hawkbit.repository.model.TargetMetadata;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetTagAssignmentResult;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetTypeAssignmentResult;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.Vehicle;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Root;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA implementation of {@link TargetManagement}.
 */
@Transactional(readOnly = true)
@Validated
public class JpaTargetManagement implements TargetManagement {

    public static final String REGISTERED_KEY_IN_CONTROLLER_ID_MAP = "registered";
    public static final String UNREGISTERED_KEY_IN_CONTROLLER_ID_MAP = "unregistered";
    public static final String DUPLICATE_KEY_IN_CONTROLLER_ID_MAP = "duplicate";

    private final EntityManager entityManager;

    private final DistributionSetManagement distributionSetManagement;

    private final QuotaManagement quotaManagement;

    private final TargetRepository targetRepository;

    private final TargetTypeRepository targetTypeRepository;

    private final TargetMetadataRepository targetMetadataRepository;

    private final RolloutGroupRepository rolloutGroupRepository;

    private final TargetFilterQueryRepository targetFilterQueryRepository;

    private final TargetTagRepository targetTagRepository;

    private final EventPublisherHolder eventPublisherHolder;

    private final TenantAware tenantAware;

    private final AfterTransactionCommitExecutor afterCommit;

    private final VirtualPropertyReplacer virtualPropertyReplacer;

    private final Database database;

    private final VehicleManagement vehicleManagement;

    private final RolloutManagement rolloutManagement;

    private final TargetInventoryRepository targetInventoryRepository;

    private final ActionRepository actionRepository;

    public JpaTargetManagement(final EntityManager entityManager,
                               final DistributionSetManagement distributionSetManagement, final QuotaManagement quotaManagement,
                               final TargetRepository targetRepository, final TargetTypeRepository targetTypeRepository,
                               final TargetMetadataRepository targetMetadataRepository,
                               final RolloutGroupRepository rolloutGroupRepository,
                               final TargetFilterQueryRepository targetFilterQueryRepository,
                               final TargetTagRepository targetTagRepository, final EventPublisherHolder eventPublisherHolder,
                               final TenantAware tenantAware, final AfterTransactionCommitExecutor afterCommit,
                               final VirtualPropertyReplacer virtualPropertyReplacer, final Database database,
                               final VehicleManagement vehicleManagement,
                               final RolloutManagement rolloutManagement,
                               final ActionRepository actionRepository,
                               final TargetInventoryRepository targetInventoryRepository) {
        this.entityManager = entityManager;
        this.distributionSetManagement = distributionSetManagement;
        this.quotaManagement = quotaManagement;
        this.targetRepository = targetRepository;
        this.targetTypeRepository = targetTypeRepository;
        this.targetMetadataRepository = targetMetadataRepository;
        this.rolloutGroupRepository = rolloutGroupRepository;
        this.targetFilterQueryRepository = targetFilterQueryRepository;
        this.targetTagRepository = targetTagRepository;
        this.eventPublisherHolder = eventPublisherHolder;
        this.tenantAware = tenantAware;
        this.afterCommit = afterCommit;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.database = database;
        this.vehicleManagement = vehicleManagement;
        this.rolloutManagement = rolloutManagement;
        this.actionRepository = actionRepository;
        this.targetInventoryRepository = targetInventoryRepository;
    }

    private static boolean hasTagsFilterActive(final FilterParams filterParams) {
        final boolean isNoTagActive = Boolean.TRUE.equals(filterParams.getSelectTargetWithNoTag());
        final boolean isAtLeastOneTagActive = filterParams.getFilterByTagNames() != null
                && filterParams.getFilterByTagNames().length > 0;

        return isNoTagActive || isAtLeastOneTagActive;
    }

    private static boolean hasTypesFilterActive(final FilterParams filterParams) {
        return filterParams.getFilterByTargetType() != null;
    }

    private static boolean hasNoTypeFilterActive(final FilterParams filterParams) {
        return Boolean.TRUE.equals(filterParams.getSelectTargetWithNoTargetType());
    }

    @Override
    public Optional<Target> getByControllerID(final String controllerId) {
        return targetRepository.findOne(TargetSpecifications.hasControllerId(controllerId)).map(Target.class::cast);
    }

    @Override
    public Optional<Target> getByVin(final String vin) {
        return targetRepository.findOne(TargetSpecifications.hasVin(vin)).map(Target.class::cast);
    }

    @Override
    public Map<String, List<String>> separateRegisteredAndUnregisteredControllerIds(final Collection<String> controllerIds) {

        final List<String> registeredControllerIds = targetRepository
                .findAll(TargetSpecifications.hasControllerIdIn(controllerIds)).stream()
                .map(Target::getControllerId).toList();

        final List<String> unregisteredControllerIds = controllerIds.stream()
                .filter(id -> !registeredControllerIds.contains(id)).toList();

        Map<String, List<String>> result = new HashMap<>();
        result.put(REGISTERED_KEY_IN_CONTROLLER_ID_MAP, registeredControllerIds);
        result.put(UNREGISTERED_KEY_IN_CONTROLLER_ID_MAP, unregisteredControllerIds);

        return result;
    }

    private JpaTarget getByControllerIdAndThrowIfNotFound(final String controllerId) {
        return targetRepository.findOne(TargetSpecifications.hasControllerId(controllerId))
                .orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));
    }

    private JpaTargetType getTargetTypeByIdAndThrowIfNotFound(final long id) {
        return targetTypeRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(TargetType.class, id));
    }

    @Override
    public List<Target> getByControllerID(final Collection<String> controllerIDs) {
        return Collections.unmodifiableList(
                targetRepository.findAll(TargetSpecifications.byControllerIdWithAssignedDsInJoin(controllerIDs)));
    }

    @Override
    public List<Target> findByControllerIdOrNameOrTarget(final String controllerId, final String name,
                                                         final String serialNumber) {
        return targetRepository.findByControllerIdOrNameOrSerialNumber(controllerId, name, serialNumber);
    }

    @Override
    public long count() {
        return targetRepository.count();
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<TargetMetadata> createMetaData(final String controllerId, final Collection<MetaData> md) {

        final JpaTarget target = getByControllerIdAndThrowIfNotFound(controllerId);

        md.forEach(meta -> checkAndThrowIfTargetMetadataAlreadyExists(
                new TargetMetadataCompositeKey(target.getId(), meta.getKey())));

        assertMetaDataQuota(target.getId(), md.size());

        final JpaTarget updatedTarget = JpaManagementHelper.touch(entityManager, targetRepository, target);

        final List<TargetMetadata> createdMetadata = Collections.unmodifiableList(md.stream()
                .map(meta -> targetMetadataRepository
                        .save(new JpaTargetMetadata(meta.getKey(), meta.getValue(), updatedTarget)))
                .toList());

        // TargetUpdatedEvent is not sent within the touch() method due to the
        // "lastModifiedAt" field being ignored in JpaTarget
        eventPublisherHolder.getEventPublisher()
                .publishEvent(new TargetUpdatedEvent(updatedTarget, eventPublisherHolder.getApplicationId()));

        return createdMetadata;
    }

    private void checkAndThrowIfTargetMetadataAlreadyExists(final TargetMetadataCompositeKey metadataId) {
        if (targetMetadataRepository.existsById(metadataId)) {
            throw new EntityAlreadyExistsException(
                    "Metadata entry with key '" + metadataId.getKey() + "' already exists");
        }
    }

    private void assertMetaDataQuota(final Long targetId, final int requested) {
        QuotaHelper.assertAssignmentQuota(targetId, requested, quotaManagement.getMaxMetaDataEntriesPerTarget(),
                TargetMetadata.class, Target.class, targetMetadataRepository::countByTargetId);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetMetadata updateMetadata(final String controllerId, final MetaData md) {

        // check if exists otherwise throw entity not found exception
        final JpaTargetMetadata updatedMetadata = (JpaTargetMetadata) getMetaDataByControllerId(controllerId,
                md.getKey()).orElseThrow(
                () -> new EntityNotFoundException(TargetMetadata.class, controllerId, md.getKey()));
        updatedMetadata.setValue(md.getValue());
        // touch it to update the lock revision because we are modifying the
        // target indirectly
        final JpaTarget target = JpaManagementHelper.touch(entityManager, targetRepository,
                getByControllerIdAndThrowIfNotFound(controllerId));
        final JpaTargetMetadata metadata = targetMetadataRepository.save(updatedMetadata);
        // target update event is set to ignore "lastModifiedAt" field so it is
        // not send automatically within the touch() method
        eventPublisherHolder.getEventPublisher()
                .publishEvent(new TargetUpdatedEvent(target, eventPublisherHolder.getApplicationId()));
        return metadata;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteMetaData(final String controllerId, final String key) {
        final JpaTargetMetadata metadata = (JpaTargetMetadata) getMetaDataByControllerId(controllerId, key)
                .orElseThrow(() -> new EntityNotFoundException(TargetMetadata.class, controllerId, key));

        final JpaTarget target = JpaManagementHelper.touch(entityManager, targetRepository,
                getByControllerIdAndThrowIfNotFound(controllerId));
        targetMetadataRepository.deleteById(metadata.getId());
        // target update event is set to ignore "lastModifiedAt" field, so it is
        // not send automatically within the touch() method
        eventPublisherHolder.getEventPublisher()
                .publishEvent(new TargetUpdatedEvent(target, eventPublisherHolder.getApplicationId()));
    }

    @Override
    public Page<TargetMetadata> findMetaDataByControllerId(final Pageable pageable, final String controllerId) {
        final Long targetId = getByControllerIdAndThrowIfNotFound(controllerId).getId();

        return JpaManagementHelper.findAllWithCountBySpec(targetMetadataRepository, pageable,
                Collections.singletonList(metadataByTargetIdSpec(targetId)));
    }

    private Specification<JpaTargetMetadata> metadataByTargetIdSpec(final Long targetId) {
        return (root, query, cb) -> cb.equal(root.get(JpaTargetMetadata_.target).get(AbstractJpaBaseEntity_.id), targetId);
    }

    @Override
    public long countMetaDataByControllerId(@NotEmpty final String controllerId) {
        final Long targetId = getByControllerIdAndThrowIfNotFound(controllerId).getId();

        return JpaManagementHelper.countBySpec(targetMetadataRepository,
                Collections.singletonList(metadataByTargetIdSpec(targetId)));
    }

    @Override
    public Page<TargetMetadata> findMetaDataByControllerIdAndRsql(final Pageable pageable, final String controllerId,
                                                                  final String rsqlParam) {
        final Long targetId = getByControllerIdAndThrowIfNotFound(controllerId).getId();

        final List<Specification<JpaTargetMetadata>> specList = Arrays.asList(RSQLUtility
                        .buildRsqlSpecification(rsqlParam, TargetMetadataFields.class, virtualPropertyReplacer, database),
                metadataByTargetIdSpec(targetId));

        return JpaManagementHelper.findAllWithCountBySpec(targetMetadataRepository, pageable, specList);
    }

    @Override
    public List<Target> findByVehicleModelId(Long vehicleModelId) {
        return Collections.unmodifiableList(targetRepository.findByVehicleModelId(vehicleModelId));
    }

    @Override
    public List<Target> findByControllerIdIn(List<String> vins) {
        return targetRepository.findByControllerIdIn(vins).stream().map(Target.class::cast).toList();
    }

    @Override
    public List<Long> findTargetIdByControllerIds(List<String> controllerIds) {
        return targetRepository.findTargetIdByControllerIds(controllerIds);
    }

    @Override
    public Optional<TargetMetadata> getMetaDataByControllerId(final String controllerId, final String key) {
        final Long targetId = getByControllerIdAndThrowIfNotFound(controllerId).getId();

        return targetMetadataRepository.findById(new TargetMetadataCompositeKey(targetId, key)).map(t -> t);
    }

    @Override
    public Slice<Target> findAll(final Pageable pageable) {
        return JpaManagementHelper.findAllWithoutCountBySpec(targetRepository, pageable, null);
    }

    @Override
    public Slice<Target> findByTargetFilterQuery(final Pageable pageable, final long targetFilterQueryId) {
        final TargetFilterQuery targetFilterQuery = targetFilterQueryRepository.findById(targetFilterQueryId)
                .orElseThrow(() -> new EntityNotFoundException(TargetFilterQuery.class, targetFilterQueryId));

        return JpaManagementHelper.findAllWithoutCountBySpec(targetRepository, pageable,
                Collections.singletonList(RSQLUtility.buildRsqlSpecification(targetFilterQuery.getQuery(),
                        TargetFields.class, virtualPropertyReplacer, database)));
    }

    @Override
    public Slice<Target> findByRsql(final Pageable pageable, final String targetFilterQuery) {
        return JpaManagementHelper.findAllWithoutCountBySpec(targetRepository, pageable,
                Collections.singletonList(RSQLUtility.buildRsqlSpecification(targetFilterQuery, TargetFields.class,
                        virtualPropertyReplacer, database)));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Target update(final TargetUpdate u) {
        final JpaTargetUpdate update = (JpaTargetUpdate) u;

        final JpaTarget target = getByControllerIdAndThrowIfNotFound(update.getControllerId());

        update.getName().ifPresent(target::setName);
        update.getDescription().ifPresent(target::setDescription);
        update.getSecurityToken().ifPresent(target::setSecurityToken);
        if (update.getTargetTypeId() != null) {
            final TargetType targetType = getTargetTypeByIdAndThrowIfNotFound(update.getTargetTypeId());
            target.setTargetType(targetType);
        }
        if (update.getVehicleModelId() != null) {
            findByVehicleIdOrThrowIfNotFound(update.getVehicleModelId());
            target.setVehicleModelId(update.getVehicleModelId());
        }
        return targetRepository.save(target);
    }

    private Vehicle findByVehicleIdOrThrowIfNotFound(Long vehicleModelId) {
        return vehicleManagement.get(vehicleModelId)
                .orElseThrow(() -> new EntityNotFoundException(Vehicle.class, vehicleModelId));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final Collection<Long> targetIDs) {
        final List<JpaTarget> targets = targetRepository.findAll(TargetSpecifications.hasIdIn(targetIDs));

        if (targets.size() < targetIDs.size()) {
            throw new EntityNotFoundException(Target.class, targetIDs,
                    targets.stream().map(Target::getId).toList());
        }

        targetRepository.deleteByIdIn(targetIDs);

        afterCommit
                .afterCommit(() -> targets.forEach(target -> eventPublisherHolder.getEventPublisher()
                        .publishEvent(new TargetDeletedEvent(tenantAware.getCurrentTenant(), target.getId(),
                                target.getControllerId(),
                                JpaTarget.class, eventPublisherHolder.getApplicationId()))));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteByControllerID(final String controllerID) {
        final Target target = getByControllerIdAndThrowIfNotFound(controllerID);
        targetRepository.deleteById(target.getId());
    }

    @Override
    public Page<Target> findByAssignedDistributionSet(final Pageable pageReq, final long distributionSetID) {
        final DistributionSet validDistSet = distributionSetManagement.getOrElseThrowException(distributionSetID);

        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, pageReq,
                Collections.singletonList(TargetSpecifications.hasAssignedDistributionSet(validDistSet.getId())));
    }

    @Override
    public Page<Target> findByAssignedDistributionSetAndRsql(final Pageable pageReq, final long distributionSetID,
                                                             final String rsqlParam) {
        final DistributionSet validDistSet = distributionSetManagement.getOrElseThrowException(distributionSetID);

        final List<Specification<JpaTarget>> specList = Arrays.asList(
                RSQLUtility.buildRsqlSpecification(rsqlParam, TargetFields.class, virtualPropertyReplacer, database),
                TargetSpecifications.hasAssignedDistributionSet(validDistSet.getId()));

        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, pageReq, specList);
    }

    @Override
    public Page<Target> findByInstalledDistributionSet(final Pageable pageReq, final long distributionSetID) {
        final DistributionSet validDistSet = distributionSetManagement.getOrElseThrowException(distributionSetID);

        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, pageReq,
                Collections.singletonList(TargetSpecifications.hasInstalledDistributionSet(validDistSet.getId())));
    }

    @Override
    public Page<Target> findByInstalledDistributionSetAndRsql(final Pageable pageable, final long distributionSetId,
                                                              final String rsqlParam) {
        final DistributionSet validDistSet = distributionSetManagement.getOrElseThrowException(distributionSetId);

        final List<Specification<JpaTarget>> specList = Arrays.asList(
                RSQLUtility.buildRsqlSpecification(rsqlParam, TargetFields.class, virtualPropertyReplacer, database),
                TargetSpecifications.hasInstalledDistributionSet(validDistSet.getId()));

        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, pageable, specList);
    }

    @Override
    public Page<Target> findByUpdateStatus(final Pageable pageable, final TargetUpdateStatus status) {
        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, pageable,
                Collections.singletonList(TargetSpecifications.hasTargetUpdateStatus(status)));
    }

    @Override
    public Slice<Target> findByFilters(final Pageable pageable, final FilterParams filterParams) {
        final List<Specification<JpaTarget>> specList = buildSpecificationList(filterParams);
        return JpaManagementHelper.findAllWithoutCountBySpec(targetRepository, pageable, specList);
    }

    @Override
    public long countByFilters(final FilterParams filterParams) {
        final List<Specification<JpaTarget>> specList = buildSpecificationList(filterParams);
        return JpaManagementHelper.countBySpec(targetRepository, specList);
    }

    private List<Specification<JpaTarget>> buildSpecificationList(final FilterParams filterParams) {
        final List<Specification<JpaTarget>> specList = new ArrayList<>();
        if ((filterParams.getFilterByStatus() != null) && !filterParams.getFilterByStatus().isEmpty()) {
            specList.add(TargetSpecifications.hasTargetUpdateStatus(filterParams.getFilterByStatus()));
        }
        if (filterParams.getOverdueState() != null && filterParams.getOverdueState()) {
            specList.add(TargetSpecifications.isOverdue(TimestampCalculator.calculateOverdueTimestamp()));
        }
        if (filterParams.getFilterByDistributionId() != null) {
            final DistributionSet validDistSet = distributionSetManagement
                    .getOrElseThrowException(filterParams.getFilterByDistributionId());

            specList.add(TargetSpecifications.hasInstalledOrAssignedDistributionSet(validDistSet.getId()));
        }
        if (StringUtils.hasText(filterParams.getFilterBySearchText())) {
            specList.add(TargetSpecifications.likeControllerIdOrName(filterParams.getFilterBySearchText()));
        }
        if (hasTagsFilterActive(filterParams)) {
            specList.add(TargetSpecifications.hasTags(filterParams.getFilterByTagNames(),
                    filterParams.getSelectTargetWithNoTag()));
        }

        if (hasTypesFilterActive(filterParams)) {
            specList.add(TargetSpecifications.hasTargetType(filterParams.getFilterByTargetType()));
        } else if (hasNoTypeFilterActive(filterParams)) {
            specList.add(TargetSpecifications.hasNoTargetType());
        }

        return specList;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetTagAssignmentResult toggleTagAssignment(final Collection<String> controllerIds, final String tagName) {
        final TargetTag tag = targetTagRepository.findByNameEquals(tagName)
                .orElseThrow(() -> new EntityNotFoundException(TargetTag.class, tagName));
        final List<JpaTarget> allTargets = targetRepository
                .findAll(TargetSpecifications.byControllerIdWithTagsInJoin(controllerIds));

        if (allTargets.size() < controllerIds.size()) {
            throw new EntityNotFoundException(Target.class, controllerIds,
                    allTargets.stream().map(Target::getControllerId).toList());
        }

        final List<JpaTarget> alreadyAssignedTargets = targetRepository.findAll(
                TargetSpecifications.hasTagName(tagName).and(TargetSpecifications.hasControllerIdIn(controllerIds)));

        // all are already assigned -> unassign
        if (alreadyAssignedTargets.size() == allTargets.size()) {
            alreadyAssignedTargets.forEach(target -> target.removeTag(tag));
            return new TargetTagAssignmentResult(0, Collections.emptyList(),
                    Collections.unmodifiableList(alreadyAssignedTargets), tag);
        }

        allTargets.removeAll(alreadyAssignedTargets);
        // some or none are assigned -> assign
        allTargets.forEach(target -> target.addTag(tag));
        final TargetTagAssignmentResult result = new TargetTagAssignmentResult(alreadyAssignedTargets.size(),
                Collections
                        .unmodifiableList(allTargets.stream().map(targetRepository::save).toList()),
                Collections.emptyList(), tag);

        // no reason to persist the tag
        entityManager.detach(tag);
        return result;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetTypeAssignmentResult assignType(final Collection<String> controllerIds, final Long typeId) {
        final TargetType type = targetTypeRepository.findById(typeId)
                .orElseThrow(() -> new EntityNotFoundException(TargetType.class, typeId));

        final List<JpaTarget> targetsWithSameType = findTargetsByInSpecification(controllerIds,
                TargetSpecifications.hasTargetType(typeId));

        final List<JpaTarget> targetsWithoutSameType = findTargetsByInSpecification(controllerIds,
                TargetSpecifications.hasTargetTypeNot(typeId));

        // set new target type to all targets without that type
        targetsWithoutSameType.forEach(target -> target.setTargetType(type));

        final TargetTypeAssignmentResult result = new TargetTypeAssignmentResult(targetsWithSameType.size(),
                Collections.unmodifiableList(
                        targetsWithoutSameType.stream().map(targetRepository::save).toList()),
                Collections.emptyList(), type);

        // no reason to persist the type
        entityManager.detach(type);
        return result;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetTypeAssignmentResult unAssignType(final Collection<String> controllerIds) {
        final List<JpaTarget> allTargets = findTargetsByInSpecification(controllerIds, null);

        if (allTargets.size() < controllerIds.size()) {
            throw new EntityNotFoundException(Target.class, controllerIds,
                    allTargets.stream().map(Target::getControllerId).toList());
        }

        // set new target type to null for all targets
        allTargets.forEach(target -> target.setTargetType(null));

        return new TargetTypeAssignmentResult(0, Collections.emptyList(), Collections
                .unmodifiableList(allTargets.stream().map(targetRepository::save).toList()), null);
    }

    private List<JpaTarget> findTargetsByInSpecification(final Collection<String> controllerIds,
                                                         final Specification<JpaTarget> specification) {
        return Lists.partition(new ArrayList<>(controllerIds), Constants.MAX_ENTRIES_IN_STATEMENT).stream()
                .map(ids -> targetRepository.findAll(TargetSpecifications.hasControllerIdIn(ids).and(specification)))
                .flatMap(List::stream).toList();
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<Target> assignTag(final Collection<String> controllerIds, final long tagId) {

        final List<JpaTarget> allTargets = targetRepository
                .findAll(TargetSpecifications.byControllerIdWithTagsInJoin(controllerIds));

        if (allTargets.size() < controllerIds.size()) {
            throw new EntityNotFoundException(Target.class, controllerIds,
                    allTargets.stream().map(Target::getControllerId).toList());
        }

        final JpaTargetTag tag = targetTagRepository.findById(tagId)
                .orElseThrow(() -> new EntityNotFoundException(TargetTag.class, tagId));

        allTargets.forEach(target -> target.addTag(tag));

        final List<Target> result = Collections.unmodifiableList(allTargets.stream().map(targetRepository::save)
                .toList());

        // No reason to save the tag
        entityManager.detach(tag);
        return result;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Target unAssignTag(final String controllerID, final long targetTagId) {
        final JpaTarget target = getByControllerIdAndThrowIfNotFound(controllerID);

        final TargetTag tag = targetTagRepository.findById(targetTagId)
                .orElseThrow(() -> new EntityNotFoundException(TargetTag.class, targetTagId));
        if (!target.getTags().contains(tag)) {
            throw new IllegalArgumentException("Tag ID is not linked to the Controller ID.");
        }
        if (!rolloutManagement.findAllRolloutByTargetId(target).isEmpty()) {
            throw new IllegalArgumentException("Tag cannot be removed from Target Because Target is assigned to a rollout.");
        }

        target.removeTag(tag);

        final Target result = targetRepository.save(target);

        // No reason to save the tag
        entityManager.detach(tag);
        return result;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Target unAssignType(final String controllerID) {
        final JpaTarget target = getByControllerIdAndThrowIfNotFound(controllerID);
        target.setTargetType(null);
        return targetRepository.save(target);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Target assignType(final String controllerID, final Long targetTypeId) {
        final JpaTarget target = getByControllerIdAndThrowIfNotFound(controllerID);
        final JpaTargetType targetType = getTargetTypeByIdAndThrowIfNotFound(targetTypeId);
        target.setTargetType(targetType);
        return targetRepository.save(target);
    }

    @Override
    public Slice<Target> findByFilterOrderByLinkedDistributionSet(final Pageable pageable,
                                                                  final long orderByDistributionId, final FilterParams filterParams) {
        // remove default sort from pageable to not overwrite sorted spec
        final OffsetBasedPageRequest unsortedPage = new OffsetBasedPageRequest(pageable.getOffset(),
                pageable.getPageSize(), Sort.unsorted());

        final List<Specification<JpaTarget>> specList = buildSpecificationList(filterParams);
        specList.add(TargetSpecifications.orderedByLinkedDistributionSet(orderByDistributionId, pageable.getSort()));

        return JpaManagementHelper.findAllWithoutCountBySpec(targetRepository, unsortedPage, specList);
    }

    @Override
    public long countByAssignedDistributionSet(final long distId) {
        final DistributionSet validDistSet = distributionSetManagement.getOrElseThrowException((distId));

        return targetRepository.count(TargetSpecifications.hasAssignedDistributionSet(validDistSet.getId()));
    }

    @Override
    public long countByInstalledDistributionSet(final long distId) {
        final DistributionSet validDistSet = distributionSetManagement.getOrElseThrowException((distId));

        return targetRepository.count(TargetSpecifications.hasInstalledDistributionSet(validDistSet.getId()));
    }

    @Override
    public boolean existsByInstalledOrAssignedDistributionSet(final long distId) {
        final DistributionSet validDistSet = distributionSetManagement.getOrElseThrowException((distId));

        return targetRepository
                .exists(TargetSpecifications.hasInstalledOrAssignedDistributionSet(validDistSet.getId()));
    }

    /**
     * @param controllerId  the unique identifier of the target (controller ID) for which the tenant is to be updated
     * @param currentTenant the existing tenant of the target, used as a condition to confirm the update
     * @param newTenant     the new tenant to set for the target; will be converted to uppercase before updating
     * @return {@code true} if the tenant was successfully updated; {@code false} otherwise
     * @throws ResponseStatusException with HTTP status 500 if an unexpected error occurs during the update
     */
    @Override
    public boolean updateTenant(String controllerId, String currentTenant, String newTenant) {
        try {
            newTenant = newTenant.toUpperCase();
            int rowsUpdated = targetRepository.updateTenant(newTenant, controllerId, currentTenant);
            return rowsUpdated > 0;

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred while updating tenant", e);
        }
    }

    @Override
    public Slice<Target> findByTargetFilterQueryAndNonDSAndCompatible(final Pageable pageRequest,
                                                                      final long distributionSetId, final String targetFilterQuery) {
        final DistributionSet jpaDistributionSet = distributionSetManagement.getOrElseThrowException(distributionSetId);
        final Long distSetTypeId = jpaDistributionSet.getType().getId();

        final List<Specification<JpaTarget>> specList = Arrays.asList(
                RSQLUtility.buildRsqlSpecification(targetFilterQuery, TargetFields.class, virtualPropertyReplacer,
                        database),
                TargetSpecifications.hasNotDistributionSetInActions(distributionSetId),
                TargetSpecifications.isCompatibleWithDistributionSetType(distSetTypeId));

        return JpaManagementHelper.findAllWithoutCountBySpec(targetRepository, pageRequest, specList);
    }

    @Override
    public Slice<Target> findByTargetFilterQueryAndNotInRolloutGroupsAndCompatible(final Pageable pageRequest,
                                                                                   final Collection<Long> groups, final String targetFilterQuery, final DistributionSetType dsType) {
        final List<Specification<JpaTarget>> specList = Arrays.asList(
                RSQLUtility.buildRsqlSpecification(targetFilterQuery, TargetFields.class, virtualPropertyReplacer,
                        database),
                TargetSpecifications.isNotInRolloutGroups(groups),
                TargetSpecifications.isCompatibleWithDistributionSetType(dsType.getId()));

        return JpaManagementHelper.findAllWithoutCountBySpec(targetRepository, pageRequest, specList);
    }

    @Override
    public List<Target> findByTargetFilterAndTargetIdIn(final String targetFilterQuery, final List<String> targetId) {
        List<Specification<JpaTarget>> specList = Arrays.asList(
                RSQLUtility.buildRsqlSpecification(targetFilterQuery, TargetFields.class, virtualPropertyReplacer, database),
                TargetSpecifications.hasControllerIdIn(targetId)
        );

        return targetRepository.findAll(Specification.where(specList.get(0)).and(specList.get(1)))
                .stream()
                .map(Target.class::cast)
                .toList();

    }

    @Override
    public Slice<Target> findByInRolloutGroupWithoutAction(final Pageable pageRequest, final long group) {
        if (!rolloutGroupRepository.existsById(group)) {
            throw new EntityNotFoundException(RolloutGroup.class, group);
        }

        return JpaManagementHelper.findAllWithoutCountBySpec(targetRepository, pageRequest,
                Collections.singletonList(TargetSpecifications.hasNoActionInRolloutGroup(group)));
    }

    @Override
    public long countByRsqlAndNotInRolloutGroupsAndCompatible(final Collection<Long> groups,
                                                              final String targetFilterQuery, final DistributionSetType dsType) {
        final List<Specification<JpaTarget>> specList = Arrays.asList(
                RSQLUtility.buildRsqlSpecification(targetFilterQuery, TargetFields.class, virtualPropertyReplacer,
                        database),
                TargetSpecifications.isNotInRolloutGroups(groups),
                TargetSpecifications.isCompatibleWithDistributionSetType(dsType.getId()));

        return JpaManagementHelper.countBySpec(targetRepository, specList);
    }

    @Override
    public long countByRsqlAndNonDSAndCompatible(final long distributionSetId, final String targetFilterQuery) {
        final DistributionSet jpaDistributionSet = distributionSetManagement.getOrElseThrowException(distributionSetId);
        final Long distSetTypeId = jpaDistributionSet.getType().getId();

        final List<Specification<JpaTarget>> specList = Arrays.asList(
                RSQLUtility.buildRsqlSpecification(targetFilterQuery, TargetFields.class, virtualPropertyReplacer,
                        database),
                TargetSpecifications.hasNotDistributionSetInActions(distributionSetId),
                TargetSpecifications.isCompatibleWithDistributionSetType(distSetTypeId));

        return JpaManagementHelper.countBySpec(targetRepository, specList);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Target create(final TargetCreate c) {
        final JpaTargetCreate create = (JpaTargetCreate) c;
        return targetRepository.save(create.build());
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<Target> create(final Collection<TargetCreate> targets) {
        final List<JpaTarget> targetList = targets.stream().map(JpaTargetCreate.class::cast).map(JpaTargetCreate::build)
                .toList();
        return Collections.unmodifiableList(targetRepository.saveAll(targetList));
    }

    @Override
    public Page<Target> findByTag(final Pageable pageable, final long tagId) {
        throwEntityNotFoundExceptionIfTagDoesNotExist(tagId);

        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, pageable,
                Collections.singletonList(TargetSpecifications.hasTag(tagId)));
    }

    private void throwEntityNotFoundExceptionIfTagDoesNotExist(final Long tagId) {
        if (!targetTagRepository.existsById(tagId)) {
            throw new EntityNotFoundException(TargetTag.class, tagId);
        }
    }

    @Override
    public Page<Target> findByRsqlAndTag(final Pageable pageable, final String rsqlParam, final long tagId) {
        throwEntityNotFoundExceptionIfTagDoesNotExist(tagId);

        final List<Specification<JpaTarget>> specList = Arrays.asList(
                RSQLUtility.buildRsqlSpecification(rsqlParam, TargetFields.class, virtualPropertyReplacer, database),
                TargetSpecifications.hasTag(tagId));

        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, pageable, specList);
    }

    @Override
    public long countByTargetFilterQuery(final long targetFilterQueryId) {
        final TargetFilterQuery targetFilterQuery = targetFilterQueryRepository.findById(targetFilterQueryId)
                .orElseThrow(() -> new EntityNotFoundException(TargetFilterQuery.class, targetFilterQueryId));

        return countByRsql(targetFilterQuery.getQuery());
    }

    @Override
    public long countByRsql(final String targetFilterQuery) {
        return JpaManagementHelper.countBySpec(targetRepository, Collections.singletonList(RSQLUtility
                .buildRsqlSpecification(targetFilterQuery, TargetFields.class, virtualPropertyReplacer, database)));
    }

    @Override
    public long countByRsqlAndCompatible(final String targetFilterQuery, final Long dsTypeId) {
        final List<Specification<JpaTarget>> specList = Arrays.asList(RSQLUtility
                        .buildRsqlSpecification(targetFilterQuery, TargetFields.class, virtualPropertyReplacer, database),
                TargetSpecifications.isCompatibleWithDistributionSetType(dsTypeId));

        return JpaManagementHelper.countBySpec(targetRepository, specList);
    }

    @Override
    public long countByRsqlAndInControllerIdAndCompatible(final String targetFilterQuery, final List<String> controllerIds, final Long dsTypeId) {
        final List<Specification<JpaTarget>> specList = Arrays.asList(RSQLUtility
                        .buildRsqlSpecification(targetFilterQuery, TargetFields.class, virtualPropertyReplacer, database),
                TargetSpecifications.hasControllerIdIn(controllerIds),
                TargetSpecifications.isCompatibleWithDistributionSetType(dsTypeId));

        return JpaManagementHelper.countBySpec(targetRepository, specList);
    }

    @Override
    public Optional<Target> get(final long id) {
        return targetRepository.findById(id).map(t -> t);
    }

    @Override
    public List<Target> get(final Collection<Long> ids) {
        return Collections.unmodifiableList(targetRepository.findAll(TargetSpecifications.hasIdIn(ids)));
    }

    @Override
    public Map<String, String> getControllerAttributes(final String controllerId) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);

        final Root<JpaTarget> targetRoot = query.from(JpaTarget.class);
        query.where(cb.equal(targetRoot.get(JpaTarget_.controllerId), controllerId));

        final MapJoin<JpaTarget, String, String> attributes = targetRoot.join(JpaTarget_.controllerAttributes);
        query.multiselect(attributes.key(), attributes.value());
        query.orderBy(cb.asc(attributes.key()));

        final List<Object[]> attr = entityManager.createQuery(query).getResultList();

        return attr.stream().collect(Collectors.toMap(entry -> (String) entry[0], entry -> (String) entry[1],
                (v1, v2) -> v1, LinkedHashMap::new));
    }

    @Override
    public Map<String, String> getControllerSoftwareAttributes(final String controllerId) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);

        final Root<JpaTarget> targetRoot = query.from(JpaTarget.class);
        query.where(cb.equal(targetRoot.get(JpaTarget_.controllerId), controllerId));

        final ListJoin<JpaTarget, JpaTargetSoftware> softwares = targetRoot.join(JpaTarget_.softwares);
        query.multiselect(softwares.get(AbstractJpaTargetSoftware_.componentId),
                softwares.get(AbstractJpaTargetSoftware_.version),
                softwares.get(AbstractJpaTargetSoftware_.node));

        final List<Object[]> attr = entityManager.createQuery(query).getResultList();

        Map<String, String> softwareAttributesMap = new LinkedHashMap<>();
        Map<String, Integer> nodeCounters = new HashMap<>(); // Contatori per nodo
        for (Object[] result : attr) {
            String componentId = (String) result[0]; // ComponentId from array
            String version = (String) result[1]; // Version from array
            String node = (String) result[2]; // Value from array

            nodeCounters.putIfAbsent(node, 1);
            int counter = nodeCounters.get(node);

            String componentIdKey = node + "." + "software" + "." + counter + "." + "componentId";
            String versionKey = node + "." + "software" + "." + counter + "." + "version";
            softwareAttributesMap.put(componentIdKey, componentId);
            softwareAttributesMap.put(versionKey, version);
            nodeCounters.put(node, counter + 1);
        }

        return softwareAttributesMap;
    }


    @Override
    public void requestControllerAttributes(final String controllerId) {
        final JpaTarget target = getByControllerIdAndThrowIfNotFound(controllerId);

        target.setRequestControllerAttributes(true);

        eventPublisherHolder.getEventPublisher()
                .publishEvent(new TargetAttributesRequestedEvent(tenantAware.getCurrentTenant(), target.getId(), target.getControllerId(),
                        JpaTarget.class, eventPublisherHolder.getApplicationId()));
    }

    /**
     * Retrieves a paginated list of {@link InventoryWithAction} objects for a given controller.
     * <p>
     * For the specified controller ID, this method:
     * <ol>
     *   <li>Resolves the {@link JpaTarget} entity for the controller.</li>
     *   <li>Finds the latest {@link Action} for that controller (if any exist) and gets its ID.</li>
     *   <li>Fetches the target's {@link TargetInventory} entries in descending order using the given {@link Pageable}.</li>
     *   <li>Maps each inventory entry into an {@link InventoryWithAction}, combining the inventory data with the resolved action ID.</li>
     * </ol>
     *
     * @param controllerId the unique controller ID to look up inventories for
     * @param pageable     the pagination and sorting information
     * @return a page of {@link InventoryWithAction} containing inventories and the latest action ID (if present)
     */

    public Page<InventoryWithAction> findByTargetInventoryInDesc(String controllerId, Pageable pageable) {
        final JpaTarget target = getByControllerIdAndThrowIfNotFound(controllerId);


        Slice<Action> action = actionRepository.findByTargetControllerIdAndActive(pageable, controllerId, true);
        final Long actionId = action.hasContent() ? action.getContent().get(0).getId() : null;

        Page<TargetInventory> inventoryPage = targetInventoryRepository.findByTargetInventoryInDesc(target.getControllerId(), pageable);

        return inventoryPage.map(inv -> new InventoryWithAction(inv.getInventory(), actionId));
    }

    @Override
    public long countByTargetControllerId(String controllerId) {
        return targetInventoryRepository.countTargetInventory(controllerId);
    }

    @Override
    public boolean isControllerAttributesRequested(final String controllerId) {
        final JpaTarget target = getByControllerIdAndThrowIfNotFound(controllerId);

        return target.isRequestControllerAttributes();
    }

    @Override
    public boolean existsByControllerId(final String controllerId) {
        return targetRepository.exists(TargetSpecifications.hasControllerId(controllerId));
    }

    @Override
    public boolean isTargetMatchingQueryAndDSNotAssignedAndCompatible(final String controllerId,
                                                                      final long distributionSetId, final String targetFilterQuery) {
        RSQLUtility.validateRsqlFor(targetFilterQuery, TargetFields.class);
        final DistributionSet ds = distributionSetManagement.get(distributionSetId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSet.class, distributionSetId));
        final Long distSetTypeId = ds.getType().getId();
        final List<Specification<JpaTarget>> specList = Arrays.asList(
                RSQLUtility.buildRsqlSpecification(targetFilterQuery, TargetFields.class, virtualPropertyReplacer,
                        database),
                TargetSpecifications.hasNotDistributionSetInActions(distributionSetId),
                TargetSpecifications.isCompatibleWithDistributionSetType(distSetTypeId),
                TargetSpecifications.hasControllerId(controllerId));

        final Specification<JpaTarget> combinedSpecification = Objects
                .requireNonNull(SpecificationsBuilder.combineWithAnd(specList));
        return targetRepository.exists(combinedSpecification);
    }

    @Override
    public Page<Target> findByControllerAttributesRequested(final Pageable pageReq) {
        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, pageReq,
                Collections.singletonList(TargetSpecifications.hasRequestControllerAttributesTrue()));
    }

    /**
     * Verifies of the given list of controller IDs(Vins) are associated with the provided ecu node address.
     *
     * @param ecuNodeAddress ecu node address
     * @param controllerIds  list of controller IDs(Vins)
     * @return true if the ecu node address matches with all the controller IDs(Vins), false otherwise.
     */
    @Override
    public boolean isEcuNodeAddressMatchControllerIds(String ecuNodeAddress, List<String> controllerIds) {
        List<Target> targets = targetRepository.findAllByControllerIdIn(controllerIds);

        var vehicleModelIds = targets.stream()
                .map(Target::getVehicleModelId)
                .collect(Collectors.toList());

        var vehicles = vehicleManagement.findAllById(vehicleModelIds);

        //Create a map of vehicleModelId to Vehicle
        var vehicleMap = vehicles.stream()
                .collect(Collectors.toMap(Vehicle::getId, vehicle -> vehicle));

        for (Target target : targets) {

            Vehicle vehicle = vehicleMap.get(target.getVehicleModelId());
            if (vehicle.getVehicleEcu().isEmpty()) {
                return false;
            }

            boolean anyMatch = vehicle.getVehicleEcu().stream()
                    .anyMatch(ecuModel -> ecuNodeAddress.equals(ecuModel.getEcuNodeId()));
            if (!anyMatch) {
                return false;
            }

        }
        return true;
    }

}
