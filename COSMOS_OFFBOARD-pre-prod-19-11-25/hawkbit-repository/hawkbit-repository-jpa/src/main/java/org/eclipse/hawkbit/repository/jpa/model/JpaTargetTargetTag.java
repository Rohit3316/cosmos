package org.eclipse.hawkbit.repository.jpa.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.model.TargetTargetTag;

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

/**
 * Represents the association between a Target and a TargetTag.
 *
 * This entity is mapped to the "sp_target_target_tag" table in the database.
 * It defines a many-to-many relationship between targets and target tags.
 *
 * The primary key is a composite key represented by {@link TargetTargetTagId}.
 *
 * Fields:
 * - target: The {@link JpaTarget} entity associated with this relationship.
 * - tag: The {@link JpaTargetTag} entity associated with this relationship.
 *
 * This class is annotated with:
 * - {@code @Entity} to define it as a JPA entity.
 * - {@code @Table} to specify the corresponding table in the database.
 * - {@code @IdClass(TargetTargetTagId.class)} to define the composite primary key.
 * - {@code @ManyToOne} to establish relationships with the {@link JpaTarget} and {@link JpaTargetTag} entities.
 * - {@code @JoinColumn} to specify the foreign key constraints.
 */
@IdClass(TargetTargetTagId.class)
@Entity
@Table(name = "sp_target_target_tag")
@NoArgsConstructor
@AllArgsConstructor
public class JpaTargetTargetTag implements TargetTargetTag {

    @Id
    @ManyToOne(optional = false, targetEntity = JpaTarget.class, fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST })
    @JoinColumn(name = "target", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_rollouttargetgroup_target"))
    private JpaTarget target;

    @Id
    @ManyToOne(optional = false, targetEntity = JpaTargetTag.class, fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST })
    @JoinColumn(name = "tag", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_rollouttargetgroup_target"))
    private JpaTargetTag tag;

    public TargetTargetTagId getId() {
        return new TargetTargetTagId(target, tag);
    }

}
