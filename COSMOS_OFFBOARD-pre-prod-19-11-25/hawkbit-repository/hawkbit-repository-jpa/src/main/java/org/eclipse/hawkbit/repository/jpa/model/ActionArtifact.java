package org.eclipse.hawkbit.repository.jpa.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Artifacts;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents the association between an action and an artifact in the system.
 *
 * <p>This class is a composite key entity that links an action to an artifact.
 * It is used to persist and manage the relationship between these two entities
 * in the database. The class implements {@link Serializable} to ensure that
 * instances can be serialized for use in distributed systems or caching.</p>
 *
 * <p>Key Features:</p>
 * <ul>
 *   <li>Stores the IDs of the associated action and artifact.</li>
 *   <li>Provides constructors for creating instances from {@link Action} and {@link Artifacts} objects.</li>
 *   <li>Overrides {@code equals} and {@code hashCode} for proper comparison and hashing.</li>
 * </ul>
 *
 * <p>Annotations:</p>
 * <ul>
 *   <li>{@code @Getter}, {@code @Setter}, {@code @NoArgsConstructor}, {@code @AllArgsConstructor}, and {@code @ToString}
 *       are used to automatically generate boilerplate code such as getters, setters, constructors, and the {@code toString} method.</li>
 * </ul>
 *
 * @see Action
 * @see Artifacts
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
@ToString
public class ActionArtifact implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long action;
    private Long artifact;

    public ActionArtifact(final Action action, final Artifacts artifacts) {
        this.action = action.getId();
        this.artifact = artifacts.getId();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ActionArtifact actionArtifact = (ActionArtifact) obj;
        return Objects.equals(action, actionArtifact.action) &&
                Objects.equals(artifact, actionArtifact.artifact);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, artifact);
    }
}
