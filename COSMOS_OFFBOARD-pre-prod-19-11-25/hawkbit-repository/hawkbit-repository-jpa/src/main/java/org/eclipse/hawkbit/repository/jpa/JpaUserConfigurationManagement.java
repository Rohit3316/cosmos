package org.eclipse.hawkbit.repository.jpa;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.eclipse.hawkbit.repository.UserConfigurationManagement;
import org.eclipse.hawkbit.repository.exception.EntityCannotNullException;
import org.eclipse.hawkbit.repository.jpa.model.JpaUser;
import org.eclipse.hawkbit.repository.jpa.model.JpaUserConfiguration;
import org.eclipse.hawkbit.repository.model.User;
import org.eclipse.hawkbit.repository.model.UserConfiguration;
import org.eclipse.hawkbit.repository.model.dto.UserConfigurationDTO;
import org.eclipse.hawkbit.utils.MapperUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link UserConfigurationManagement}.
 */
@Validated
@Transactional(readOnly = true)
public class JpaUserConfigurationManagement implements UserConfigurationManagement {


    private final UserConfigurationRepository userConfigurationRepository;

    @Autowired
    public JpaUserConfigurationManagement(final UserConfigurationRepository userConfigurationRepository) {
        this.userConfigurationRepository = userConfigurationRepository;
    }


    @Override
    public List<UserConfiguration> getUserConfiguration(User user) {
        return userConfigurationRepository.findByUser((JpaUser) user);
    }

    @Override
    public UserConfiguration getUserConfiguration(User user, String key) {
        return userConfigurationRepository.findByUserAndKey((JpaUser) user, key).orElse(null);
    }

    @Override
    public List<UserConfiguration> create(List<UserConfigurationDTO> userConfigurations) {
        if (userConfigurations != null && !userConfigurations.isEmpty()) {
            List<JpaUserConfiguration> createdUserConfigurations = userConfigurationRepository.saveAll(MapperUtil.convertToList(userConfigurations, JpaUserConfiguration.class));
            return Collections.unmodifiableList(createdUserConfigurations);
        } else {
            throw new EntityCannotNullException("Null or empty list of user configurations");
        }
    }

    @Override
    public void delete(Collection<Long> ids) {
        userConfigurationRepository.deleteAllById(ids);
    }
}
