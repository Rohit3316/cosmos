/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.eclipse.hawkbit.repository.event.remote.SoftwareModuleTypeDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleFormatCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleFormatUpdatedEvent;
import org.eclipse.hawkbit.repository.model.SoftwareModuleFormat;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.persistence.descriptors.DescriptorEvent;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Type of a software modules.
 *
 */
@Entity
@Table(name = "sp_software_module_format", uniqueConstraints = @UniqueConstraint(columnNames = {"name",
        "tenant"}, name = "uk_sw_module_format_tenant_name"))

// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public class JpaSoftwareModuleFormat extends AbstractJpaNamedEntity implements SoftwareModuleFormat, EventAwareEntity {
    private static final long serialVersionUID = 1L;

    @Column(name = "type_key", nullable = false, length = SoftwareModuleType.KEY_MAX_SIZE)
    @Size(min = 1, max = SoftwareModuleType.KEY_MAX_SIZE)
    @NotNull
    private String key;


    @Column(name = "deleted")
    private boolean deleted;

    /**
     * Constructor.
     *
     * @param key
     *            of the type
     * @param name
     *            of the type
     * @param description
     *            of the type
     * @param maxAssignments
     *            assignments to a DS
     */
    public JpaSoftwareModuleFormat(final String key, final String name, final String description,
                                   final Integer maxAssignments) {
        this(key, name, description);
    }

    /**
     * Constructor.
     *
     * @param key
     *            of the type
     * @param name
     *            of the type
     * @param description
     *            of the type

     */
    public JpaSoftwareModuleFormat(final String key, final String name, final String description) {
        this.key = key;
        setDescription(description);
        setName(name);
    }

    /**
     * Default Constructor for JPA.
     */
    public JpaSoftwareModuleFormat() {
        // Default Constructor for JPA.
    }


    @Override
    public String getKey() {
        return key;
    }



    @Override
    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }




    @Override
    public String toString() {
        return "SoftwareModuleType [key=" + key + ", getName()=" + getName() + ", getId()=" + getId() + "]";
    }

    public void setKey(final String key) {
        this.key = key;
    }

    @Override
    public void fireCreateEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(
                new SoftwareModuleFormatCreatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireUpdateEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(
                new SoftwareModuleFormatUpdatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireDeleteEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new SoftwareModuleTypeDeletedEvent(
                getTenant(), getId(), getClass(), EventPublisherHolder.getInstance().getApplicationId()));
    }
}
