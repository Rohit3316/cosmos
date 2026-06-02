/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.SoftwareInstallerTypeManagement;
import org.eclipse.hawkbit.repository.model.SoftwareInstallerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.ValidationException;
import java.util.Collections;
import java.util.List;

/**
 * JPA implementation of {@link SoftwareInstallerTypeManagement}.
 *
 */
@Transactional
@Validated
public class JpaSoftwareInstallerTypeManagement implements SoftwareInstallerTypeManagement {

    private final SoftwareInstallerTypeRepository softwareInstallerTypeRepository;

    public JpaSoftwareInstallerTypeManagement(final SoftwareInstallerTypeRepository softwareInstallerTypeRepository) {
        this.softwareInstallerTypeRepository = softwareInstallerTypeRepository;
    }

    public SoftwareInstallerType getSwInstallerTypeByName(final String name) {
        return softwareInstallerTypeRepository.findByName(name)
                .orElseThrow(() -> new ValidationException("Invalid Software Installer Type: " + name));
    }
    
    @Override
    public List<SoftwareInstallerType> findAllSoftwareInstallerTypes(Pageable pageable) {
        return Collections.unmodifiableList(softwareInstallerTypeRepository.findAll(pageable).toList());
    }

    /**
     * @return list of SoftwareInstallerType
     */
    @Override
    public Page<SoftwareInstallerType> findAll(final Pageable pageable) {
        return JpaManagementHelper.findAllWithCountBySpec(softwareInstallerTypeRepository, pageable, null);
    }

    @Override
    public long count() {
        return softwareInstallerTypeRepository.countByDeleted(false);
    }

}
