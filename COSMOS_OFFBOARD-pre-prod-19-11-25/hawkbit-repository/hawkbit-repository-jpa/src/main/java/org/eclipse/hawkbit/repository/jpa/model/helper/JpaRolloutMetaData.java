package org.eclipse.hawkbit.repository.jpa.model.helper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaTenantAwareBaseEntity;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entity representing metadata for a rollout in the system.
 * Stores key-value pairs associated with a specific rollout.
 * <p>
 * This class is mapped to the {@code sp_rollout_metadata} table.
 * </p>
 */
@Entity
@Table(name = "sp_rollout_metadata")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JpaRolloutMetaData extends AbstractJpaTenantAwareBaseEntity {


    /**
     * The rollout associated with this metadata.
     * References the {@link JpaRollout} entity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rollout_id", nullable = false)
    private JpaRollout rolloutId;


    /**
     * The key for the metadata entry.
     */
    @Column(name="metadata_key", nullable = false)
    private String key;

    /**
     * The value for the metadata entry.
     */
    @Column(name ="metadata_value", nullable = false)
    private String value;
}
