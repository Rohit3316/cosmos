/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.eclipse.hawkbit.repository.event.remote.SoftwareModuleDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleUpdatedEvent;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.EcuModel;
import org.eclipse.hawkbit.repository.model.IDistributionSetModule;
import org.eclipse.hawkbit.repository.model.SoftwareInstallerType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleFormat;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.eclipse.persistence.descriptors.DescriptorEvent;
import org.eclipse.persistence.queries.UpdateObjectQuery;
import org.eclipse.persistence.sessions.changesets.DirectToFieldChangeRecord;
import org.eclipse.persistence.sessions.changesets.ObjectChangeSet;

/**
 * Base Software Module that is supported by OS level provisioning mechanism on
 * the edge controller, e.g. OS, JVM, AgentHub.
 */
@Entity
@Table(name = "sp_base_software_module",
        uniqueConstraints = {
        @UniqueConstraint(columnNames = {"module_type", "name", "version", "tenant"}, name = "uk_base_sw_mod"),
                @UniqueConstraint(columnNames = {"tenant", "name"}, name = "uk_base_sw_mod_tenant_name")  },
        indexes = {
        @Index(name = "sp_idx_base_sw_module_01", columnList = "tenant,deleted,name,version"),
        @Index(name = "sp_idx_base_sw_module_02", columnList = "tenant,deleted,module_type"),
        @Index(name = "sp_idx_base_sw_module_prim", columnList = "tenant,id")})
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public class JpaSoftwareModule extends AbstractJpaNamedVersionedEntity implements SoftwareModule, EventAwareEntity {
    private static final long serialVersionUID = 1L;

    private static final String DELETED_PROPERTY = "deleted";

    @OneToMany(mappedBy = "softwareModule", cascade = CascadeType.ALL)
    private Set<JpaArtifactSoftwareModuleAssociationEntity> artifactSoftwareModuleAssociations;
    @ManyToOne
    @JoinColumn(name = "module_type", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_module_type"))
    @NotNull
    private JpaSoftwareModuleType type;

    @OneToMany(mappedBy = "sm", cascade = CascadeType.MERGE, orphanRemoval = true)
    private List<DistributionSetModule> assignedTo;

    @Column(name = "deleted")
    private boolean deleted;

    @Column(name = "vendor", nullable = true, length = SoftwareModule.VENDOR_MAX_SIZE)
    @Size(max = SoftwareModule.VENDOR_MAX_SIZE)
    private String vendor;

    @ManyToOne
    @JoinColumn(name = "module_format", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_module_format"))
    @NotNull
    private JpaSoftwareModuleFormat format;

    @CascadeOnDelete
    @OneToMany(mappedBy = "softwareModule", fetch = FetchType.LAZY, targetEntity = JpaSoftwareModuleMetadata.class)
    private List<JpaSoftwareModuleMetadata> metadata;

    @Column(name = "encrypted")
    private boolean encrypted;


    @CascadeOnDelete
    @ManyToMany(targetEntity = JpaEcuModel.class)
    @JoinTable(
            name = "sp_software_ecu_model",
            joinColumns = @JoinColumn(name = "software_module_id"),
            inverseJoinColumns = @JoinColumn(name = "ecu_model_id")
    )
    private Set<EcuModel> softwareEcuModels;

    @ManyToOne
    @JoinColumn(name = "software_installer_type", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_software_installer_type"))
    @NotNull
    private JpaSoftwareInstallerType softwareInstallerType;

    /**
     * Default constructor.
     */
    public JpaSoftwareModule() {
        // Default constructor for JPA
    }

    /**
     * parameterized constructor.
     *
     * @param type    of the {@link SoftwareModule}
     * @param name    abstract name of the {@link SoftwareModule}
     * @param version of the {@link SoftwareModule}
     */
    public JpaSoftwareModule(final SoftwareModuleType type, final String name, final String version,
                             final SoftwareModuleFormat format, final SoftwareInstallerType softwareInstallerType) {
        this(type, name, version, null, null, false, format, softwareInstallerType);
    }

    /**
     * parameterized constructor.
     *
     * @param type        of the {@link SoftwareModule}
     * @param name        abstract name of the {@link SoftwareModule}
     * @param version     of the {@link SoftwareModule}
     * @param description of the {@link SoftwareModule}
     * @param vendor      of the {@link SoftwareModule}
     * @param encrypted   encryption flag of the {@link SoftwareModule}
     */
    public JpaSoftwareModule(final SoftwareModuleType type, final String name, final String version,
                             final String description, final String vendor, final boolean encrypted,
                             final SoftwareModuleFormat format, final SoftwareInstallerType softwareInstallerType) {
        super(name, version, description);
        this.vendor = vendor;
        this.type = (JpaSoftwareModuleType) type;
        this.encrypted = encrypted;
        this.format = (JpaSoftwareModuleFormat) format;
        this.softwareInstallerType = (JpaSoftwareInstallerType) softwareInstallerType;

    }

    /**
     * Method is used to validate, event is of soft delete
     * @param event the DescriptorEvent of {@link this#fireUpdateEvent(DescriptorEvent)}
     * @return boolean
     */
    private static boolean isSoftDeleted(final DescriptorEvent event) {
        final ObjectChangeSet changeSet = ((UpdateObjectQuery) event.getQuery()).getObjectChangeSet();
        final List<DirectToFieldChangeRecord> changes = changeSet.getChanges().stream()
                .filter(DirectToFieldChangeRecord.class::isInstance).map(DirectToFieldChangeRecord.class::cast)
                .toList();

        return changes.stream().anyMatch(changeRecord -> DELETED_PROPERTY.equals(changeRecord.getAttribute())
                && Boolean.parseBoolean(changeRecord.getNewValue().toString()));
    }

    /**
     * Get set of software modules ECU Models
     * @return Set of {@link EcuModel}
     */
    @Override
    public Set<EcuModel> getSoftwareEcuModels() {
        if (softwareEcuModels == null) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(softwareEcuModels);
    }


    /**
     *
     * @return Set of all Artifact software module associations
     */
    @Override
    public Set<ArtifactSoftwareModuleAssociation> getArtifactSoftwareModuleAssociations() {
        return artifactSoftwareModuleAssociations.stream()
                .map(jpaArtifactSoftwareModuleAssociationEntity -> (ArtifactSoftwareModuleAssociation)jpaArtifactSoftwareModuleAssociationEntity)
                .collect(Collectors.toSet());
    }

    /**
     * Set, set of software modules ECU Models
     * @param softwareEcuModels the Set of {@link EcuModel}
     */
    public void setSoftwareEcuModels(Set<EcuModel> softwareEcuModels) {
        this.softwareEcuModels = softwareEcuModels;
    }

    /**
     * Add {@link EcuModel} into the set of software modules ECU Models
     * @param ecuModel the {@link EcuModel}
     * @return boolean
     */
    public boolean addSoftwareEcu(final EcuModel ecuModel) {
        if (softwareEcuModels == null) {
            softwareEcuModels = new HashSet<>();
        }

        return softwareEcuModels.add(ecuModel);
    }

    @Override
    public String getVendor() {
        return vendor;
    }

    public void setVendor(final String vendor) {
        this.vendor = vendor;
    }

    @Override
    public SoftwareModuleType getType() {
        return type;
    }

    public void setType(final JpaSoftwareModuleType type) {
        this.type = type;
    }

    @Override
    public SoftwareModuleFormat getFormat() {
        return format;
    }

    public void setFormat(final JpaSoftwareModuleFormat format) {
        this.format = format;
    }

    @Override
    public JpaSoftwareInstallerType getSoftwareInstallerType() {
        return softwareInstallerType;
    }

    public void setSoftwareInstallerType(JpaSoftwareInstallerType softwareInstallerType) {
        this.softwareInstallerType = softwareInstallerType;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * Marks or un-marks this software module as deleted.
     *
     * @param deleted {@code true} if the software module should be marked as deleted
     *                otherwise {@code false}
     */
    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public boolean isEncrypted() {
        return encrypted;
    }

    /**
     * Marks this software module as encrypted.
     *
     * @param encrypted {@code true} if the software module should be marked as encrypted
     *                  otherwise {@code false}
     */
    public void setEncrypted(final boolean encrypted) {
        this.encrypted = encrypted;
    }

    @Override
    public String toString() {
        return "SoftwareModule [deleted=" + isDeleted() + ", encrypted=" + isEncrypted() + ", name=" + getName()
                + ", version=" + getVersion() + ", revision=" + getOptLockRevision() + ", Id=" + getId() + ", type="
                + getType().getName() + "]";
    }

    @Override
    public List<DistributionSet> getAssignedTo() {
        if (assignedTo == null) {
            return Collections.emptyList();
        }

        List<DistributionSet> list = new ArrayList<>();
        for (DistributionSetModule distributionSetModule : assignedTo) {
            JpaDistributionSet dsSet = distributionSetModule.getDsSet();
            list.add(dsSet);
        }
        return list;
    }

    @Override
    public Set<EcuModel> getAssociatedEcuModels() {
        if (softwareEcuModels == null) {
            return Collections.emptySet();
        }
        return softwareEcuModels;
    }

    public List<IDistributionSetModule> getDsmRelation() {
        return assignedTo.stream().map(IDistributionSetModule.class::cast).toList();
    }

    @Override
    public void fireCreateEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(
                new SoftwareModuleCreatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireUpdateEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(
                new SoftwareModuleUpdatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));

        if (isSoftDeleted(descriptorEvent)) {
            EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new SoftwareModuleDeletedEvent(
                    getTenant(), getId(), getClass(), EventPublisherHolder.getInstance().getApplicationId()));
        }
    }

    @Override
    public void fireDeleteEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new SoftwareModuleDeletedEvent(getTenant(),
                getId(), getClass(), EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public List<String> getUpdateIgnoreFields() {
        return EventAwareEntity.super.getUpdateIgnoreFields();
    }

}
