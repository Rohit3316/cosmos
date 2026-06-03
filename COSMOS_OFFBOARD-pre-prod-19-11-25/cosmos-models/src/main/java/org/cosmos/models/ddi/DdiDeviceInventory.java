/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * Put inventory request body
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiDeviceInventory {

    @NotNull(message = "Inventory Signature cannot be null")
    private DdiSignature inventorySignature;

    @NotBlank(message = "Inventory details are missing")
    private String inventoryDetails;

    private String staticInventoryHash;

    private DdiSignature staticInventorySignature;

    private String rawInventoryDetails;

    private DdiSignature rawInventorySignature;

}
