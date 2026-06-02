package org.eclipse.hawkbit.repository.model;

/**
 * User Configuration
 */
public interface UserConfiguration extends BaseEntity  {

    /**
     * Maximum length of config_key string.
     */
    int CONFIG_KEY_MAX_SIZE = 100;

    /**
     * Maximum length of config_value string.
     */
    int CONFIG_VALUE_MAX_SIZE = 100;

    /**
     * {@link UserConfiguration} config_key
     * @return key
     */
    String getKey();

    /**
     * {@link UserConfiguration} config_value
     * @return value
     */
    String getValue();

    /**
     * {@link UserConfiguration} user
     * @return user
     */
    User getUser();
}
