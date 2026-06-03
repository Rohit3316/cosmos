package org.eclipse.hawkbit.repository.model;

public interface SoftwareInstallerType extends BaseEntity {

    int INSTALLER_TYPE_NAME_MAX_SIZE =50;

    int INSTALLER_TYPE_NAME_MIN_SIZE = 1;

    String getName();

    String getDescription();

    boolean isDeleted();

    String getNameAndDescription();

}
