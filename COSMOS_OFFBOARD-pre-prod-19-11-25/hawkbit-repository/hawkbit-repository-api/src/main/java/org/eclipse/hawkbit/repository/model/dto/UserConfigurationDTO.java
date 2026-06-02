package org.eclipse.hawkbit.repository.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.eclipse.hawkbit.repository.model.User;
import org.eclipse.hawkbit.repository.model.UserConfiguration;

public class UserConfigurationDTO {

    @Size(min = 1, max = UserConfiguration.CONFIG_KEY_MAX_SIZE)
    @NotNull
    private String key;

    @Size(min = 1, max = UserConfiguration.CONFIG_VALUE_MAX_SIZE)
    @NotNull
    private String value;

    @NotNull
    private User user;

    public UserConfigurationDTO(){}

    public UserConfigurationDTO(String key, String value, User user) {
        this.key = key;
        this.value = value;
        this.user = user;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
