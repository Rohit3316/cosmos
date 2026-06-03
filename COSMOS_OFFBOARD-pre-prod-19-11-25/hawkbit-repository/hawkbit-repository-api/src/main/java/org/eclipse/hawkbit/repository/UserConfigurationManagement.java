package org.eclipse.hawkbit.repository;

import java.util.Collection;
import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.model.User;
import org.eclipse.hawkbit.repository.model.UserConfiguration;
import org.eclipse.hawkbit.repository.model.dto.UserConfigurationDTO;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Service for {@link UserConfiguration} management operations.
 *
 */
public interface UserConfigurationManagement {

    /**
     * Get list of {@link UserConfiguration} for a given {@link User}.
     * @param user User
     * @return UserConfiguration list
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    List<UserConfiguration> getUserConfiguration(final User user);

    /**
     * Get a {@link UserConfiguration} for given {@link User} and {@link UserConfiguration#getKey()}
     * @param user String
     * @param key String
     * @return UserConfiguration
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    UserConfiguration getUserConfiguration(final User user, final String key);

    /**
     * Create user configurations
     * @param userConfigurations list of UserConfigurations
     * @return UserConfiguration created
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_UPDATE_REPOSITORY)
    List<UserConfiguration> create(final @NotEmpty List<UserConfigurationDTO> userConfigurations);

    /**
     * Delete user configurations
     * @param ids list of user configurations id
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    void delete(final @NotEmpty Collection<Long> ids);
}
