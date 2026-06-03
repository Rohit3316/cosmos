/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ValidationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.cosmos.models.mgmt.rollout.constants.RolloutGroupStatus;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutCondition;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutErrorAction;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutSuccessAction;
import org.cosmos.models.mgmt.rolloutgroup.dto.MgmtRolloutGroup;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.RolloutIllegalStateException;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * A collection of static helper methods for the {@link RolloutManagement}
 */
@Component
public final class RolloutHelper {

    private static final Logger LOG = LoggerFactory.getLogger(RolloutHelper.class);
    private static final int CSV_PARSER_BATCH_SIZE = 1000;
    private static final String NOT_SUPPORTED = " is not supported";

    private RolloutHelper() {
    }

    /**
     * Verifies that the required success condition and action are actually set.
     *
     * @param conditions
     *            input conditions and actions
     */
    public static void verifyRolloutGroupConditions(final RolloutGroupConditions conditions) {
        if (conditions.getSuccessCondition() == null) {
            throw new ValidationException("Rollout group is missing success condition");
        }
        if (conditions.getSuccessAction() == null) {
            throw new ValidationException("Rollout group is missing success action");
        }
    }

    /**
     * Verifies that the group has the required success condition and action and a
     * valid target percentage.
     *
     * @param group
     *            the input group
     * @return the verified group
     */
    public static RolloutGroup verifyRolloutGroupHasConditions(final RolloutGroup group) {
        if (group.getTargetPercentage() < 1F || group.getTargetPercentage() > 100F) {
            throw new ValidationException("Target percentage has to be between 1 and 100");
        }
        if (Float.parseFloat(group.getSuccessConditionExp()) < 1F || Float.parseFloat(group.getSuccessConditionExp()) > 100F) {
            throw new ValidationException("Success condition has to be between 1 and 100");
        }
        if (Float.parseFloat(group.getErrorConditionExp()) < 1F || Float.parseFloat(group.getErrorConditionExp()) > 100F) {
            throw new ValidationException("Error condition has to be between 1 and 100");
        }

        if (group.getSuccessCondition() == null) {
            throw new ValidationException("Rollout group is missing success condition");
        }
        if (group.getSuccessAction() == null) {
            throw new ValidationException("Rollout group is missing success action");
        }
        return group;
    }

    /**
     * Verify if the supplied amount of groups is in range
     *
     * @param amountGroup
     *            amount of groups
     * @param quotaManagement
     *            to retrieve maximum number of groups allowed
     */
    public static void verifyRolloutGroupParameter(final int amountGroup, final QuotaManagement quotaManagement) {
        if (amountGroup <= 0) {
            throw new ValidationException("The amount of groups cannot be lower than zero");
        } else if (amountGroup > quotaManagement.getMaxRolloutGroupsPerRollout()) {
            throw new AssignmentQuotaExceededException(
                    "The amount of groups cannot be greater than " + quotaManagement.getMaxRolloutGroupsPerRollout());

        }
    }

    /**
     * Verify that the supplied percentage is in range
     *
     * @param percentage
     *            the percentage
     */
    public static void verifyRolloutGroupTargetPercentage(final float percentage) {
        if (percentage <= 0) {
            throw new ValidationException("The percentage must be greater than zero");
        } else if (percentage > 100) {
            throw new ValidationException("The percentage must not be greater than 100");
        }
    }

    /**
     * Modifies the target filter query to only match targets that were created
     * after the Rollout.
     *
     * @param rollout
     *            Rollout to derive the filter from
     * @return resulting target filter query
     */
    public static String getTargetFilterQuery(final Rollout rollout) {
        return getTargetFilterQuery(rollout.getTargetFilterQuery(), rollout.getCreatedAt());
    }

    /**
     * @param targetFilter
     *            the target filter tp be extended
     * @param createdAt
     *            timestamp
     * @return a target filter query that only matches targets that were created
     *         after the provided timestamp.
     */
    public static String getTargetFilterQuery(final String targetFilter, final Long createdAt) {
        if (createdAt != null) {
            return targetFilter + ";createdat=le=" + createdAt;
        }
        return targetFilter;
    }

    /**
     * Verifies that the given Rollout is in the required status.
     * If the rollout's current status does not match the expected status, an exception is thrown.
     *
     * @param rollout The rollout to check.
     * @param status The expected status of the rollout.
     * @throws RolloutIllegalStateException If the rollout is not in the required status.
     */
    public static void verifyRolloutInStatus(final Rollout rollout, final RolloutStatus status) {
        if (rollout.getStatus() != status) {
            throw new RolloutIllegalStateException(String.format("The requested operation can be performed only when the " +
                    "Rollout is in status %s but the current state of the rollout is %s", status.toString(), rollout.getStatus()));
        }
    }

    /**
     * Verifies that the given Rollout is in the required list of statuses.
     * If the rollout's current status does not match the expected status, an exception is thrown.
     *
     * @param rollout The rollout to check.
     * @param statuses The expected status of the rollout.
     * @throws RolloutIllegalStateException If the rollout is not in the required status.
     */
    public static void verifyRolloutInStatuses(final Rollout rollout, final List<RolloutStatus> statuses) {
        if (!statuses.contains(rollout.getStatus())) {
            throw new RolloutIllegalStateException(String.format("The requested operation can be performed only when the " +
                    "Rollout is in one of the statuses %s but the current state of the rollout is %s", statuses, rollout.getStatus()));
        }
    }

    /**
     * Validates the deletion of a rollout group.
     * Ensures that the rollout exists and that its status matches the specified status.
     * If the validation passes, returns the validated rollout.
     * Otherwise, throws an exception.
     *
     * @param rolloutId The ID of the rollout to validate.
     * @param rolloutManagement The rollout management service used for validation.
     * @param status The expected status of the rollout.
     * @return The validated rollout.
     * @throws EntityNotFoundException If the rollout is not found.
     * @throws RolloutIllegalStateException If the rollout does not match the expected status.
     */


    public static Rollout validateRolloutAndStatus(final Long rolloutId, final RolloutManagement rolloutManagement, RolloutStatus status) {
        Rollout rollout = rolloutManagement.get(rolloutId).orElseThrow(() ->
                new EntityNotFoundException(Rollout.class, rolloutId)
        );
        verifyRolloutInStatus(rollout, status);
        return rollout;
    }

    /**
     * Validates the deletion of a rollout group by ensuring that it belongs to the specified rollout
     * and that its status matches the expected group status.
     * If the validation passes, the validated rollout group is returned.
     * Otherwise, an exception is thrown.
     *
     * @param rolloutId The ID of the rollout.
     * @param rolloutGroupId The ID of the group to be validated.
     * @param rolloutGroupManagement The rollout group management service used for validation.
     * @param groupStatus The expected status of the rollout group.
     * @return The validated rollout group.
     * @throws EntityNotFoundException If the rollout or group is not found.
     * @throws RolloutIllegalStateException If the group is not in the expected status.
     */

    public static RolloutGroup validateRolloutGroupAndStatus(final Long rolloutId, final Long rolloutGroupId,
                                                             final RolloutGroupManagement rolloutGroupManagement, RolloutGroupStatus groupStatus) {
        RolloutGroup rolloutGroup = validateRolloutGroup(rolloutId, rolloutGroupId, rolloutGroupManagement);
        verifyRolloutGroupInStatus(rolloutGroup, groupStatus);
        return rolloutGroup;
    }

    /**
     * Verifies that the Rollout Group is in the required status before proceeding with an operation.
     * If the group is not in the expected status, an exception is thrown.
     *
     * @param rolloutGroup The Rollout Group to be validated.
     * @param status The expected status of the Rollout Group.
     * @throws RolloutIllegalStateException If the rollout group is not in the expected status.
     */

    private static void verifyRolloutGroupInStatus(final RolloutGroup rolloutGroup, final RolloutGroupStatus status) {
        if (rolloutGroup.getStatus() != status) {
            throw new RolloutIllegalStateException(String.format("The requested operation can be performed only when the " +
                    "RolloutGroup is in status %s but the current state of the rollout group is %s", status.toString(), rolloutGroup.getStatus()));
        }
    }

    /**
     * Verifies that the Rollout Groups is in the required status before proceeding with an operation.
     * If the groups are not in the expected status, an exception is thrown.
     *
     * @param rolloutGroups The lis of Rollout Groups of the rollout to be validated.
     * @param status        The expected status of the Rollout Group.
     * @throws RolloutIllegalStateException If the rollout groups are not in the expected status.
     */

    public static void verifyAllRolloutGroupsStatus(final List<RolloutGroup> rolloutGroups, final RolloutGroupStatus status) {
        List<RolloutGroup> fileteredGroupslist = rolloutGroups.stream().filter(g -> status.equals(g.getStatus())).toList();
        if (fileteredGroupslist.isEmpty()) {
            throw new RolloutIllegalStateException(String.format("No rollout groups found to perform the requested operation " +
                    "which are in status %s", status.toString()));
        }
    }

    /**
     * Verifies the rollout is in required state to add device details
     *
     * @param rollout to check the status for.
     */
    public static void verifyRolloutInStatusForAddDeviceDetails(final Rollout rollout) {

        if (rollout.getStatus() == RolloutStatus.FINISHING ||
                rollout.getStatus() == RolloutStatus.FINISHED ||
                rollout.getStatus() == RolloutStatus.CANCELING ||
                rollout.getStatus() == RolloutStatus.CANCELED ||
                rollout.getStatus() == RolloutStatus.DELETING ||
                rollout.getStatus() == RolloutStatus.DELETED) {

            throw new RolloutIllegalStateException("Adding device details in the rollout status - " + rollout.getStatus() + " is not allowed");
        }
    }

    /**
     * Filters the groups of a Rollout to match a specific status and adds a group
     * to the result.
     *
     * @param status
     *            the required status for the groups
     * @param group
     *            the group to add
     * @return list of groups
     */
    public static List<Long> getGroupsByStatusIncludingGroup(final List<RolloutGroup> groups,
                                                             final RolloutGroupStatus status, final RolloutGroup group) {
        return groups.stream().filter(innerGroup -> innerGroup.getStatus() == status || innerGroup.equals(group))
                .map(RolloutGroup::getId).collect(Collectors.toList());
    }

    /**
     * Creates an RSQL expression that matches all targets in the provided groups.
     * Links all target filter queries with OR.
     *
     * @param groups
     *            the rollout groups
     * @return RSQL string without base filter of the Rollout. Can be an empty
     *         string.
     */
    public static String getAllGroupsTargetFilter(final List<RolloutGroup> groups) {
        if (groups.stream().anyMatch(group -> StringUtils.isEmpty(group.getTargetFilterQuery()))) {
            return "";
        }

        return "(" + groups.stream().map(RolloutGroup::getTargetFilterQuery).distinct().sorted()
                .collect(Collectors.joining("),(")) + ")";
    }

    /**
     * Creates an RSQL Filter that matches all targets that are in the provided
     * group and in the provided groups.
     *
     * @param baseFilter
     *            the base filter from the rollout
     * @param groups
     *            the rollout groups
     * @param group
     *            the target group
     * @return RSQL string without base filter of the Rollout. Can be an empty
     *         string.
     */
    public static String getOverlappingWithGroupsTargetFilter(final String baseFilter, final List<RolloutGroup> groups,
                                                              final RolloutGroup group) {
        final String groupFilter = group.getTargetFilterQuery();
        // when any previous group has the same filter as the target group the
        // overlap is 100%
        if (isTargetFilterInGroups(groupFilter, groups)) {
            return concatAndTargetFilters(baseFilter, groupFilter);
        }
        final String previousGroupFilters = getAllGroupsTargetFilter(groups);
        if (!StringUtils.isEmpty(previousGroupFilters)) {
            if (!StringUtils.isEmpty(groupFilter)) {
                return concatAndTargetFilters(baseFilter, groupFilter, previousGroupFilters);
            }
            return concatAndTargetFilters(baseFilter, previousGroupFilters);
        }
        if (!StringUtils.isEmpty(groupFilter)) {
            return concatAndTargetFilters(baseFilter, groupFilter);
        }
        return baseFilter;
    }

    private static boolean isTargetFilterInGroups(final String groupFilter, final List<RolloutGroup> groups) {
        return !StringUtils.isEmpty(groupFilter)
                && groups.stream().anyMatch(prevGroup -> !StringUtils.isEmpty(prevGroup.getTargetFilterQuery())
                && prevGroup.getTargetFilterQuery().equals(groupFilter));
    }

    private static String concatAndTargetFilters(final String... filters) {
        return "(" + Arrays.stream(filters).collect(Collectors.joining(");(")) + ")";
    }

    /**
     * Checks if there are any targets left
     *
     * @param targetCount the count of left targets
     * @return true if any targets are left, false otherwise.
     */
    public static boolean verifyRemainingTargets(final long targetCount) {
        if (targetCount > 0) {
            LOG.debug("{} targets are left and are not part of any rollout group", targetCount);
            return true;

        } else if (targetCount < 0) {
            throw new ValidationException("Rollout groups target count verification failed");
        }
        return false;
    }

    /**
     * @param baseFilter the base filter from the rollout
     * @param group      group for which the filter string should be created
     * @return the final target filter query for a rollout group
     */
    public static String getGroupTargetFilter(final String baseFilter, final RolloutGroup group) {
        if (StringUtils.isEmpty(group.getTargetFilterQuery())) {
            return baseFilter;
        }
        return concatAndTargetFilters(baseFilter, group.getTargetFilterQuery());
    }

    /**
     * Verifies that Rollout must be in draft status
     */
    public static void checkIfRolloutCanFreeze(final Rollout rollout) {
        if (RolloutStatus.DRAFT != rollout.getStatus()) {
            throw new RolloutIllegalStateException("Rollout can only be frozen in state draft but current state is "
                    + rollout.getStatus().name().toLowerCase());
        }
        if (rollout.getTotalTargets() == 0) {
            throw new RolloutIllegalStateException("At least one registered device should be associated with Rollout");
        }
        if (Objects.isNull(rollout.getDistributionSet()) || rollout.getDistributionSet().getDistributionSetModules().isEmpty()) {
            throw new RolloutIllegalStateException("At least one Software Module with Target Version shall be part of the Distribution Set");
        }
        //Artifacts association to Software Module should be covered in Software Rollout Linking API
    }

    public static void checkIfRolloutCanStarted(final Rollout rollout, final Rollout mergedRollout) {
        if (RolloutStatus.READY != mergedRollout.getStatus()) {
            throw new RolloutIllegalStateException("Rollout can only be started in state ready but current state is "
                    + rollout.getStatus().name().toLowerCase());
        }
    }

    /**
     * Parses the provided CSV file and returns a list of controller ids present in it.
     *
     * @param csvFile - Provided csv file that contains controller ids
     * @return list of controller ids
     */
    public static List<String> parseControllerIdsFromCSV(MultipartFile csvFile) {
        List<String> targets = new ArrayList<>();
        LOG.debug("Starting to parse controller IDs from CSV file");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvFile.getInputStream(), StandardCharsets.UTF_8))) {
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);

            List<String> batch = new ArrayList<>();
            boolean firstLine = true;
            for (CSVRecord csvRecord : csvParser) {
                String controllerId = csvRecord.get(0).trim();

                if (firstLine && controllerId.startsWith("\uFEFF")) {
                    controllerId = controllerId.substring(1);
                }
                firstLine = false;

                if (!controllerId.isEmpty()) {
                    batch.add(controllerId);
                }

                //Process the batch when limit is reached
                if (batch.size() == CSV_PARSER_BATCH_SIZE) {
                    targets.addAll(batch);
                    LOG.debug("Processed a batch of size: {}", batch.size());
                    batch.clear();
                }
            }
            //process any remaining records
            if (!batch.isEmpty()) {
                targets.addAll(batch);
                LOG.debug("Processed the final batch of size: {}", batch.size());
            }
        } catch (IOException e) {
            LOG.error("Error reading target devices from the file", e);
            throw new ValidationException("Error reading target devices from the file", e);
        }
        LOG.debug("Finished parsing controller IDs from CSV file");
        return targets;
    }

    /**
     * Convert the groups json from the request to list of {@link MgmtRolloutGroup}
     *
     * @param groupsJson groups details
     * @return list of {@link MgmtRolloutGroup}
     */
    public static List<MgmtRolloutGroup> parseGroupsJson(String groupsJson) {

        if (groupsJson == null) {
            return Collections.emptyList();
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(groupsJson, new TypeReference<List<MgmtRolloutGroup>>() {
            });
        } catch (IOException e) {
            LOG.error("Error parsing groups JSON - {}", e.getMessage());
            throw new ValidationException("Error parsing groups JSON", e);
        }
    }

    /**
     * Build rollout group conditions with default values
     *
     * @return {@link RolloutGroupConditions}
     */
    public static RolloutGroupConditions buildDefaultRolloutGroupConditions() {
        return buildRolloutGroupConditions(new MgmtRolloutGroup(), true);
    }

    /**
     * Build Rollout Group conditions using provided rollout group.
     *
     * @param restRequest  rollout group
     * @param withDefaults if true, use default values in any missing grouping condition
     * @return {@link RolloutGroupConditions}
     */
    public static RolloutGroupConditions buildRolloutGroupConditions(final MgmtRolloutGroup restRequest,
                                                                     final boolean withDefaults) {
        final RolloutGroupConditionBuilder conditions = new RolloutGroupConditionBuilder();

        if (withDefaults) {
            conditions.withDefaults();
        }

        if (restRequest.getSuccessCondition() != null) {
            conditions.successCondition(mapFinishCondition(restRequest.getSuccessCondition().getCondition()),
                    restRequest.getSuccessCondition().getExpression());
        }

        if (restRequest.getSuccessAction() != null) {
            conditions.successAction(map(restRequest.getSuccessAction().getAction()),
                    restRequest.getSuccessAction().getExpression());
        }

        if (restRequest.getErrorCondition() != null) {
            conditions.errorCondition(mapErrorCondition(restRequest.getErrorCondition().getCondition()),
                    restRequest.getErrorCondition().getExpression());
        }

        if (restRequest.getErrorAction() != null) {
            conditions.errorAction(map(restRequest.getErrorAction().getAction()),
                    restRequest.getErrorAction().getExpression());
        }

        LOG.debug("Group conditions for group name: {} are built: {}", restRequest.getName(), conditions);
        return conditions.build();
    }

    /**
     * Validate rollout group conditions
     *
     * @return {@link RolloutGroup}
     */

    private static RolloutGroup validateRolloutGroup(final Long rolloutId, final Long rolloutGroupId, final RolloutGroupManagement rolloutGroupManagement) {

        RolloutGroup rolloutGroup = rolloutGroupManagement.get(rolloutGroupId).orElseThrow(() ->
                new EntityNotFoundException(RolloutGroup.class, rolloutGroupId)
        );

        if (!Objects.equals(rolloutGroup.getRollout().getId(), rolloutId)) {
            throw new EntityNotFoundException(String.format("The given Rollout group %d either does not exist or does not belong to the rollout %d", rolloutGroupId, rolloutId));
        }
        return rolloutGroup;
    }

    private static RolloutGroup.RolloutGroupErrorCondition mapErrorCondition(final MgmtRolloutCondition.Condition condition) {
        if (MgmtRolloutCondition.Condition.THRESHOLD == condition) {
            return RolloutGroup.RolloutGroupErrorCondition.THRESHOLD;
        }
        throw new IllegalArgumentException(createIllegalArgumentLiteral(condition));
    }

    private static RolloutGroup.RolloutGroupSuccessCondition mapFinishCondition(final MgmtRolloutCondition.Condition condition) {
        if (MgmtRolloutCondition.Condition.THRESHOLD == condition) {
            return RolloutGroup.RolloutGroupSuccessCondition.THRESHOLD;
        }
        LOG.error("Unsupported condition - {}", condition);
        throw new IllegalArgumentException(createIllegalArgumentLiteral(condition));
    }

    private static String createIllegalArgumentLiteral(final MgmtRolloutCondition.Condition condition) {
        return "Condition " + condition + NOT_SUPPORTED;
    }

    private static RolloutGroup.RolloutGroupErrorAction map(final MgmtRolloutErrorAction.ErrorAction action) {
        if (MgmtRolloutErrorAction.ErrorAction.PAUSE == action) {
            return RolloutGroup.RolloutGroupErrorAction.PAUSE;
        }
        LOG.error("Unsupported Error Action - {}", action);
        throw new IllegalArgumentException("Error Action " + action + NOT_SUPPORTED);
    }

    private static RolloutGroup.RolloutGroupSuccessAction map(final MgmtRolloutSuccessAction.SuccessAction action) {
        if (MgmtRolloutSuccessAction.SuccessAction.NEXTGROUP == action) {
            return RolloutGroup.RolloutGroupSuccessAction.NEXTGROUP;
        }
        LOG.error("Unsupported Success Action - {}", action);
        throw new IllegalArgumentException("Success Action " + action + NOT_SUPPORTED);
    }
}
