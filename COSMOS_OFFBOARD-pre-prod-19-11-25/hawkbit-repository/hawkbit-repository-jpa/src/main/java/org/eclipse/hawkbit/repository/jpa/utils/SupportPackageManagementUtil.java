package org.eclipse.hawkbit.repository.jpa.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;
import jakarta.validation.ValidationException;
import lombok.experimental.UtilityClass;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtFileUrlSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtBaseSupportPackageCreateRequest;
import org.eclipse.hawkbit.repository.jpa.model.JpaEsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaEspEcuRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaRspRollout;
import org.eclipse.hawkbit.repository.model.Esp;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rsp;

@UtilityClass
public class SupportPackageManagementUtil {

    /**
     * Converts the given {@link MgmtBaseSupportPackageCreateRequest} and {@link Esp} into a {@link JpaEsp} object.
     * If the {@link Esp} parameter is null, a new {@link JpaEsp} object is created.
     * The function populates the {@link JpaEsp} object with the data from the {@link MgmtBaseSupportPackageCreateRequest}.
     *
     * @param packageCreateRequest The request object containing the necessary data for creating an ESP.
     * @param esp                  The ESP object to be converted. If null, a new {@link JpaEsp} object is created.
     * @return A {@link JpaEsp} object with the data from the {@link MgmtBaseSupportPackageCreateRequest} and {@link Esp}.
     */
    public JpaEsp toJpaEsp(JpaEsp source, Esp target) {
        return createJpaEsp(source, target);
    }

    /**
     * Converts the given {@link MgmtBaseSupportPackageCreateRequest} and {@link Esp} into a {@link JpaEsp} object.
     * If the {@link Esp} parameter is null, a new {@link JpaEsp} object is created.
     * The function populates the {@link JpaEsp} object with the data from the {@link MgmtBaseSupportPackageCreateRequest}.
     *
     * @param packageCreateRequest The request object containing the necessary data for creating an ESP.
     * @param esp                  The ESP object to be converted. If null, a new {@link JpaEsp} object is created.
     * @return A {@link JpaEsp} object with the data from the {@link MgmtBaseSupportPackageCreateRequest} and {@link Esp}.
     * If the {@link Esp} parameter is null, a new {@link JpaEsp} object is created.
     * @throws JsonProcessingException If there is an error while processing the file metadata.
     */

    private JpaEsp createJpaEsp(JpaEsp source, Esp target) {
        JpaEsp jpaEsp;
        jpaEsp = (target == null) ? new JpaEsp() : (JpaEsp) target;

        jpaEsp.setFileMetadata(source.getFileMetadata());
        jpaEsp.setFileType(source.getFileType());
        jpaEsp.setFileVersion(source.getFileVersion());
        jpaEsp.setFileContentDescription(source.getFileContentDescription());
        jpaEsp.setFileInfoUrl(source.getFileInfoUrl());
        jpaEsp.setFileName(source.getFileName());
        jpaEsp.setSha256Hash(source.getSha256Hash());
        jpaEsp.setFileUrl(source.getFileUrl());
        return jpaEsp;
    }


    /**
     * Converts a given rollout, list of VINs, and ECU node address into a list of {@link JpaEspEcuRollout} objects.
     * Each {@link JpaEspEcuRollout} object represents a specific ECU node address and VIN combination for a given rollout.
     *
     * @param rollout        The rollout for which the ECU-VIN combinations are being created.
     * @param vins           A list of Vehicle Identification Numbers (VINs) associated with the rollout.
     * @param ecuNodeAddress The ECU node address for the ECU-VIN combinations.
     * @return A list of {@link JpaEspEcuRollout} objects representing the ECU-VIN combinations for the given rollout and ECU node address.
     */
    public List<JpaEspEcuRollout> toJpaEspEcuRollout(Rollout rollout, List<String> vins, String ecuNodeAddress) {

        return vins.stream().map(vin -> {
            JpaEspEcuRollout jpaEspEcuRollout = new JpaEspEcuRollout();
            jpaEspEcuRollout.setRollout((JpaRollout) rollout);
            jpaEspEcuRollout.setEcuNodeAddress(ecuNodeAddress);
            jpaEspEcuRollout.setControllerId(vin);
            return jpaEspEcuRollout;
        }).toList();
    }

    /**
     * Converts a given rollout, list of VINs, and ECU node address into a list of {@link JpaEspEcuRollout} objects.
     * Each {@link JpaEspEcuRollout} object represents a specific ECU node address and VIN combination for a given rollout.
     *
     * @param rollout        The rollout for which the ECU-VIN combinations are being created.
     * @param vins           A list of Vehicle Identification Numbers (VINs) associated with the rollout.
     * @param ecuNodeAddress The ECU node address for the ECU-VIN combinations.
     * @return A list of {@link JpaEspEcuRollout} objects representing the ECU-VIN combinations for the given rollout and ECU node address.
     */
    public List<JpaEspEcuRollout> toJpaEspEcuRollout(Rollout rollout, List<String> vins, String ecuNodeAddress, JpaEsp esp) {

        return vins.stream().map(vin -> {
            JpaEspEcuRollout jpaEspEcuRollout = new JpaEspEcuRollout();
            jpaEspEcuRollout.setRollout((JpaRollout) rollout);
            jpaEspEcuRollout.setEcuNodeAddress(ecuNodeAddress);
            jpaEspEcuRollout.setControllerId(vin);
            jpaEspEcuRollout.setSupportPackage(esp);
            return jpaEspEcuRollout;
        }).toList();

    }

    /**
     * Creates a new JpaRspRollout object and associates it with the given rollout and RSP.
     *
     * @param rollout The rollout object to be associated with the JpaRspRollout.
     * @param rsp     The RSP object to be associated with the JpaRspRollout.
     * @return A new JpaRspRollout object with the provided rollout and RSP objects.
     * The JpaRspRollout object is initialized with the provided rollout and RSP,
     * and the other fields are set to their default values.
     */
    public JpaRspRollout toJpaRspRollout(Rollout rollout, Rsp rsp) {
        JpaRspRollout jpaRspRollout = new JpaRspRollout();
        jpaRspRollout.setSupportPackage((JpaRsp) rsp);
        jpaRspRollout.setRollout((JpaRollout) rollout);
        return jpaRspRollout;
    }

    /**
     * Converts the given {@link JpaRsp} source object and {@link Rsp} target object into a new {@link JpaRsp} object.
     * This method delegates the creation and population of the resulting {@link JpaRsp} object to the {@code createJpaRsp} method.
     *
     * @param source The original {@link JpaRsp} object containing data to be used in the conversion.
     * @param target The {@link Rsp} object containing additional data to be incorporated into the new {@link JpaRsp} object.
     * @return A new {@link JpaRsp} object populated with data from the {@link JpaRsp} source and {@link Rsp} target.
     */
    public JpaRsp toJpaRsp(JpaRsp source, Rsp target) {
        return createJpaRsp(source, target);
    }

    /**
     * Converts the given {@link JpaRsp} object into a new {@link JpaRsp} object
     * using the {@code createJpaRsp} method.
     *
     * @param rsp The {@link JpaRsp} object to be converted.
     * @return A new {@link JpaRsp} object populated with the data from the given {@link JpaRsp}.
     */

    public JpaRsp toJpaRsp(JpaRsp rsp) {
        return createJpaRsp(rsp, null);
    }

    /**
     * Creates a new JpaRsp object and populates it with data from the given {@link MgmtBaseSupportPackageCreateRequest}.
     * If the {@link Rsp} parameter is not null, the function populates the fields of the existing {@link JpaRsp} object.
     * Otherwise, a new {@link JpaRsp} object is created.
     *
     * @param packageCreateRequest The request object containing the necessary data for creating an RSP.
     * @param rsp                  The RSP object to be converted. If null, a new {@link JpaRsp} object is created.
     * @return A {@link JpaRsp} object with the data from the {@link MgmtBaseSupportPackageCreateRequest} and {@link Rsp}.
     * If the {@link Rsp} parameter is null, a new {@link JpaRsp} object is created.
     * @throws JsonProcessingException If there is an error while processing the file metadata.
     */
    private JpaRsp createJpaRsp(JpaRsp source, Rsp target) {
        JpaRsp jpaRsp;

        jpaRsp = (target == null) ? new JpaRsp() : (JpaRsp) target;
        jpaRsp.setFileMetadata(source.getFileMetadata());

        jpaRsp.setFileType(source.getFileType());
        jpaRsp.setFileVersion(source.getFileVersion());
        jpaRsp.setFileContentDescription(source.getFileContentDescription());
        jpaRsp.setFileInfoUrl(source.getFileInfoUrl());
        jpaRsp.setFileName(source.getFileName());
        jpaRsp.setSha256Hash(source.getSha256Hash());
        jpaRsp.setFileUrl(source.getFileUrl());
        return jpaRsp;
    }

    /**
     * Sanitizes the given file name by replacing any invalid characters with null.
     * @param fileName The file name to be sanitized.
     * @return The sanitized file name.
     */
    public String sanitizeFileName(String fileName) {
        if (fileName == null) return null;
        return fileName.replaceAll("\\s+", "");
    }
}
