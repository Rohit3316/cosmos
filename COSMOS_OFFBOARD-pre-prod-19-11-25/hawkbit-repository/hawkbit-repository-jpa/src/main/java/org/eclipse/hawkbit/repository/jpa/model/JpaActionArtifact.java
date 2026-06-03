package org.eclipse.hawkbit.repository.jpa.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Artifacts;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.ValidationException;
import java.io.Serializable;
import java.util.Objects;

/**
 * Represents the association between an action and an artifact in the system.
 *
 * <p>This class is a JPA entity that maps the relationship between an action
 * and an artifact in the database. It uses a composite key defined by the
 * {@link ActionArtifact} class.</p>
 *
 * <p>Key Features:</p>
 * <ul>
 *   <li>Defines a many-to-one relationship with {@link JpaAction} and {@link JpaArtifacts}.</li>
 *   <li>Uses {@code @IdClass} to represent the composite primary key.</li>
 *   <li>Ensures that both action and artifact are non-null during instantiation.</li>
 *   <li>Overrides {@code equals} and {@code hashCode} for proper comparison and hashing.</li>
 * </ul>
 *
 * <p>Annotations:</p>
 * <ul>
 *   <li>{@code @Entity} marks this class as a JPA entity.</li>
 *   <li>{@code @Table} specifies the database table name as "sp_action_artifact".</li>
 *   <li>{@code @IdClass} indicates the composite key class.</li>
 *   <li>{@code @ManyToOne} and {@code @JoinColumn} define the relationships and foreign keys.</li>
 *   <li>Lombok annotations {@code @Getter}, {@code @Setter}, and {@code @NoArgsConstructor} reduce boilerplate code.</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <p>This class is used to persist and manage the association between actions
 * and artifacts in the database. It ensures referential integrity and provides
 * a structured way to handle this relationship in the application.</p>
 *
 * @see JpaAction
 * @see JpaArtifacts
 * @see ActionArtifact
 */
@IdClass(ActionArtifact.class)
@Table(name = "sp_action_artifact")
@Entity
@NoArgsConstructor
@Getter
@Setter
public class JpaActionArtifact implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "action_id", nullable = false, updatable = false, foreignKey = @ForeignKey(value =
            ConstraintMode.CONSTRAINT, name = "fk_action"))
    private JpaAction action;

    @Id
    @ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "artifact_id", nullable = false, updatable = false, foreignKey = @ForeignKey(value =
            ConstraintMode.CONSTRAINT, name = "fk_artifact"))
    private JpaArtifacts artifact;

    /**
     * Constructor for JpaActionArtifact.
     *
     * @param action   the associated action, must not be null
     * @param artifact the associated artifact, must not be null
     * @throws ValidationException if action or artifact is null
     */
    public JpaActionArtifact(final Action action, final Artifacts artifact) {
        if (action == null || artifact == null) {
            throw new ValidationException("Action and Artifact cannot be null");
        }
        this.action = (JpaAction) action;
        this.artifact = (JpaArtifacts) artifact;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        JpaActionArtifact jpaActionArtifact = (JpaActionArtifact) obj;
        return Objects.equals(action, jpaActionArtifact.action) && Objects.equals(artifact, jpaActionArtifact.artifact);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, artifact);
    }

}
