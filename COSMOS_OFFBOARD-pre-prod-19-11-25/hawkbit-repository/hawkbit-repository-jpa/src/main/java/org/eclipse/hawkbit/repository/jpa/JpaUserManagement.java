/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.UserManagement;
import org.eclipse.hawkbit.repository.builder.UserCreate;
import org.eclipse.hawkbit.repository.builder.UserUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaUserCreate;
import org.eclipse.hawkbit.repository.jpa.builder.JpaUserUpdate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity;
import org.eclipse.hawkbit.repository.jpa.model.JpaTenantMetaData;
import org.eclipse.hawkbit.repository.jpa.model.JpaUser;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.repository.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * JPA implementation of {@link TargetManagement}.
 *
 */
@Transactional(readOnly = true)
@Validated
public class JpaUserManagement implements UserManagement{

    private static final Logger LOG = LoggerFactory.getLogger(JpaUserManagement.class);
    private final UserRepository userRepository;
    private final TenantMetaDataRepository tenantMetaDataRepository;

    public JpaUserManagement(final UserRepository userRepository,
                             final TenantMetaDataRepository tenantMetaDataRepository) {
        this.userRepository = userRepository;
        this.tenantMetaDataRepository = tenantMetaDataRepository;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public User create(final UserCreate c) {

        final JpaUserCreate create = (JpaUserCreate) c;
        return  userRepository.save(create.build());
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public User update(final UserUpdate u) {
        final JpaUserUpdate userUpdate = (JpaUserUpdate) u;
        final JpaUser user = getByIdAndThrowIfNotFound(userUpdate.getId());
        Optional<Collection<Long>> tenants = userUpdate.getTenants();
        Collection<Long> nTenant = null;
        //Update metadata of user
        userUpdate.getFirstname().ifPresent(user::setFirstname);
        userUpdate.getLastname().ifPresent(user::setLastname);
        if (tenants.isPresent()) {
            nTenant = tenants.get();


            //Check on the tenants passed in input
            final List<JpaTenantMetaData> foundTenants = tenantMetaDataRepository.findAllById(tenants.get());
            if (foundTenants.size() < nTenant.size()) {
                nTenant.removeAll(foundTenants.stream().map(AbstractJpaBaseEntity::getId).toList());
                throw new EntityNotFoundException(TenantMetaData.class, nTenant.toString());
            }
            //Remove the tenants that are no longer present
            user.removeTenantsIfNotPresent(nTenant);

            for (JpaTenantMetaData tenant : foundTenants) {
                //Remove from the insert the tenant that are alredy present.
                Optional<TenantMetaData> res = user.getTenantMetadata().stream().filter(t -> t.getId().equals(tenant.getId())).findFirst();
                if (res.isEmpty()) {
                    user.addTenant(tenant);
                }
            }
            userRepository.save(user);
            LOG.info("User {} updated correctly", user.getId());
            return user;
        }
        return user;
    }

    @Override
    public User getUserByIdNoPermission(long userId) {
        return getByIdAndThrowIfNotFound(userId);
    }

    private JpaUser getByIdAndThrowIfNotFound(final Long id) {
        return (JpaUser) userRepository.getUserById(id)
                .orElseThrow(() -> new EntityNotFoundException(User.class, id));
    }



}
