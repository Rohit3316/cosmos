/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.distributionsettype.constants;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.cosmos.models.mgmt.MgmtId;

/**
 * Request Body of DistributionSetType for assignment operations (ID only).
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtDistributionSetTypeAssignment extends MgmtId {
}
