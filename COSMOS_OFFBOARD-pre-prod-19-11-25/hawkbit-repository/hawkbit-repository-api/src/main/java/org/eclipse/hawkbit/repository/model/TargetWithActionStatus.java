/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.eclipse.hawkbit.repository.Identifiable;

/**
 *
 * Target with action status.
 *
 */
public class TargetWithActionStatus implements Identifiable<Long> {

    private Target target;

    private DeviceActionStatus status;

    private Integer lastActionStatusCode;

    public TargetWithActionStatus(final Target target) {
        this.target = target;
    }

    public TargetWithActionStatus(final Target target, final DeviceActionStatus status) {
        this.status = status;
        this.target = target;
    }

    public TargetWithActionStatus(final Target target, final DeviceActionStatus status, final Integer lastActionStatusCode) {
        this.status = status;
        this.target = target;
        this.lastActionStatusCode = lastActionStatusCode;
    }

    public Target getTarget() {
        return target;
    }

    public DeviceActionStatus getStatus() {
        return status;
    }

    public void setTarget(final Target target) {
        this.target = target;
    }

    public void setStatus(final DeviceActionStatus status) {
        this.status = status;
    }

    @Override
    public Long getId() {
        return target.getId();
    }

    public Integer getLastActionStatusCode() {
        return lastActionStatusCode;
    }

    public void setLastActionStatusCode(final Integer lastActionStatusCode) {
        this.lastActionStatusCode = lastActionStatusCode;
    }
}
