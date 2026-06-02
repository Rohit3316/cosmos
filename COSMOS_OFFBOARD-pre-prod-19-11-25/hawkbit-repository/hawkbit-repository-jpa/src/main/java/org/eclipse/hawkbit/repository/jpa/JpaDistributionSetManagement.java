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
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.eclipse.hawkbit.repository.ArtifactsManagement;
import org.eclipse.hawkbit.repository.DistributionSetFields;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetMetadataFields;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetUpdate;
import org.eclipse.hawkbit.repository.builder.GenericDistributionSetUpdate;
import org.eclipse.hawkbit.repository.event.remote.DistributionSetDeletedEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.InvalidDistributionSetException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaDistributionSetCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.model.DistributionSetModule;
import org.eclipse.hawkbit.repository.jpa.model.DsMetadataCompositeKey;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetMetadata_;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaVersion;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.DistributionSetSpecification;
import org.eclipse.hawkbit.repository.jpa.specifications.SpecificationsBuilder;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetTagAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.DistributionSoftwareVersionWrapper;
import org.eclipse.hawkbit.repository.model.IDistributionSetModule;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Statistic;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.repository.model.dto.ArtifactsExpiryDTO;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import jakarta.persistence.EntityManager;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JPA implementation of {@link DistributionSetManagement}.
 */
@Transactional(readOnly = true)
@Validated
public class JpaDistributionSetManagement implements DistributionSetManagement {

    private final EntityManager entityManager;

    private final DistributionSetRepository distributionSetRepository;

    private final DistributionSetTagManagement distributionSetTagManagement;

    private final ArtifactsManagement artifactsManagement;

    private final SystemManagement systemManagement;

    private final DistributionSetTypeManagement distributionSetTypeManagement;

    private final QuotaManagement quotaManagement;

    private final DistributionSetMetadataRepository distributionSetMetadataRepository;

    private final TargetFilterQueryRepository targetFilterQueryRepository;

    private final ActionRepository actionRepository;

    private final EventPublisherHolder eventPublisherHolder;

    private final TenantAware tenantAware;

    private final VirtualPropertyReplacer virtualPropertyReplacer;

    private final SoftwareModuleRepository softwareModuleRepository;

    private final DistributionSetTagRepository distributionSetTagRepository;

    private final AfterTransactionCommitExecutor afterCommit;

    private final DistributionSetModuleRepository distributionSetModuleRepository;

    private final VersionRepository versionRepository;

    private final Database database;

    private final RolloutManagement rolloutManagement;

    @Autowired
    private RolloutRepository rolloutRepository;

    JpaDistributionSetManagement(final EntityManager entityManager,
                                 final DistributionSetRepository distributionSetRepository, final ArtifactsManagement artifactsManagement,
                                 final DistributionSetTagManagement distributionSetTagManagement, final SystemManagement systemManagement,
                                 final DistributionSetTypeManagement distributionSetTypeManagement, final QuotaManagement quotaManagement,
                                 final DistributionSetMetadataRepository distributionSetMetadataRepository,
                                 final TargetFilterQueryRepository targetFilterQueryRepository, final ActionRepository actionRepository,
                                 final EventPublisherHolder eventPublisherHolder, final TenantAware tenantAware,
                                 final VirtualPropertyReplacer virtualPropertyReplacer,
                                 final SoftwareModuleRepository softwareModuleRepository,
                                 final DistributionSetTagRepository distributionSetTagRepository,
                                 final DistributionSetModuleRepository distributionSetModuleRepository,
                                 final VersionRepository versionRepository,
                                 final AfterTransactionCommitExecutor afterCommit, final Database database,
                                 final RolloutManagement rolloutManagement) {
        this.entityManager = entityManager;
        this.distributionSetRepository = distributionSetRepository;
        this.distributionSetTagManagement = distributionSetTagManagement;
        this.artifactsManagement = artifactsManagement;
        this.systemManagement = systemManagement;
        this.distributionSetTypeManagement = distributionSetTypeManagement;
        this.quotaManagement = quotaManagement;
        this.distributionSetMetadataRepository = distributionSetMetadataRepository;
        this.targetFilterQueryRepository = targetFilterQueryRepository;
        this.actionRepository = actionRepository;
        this.eventPublisherHolder = eventPublisherHolder;
        this.tenantAware = tenantAware;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.softwareModuleRepository = softwareModuleRepository;
        this.distributionSetTagRepository = distributionSetTagRepository;
        this.distributionSetModuleRepository = distributionSetModuleRepository;
        this.versionRepository = versionRepository;
        this.afterCommit = afterCommit;
        this.database = database;
        this.rolloutManagement = rolloutManagement;
    }

    private static List<Specification<JpaDistributionSet>> buildDistributionSetSpecifications(
            final DistributionSetFilter distributionSetFilter) {
        final List<Specification<JpaDistributionSet>> specList = Lists.newArrayListWithExpectedSize(9);

        Specification<JpaDistributionSet> spec;

        if (distributionSetFilter.getIsComplete() != null) {
            spec = DistributionSetSpecification.isCompleted(distributionSetFilter.getIsComplete());
            specList.add(spec);
        }

        if (distributionSetFilter.getIsDeleted() != null) {
            spec = DistributionSetSpecification.isDeleted(distributionSetFilter.getIsDeleted());
            specList.add(spec);
        }

        if (distributionSetFilter.getIsValid() != null) {
            spec = DistributionSetSpecification.isValid(distributionSetFilter.getIsValid());
            specList.add(spec);
        }

        if (distributionSetFilter.getTypeId() != null) {
            spec = DistributionSetSpecification.byType(distributionSetFilter.getTypeId());
            specList.add(spec);
        }

        if (StringUtils.hasText(distributionSetFilter.getSearchText())) {
            final String[] dsFilterNameAndVersionEntries = JpaManagementHelper
                    .getFilterNameAndVersionEntries(distributionSetFilter.getSearchText().trim());
            spec = DistributionSetSpecification.likeNameAndVersion(dsFilterNameAndVersionEntries[0],
                    dsFilterNameAndVersionEntries[1]);
            specList.add(spec);
        }

        if (hasTagsFilterActive(distributionSetFilter)) {
            spec = DistributionSetSpecification.hasTags(distributionSetFilter.getTagNames(),
                    distributionSetFilter.getSelectDSWithNoTag());
            specList.add(spec);
        }
        if (distributionSetFilter.getInstalledTargetId() != null) {
            spec = DistributionSetSpecification.installedTarget(distributionSetFilter.getInstalledTargetId());
            specList.add(spec);
        }
        if (distributionSetFilter.getAssignedTargetId() != null) {
            spec = DistributionSetSpecification.assignedTarget(distributionSetFilter.getAssignedTargetId());
            specList.add(spec);
        }
        return specList;
    }

    private static boolean hasTagsFilterActive(final DistributionSetFilter distributionSetFilter) {
        final boolean isNoTagActive = Boolean.TRUE.equals(distributionSetFilter.getSelectDSWithNoTag());
        final boolean isAtLeastOneTagActive = !CollectionUtils.isEmpty(distributionSetFilter.getTagNames());

        return isNoTagActive || isAtLeastOneTagActive;
    }

    private static void assertTargetVersionIsLinkedToSoftwareModule(long moduleId, long versionModuleId) {
        if (!Objects.equals(versionModuleId, moduleId)) {
            throw new ValidationException("The software module is not linked to the specified target version");
        }
    }

    @Override
    public Optional<DistributionSet> getWithDetails(final long distid) {
        return distributionSetRepository.findOne(DistributionSetSpecification.byId(distid))
                .map(DistributionSet.class::cast);
    }

    @Override
    public long countByTypeId(final long typeId) {
        if (!distributionSetTypeManagement.exists(typeId)) {
            throw new EntityNotFoundException(DistributionSetType.class, typeId);
        }

        return distributionSetRepository.countByTypeId(typeId);
    }

    @Override
    public List<Statistic> countRolloutsByStatusForDistributionSet(Long dsId) {
        return distributionSetRepository.countRolloutsByStatusForDistributionSet(dsId).stream().map(Statistic.class::cast).toList();
    }

    @Override
    public List<Statistic> countActionsByStatusForDistributionSet(Long dsId) {
        return distributionSetRepository.countActionsByStatusForDistributionSet(dsId).stream().map(Statistic.class::cast).toList();
    }

    @Override
    public Long countAutoAssignmentsForDistributionSet(Long dsId) {
        return distributionSetRepository.countAutoAssignmentsForDistributionSet(dsId);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetTagAssignmentResult toggleTagAssignment(final Collection<Long> dsIds, final String tagName) {
        final List<JpaDistributionSet> sets = findDistributionSetListWithDetails(dsIds);

        if (sets.size() < dsIds.size()) {
            throw new EntityNotFoundException(DistributionSet.class, dsIds,
                    sets.stream().map(DistributionSet::getId).toList());
        }

        final DistributionSetTag myTag = distributionSetTagManagement.getByName(tagName)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetTag.class, tagName));

        DistributionSetTagAssignmentResult result;

        final List<JpaDistributionSet> toBeChangedDSs = sets.stream().filter(set -> set.addTag(myTag))
                .collect(Collectors.toList());

        // un-assignment case
        if (toBeChangedDSs.isEmpty()) {
            for (final JpaDistributionSet set : sets) {
                if (set.removeTag(myTag)) {
                    toBeChangedDSs.add(set);
                }
            }
            result = new DistributionSetTagAssignmentResult(dsIds.size() - toBeChangedDSs.size(),
                    Collections.emptyList(),
                    Collections.unmodifiableList(
                            toBeChangedDSs.stream().map(distributionSetRepository::save).toList()),
                    myTag);
        } else {
            result = new DistributionSetTagAssignmentResult(dsIds.size() - toBeChangedDSs.size(),
                    Collections.unmodifiableList(
                            toBeChangedDSs.stream().map(distributionSetRepository::save).toList()),
                    Collections.emptyList(), myTag);
        }

        // no reason to persist the tag
        entityManager.detach(myTag);
        return result;
    }

    private List<JpaDistributionSet> findDistributionSetListWithDetails(final Collection<Long> distributionIdSet) {
        return distributionSetRepository.findAll(DistributionSetSpecification.byIds(distributionIdSet));
    }

    @Override
    @Transactional
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSet update(final DistributionSetUpdate u) {
        final GenericDistributionSetUpdate update = (GenericDistributionSetUpdate) u;
        final JpaDistributionSet set = (JpaDistributionSet) getValid(update.getId());
        Set<RolloutStatus> allowedStatuses = JpaRolloutManagement.ALLOWED_ROLLOUT_STATUSES_FOR_LINKING_AND_UNLINKING;
        List<Rollout> associatedRollouts = rolloutRepository.findAllByDistributionSetId(set.getId());

        boolean canUpdateAll = associatedRollouts.stream().allMatch(rollout ->
                allowedStatuses.contains(rollout.getStatus())
        );

        if (canUpdateAll) {
            update.getName().ifPresent(set::setName);
            update.getDescription().ifPresent(set::setDescription);
            update.getVersion().ifPresent(set::setVersion);
            if (update.isSoftwareDowngradeEnabled() != null) {
                set.setSoftwareDowngradeEnabled(update.isSoftwareDowngradeEnabled());
            }
        } else {
            update.getDescription().ifPresent(set::setDescription);
        }

        if (update.isRequiredMigrationStep() != null && !update.isRequiredMigrationStep().equals(set.isRequiredMigrationStep())) {
            assertDistributionSetIsNotAssignedToTargets(update.getId());
            set.setRequiredMigrationStep(update.isRequiredMigrationStep());
        }

        return distributionSetRepository.save(set);
    }

    private JpaSoftwareModule findSoftwareModuleAndThrowExceptionIfNotFound(final Long moduleId) {
        return softwareModuleRepository.findById(moduleId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, moduleId));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final Collection<Long> distributionSetIDs) {
        final List<DistributionSet> setsFound = get(distributionSetIDs);

        if (setsFound.size() < distributionSetIDs.size()) {
            throw new EntityNotFoundException(DistributionSet.class, distributionSetIDs,
                    setsFound.stream().map(DistributionSet::getId).toList());
        }

        final List<Long> assigned = distributionSetRepository
                .findAssignedToTargetDistributionSetsById(distributionSetIDs);
        assigned.addAll(distributionSetRepository.findAssignedToRolloutDistributionSetsById(distributionSetIDs));

        // soft delete assigned
        if (!assigned.isEmpty()) {
            final Long[] dsIds = assigned.toArray(new Long[assigned.size()]);
            distributionSetRepository.deleteDistributionSet(dsIds);
            targetFilterQueryRepository.unsetAutoAssignDistributionSetAndUserAcceptanceRequired(dsIds);
        }

        // mark the rest as hard delete
        final List<Long> toHardDelete = distributionSetIDs.stream().filter(setId -> !assigned.contains(setId))
                .toList();

        // hard delete the rest if exists
        if (!toHardDelete.isEmpty()) {
            targetFilterQueryRepository
                    .unsetAutoAssignDistributionSetAndUserAcceptanceRequired(toHardDelete.toArray(new Long[toHardDelete.size()]));
            // don't give the delete statement an empty list, JPA/Oracle cannot
            // handle the empty list
            distributionSetRepository.deleteByIdIn(toHardDelete);
        }

        afterCommit.afterCommit(() -> distributionSetIDs.forEach(dsId -> eventPublisherHolder.getEventPublisher()
                .publishEvent(new DistributionSetDeletedEvent(tenantAware.getCurrentTenant(), dsId,
                        JpaDistributionSet.class, eventPublisherHolder.getApplicationId()))));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSet create(final DistributionSetCreate c) {
        final JpaDistributionSetCreate create = (JpaDistributionSetCreate) c;
        if (create.getType() == null) {
            create.type(systemManagement.getTenantMetadata().getDefaultDsType().getKey());
        }

        return distributionSetRepository.save(create.build());
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<DistributionSet> create(final Collection<DistributionSetCreate> creates) {
        return creates.stream().map(this::create).toList();
    }

    @Override
    public void updateDsTargetVersion(Long dsId, String targetVersion) {
        Optional<JpaDistributionSet> distribution = distributionSetRepository.findById(dsId);

        if (distribution.isPresent()) {
            distribution.get().setVersion(targetVersion);
        } else {
            throw new EntityNotFoundException(DistributionSet.class, dsId);
        }
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSet assignSoftwareModules(final long setId, final Collection<DistributionSoftwareVersionWrapper> dsWrappers) {

        assertDistributionSetIsNotAssignedToTargets(setId);
        final JpaDistributionSet set = (JpaDistributionSet) getValid(setId);
        for (DistributionSoftwareVersionWrapper dsWrapper : dsWrappers) {

            /* Validate software module is not assigned to distribution */
            assertSoftwareModuleIsNotAssignedToDistribution(set, dsWrapper);

            SoftwareModule module = softwareModuleRepository.findById(dsWrapper.getSoftwareModule().getId())
                    .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, dsWrapper.getSoftwareModule().getId()));
            ArtifactSoftwareModuleAssociation artifactSoftwareModuleAssociation = artifactsManagement.findFirstBySoftwareModuleIdAndTargetVersionId(module.getId(), dsWrapper.getVersion().getId())
                    .orElseThrow(() -> new EntityNotFoundException(ArtifactSoftwareModuleAssociation.class, module.getId()));

            Version version = artifactSoftwareModuleAssociation.getTargetVersion();
            set.addModule(module, version);
        }
        assertSoftwareModuleQuota(setId, dsWrappers.size());

        return distributionSetRepository.save(set);

    }

    /**
     * Method to validate {@link DistributionSoftwareVersionWrapper#getSoftwareModule()} is not assigned to {@link DistributionSet}
     *
     * @param set       {@link DistributionSet}
     * @param dsWrapper {@link DistributionSoftwareVersionWrapper}
     */
    private void assertSoftwareModuleIsNotAssignedToDistribution(final JpaDistributionSet set, DistributionSoftwareVersionWrapper dsWrapper) {
        IDistributionSetModule dsModule = set.getDistributionSetModules().stream()
                .filter(dsm -> dsm.getSm().getId().equals(dsWrapper.getSoftwareModule().getId())).findAny().orElse(null);
        if (dsModule != null) {
            throw new EntityAlreadyExistsException("Software module " + dsModule.getSm().getName()
                    + " has already assigned with target version " + dsModule.getVersion().getName());
        }
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSet unassignSoftwareModule(final long setId, final long moduleId, final long version) {
        final JpaDistributionSet set = (JpaDistributionSet) getValid(setId);

        // If Distribution is already part of Rollout,
        // then during unlink Rollout must be in one of the stages (CREATING, DRAFT, DELETING, DELETED).
        rolloutManagement.ensureRolloutIsInactiveOrThrow(setId, Constants.UNLINK);

        // Software module ID shall be present
        final JpaSoftwareModule module = findSoftwareModuleAndThrowExceptionIfNotFound(moduleId);

        // Software module target version ID shall be present.
        final JpaVersion vers = findSoftwareModuleTargetVersionAndThrowExceptionIfNotFound(version);

        // Software module target version ID shall belong to the Software module ID
        assertTargetVersionIsLinkedToSoftwareModule(moduleId, vers.getSoftwareModuleId().getId());

        // Software module ID should be already linked to the distribution ID
        assertSoftwareModuleIsAlreadyLinkedToDistributionSet(setId, moduleId);

        assertDistributionSetIsNotAssignedToTargets(setId);
        set.removeModule(module, vers);

        DistributionSet result = distributionSetRepository.save(set);
        entityManager.flush();
        entityManager.clear();
        return result;
    }

    private JpaVersion findSoftwareModuleTargetVersionAndThrowExceptionIfNotFound(long version) {
        return versionRepository.findById(version).orElseThrow(() ->
                new ValidationException("The software module target version id does not exists"));
    }

    private void assertSoftwareModuleIsAlreadyLinkedToDistributionSet(long setId, long moduleId) {
        distributionSetRepository.getDistributionSetByModulesIdAndDsId(moduleId, setId).ifPresentOrElse(distributionSet -> {
                },
                () -> {
                    throw new ValidationException("The software module is not yet linked to the distribution set");
                });
    }

    @Override
    public Slice<DistributionSet> findByDistributionSetFilter(final Pageable pageable,
                                                              final DistributionSetFilter distributionSetFilter) {
        final List<Specification<JpaDistributionSet>> specList = buildDistributionSetSpecifications(
                distributionSetFilter);

        return JpaManagementHelper.findAllWithoutCountBySpec(distributionSetRepository, pageable, specList);
    }

    @Override
    public long countByDistributionSetFilter(@NotNull final DistributionSetFilter distributionSetFilter) {
        final List<Specification<JpaDistributionSet>> specList = buildDistributionSetSpecifications(
                distributionSetFilter);

        return JpaManagementHelper.countBySpec(distributionSetRepository, specList);
    }

    @Override
    public Slice<DistributionSet> findByCompleted(final Pageable pageReq, final Boolean complete) {
        return JpaManagementHelper.findAllWithoutCountBySpec(distributionSetRepository, pageReq,
                buildSpecsByComplete(complete));
    }

    private List<Specification<JpaDistributionSet>> buildSpecsByComplete(final Boolean complete) {
        return complete != null
                ? Arrays.asList(DistributionSetSpecification.isDeleted(false),
                DistributionSetSpecification.isCompleted(complete))
                : Collections.singletonList(DistributionSetSpecification.isDeleted(false));
    }

    @Override
    public long countByCompleted(final Boolean complete) {
        return JpaManagementHelper.countBySpec(distributionSetRepository, buildSpecsByComplete(complete));
    }

    @Override
    public Slice<DistributionSet> findByDistributionSetFilterOrderByLinkedTarget(final Pageable pageable,
                                                                                 final DistributionSetFilter distributionSetFilter, final String assignedOrInstalled) {
        // remove default sort from pageable to not overwrite sorted spec
        final OffsetBasedPageRequest unsortedPage = new OffsetBasedPageRequest(pageable.getOffset(),
                pageable.getPageSize(), Sort.unsorted());

        final List<Specification<JpaDistributionSet>> specList = buildDistributionSetSpecifications(
                distributionSetFilter);
        specList.add(DistributionSetSpecification.orderedByLinkedTarget(assignedOrInstalled, pageable.getSort()));

        return JpaManagementHelper.findAllWithoutCountBySpec(distributionSetRepository, unsortedPage, specList);
    }

    @Override
    public Optional<DistributionSet> getByNameAndVersion(final String distributionName, final String version) {
        final Specification<JpaDistributionSet> spec = DistributionSetSpecification
                .equalsNameAndVersionIgnoreCase(distributionName, version);
        return distributionSetRepository.findOne(spec).map(DistributionSet.class::cast);

    }

    @Override
    public long count() {
        final Specification<JpaDistributionSet> spec = DistributionSetSpecification.isDeleted(Boolean.FALSE);

        return distributionSetRepository.count(SpecificationsBuilder.combineWithAnd(List.of(spec)));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<DistributionSetMetadata> createMetaData(final long dsId, final Collection<MetaData> md) {

        md.forEach(meta -> checkAndThrowIfDistributionSetMetadataAlreadyExists(
                new DsMetadataCompositeKey(dsId, meta.getKey())));

        assertMetaDataQuota(dsId, md.size());

        final JpaDistributionSet set = JpaManagementHelper.touch(entityManager, distributionSetRepository,
                (JpaDistributionSet) getValid(dsId));

        return Collections.unmodifiableList(md.stream()
                .map(meta -> distributionSetMetadataRepository
                        .save(new JpaDistributionSetMetadata(meta.getKey(), set, meta.getValue())))
                .toList());
    }

    private void assertMetaDataQuota(final Long dsId, final int requested) {
        QuotaHelper.assertAssignmentQuota(dsId, requested, quotaManagement.getMaxMetaDataEntriesPerDistributionSet(),
                DistributionSetMetadata.class, DistributionSet.class,
                distributionSetMetadataRepository::countByDistributionSetId);
    }

    private void assertSoftwareModuleQuota(final Long id, final int requested) {
        QuotaHelper.assertAssignmentQuota(id, requested, quotaManagement.getMaxSoftwareModulesPerDistributionSet(),
                SoftwareModule.class, DistributionSet.class, softwareModuleRepository::countByAssignedToId);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSetMetadata updateMetaData(final long dsId, final MetaData md) {

        // check if exists otherwise throw entity not found exception
        final JpaDistributionSetMetadata toUpdate = (JpaDistributionSetMetadata) getMetaDataByDistributionSetId(dsId,
                md.getKey()).orElseThrow(
                () -> new EntityNotFoundException(DistributionSetMetadata.class, dsId, md.getKey()));
        toUpdate.setValue(md.getValue());
        // touch it to update the lock revision because we are modifying the
        // DS indirectly
        JpaManagementHelper.touch(entityManager, distributionSetRepository, (JpaDistributionSet) getValid(dsId));
        return distributionSetMetadataRepository.save(toUpdate);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteMetaData(final long distributionSetId, final String key) {
        final JpaDistributionSetMetadata metadata = (JpaDistributionSetMetadata) getMetaDataByDistributionSetId(
                distributionSetId, key).orElseThrow(
                () -> new EntityNotFoundException(DistributionSetMetadata.class, distributionSetId, key));

        JpaManagementHelper.touch(entityManager, distributionSetRepository,
                (JpaDistributionSet) metadata.getDistributionSet());
        distributionSetMetadataRepository.deleteById(metadata.getId());
    }

    @Override
    public Page<DistributionSetMetadata> findMetaDataByDistributionSetId(final Pageable pageable,
                                                                         final long distributionSetId) {
        throwExceptionIfDistributionSetDoesNotExist(distributionSetId);

        return JpaManagementHelper.findAllWithCountBySpec(distributionSetMetadataRepository, pageable,
                Collections.singletonList(byDsIdSpec(distributionSetId)));
    }

    private Specification<JpaDistributionSetMetadata> byDsIdSpec(final long dsId) {
        return (root, query, cb) -> cb
                .equal(root.get(JpaDistributionSetMetadata_.distributionSet).get(AbstractJpaBaseEntity_.id), dsId);
    }

    @Override
    public long countMetaDataByDistributionSetId(final long setId) {
        throwExceptionIfDistributionSetDoesNotExist(setId);

        return distributionSetMetadataRepository.countByDistributionSetId(setId);
    }

    @Override
    public Page<DistributionSetMetadata> findMetaDataByDistributionSetIdAndRsql(final Pageable pageable,
                                                                                final long distributionSetId, final String rsqlParam) {
        throwExceptionIfDistributionSetDoesNotExist(distributionSetId);

        final List<Specification<JpaDistributionSetMetadata>> specList = Arrays
                .asList(RSQLUtility.buildRsqlSpecification(rsqlParam, DistributionSetMetadataFields.class,
                        virtualPropertyReplacer, database), byDsIdSpec(distributionSetId));

        return JpaManagementHelper.findAllWithCountBySpec(distributionSetMetadataRepository, pageable, specList);
    }

    @Override
    public Optional<DistributionSetMetadata> getMetaDataByDistributionSetId(final long setId, final String key) {
        throwExceptionIfDistributionSetDoesNotExist(setId);

        return distributionSetMetadataRepository.findById(new DsMetadataCompositeKey(setId, key))
                .map(DistributionSetMetadata.class::cast);
    }

    @Override
    public Optional<DistributionSet> getByAction(final long actionId) {
        if (!actionRepository.existsById(actionId)) {
            throw new EntityNotFoundException(Action.class, actionId);
        }

        return Optional.ofNullable(distributionSetRepository.findByActionId(actionId));
    }

    @Override
    public boolean isInUse(final long setId) {
        throwExceptionIfDistributionSetDoesNotExist(setId);

        return actionRepository.countByDistributionSetId(setId) > 0;
    }

    private void assertDistributionSetIsNotAssignedToTargets(final Long distributionSet) {
        if (actionRepository.countByDistributionSetId(distributionSet) > 0) {
            throw new EntityReadOnlyException(String.format(
                    "Distribution set %s is already assigned to targets and cannot be changed", distributionSet));
        }
    }

    private void checkAndThrowIfDistributionSetMetadataAlreadyExists(final DsMetadataCompositeKey metadataId) {
        if (distributionSetMetadataRepository.existsById(metadataId)) {
            throw new EntityAlreadyExistsException(
                    "Metadata entry with key '" + metadataId.getKey() + "' already exists");
        }
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<DistributionSet> assignTag(final Collection<Long> dsIds, final long dsTagId) {
        final List<JpaDistributionSet> allDs = findDistributionSetListWithDetails(dsIds);

        if (allDs.size() < dsIds.size()) {
            throw new EntityNotFoundException(DistributionSet.class, dsIds,
                    allDs.stream().map(DistributionSet::getId).toList());
        }

        final DistributionSetTag distributionSetTag = distributionSetTagManagement.get(dsTagId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetTag.class, dsTagId));

        allDs.forEach(ds -> ds.addTag(distributionSetTag));

        final List<DistributionSet> result = Collections
                .unmodifiableList(allDs.stream().map(distributionSetRepository::save).toList());

        // No reason to save the tag
        entityManager.detach(distributionSetTag);
        return result;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSet unAssignTag(final long dsId, final long dsTagId) {
        final JpaDistributionSet set = (JpaDistributionSet) getWithDetails(dsId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSet.class, dsId));

        final DistributionSetTag distributionSetTag = distributionSetTagManagement.get(dsTagId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetTag.class, dsTagId));

        set.removeTag(distributionSetTag);

        final JpaDistributionSet result = distributionSetRepository.save(set);

        // No reason to save the tag
        entityManager.detach(distributionSetTag);
        return result;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final long setId) {
        throwExceptionIfDistributionSetDoesNotExist(setId);

        delete(Collections.singletonList(setId));
    }

    private void throwExceptionIfDistributionSetDoesNotExist(final Long setId) {
        if (!distributionSetRepository.existsById(setId)) {
            throw new EntityNotFoundException(DistributionSet.class, setId);
        }
    }

    @Override
    public List<DistributionSet> get(final Collection<Long> ids) {
        return Collections.unmodifiableList(distributionSetRepository.findAllById(ids));
    }

    @Override
    public Page<DistributionSet> findByTag(final Pageable pageable, final long tagId) {
        throwEntityNotFoundExceptionIfDsTagDoesNotExist(tagId);

        return JpaManagementHelper.convertPage(distributionSetRepository.findByTag(pageable, tagId), pageable);

    }


    private void throwEntityNotFoundExceptionIfDsTagDoesNotExist(final Long tagId) {
        if (!distributionSetTagRepository.existsById(tagId)) {
            throw new EntityNotFoundException(DistributionSetTag.class, tagId);
        }
    }

    @Override
    public Page<DistributionSet> findByRsqlAndTag(final Pageable pageable, final String rsqlParam, final long tagId) {
        throwEntityNotFoundExceptionIfDsTagDoesNotExist(tagId);

        final List<Specification<JpaDistributionSet>> specList = Arrays.asList(
                RSQLUtility.buildRsqlSpecification(rsqlParam, DistributionSetFields.class, virtualPropertyReplacer,
                        database),
                DistributionSetSpecification.hasTag(tagId), DistributionSetSpecification.isDeleted(false));

        return JpaManagementHelper.findAllWithCountBySpec(distributionSetRepository, pageable, specList);
    }

    @Override
    public Slice<DistributionSet> findAll(final Pageable pageable) {
        return JpaManagementHelper.findAllWithoutCountBySpec(distributionSetRepository, pageable,
                Collections.singletonList(DistributionSetSpecification.isDeleted(false)));
    }

    @Override
    public Page<DistributionSet> findByRsql(final Pageable pageable, final String rsqlParam) {
        final List<Specification<JpaDistributionSet>> specList = Arrays.asList(RSQLUtility
                        .buildRsqlSpecification(rsqlParam, DistributionSetFields.class, virtualPropertyReplacer, database),
                DistributionSetSpecification.isDeleted(false));

        return JpaManagementHelper.findAllWithCountBySpec(distributionSetRepository, pageable, specList);
    }

    @Override
    public Optional<DistributionSet> get(final long id) {
        return distributionSetRepository.findById(id).map(d -> d);
    }

    @Override
    public boolean exists(final long id) {
        return distributionSetRepository.existsById(id);
    }

    @Override
    public DistributionSet getOrElseThrowException(final long id) {
        return get(id).orElseThrow(() -> new EntityNotFoundException(DistributionSet.class, id));
    }

    @Override
    public DistributionSet getValidAndComplete(final long id) {
        final DistributionSet distributionSet = getValid(id);

        if (!distributionSet.isComplete()) {
            throw new IncompleteDistributionSetException("Distribution set of type "
                    + distributionSet.getType().getKey() + " is incomplete: " + distributionSet.getId());
        }

        return distributionSet;
    }

    @Override
    public DistributionSet getValid(final long id) {
        final DistributionSet distributionSet = getOrElseThrowException(id);

        if (!distributionSet.isValid()) {
            throw new InvalidDistributionSetException("Distribution set of type " + distributionSet.getType().getKey()
                    + " is invalid: " + distributionSet.getId());
        }

        return distributionSet;
    }

    @Override
    @Transactional
    public void invalidate(final DistributionSet set) {
        final JpaDistributionSet jpaSet = (JpaDistributionSet) set;
        jpaSet.invalidate();
        distributionSetRepository.save(jpaSet);
    }

    @Override
    public List<IDistributionSetModule> getDistributionSetModule(final long id) {
        getOrElseThrowException(id);
        return Collections.unmodifiableList(distributionSetModuleRepository.findByDsSet(id,
                Sort.by(Sort.Direction.ASC, "sm")));
    }

    @Override
    public List<ArtifactsExpiryDTO> getExpiringArtifactsForGivenDistributionSetBeforeEndDate(final long id, final long endDate) {
        //Converting list of objects to ArtifactExpiryDTO
        List<Object[]> expiringArtifacts = distributionSetModuleRepository.findExpiringArtifactsForADistributionSetBeforeEndDate(id, endDate);
        return expiringArtifacts.stream()
                .map(artifact ->
                        new ArtifactsExpiryDTO((String) artifact[0], (Long) artifact[1], (String) artifact[2]))
                .collect(Collectors.toList());
    }

    @Override
    public List<IDistributionSetModule> findDsModuleBySoftwareModuleIdAndTargetVersionId(final long smId, final long targetVersionId) {
        return Collections.unmodifiableList(distributionSetModuleRepository.findBySoftwareModuleIdAndTargetVersionId(smId, targetVersionId));
    }

    @Override
    public IDistributionSetModule findByDsIdAndSoftwareModuleIdAndTargetVersionId(final long dsSetId, final long smId, final long targetVersionId) {
        return distributionSetModuleRepository.findByDsIdAndSoftwareModuleIdAndTargetVersionId(dsSetId, smId, targetVersionId);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public IDistributionSetModule createDistributionSetModule(final IDistributionSetModule create) {
        final DistributionSetModule distributionSetModule = (DistributionSetModule) create;
        return distributionSetModuleRepository.save(distributionSetModule);
    }


    /**
     * Fetches module IDs and software version IDs for the given rollout ID.
     *
     * @param rolloutId The ID of the rollout.
     * @return A list of Object arrays, where each array contains:
     *         [0] module ID (Long)
     *         [1] software version ID (Long)
     */
    public List<IDistributionSetModule> getModulesAndVersionsByRolloutId(Long rolloutId) {
        return Collections.unmodifiableList(distributionSetModuleRepository.fetchDistributionSetDetailsByRolloutId(rolloutId));
    }




}