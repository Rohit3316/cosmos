/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.eclipse.hawkbit.repository.SoftwareModuleFormatFields;
import org.eclipse.hawkbit.repository.SoftwareModuleFormatManagement;
import org.eclipse.hawkbit.repository.builder.GenericSoftwareModuleFormatUpdate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleFormatCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleFormatUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaSoftwareModuleFormatCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleFormat;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.SoftwareModuleFormatSpecification;
import org.eclipse.hawkbit.repository.model.SoftwareModuleFormat;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link JpaSoftwareModuleFormatManagement}.
 *
 */
@Transactional(readOnly = true)
@Validated
public class JpaSoftwareModuleFormatManagement implements SoftwareModuleFormatManagement {


    private final SoftwareModuleFormatRepository softwareModuleFormatRepository;

    private final VirtualPropertyReplacer virtualPropertyReplacer;

    private final SoftwareModuleRepository softwareModuleRepository;

    private final Database database;

    public JpaSoftwareModuleFormatManagement(
                                             final SoftwareModuleFormatRepository softwareModuleFormatRepository,
                                             final VirtualPropertyReplacer virtualPropertyReplacer,
                                             final SoftwareModuleRepository softwareModuleRepository, final Database database) {

        this.softwareModuleFormatRepository = softwareModuleFormatRepository;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.softwareModuleRepository = softwareModuleRepository;
        this.database = database;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public SoftwareModuleFormat update(final SoftwareModuleFormatUpdate u) {
        final GenericSoftwareModuleFormatUpdate update = (GenericSoftwareModuleFormatUpdate) u;

        final JpaSoftwareModuleFormat format = (JpaSoftwareModuleFormat) get(update.getId())
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModuleFormat.class, update.getId()));

        update.getDescription().ifPresent(format::setDescription);

        return softwareModuleFormatRepository.save(format);
    }

    @Override
    public Page<SoftwareModuleFormat> findByRsql(final Pageable pageable, final String rsqlParam) {
        return JpaManagementHelper
                .findAllWithCountBySpec(softwareModuleFormatRepository, pageable,
                        Arrays.asList(
                                RSQLUtility.buildRsqlSpecification(rsqlParam, SoftwareModuleFormatFields.class,
                                        virtualPropertyReplacer, database),
                                SoftwareModuleFormatSpecification.isDeleted(false)));
    }

    @Override
    public Slice<SoftwareModuleFormat> findAll(final Pageable pageable) {
        return JpaManagementHelper.findAllWithoutCountBySpec(softwareModuleFormatRepository, pageable,
                Collections.singletonList(SoftwareModuleFormatSpecification.isDeleted(false)));
    }

    @Override
    public long count() {
        return softwareModuleFormatRepository.countByDeleted(false);
    }

    @Override
    public Optional<SoftwareModuleFormat> getByKey(final String key) {
        return softwareModuleFormatRepository.findByKey(key);
    }

    @Override
    public Optional<SoftwareModuleFormat> getByName(final String name) {
        return softwareModuleFormatRepository.findByName(name);
    }

    @Override
    public Optional<List<SoftwareModuleFormat>> findBykeyIn(List<String> names) {
        return softwareModuleFormatRepository.findByKeyIn(names);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public SoftwareModuleFormat create(final SoftwareModuleFormatCreate c) {
        final JpaSoftwareModuleFormatCreate create = (JpaSoftwareModuleFormatCreate) c;

        return softwareModuleFormatRepository.save(create.build());
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final long formatId) {
        final JpaSoftwareModuleFormat toDelete = softwareModuleFormatRepository.findById(formatId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModuleFormat.class, formatId));
        if (softwareModuleRepository.countByFormat(toDelete) > 0  ) {
            toDelete.setDeleted(true);
            softwareModuleFormatRepository.save(toDelete);
        } else {
            softwareModuleFormatRepository.delete(toDelete);
        }
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<SoftwareModuleFormat> create(final Collection<SoftwareModuleFormatCreate> creates) {
        return creates.stream().map(this::create).toList();
    }

    @Override
    public void create(final List<SoftwareModuleFormat> moduleFormatList) {
        List<JpaSoftwareModuleFormat> jpaSoftwareModuleFormats=moduleFormatList.stream()
                .map(JpaSoftwareModuleFormat.class::cast)
                .toList();
        softwareModuleFormatRepository.saveAll(jpaSoftwareModuleFormats);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final Collection<Long> ids) {
        final List<JpaSoftwareModuleFormat> setsFound = softwareModuleFormatRepository.findAllById(ids);

        if (setsFound.size() < ids.size()) {
            throw new EntityNotFoundException(SoftwareModuleFormat.class, ids,
                    setsFound.stream().map(SoftwareModuleFormat::getId).toList());
        }

        softwareModuleFormatRepository.deleteAll(setsFound);
    }

    @Override
    public List<SoftwareModuleFormat> get(final Collection<Long> ids) {
        return Collections.unmodifiableList(softwareModuleFormatRepository.findAllById(ids));
    }

    @Override
    public Optional<SoftwareModuleFormat> get(final long id) {
        return softwareModuleFormatRepository.findById(id).map(SoftwareModuleFormat.class::cast);
    }

    @Override
    public boolean exists(final long id) {
        return softwareModuleFormatRepository.existsById(id);
    }

}
