package org.eclipse.hawkbit.repository.model;

/**
 * <p>
 * The {@link EcuModelType} is the user that can do operation on the platform
 * </p>
 */
public interface EcuModelType {

    int ECU_MODEL_TYPE_NAME_MAX_SIZE =20;

    int ECU_MODEL_TYPE_NAME_MIN_SIZE = 1;

    String getEcuModelTypeName();
}
