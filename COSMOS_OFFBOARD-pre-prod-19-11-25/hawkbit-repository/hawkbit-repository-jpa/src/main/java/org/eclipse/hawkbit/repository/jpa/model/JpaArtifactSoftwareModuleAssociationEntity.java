package org.eclipse.hawkbit.repository.jpa.model;

import java.io.Serial;
import java.util.Objects;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.persistence.annotations.CascadeOnDelete;


/**
 * This class represents a JPA entity for the association between artifacts and software modules.
 * It is annotated with JPA annotations to define the table name, primary key, and foreign key relationships.
 * It also implements the ArtifactSoftwareModuleAssociation and Identifiable interfaces.
 */

@Entity
@Table(name = "sp_artifact_software_module", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"artifact_id", "software_module_id", "source_version", "target_version"}, name = "uk_module_source_target_version")})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class JpaArtifactSoftwareModuleAssociationEntity extends AbstractJpaBaseEntity implements ArtifactSoftwareModuleAssociation, Identifiable<Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The artifact associated with the association.
     * It is fetched lazily to optimize performance.
     * The foreign key constraint is defined with the name "fk_artifact_module_link_artifact".
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artifact_id", nullable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_artifact_module_link_artifact"))
    @NotNull
    private JpaArtifacts artifact;

    /**
     * The software module associated with the association.
     * It is fetched lazily to optimize performance.
     * The foreign key constraint is defined with the name "fk_artifact_module_link_software_module".
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @CascadeOnDelete
    @JoinColumn(name = "software_module_id", nullable = false, foreignKey = @ForeignKey(name = "fk_artifact_module_link_software_module"))
    private JpaSoftwareModule softwareModule;

    /**
     * The source version of the association.
     * DELTA file_type will have only one version
     * FULL file_type will have one or more source versions for given artifactId and softwareModuleId
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_version", foreignKey = @ForeignKey(name = "fk_artifact_module_link_source_version"))
    private JpaVersion sourceVersion;

    /**
     * The target version of the association.
     * It is fetched lazily to optimize performance.
     * The foreign key constraint is defined with the name "fk_artifact_module_link_target_version".
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_version", nullable = false, foreignKey = @ForeignKey(name = "fk_artifact_module_link_target_version"))
    private JpaVersion targetVersion;

    @Override
    public int hashCode() {
        return Objects.hash(artifact.getId(), softwareModule.getId(), sourceVersion != null ? sourceVersion.getId() : null, targetVersion.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JpaArtifactSoftwareModuleAssociationEntity that = (JpaArtifactSoftwareModuleAssociationEntity) o;
        return Objects.equals(artifact, that.artifact) &&
                Objects.equals(softwareModule, that.softwareModule) &&
                Objects.equals(sourceVersion, that.sourceVersion) &&
                Objects.equals(targetVersion, that.targetVersion);
    }

}
