package org.eclipse.hawkbit.repository.jpa.model;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.eclipse.hawkbit.repository.model.User;
import org.eclipse.hawkbit.repository.model.UserConfiguration;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * JPA implementation of a {@link UserConfiguration}.
 *
 */
@Entity
@Table(name = "sp_user_configuration", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "conf_key"}, name = "uk_user_conf_key"))
public class JpaUserConfiguration extends AbstractJpaBaseEntity implements UserConfiguration {

    private static final long serialVersionUID = 1L;

    @Column(name = "conf_key", length = UserConfiguration.CONFIG_KEY_MAX_SIZE, nullable = false)
    @Size(min = 1, max = UserConfiguration.CONFIG_KEY_MAX_SIZE)
    @NotNull
    private String key;

    @Column(name = "conf_value", length = UserConfiguration.CONFIG_VALUE_MAX_SIZE, nullable = false)
    @Size(min = 1, max = UserConfiguration.CONFIG_VALUE_MAX_SIZE)
    @NotNull
    private String value;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_user_configuration_user"))
    @NotNull
    private JpaUser user;

    public JpaUserConfiguration(){}

    public JpaUserConfiguration(String key, String value, JpaUser user) {
        this.key = key;
        this.value = value;
        this.user = user;
    }


    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public User getUser() {
        return user;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setUser(JpaUser user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        JpaUserConfiguration that = (JpaUserConfiguration) o;
        return Objects.equals(key, that.key) && Objects.equals(value, that.value)
                && Objects.equals(user.getUsername(), that.user.getUsername());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), key, value, user);
    }
}
