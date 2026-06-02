/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.repository.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * This class stores the combination of source and target version for a software module
 *
 * @param <S> source version
 * @param <T> target version
 */

@Getter
@Setter
public class SourceTargetVersionPair<S, T> {

    private final S sourceVersionId;
    private final T targetVersionId;

    public SourceTargetVersionPair(S sourceVersionId, T targetVersionId) {
        this.sourceVersionId = sourceVersionId;
        this.targetVersionId = targetVersionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourceTargetVersionPair<?, ?> versions = (SourceTargetVersionPair<?, ?>) o;
        return Objects.equals(sourceVersionId, versions.sourceVersionId) &&
                Objects.equals(targetVersionId, versions.targetVersionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceVersionId, targetVersionId);
    }
}
