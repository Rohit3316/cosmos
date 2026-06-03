package org.eclipse.hawkbit.repository.model;

public interface Permission extends BaseEntity {

    /**
     * Maximum length of name string.
     */
    int NAME_MAX_SIZE = 100;

    /**
     * Maximum length of description string.
     */
    int DESCRIPTION_MAX_SIZE = 512;

    /**
     * {@link Role} name
     * @return name
     */
    String getName();

    /**
     * {@link Role} description
     * @return description
     */
    String description();

    /**
     * {@link Role} active or inactive
     * @return TRUE/FALSE
     */
    Boolean isActive();
}
