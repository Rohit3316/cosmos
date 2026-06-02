/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import lombok.Getter;
import lombok.Setter;
import org.cosmos.models.mgmt.FileType;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.eclipse.hawkbit.repository.event.remote.entity.ArtifactsCreatedEvent;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.eclipse.persistence.descriptors.DescriptorEvent;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JPA implementation of {@link Artifacts}. This class represents a collection of artifacts
 * stored in the repository.
 */

@Table(name = "sp_artifacts")
@Entity
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
@Getter
@Setter
public class JpaArtifacts extends AbstractJpaTenantAwareBaseEntity implements Artifacts, EventAwareEntity {
    private static final long serialVersionUID = 1L;

    /**
     * The name of the file associated with the artifact.
     */
    @Column(name = "file_name", nullable = false, length = 300)
    private String fileName;

    /**
     * The type of the file associated with the artifact.
     */
    @Column(name = "file_type", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private FileType fileType;

    /**
     * A brief description of the artifact.
     */
    @Column(name = "description", nullable = false, length = 300)
    private String description;

    /**
     * The expiry date of the artifact, represented as a Unix timestamp.
     */
    @NotNull
    @Column(name = "expiry_date")
    private Long expiryDate;

    /**
     * The SHA-256 hash of the artifact file.
     */
    @Column(name = "sha256_hash", length = 64)
    private String sha256Hash;


    @Column(name = "md5_hash", length = 32)
    private String md5Hash;

    /**
     * The size of the file associated with this artifact, in bytes.
     * This field is nullable and maps to the `file_size` column in the database.
     */
    @Column(name = "file_size", nullable = true)
    private Long fileSize;

    /**
     * The status of the file associated with the artifact.
     */
    @Column(name = "file_status", length = 50)
    private String fileStatus;

    @Column(name ="status",nullable = false,length = 50)
    private String artifactStatus;

    @OneToMany(mappedBy = "artifact", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST, orphanRemoval = true)
    @CascadeOnDelete
    private List<JpaActionArtifact> jpaActionArtifacts;

    /**
     * A set of associations between the artifact and software modules.
     */
    @OneToMany(mappedBy = "artifact", targetEntity = JpaArtifactSoftwareModuleAssociationEntity.class, fetch = FetchType.LAZY)
    private List<JpaArtifactSoftwareModuleAssociationEntity> softwareModules;


    @Override
    public void fireCreateEvent(final DescriptorEvent descriptorEvent) {
        setId(this.getId());
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new ArtifactsCreatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireUpdateEvent(DescriptorEvent descriptorEvent) {
        // Do Nothing

    }

    @Override
    public void fireDeleteEvent(DescriptorEvent descriptorEvent) {
        // Do nothing

    }

    /**
     * Returns a set of artifact-software module associations.
     *
     * @return a set of {@link ArtifactSoftwareModuleAssociation} objects
     */
    @Override
    public Set<ArtifactSoftwareModuleAssociation> getArtifactSoftwareModuleAssociations() {
        return this.softwareModules.stream().map(ArtifactSoftwareModuleAssociation.class::cast).collect(Collectors.toSet());
    }

    @Override
    public FileTransferStatus getFileStatus() {
        if (fileStatus==null){
            return null;
        }
        return FileTransferStatus.valueOf(fileStatus);
    }
}
