/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Cancel action to be provided to the target.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiCancel {

    @Schema(example = "11")
    private final String id;

    @NotNull
    private final DdiCancelActionToStop cancelAction;

    /**
     * Parameterized constructor.
     *
     * @param id
     *            of the cancel action
     * @param cancelAction
     *            the action
     */
    @JsonCreator
    public DdiCancel(@JsonProperty("id") final String id,
                     @JsonProperty("cancelAction") final DdiCancelActionToStop cancelAction) {
        this.id = id;
        this.cancelAction = cancelAction;
    }

    public String getId() {
        return id;
    }

    public DdiCancelActionToStop getCancelAction() {
        return cancelAction;
    }

    @Override
    public String toString() {
        return "Cancel [id=" + id + ", cancelAction=" + cancelAction + "]";
    }

}
