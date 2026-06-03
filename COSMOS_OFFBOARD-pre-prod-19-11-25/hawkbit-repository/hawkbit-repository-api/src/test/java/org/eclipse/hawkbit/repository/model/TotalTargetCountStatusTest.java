/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Component Tests - TotalTargetCountStatus")
@Story("TotalTargetCountStatus should correctly present finished DOWNLOAD_ONLY actions")
public class TotalTargetCountStatusTest {

    private final List<TotalTargetCountActionStatus> targetCountActionStatuses = Arrays.asList(
            new TotalTargetCountActionStatus(DeviceActionStatus.USER_SCHEDULED, 1L),
            new TotalTargetCountActionStatus(DeviceActionStatus.ERROR_RESPONSE_CODE, 2L),
            new TotalTargetCountActionStatus(DeviceActionStatus.FINISHED_SUCCESS, 3L),
            new TotalTargetCountActionStatus(DeviceActionStatus.CANCELED, 4L),
            new TotalTargetCountActionStatus(DeviceActionStatus.DD_SENT, 5L),
            new TotalTargetCountActionStatus(DeviceActionStatus.RUNNING, 6L),
            new TotalTargetCountActionStatus(DeviceActionStatus.CANCELING, 9L),
            new TotalTargetCountActionStatus(DeviceActionStatus.DOWNLOAD_COMPLETED, 10L));

    @Test
    @Description("Different Action Statuses should be correctly mapped to the corresponding " +
            "TotalTargetCountStatus.Status")
    public void shouldCorrectlyMapActionStatuses() {
        TotalTargetCountStatus status = new TotalTargetCountStatus(targetCountActionStatuses, 55L,
                MgmtRolloutUserAcceptanceRequired.NO);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.SCHEDULED)).isEqualTo(1L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.ERROR)).isEqualTo(2L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.FINISHED)).isEqualTo(3L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.CANCELED)).isEqualTo(4L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.RUNNING)).isEqualTo(30L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.NOTSTARTED)).isEqualTo(15L);
        assertThat(status.getFinishedPercent()).isEqualTo((float) 100 * 3 / 55);
    }

    @Test
    @Description("When an empty list is passed to the TotalTargetCountStatus, all actions should be displayed as " +
            "NOTSTARTED")
    public void shouldCorrectlyMapActionStatusesToNotStarted() {
        TotalTargetCountStatus status = new TotalTargetCountStatus(Collections.emptyList(), 55L,
                MgmtRolloutUserAcceptanceRequired.NO);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.SCHEDULED)).isEqualTo(0L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.ERROR)).isEqualTo(0L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.FINISHED)).isEqualTo(0L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.CANCELED)).isEqualTo(0L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.RUNNING)).isEqualTo(0L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.NOTSTARTED)).isEqualTo(55L);
        assertThat(status.getFinishedPercent()).isEqualTo(0);
    }

    @Test
    @Description("Actions should be correctly mapped to FINISHED when user acceptance is required and they have ActionStatus.DOWNLOADED")
    public void shouldCorrectlyMapActionStatusesWhenUserAcceptanceIsRequired() {
        TotalTargetCountStatus status = new TotalTargetCountStatus(targetCountActionStatuses, 55L,
                MgmtRolloutUserAcceptanceRequired.YES);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.SCHEDULED)).isEqualTo(1L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.ERROR)).isEqualTo(2L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.FINISHED)).isEqualTo(13L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.CANCELED)).isEqualTo(4L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.RUNNING)).isEqualTo(20L);
        assertThat(status.getTotalTargetCountByStatus(TotalTargetCountStatus.Status.NOTSTARTED)).isEqualTo(15L);
        assertThat(status.getFinishedPercent()).isEqualTo((float) 100 * 13 / 55);
    }
}
