package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.jpa.model.JpaUser;
import org.eclipse.hawkbit.repository.jpa.model.JpaUserConfiguration;
import org.eclipse.hawkbit.repository.model.UserConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * The repository interface for the {@link UserConfiguration} model.
 */
@Repository
@Transactional(readOnly = true)
public interface UserConfigurationRepository extends JpaRepository<JpaUserConfiguration, Long> {

    List<UserConfiguration> findByUser(final JpaUser user);

    Optional<JpaUserConfiguration> findByUserAndKey(final JpaUser user, final String key);
}
