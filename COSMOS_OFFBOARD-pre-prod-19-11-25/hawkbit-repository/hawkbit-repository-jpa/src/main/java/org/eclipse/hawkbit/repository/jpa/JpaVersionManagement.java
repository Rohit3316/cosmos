/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.VersionManagement;
import org.eclipse.hawkbit.repository.builder.VersionCreate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaVersionCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaVersion;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Version;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * JPA implementation of {@link VersionManagement}.
 */
@Transactional(readOnly = true)
@Validated
public class JpaVersionManagement implements VersionManagement {

    private final VersionRepository versionRepository;
    private final SoftwareModuleRepository swRepository;

    public JpaVersionManagement(final VersionRepository versionRepository, final SoftwareModuleRepository swRepository) {
        this.versionRepository = versionRepository;
        this.swRepository = swRepository;
    }


    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Version create(final VersionCreate c) {
        final JpaVersionCreate create = (JpaVersionCreate) c;
        JpaVersion t = create.build();
        return versionRepository.save(t);
    }

    @Override
    public Optional<Version> getById(final long id) {
        return versionRepository.findById(id).map(jpaVersion -> jpaVersion);
    }

    @Override
    public void deleteVersion(Integer versionId) {
        versionRepository.deleteById(versionId.longValue());
    }

    @Override
    public Long countVersionsForSoftwareModuleId(SoftwareModule softwareModuleId, Long targetVersionId, Long sourceVersionId) {
        return versionRepository.countBySoftwareModuleIdAndTargetOrSourceVersionId((JpaSoftwareModule) softwareModuleId, targetVersionId, sourceVersionId);
    }

    @Override
    public List<Version> findAllVersionsBySoftwareModuleId(PageRequest pageRequest, Long softwareModuleId) {
        if (softwareModuleId != null) {
            JpaSoftwareModule sm = swRepository.findById(softwareModuleId)
                    .orElseThrow(() -> new EntityNotFoundException("Software module not found."));
            return Collections.unmodifiableList(versionRepository.findBySoftwareModuleId(pageRequest, sm));
        }
        return Collections.unmodifiableList(versionRepository.findBySoftwareModuleId(pageRequest, null));
    }

    @Override
    public List<Version> findVersionBySoftware(SoftwareModule software) {
        return Collections.unmodifiableList(versionRepository.findBySoftwareModuleId((JpaSoftwareModule) software));
    }

    @Override
    public Long count(PageRequest pageRequest, Long softwareModuleId) {
        if (softwareModuleId != null) {
            JpaSoftwareModule sm = swRepository.findById(softwareModuleId)
                    .orElseThrow(() -> new EntityNotFoundException("Software module not found."));
            return versionRepository.countBySoftwareModuleId(pageRequest, sm);
        }
        return versionRepository.countBySoftwareModuleId(pageRequest, null);
    }

    @Override
    public Optional<Version> findByNameOrNumberAndModuleId(final String name, final Integer number, final Integer moduleId) {
        return versionRepository.findByNameOrNumberAndSoftwareModuleIdId(name, number, moduleId);
    }
}
