/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.cosmos.models.mgmt.rollout.constants.RolloutGroupStatus;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RolloutGroupFields;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutHelper;
import org.eclipse.hawkbit.repository.RolloutStatusCache;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction_;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup_;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout_;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.jpa.model.RolloutTargetGroup;
import org.eclipse.hawkbit.repository.jpa.model.RolloutTargetGroup_;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.RolloutGroupsValidation;
import org.eclipse.hawkbit.repository.model.RolloutGroupsValidationWrapper;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetWithActionStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.repository.model.dto.AssociatedTargetsToRolloutGroup;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.ConstraintDeclarationException;
import java.util.function.Function;

import static java.util.Arrays.stream;

/**
 * JPA implementation of {@link RolloutGroupManagement}.
 */
@Validated
@Transactional(readOnly = true)
public class JpaRolloutGroupManagement implements RolloutGroupManagement {

    private static final Logger LOG = LoggerFactory.getLogger(JpaRolloutGroupManagement.class);

    /**
     * Max amount of targets that are handled in one transaction.
     */
    private static final int TRANSACTION_TARGETS = 5_000;
    private static final String DEFAULT_ROLLOUT_GROUP_NAME_SUFFIX = "_default_group";
    private static final String ROLLOUT_GROUP_NAME_SUFFIX = "_group" ;
    private static final String ROLLOUT_GROUP_DESCRIPTION_SUFFIX = "_groupDesc";
    public static final int PAGE_1=1;
    public static final int PAGE_0=0;

    private final RolloutGroupRepository rolloutGroupRepository;

    private final RolloutRepository rolloutRepository;

    private final RolloutTargetGroupRepository rolloutTargetGroupRepository;

    private final TargetManagement targetManagement;

    private final ActionRepository actionRepository;

    private final TargetRepository targetRepository;

    private final QuotaManagement quotaManagement;

    private final EntityManager entityManager;

    private final VirtualPropertyReplacer virtualPropertyReplacer;

    private final RolloutStatusCache rolloutStatusCache;

    private final Database database;

    JpaRolloutGroupManagement(final RolloutGroupRepository rolloutGroupRepository,
            final RolloutRepository rolloutRepository, final ActionRepository actionRepository,
            final TargetRepository targetRepository, final EntityManager entityManager,
            final VirtualPropertyReplacer virtualPropertyReplacer, final RolloutStatusCache rolloutStatusCache,
            final Database database, RolloutTargetGroupRepository rolloutTargetGroupRepository, TargetManagement targetManagement,
                              QuotaManagement quotaManagement) {

        this.rolloutGroupRepository = rolloutGroupRepository;
        this.rolloutRepository = rolloutRepository;
        this.actionRepository = actionRepository;
        this.targetRepository = targetRepository;
        this.entityManager = entityManager;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.rolloutStatusCache = rolloutStatusCache;
        this.database = database;
        this.rolloutTargetGroupRepository = rolloutTargetGroupRepository;
        this.targetManagement = targetManagement;
        this.quotaManagement = quotaManagement;
    }

    @Override
    public Optional<RolloutGroup> get(final long rolloutGroupId) {
        return rolloutGroupRepository.findById(rolloutGroupId).map(RolloutGroup.class::cast);
    }

    @Override
    public Page<RolloutGroup> findByRollout(final Pageable pageable, final long rolloutId) {
        throwEntityNotFoundExceptionIfRolloutDoesNotExist(rolloutId);

        return JpaManagementHelper.convertPage(rolloutGroupRepository.findByRolloutId(rolloutId, pageable), pageable);
    }

    @Override
    public Page<RolloutGroup> findByRolloutAndRsql(final Pageable pageable, final long rolloutId,
            final String rsqlParam) {
        throwEntityNotFoundExceptionIfRolloutDoesNotExist(rolloutId);

        final List<Specification<JpaRolloutGroup>> specList = Arrays.asList(
                RSQLUtility.buildRsqlSpecification(rsqlParam, RolloutGroupFields.class, virtualPropertyReplacer,
                        database),
                (root, query, cb) -> cb.equal(root.get(JpaRolloutGroup_.rollout).get(JpaRollout_.id), rolloutId));

        return JpaManagementHelper.findAllWithCountBySpec(rolloutGroupRepository, pageable, specList);
    }

    private void throwEntityNotFoundExceptionIfRolloutDoesNotExist(final Long rolloutId) {
        if (!rolloutRepository.existsById(rolloutId)) {
            throw new EntityNotFoundException(Rollout.class, rolloutId);
        }
    }

    @Override
    public Page<RolloutGroup> findByRolloutWithDetailedStatus(final Pageable pageable, final long rolloutId) {
        throwEntityNotFoundExceptionIfRolloutDoesNotExist(rolloutId);

        final Page<JpaRolloutGroup> rolloutGroups = rolloutGroupRepository.findByRolloutId(rolloutId, pageable);
        final List<Long> rolloutGroupIds = rolloutGroups.getContent().stream().map(RolloutGroup::getId)
                .collect(Collectors.toList());

        if (rolloutGroupIds.isEmpty()) {
            // groups might already deleted, so return empty list.
            return new PageImpl<>(Collections.emptyList());
        }

        final Map<Long, List<TotalTargetCountActionStatus>> allStatesForRollout = getStatusCountItemForRolloutGroup(
                rolloutGroupIds);

        for (final JpaRolloutGroup rolloutGroup : rolloutGroups) {
            final TotalTargetCountStatus totalTargetCountStatus = new TotalTargetCountStatus(
                    allStatesForRollout.get(rolloutGroup.getId()), Long.valueOf(rolloutGroup.getTotalTargets()),
                    rolloutGroup.getRollout().getUserAcceptanceRequired());
            rolloutGroup.setTotalTargetCountStatus(totalTargetCountStatus);
        }

        return JpaManagementHelper.convertPage(rolloutGroups, pageable);
    }

    @Override
    public Page<RolloutGroup> findByRolloutAndRsqlWithDetailedStatus(final Pageable pageable, final long rolloutId,
            final String rsqlParam) {
        throwEntityNotFoundExceptionIfRolloutDoesNotExist(rolloutId);

        final Page<RolloutGroup> rolloutGroups = findByRolloutAndRsql(pageable, rolloutId, rsqlParam);
        final List<Long> rolloutGroupIds = rolloutGroups.getContent().stream().map(RolloutGroup::getId)
                .collect(Collectors.toList());

        if (rolloutGroupIds.isEmpty()) {
            // groups might already deleted, so return empty list.
            return new PageImpl<>(Collections.emptyList());
        }

        final Map<Long, List<TotalTargetCountActionStatus>> allStatesForRollout = getStatusCountItemForRolloutGroup(
                rolloutGroupIds);

        for (final RolloutGroup rolloutGroup : rolloutGroups) {
            final TotalTargetCountStatus totalTargetCountStatus = new TotalTargetCountStatus(
                    allStatesForRollout.get(rolloutGroup.getId()), Long.valueOf(rolloutGroup.getTotalTargets()),
                    rolloutGroup.getRollout().getUserAcceptanceRequired());
            ((JpaRolloutGroup) rolloutGroup).setTotalTargetCountStatus(totalTargetCountStatus);
        }

        return JpaManagementHelper.convertPage(rolloutGroups, pageable);
    }

    @Override
    public Optional<RolloutGroup> getWithDetailedStatus(final long rolloutGroupId) {
        final Optional<RolloutGroup> rolloutGroup = get(rolloutGroupId);

        if (!rolloutGroup.isPresent()) {
            return rolloutGroup;
        }

        final JpaRolloutGroup jpaRolloutGroup = (JpaRolloutGroup) rolloutGroup.get();

        List<TotalTargetCountActionStatus> rolloutStatusCountItems = rolloutStatusCache
                .getRolloutGroupStatus(rolloutGroupId);

        if (CollectionUtils.isEmpty(rolloutStatusCountItems)) {
            rolloutStatusCountItems = actionRepository.getStatusCountByRolloutGroupId(rolloutGroupId, true);
            rolloutStatusCache.putRolloutGroupStatus(rolloutGroupId, rolloutStatusCountItems);
        }

        final TotalTargetCountStatus totalTargetCountStatus = new TotalTargetCountStatus(rolloutStatusCountItems,
                Long.valueOf(jpaRolloutGroup.getTotalTargets()), jpaRolloutGroup.getRollout().getUserAcceptanceRequired());
        jpaRolloutGroup.setTotalTargetCountStatus(totalTargetCountStatus);
        return rolloutGroup;

    }

    private Map<Long, List<TotalTargetCountActionStatus>> getStatusCountItemForRolloutGroup(final List<Long> groupIds) {
        final Map<Long, List<TotalTargetCountActionStatus>> fromCache = rolloutStatusCache
                .getRolloutGroupStatus(groupIds);

        final List<Long> rolloutGroupIds = groupIds.stream().filter(id -> !fromCache.containsKey(id))
                .collect(Collectors.toList());

        if (!rolloutGroupIds.isEmpty()) {
            final List<TotalTargetCountActionStatus> resultList = actionRepository
                    .getStatusCountByRolloutGroupId(rolloutGroupIds, true);
            final Map<Long, List<TotalTargetCountActionStatus>> fromDb = resultList.stream()
                    .collect(Collectors.groupingBy(TotalTargetCountActionStatus::getId));

            rolloutStatusCache.putRolloutGroupStatus(fromDb);

            fromCache.putAll(fromDb);
        }

        return fromCache;
    }

    @Override
    public Page<Target> findTargetsOfRolloutGroupByRsql(final Pageable pageable, final long rolloutGroupId,
            final String rsqlParam) {
        throwExceptionIfRolloutGroupDoesNotExist(rolloutGroupId);

        final List<Specification<JpaTarget>> specList = Arrays.asList(
                RSQLUtility.buildRsqlSpecification(rsqlParam, TargetFields.class, virtualPropertyReplacer, database),
                (root, query, cb) -> {
                    final ListJoin<JpaTarget, RolloutTargetGroup> rolloutTargetJoin = root
                            .join(JpaTarget_.rolloutTargetGroup);
                    return cb.equal(rolloutTargetJoin.get(RolloutTargetGroup_.rolloutGroup).get(JpaRolloutGroup_.id),
                            rolloutGroupId);
                });

        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, pageable, specList);
    }

    @Override
    public Page<Target> findTargetsOfRolloutGroup(final Pageable page, final long rolloutGroupId) {
        final JpaRolloutGroup rolloutGroup = rolloutGroupRepository.findById(rolloutGroupId)
                .orElseThrow(() -> new EntityNotFoundException(RolloutGroup.class, rolloutGroupId));

        if (isRolloutStatusReadyOrFreezing(rolloutGroup)) {
            // in case of status ready the action has not been created yet and
            // the relation information between target and rollout-group is
            // stored in the #TargetRolloutGroup.
            return JpaManagementHelper.findAllWithCountBySpec(targetRepository, page,
                    Collections.singletonList(TargetSpecifications.isInRolloutGroup(rolloutGroupId)));
        }
        return JpaManagementHelper.findAllWithCountBySpec(targetRepository, page,
                Collections.singletonList(TargetSpecifications.isInActionRolloutGroup(rolloutGroupId)));
    }

    private static boolean isRolloutStatusReadyOrFreezing(final RolloutGroup rolloutGroup) {
        return rolloutGroup != null && ((RolloutStatus.DRAFT == rolloutGroup.getRollout().getStatus())|| RolloutStatus.FREEZING.equals(rolloutGroup.getRollout().getStatus()));
    }

    @Override
    public Page<TargetWithActionStatus> findAllTargetsOfRolloutGroupWithActionStatus(final Pageable pageRequest,
            final long rolloutGroupId) {

        throwExceptionIfRolloutGroupDoesNotExist(rolloutGroupId);

        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        final Root<RolloutTargetGroup> targetRoot = query.distinct(true).from(RolloutTargetGroup.class);
        final Join<RolloutTargetGroup, JpaTarget> targetJoin = targetRoot.join(RolloutTargetGroup_.target);
        final ListJoin<RolloutTargetGroup, JpaAction> actionJoin = targetRoot.join(RolloutTargetGroup_.actions,
                JoinType.LEFT);

        final CriteriaQuery<Object[]> multiselect = query
                .multiselect(targetJoin, actionJoin.get(JpaAction_.actionStatus),
                        actionJoin.get(JpaAction_.lastActionStatusCode))
                .where(getRolloutGroupTargetWithRolloutGroupJoinCondition(rolloutGroupId, cb, targetRoot))
                .orderBy(getOrderBy(pageRequest, cb, targetJoin, actionJoin));
        final List<TargetWithActionStatus> targetWithActionStatus = entityManager.createQuery(multiselect)
                .setFirstResult((int) pageRequest.getOffset()).setMaxResults(pageRequest.getPageSize()).getResultList()
                .stream().map(this::getTargetWithActionStatusFromQuery).collect(Collectors.toList());

        return new PageImpl<>(targetWithActionStatus, pageRequest, 0);
    }

    private Predicate getRolloutGroupTargetWithRolloutGroupJoinCondition(final long rolloutGroupId,
            final CriteriaBuilder cb, final Root<RolloutTargetGroup> targetRoot) {
        return cb.equal(targetRoot.get(RolloutTargetGroup_.rolloutGroup).get(JpaRolloutGroup_.id), //
                rolloutGroupId);
    }

    private TargetWithActionStatus getTargetWithActionStatusFromQuery(final Object[] o) {
        return new TargetWithActionStatus((Target) o[0], DeviceActionStatus.valueOf(((JpaActionStatus) o[1]).getStatus().name()), (Integer) o[2]);
    }

    private List<Order> getOrderBy(final Pageable pageRequest, final CriteriaBuilder cb,
            final Join<RolloutTargetGroup, JpaTarget> targetJoin,
            final ListJoin<RolloutTargetGroup, JpaAction> actionJoin) {

        return pageRequest.getSort().get().flatMap(order -> {
            final List<Order> orders;
            final String property = order.getProperty();
            // we consider status, last_action_status_code as property from
            // JpaAction ...
            if ("status".equals(property) || "lastActionStatusCode".equals(property)) {
                orders = QueryUtils.toOrders(Sort.by(order.getDirection(), property), actionJoin, cb);
            }
            // ... and every other property from JpaTarget
            else {
                orders = QueryUtils.toOrders(Sort.by(order.getDirection(), property), targetJoin, cb);
            }
            return orders.stream();
        }).collect(Collectors.toList());
    }

    @Override
    public long countTargetsOfRolloutsGroup(final long rolloutGroupId) {
        throwExceptionIfRolloutGroupDoesNotExist(rolloutGroupId);

        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        final Root<RolloutTargetGroup> countQueryFrom = countQuery.from(RolloutTargetGroup.class);
        countQuery.select(cb.count(countQueryFrom))
                .where(getRolloutGroupTargetWithRolloutGroupJoinCondition(rolloutGroupId, cb, countQueryFrom));
        return entityManager.createQuery(countQuery).getSingleResult();
    }

    private void throwExceptionIfRolloutGroupDoesNotExist(final Long rolloutGroupId) {
        if (!rolloutGroupRepository.existsById(rolloutGroupId)) {
            throw new EntityNotFoundException(RolloutGroup.class, rolloutGroupId);
        }
    }

    @Override
    public long countByRollout(final long rolloutId) {
        throwEntityNotFoundExceptionIfRolloutDoesNotExist(rolloutId);

        return rolloutGroupRepository.countByRolloutId(rolloutId);
    }

    @Override
    public RolloutGroup createDefaultRolloutGroup(Rollout rollout) {
        JpaRolloutGroup parentGroup = rolloutGroupRepository.findTopByRolloutIdOrderByIdDesc(rollout.getId(), PageRequest.of(PAGE_0,PAGE_1)).stream().findFirst().orElse(null);        //Validate all the conditions for group creation
        RolloutHelper.verifyRolloutGroupParameter(1, quotaManagement);
        assertTargetsPerRolloutGroupQuota(rollout.getTotalTargets());

        RolloutGroupConditions defaultConditions = RolloutHelper.buildDefaultRolloutGroupConditions();

        long groupCountCreated = countByRollout(rollout.getId()) + 1;
        RolloutGroup createdGroup = createRolloutGroup(rollout,
                rollout.getName() + DEFAULT_ROLLOUT_GROUP_NAME_SUFFIX + "_" + groupCountCreated,
                rollout.getName() + ROLLOUT_GROUP_DESCRIPTION_SUFFIX + "_" + groupCountCreated,

                false, defaultConditions, parentGroup,
                100, null, null);

        LOG.debug("Created RolloutGroup: {}", createdGroup.getName());
        final JpaRollout jpaRollout = (JpaRollout) rollout;
        jpaRollout.setRolloutGroupsCreated((int) groupCountCreated);
        rolloutRepository.save(jpaRollout);
        return createdGroup;
    }

    @Override
    public List<RolloutGroup> createRolloutGroups(final List<RolloutGroup> groupList, final Rollout rollout, final List<String> controllerIds) {

        final JpaRollout savedRollout = (JpaRollout) rollout;
        final DistributionSetType distributionSetType = savedRollout.getDistributionSet().getType();
        RolloutGroupConditions defaultConditions = RolloutHelper.buildDefaultRolloutGroupConditions();  //default condition

        //Checking if the groups have all the conditions provided or using default conditions
        //Verifying rollout groups have valid conditions
        // prepare the groups
        final List<RolloutGroup> groups = groupList.stream()
                .map(group -> (RolloutGroup) prepareRolloutGroupWithConditions(group, defaultConditions, null)).toList();
        groups.forEach(RolloutHelper::verifyRolloutGroupHasConditions);//ensure target percentage is valid in each group

        RolloutGroupsValidationWrapper groupsValidationResult = calculateRemainingTargets(groups, savedRollout.getTargetFilterQuery(), distributionSetType.getId(), controllerIds);
        boolean isRemainingTargets = RolloutHelper.verifyRemainingTargets(groupsValidationResult.getRemainingTargets());

        // check if we need to enforce the 'max targets per group' quota
        if (quotaManagement.getMaxTargetsPerRolloutGroup() > 0) {
            validateTargetsInGroups(groups, savedRollout.getTargetFilterQuery(), savedRollout.getCreatedAt(),
                    distributionSetType.getId(), controllerIds).getValidation().getTargetsPerGroup().forEach(this::assertTargetsPerRolloutGroupQuota);
        }

        // create and persist the groups (w/o filling them with targets)
        List<RolloutGroup> createdGroups = new ArrayList<>();
        JpaRolloutGroup lastSavedGroup = rolloutGroupRepository.findTopByRolloutIdOrderByIdDesc(rollout.getId(), PageRequest.of(PAGE_0,PAGE_1)).stream().findFirst().orElse(null);
        boolean isNextGroup = false;
        int autoIncrementNum = 0;
        // Iterate over nonEmptyGroups and create only groups with targets.
        for (final RolloutGroup srcGroup : groupsValidationResult.getNonEmptyGroups()) {
            //create group
            String rolloutGroupName = srcGroup.getName();
            Optional<RolloutGroup> rolloutGroup = rolloutGroupRepository.findByNameAndRolloutId(srcGroup.getName(), rollout.getId());
            if(rolloutGroup.isPresent()) {
                if(isNextGroup)
                    autoIncrementNum++;
                rolloutGroupName = createRolloutGroupName(rollout, autoIncrementNum);
            }
            JpaRolloutGroup group = (JpaRolloutGroup) createRolloutGroup(rollout, rolloutGroupName,
                    srcGroup.getDescription(), srcGroup.isConfirmationRequired(),
                    defaultConditions, lastSavedGroup, srcGroup.getTargetPercentage(),
                    srcGroup.getTargetFilterQuery(), srcGroup);

            lastSavedGroup = rolloutGroupRepository.save(group);
            createdGroups.add(lastSavedGroup);
            isNextGroup = true;
        }
        entityManager.flush();

        //create a default group to add any remaining targets to it
        if(isRemainingTargets) {
            LOG.debug("Creating a default group for remaining targets");
            createdGroups.add(createDefaultRolloutGroup(rollout));
        }
        entityManager.flush();
        savedRollout.setRolloutGroupsCreated((int) countByRollout(rollout.getId()));
        rolloutRepository.save(savedRollout);

        // Log the completion of the rollout group creation
        LOG.info("Created {} RolloutGroups for Rollout: {}", createdGroups.size(), rollout.getId());

        return createdGroups;
    }


    private String createRolloutGroupName(Rollout rollout, int numberAuto) {
        long groupCountCreated = countByRollout(rollout.getId()) + numberAuto;
        return rollout.getName() + ROLLOUT_GROUP_NAME_SUFFIX + "_" + (++groupCountCreated);
    }

    /**
     * Enforces the quota defining the maximum number of {@link Target}s per
     * {@link RolloutGroup}.
     *
     * @param requested number of targets to check
     */
    private void assertTargetsPerRolloutGroupQuota(final long requested) {
        final int quota = quotaManagement.getMaxTargetsPerRolloutGroup();
        QuotaHelper.assertAssignmentQuota(requested, quota, Target.class, RolloutGroup.class);
    }

    @Override
    public List<AssociatedTargetsToRolloutGroup> assignTargetsToRolloutGroups(final Rollout rollout1, final List<RolloutGroup> rolloutGroups) {
        JpaRollout rollout = (JpaRollout) rollout1;
        int draftGroups = 0;
        int totalTargets = (int) rollout1.getTotalTargets();
        List<AssociatedTargetsToRolloutGroup> associatedRolloutGroups = new ArrayList<>();

        LOG.debug("Starting to assign targets to rollout groups for rollout: {}", rollout.getId());

        for (final RolloutGroup group : rolloutGroups) {
            LOG.debug("Processing rollout group: {}", group.getId());
            //if any provided group is already in draft state, just count the total targets and continue ahead.
            if (RolloutGroupStatus.DRAFT == group.getStatus()) {
                LOG.debug("Rollout group {} is already in DRAFT status", group.getId());
                draftGroups++;
                totalTargets += group.getTotalTargets();
                continue;
            }

            //Fill the targets to the provided group and update the association list.
            // Also update the total targets variable after associating.
            final AssociatedTargetsToRolloutGroup associatedRolloutGroup = fillRolloutGroupWithTargets(rollout, group);
            entityManager.flush();
            associatedRolloutGroups.add(associatedRolloutGroup);
            final RolloutGroup filledGroup = associatedRolloutGroup.getRolloutGroup();
            if (RolloutGroupStatus.DRAFT == filledGroup.getStatus()) {
                draftGroups++;
                totalTargets += filledGroup.getTotalTargets();
            }
            LOG.debug("Rollout group {} processed and updated", group.getId());
        }

        // When all groups are in draft then update the rollout with total targets
        if (draftGroups == rolloutGroups.size()) {
            LOG.debug("All rollout groups are in DRAFT status. Updating total targets for rollout: {}", rollout.getId());
            rollout.setTotalTargets(totalTargets);
            rolloutRepository.save(rollout);
        }
        LOG.debug("Completed assigning targets to rollout groups for rollout: {}", rollout.getId());
        return associatedRolloutGroups;
    }

    /**
     * This method fills the provided  group with the targets using the target filter
     * provided in the rollout and rollout group.
     *
     * @param rollout rollout with targets
     * @param rolloutGroup group to fill targets in.
     * @return {@link AssociatedTargetsToRolloutGroup} association details
     */
    private AssociatedTargetsToRolloutGroup fillRolloutGroupWithTargets(final JpaRollout rollout, final RolloutGroup rolloutGroup) {
        RolloutHelper.verifyRolloutInStatusForAddDeviceDetails(rollout);

        final JpaRolloutGroup group = (JpaRolloutGroup) rolloutGroup;
        final String baseFilter = rollout.getTargetFilterQuery();
        final String groupTargetFilter;

        if (StringUtils.isEmpty(group.getTargetFilterQuery())) {
            groupTargetFilter = baseFilter;
        } else {
            groupTargetFilter = baseFilter + ";" + group.getTargetFilterQuery();
        }

        LOG.debug("Group target filter: {}", groupTargetFilter);

        final List<Long> readyGroups = RolloutHelper.getGroupsByStatusIncludingGroup(rollout.getRolloutGroups(), RolloutGroupStatus.DRAFT, group);
        final long targetsInGroupFilter = targetManagement.countByRsqlAndNotInRolloutGroupsAndCompatible(readyGroups, groupTargetFilter, rollout.getDistributionSet().getType());
        final long expectedInGroup = Math.round((double) (group.getTargetPercentage() / 100) * (double) targetsInGroupFilter);
        final long currentlyInGroup = rolloutTargetGroupRepository.countByRolloutGroup(group);

        // Switch the Group status to DRAFT, when there are enough Targets in
        // the Group
        if (currentlyInGroup >= expectedInGroup) {
            group.setStatus(RolloutGroupStatus.DRAFT);

            //That means no new targets have been associated, so return the group with empty targets.
            return AssociatedTargetsToRolloutGroup.builder().rolloutGroup(rolloutGroupRepository.save(group)).targets(Collections.emptyList()).build();
        }

        long targetsLeftToAdd = expectedInGroup - currentlyInGroup;

        List<Target> assignedTargets = new ArrayList<>();
        do {
            assignedTargets.addAll(assignTargetsToGroup(rollout, group, groupTargetFilter, Math.min(TRANSACTION_TARGETS, targetsLeftToAdd)));
            targetsLeftToAdd -= assignedTargets.size();
        } while (targetsLeftToAdd > 0);


        group.setStatus(RolloutGroupStatus.DRAFT);
        group.setTotalTargets(assignedTargets.size());
        LOG.debug("Group status set to DRAFT with total assigned targets: {}", assignedTargets.size());
        return AssociatedTargetsToRolloutGroup.builder().rolloutGroup(rolloutGroupRepository.save(group)).targets(assignedTargets).build();
    }

    /**
     * Assigns targets to the specified rollout group based on the provided target filter.
     *
     * @param rollout the rollout containing the targets
     * @param group the rollout group to which targets will be assigned
     * @param targetFilter the filter to apply when selecting targets
     * @param limit the maximum number of targets to assign
     * @return a list of assigned targets
     */
    private List<Target> assignTargetsToGroup(final JpaRollout rollout, final RolloutGroup group,
                                              final String targetFilter, final long limit) {

        final PageRequest pageRequest = PageRequest.of(0, Math.toIntExact(limit));
        final List<Long> readyGroups = RolloutHelper.getGroupsByStatusIncludingGroup(rollout.getRolloutGroups(),
                RolloutGroupStatus.DRAFT, group);

        final Slice<Target> targets = targetManagement.findByTargetFilterQueryAndNotInRolloutGroupsAndCompatible(
                pageRequest, readyGroups, targetFilter, rollout.getDistributionSet().getType());

        targets.forEach(target -> {
            rolloutTargetGroupRepository.save(new RolloutTargetGroup(group, target));
            LOG.debug("Assigned target {} to group {}", target.getId(), group.getId());
        });

        return targets.getContent();
    }

    private RolloutGroup createRolloutGroup(Rollout rollout, String groupName, String description,
                                            boolean isConfirmationRequired,
                                            RolloutGroupConditions conditions,
                                            RolloutGroup parentGroup,
                                            float targetPercentage,
                                            String targetFilterQuery, RolloutGroup rolloutGroup){

        RolloutHelper.verifyRolloutGroupConditions(conditions);

        final JpaRolloutGroup group = new JpaRolloutGroup();
        group.setName(groupName);
        group.setDescription(description);
        group.setRollout(rollout);
        group.setParent(parentGroup);
        group.setStatus(RolloutGroupStatus.CREATING);
        group.setConfirmationRequired(isConfirmationRequired);
        group.setTargetPercentage(targetPercentage);
        group.setTargetFilterQuery(targetFilterQuery != null ? targetFilterQuery : "");
        prepareRolloutGroupWithConditions(group, conditions, rolloutGroup);

        LOG.debug("Created a rollout group {}", group);
        return rolloutGroupRepository.save(group);
    }

    /**
     * If rollout groups does not have conditions, fill with default grouping conditions
     *
     * @param restGroup rollout group
     * @param conditions grouping conditions
     * @return {@link JpaRolloutGroup}
     */
    private static JpaRolloutGroup prepareRolloutGroupWithConditions(final RolloutGroup restGroup,
                                                                     final RolloutGroupConditions conditions, RolloutGroup rolloutGroup) {

        final JpaRolloutGroup group = (JpaRolloutGroup) restGroup;
        LOG.debug("Following are grouping conditions for the rollout group: {}", group.getName());

        if (group.getSuccessCondition() == null) {
            group.setSuccessCondition(conditions.getSuccessCondition());
        }
        LOG.debug("Success condition {}", group.getSuccessCondition());

        if (group.getSuccessConditionExp() == null) {
            group.setSuccessConditionExp(conditions.getSuccessConditionExp());
        }
        LOG.debug("Success expression {}", group.getSuccessCondition());

        if (group.getSuccessAction() == null) {
            group.setSuccessAction(conditions.getSuccessAction());
        }
        LOG.debug("Success action {}", group.getSuccessCondition());

        if (group.getSuccessActionExp() == null) {
            group.setSuccessActionExp(conditions.getSuccessActionExp());
        }
        LOG.debug("Success action expression {}", group.getSuccessCondition());

        if (group.getErrorCondition() == null) {
            group.setErrorCondition(conditions.getErrorCondition());
        }
        LOG.debug("Error condition {}", group.getSuccessCondition());

        if (group.getErrorConditionExp() == null) {
            group.setErrorConditionExp(conditions.getErrorConditionExp());
        }
        LOG.debug("Error expression {}", group.getSuccessCondition());

        if (group.getErrorAction() == null) {
            group.setErrorAction(conditions.getErrorAction());
        }
        LOG.debug("Error action {}", group.getSuccessCondition());

        if (group.getErrorActionExp() == null) {
            group.setErrorActionExp(conditions.getErrorActionExp());
        }
        if(rolloutGroup != null){
            if(rolloutGroup.getSuccessConditionExp() != null)
                group.setSuccessConditionExp(rolloutGroup.getSuccessConditionExp());
            if(rolloutGroup.getErrorConditionExp()  != null)
                group.setErrorConditionExp(rolloutGroup.getErrorConditionExp());
        }
        LOG.debug("Error action expression {}", group.getSuccessCondition());

        return group;
    }

    private RolloutGroupsValidationWrapper calculateRemainingTargets(final List<RolloutGroup> groups, final String targetFilter, final Long dsTypeId, List<String> controllerIds) {
        final long totalTargets = targetManagement.countByRsqlAndInControllerIdAndCompatible(targetFilter, controllerIds, dsTypeId);
        if (totalTargets == 0) {
            LOG.error("Rollout target filter does not match any targets");
            throw new ConstraintDeclarationException("Rollout target filter does not match any targets");
        }

        final RolloutGroupsValidationWrapper validationResult = validateTargetsInGroups(groups, targetFilter, totalTargets, dsTypeId, controllerIds);

        var remainingTargets = totalTargets - validationResult.getValidation().getTargetsInGroups();
        LOG.debug("No. of remaining targets are {} ", remainingTargets);
        validationResult.setRemainingTargets(remainingTargets);
        return validationResult;
    }

    private RolloutGroupsValidationWrapper validateTargetsInGroups(final List<RolloutGroup> groups, final String baseFilter,
                                                                   final long totalTargets, final Long dsTypeId, List<String> controllerIds) {

        List<RolloutGroup> nonEmptyGroups = new ArrayList<>(groups.size());
        final List<Long> groupTargetCounts = new ArrayList<>(groups.size());
        final Map<String, Long> targetFilterCounts = groups.stream()
                .map(group -> RolloutHelper.getGroupTargetFilter(baseFilter, group)).distinct()
                .collect(Collectors.toMap(Function.identity(),
                        groupTargetFilter -> targetManagement.countByRsqlAndInControllerIdAndCompatible(groupTargetFilter, controllerIds, dsTypeId)));

        long unusedTargetsCount = 0;

        for (int i = 0; i < groups.size(); i++) {
            final RolloutGroup group = groups.get(i);
            final String groupTargetFilter = RolloutHelper.getGroupTargetFilter(baseFilter, group);
            RolloutHelper.verifyRolloutGroupTargetPercentage(group.getTargetPercentage());

            final long targetsInGroupFilter = targetFilterCounts.get(groupTargetFilter);
            final long overlappingTargets = countOverlappingTargetsWithPreviousGroups(baseFilter, groups, group, i,
                    targetFilterCounts);

            final long realTargetsInGroup;
            // Assume that targets which were not used in the previous groups
            // are used in this group
            if (overlappingTargets > 0 && unusedTargetsCount > 0) {
                realTargetsInGroup = targetsInGroupFilter - overlappingTargets + unusedTargetsCount;
                unusedTargetsCount = 0;
            } else {
                realTargetsInGroup = targetsInGroupFilter - overlappingTargets;
            }

            final long reducedTargetsInGroup = Math
                    .round(group.getTargetPercentage() / 100 * (double) realTargetsInGroup);

            //Do not create groups with Zero targets
            //Add groups with atleast one target to the nonEmptyGroups list.
            if (reducedTargetsInGroup > 0) {
                nonEmptyGroups.add(group);
            }

            groupTargetCounts.add(reducedTargetsInGroup);
            unusedTargetsCount += realTargetsInGroup - reducedTargetsInGroup;

        }

        RolloutGroupsValidation validation = new RolloutGroupsValidation(totalTargets, groupTargetCounts);
        LOG.debug("Rollout groups validation: {}", validation);
        return RolloutGroupsValidationWrapper.builder().validation(validation).nonEmptyGroups(nonEmptyGroups).build();
    }

    private long countOverlappingTargetsWithPreviousGroups(final String baseFilter, final List<RolloutGroup> groups,
                                                           final RolloutGroup group, final int groupIndex, final Map<String, Long> targetFilterCounts) {
        // there can't be overlapping targets in the first group
        if (groupIndex == 0) {
            return 0;
        }
        final List<RolloutGroup> previousGroups = groups.subList(0, groupIndex);
        final String overlappingTargetsFilter = RolloutHelper.getOverlappingWithGroupsTargetFilter(baseFilter,
                previousGroups, group);

        if (targetFilterCounts.containsKey(overlappingTargetsFilter)) {
            return targetFilterCounts.get(overlappingTargetsFilter);
        } else {
            final long overlappingTargets = targetManagement.countByRsql(overlappingTargetsFilter);
            targetFilterCounts.put(overlappingTargetsFilter, overlappingTargets);
            return overlappingTargets;
        }
    }

    public void saveRolloutGroupStatus(List<JpaRolloutGroup> rolloutGroups){
        rolloutGroupRepository.saveAll(rolloutGroups);
    }
}
