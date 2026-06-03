package org.eclipse.hawkbit.ddi.rest.resource.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import org.cosmos.models.ddi.DdiDeploymentDescriptor;
import org.cosmos.models.ddi.DdiDeviceInventory;
import org.cosmos.models.ddi.DdiSignature;
import org.cosmos.models.ddi.DeviceInventoryDetails;
import org.cosmos.models.ddi.Ecu;
import org.cosmos.models.ddi.Scomos;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.eclipse.hawkbit.ddi.rest.resource.config.InventoryConfig;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.exception.CosmosSignatureException;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import jakarta.validation.ValidationException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;



/**
 * Utility class for the DDI API.
 */

public class DdiApiHelper {

    private static final Logger LOG = LoggerFactory.getLogger(DdiApiHelper.class);
    private static final String ECU_SCOMOS_SW_VERSION = "ecu.scomos.swVersion";
    private static final String ECU_SCOMOS_SCOMO_ID = "ecu.scomos.scomoId";
    private static final String ECU_NODE_ADDR = "ecu.nodeAddr";
    private static final String ECU_PART_NUMBER = "ecu.partnumber";
    private static final String ECU_HW_VERSION = "ecu.hwVersion";
    private static final String ECU_SERIAL_NUMBER = "ecu.serialNumber";
    private static final ObjectMapper mapper = new ObjectMapper();

    private DdiApiHelper() {
    }

    /**
     * @param config    which holds public key and signing algorithm
     * @param inventory which is request body of device inventory API
     *                  This method decodes Base64 encoded payload and validates signatures
     *                  with public keys from config
     * @throws ValidationException if any signature validation fails
     */
    public static void validateInventorySignature(InventoryConfig config, DdiDeviceInventory inventory, TenantConfigHelper tenantConfigHelper) {

        try {
            // === INVENTORY SIGNATURE VALIDATION ===
            if (tenantConfigHelper.isInventorySignatureValidationEnabled()) {
                verifyDigitalSignature(config, inventory.getInventorySignature(), "inventorySignature");
            } else {
                LOG.info("Inventory signature validation disabled by tenant configuration");
            }

            // === STATIC INVENTORY SIGNATURE VALIDATION ===
            if (tenantConfigHelper.isStaticInventorySignatureValidationEnabled()) {
                verifyDigitalSignature(config, inventory.getStaticInventorySignature(), "staticInventorySignature");
            } else {
                LOG.info("Static inventory signature validation disabled by tenant configuration");
            }

            // === RAW INVENTORY SIGNATURE VALIDATION ===
            if (tenantConfigHelper.isRawInventorySignatureValidationEnabled()) {
                if (inventory.getRawInventorySignature() != null && inventory.getRawInventoryDetails() != null) {
                    verifyDigitalSignature(config, inventory.getRawInventorySignature(), "rawInventorySignature");
                }
            } else {
                LOG.info("Raw inventory signature validation disabled by tenant configuration");
            }
        } catch (Exception e) {
            LOG.error("Inventory signature validation failed: {}", e.getMessage(), e);
            throw new ValidationException("Inventory signature validation failed", e);
        }
    }

    /**
     *
     * Helper method to decode and verify a digital signature using provided config.
     *
     * @param config         the config containing public key and algorithm
     * @param ddiSignature   the DdiSignature object containing the signature string
     * @param signatureLabel label for logging and error messages
     * @throws ValidationException if verification fails due to cryptographic errors
     */
    private static void verifyDigitalSignature(InventoryConfig config, DdiSignature ddiSignature, String signatureLabel) {
        try {
            // Decode Base64-encoded signature and public key
            // TODO: Using com.nimbusds.jose.util.Base64 library which needs to be addressed later.
            byte[] decodedSignature = new com.nimbusds.jose.util.Base64(ddiSignature.getSignature()).decode();
            byte[] publicKeyBytes = new com.nimbusds.jose.util.Base64(config.getPublicKey()).decode();

            // Prepare public key
            PublicKey genPublicKey = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            // Create Signature instance for verification
            Signature signature = Signature.getInstance(config.getSigningAlgorithm());
            signature.initVerify(genPublicKey);
            signature.verify(decodedSignature);

            LOG.info("{} verification successful", signatureLabel);
        } catch (Exception e) {
                LOG.error("Error verifying {}: {}", signatureLabel, e.getMessage());
                throw new ValidationException(signatureLabel + " verification failed", e);
            }
    }

    /**
     * Validates the signature details in the given device inventory request.
     * <p>
     * This method checks mandatory signatures (inventory and static inventory signatures)
     * for presence and validity, and conditionally validates the raw inventory signature
     * if raw inventory details are provided.
     * <p>
     * Note: This method does not perform the actual decoding or cryptographic verification,
     * only presence and non-nullity checks of signatures.
     *
     * @param inventory the device inventory request body to validate
     * @throws ValidationException if any required signature details are missing or invalid
     */
    public static void validateDeviceInventorySignatureDetails(DdiDeviceInventory inventory) {
        validateSignature(inventory.getInventorySignature(), "Device Inventory");
        if(inventory.getStaticInventorySignature() != null && inventory.getStaticInventoryHash() != null) {
            validateSignature(inventory.getStaticInventorySignature(), "Device Static Inventory");
        }
        if (inventory.getRawInventorySignature() != null && inventory.getRawInventoryDetails() != null) {
            validateSignature(inventory.getRawInventorySignature(), "Raw Inventory");
        }
    }

    /**
     * Validates that the provided signature object and its signature content are not null.
     *
     * @param signature the signature object to validate
     * @param context a descriptive context used for logging and exception messages
     * @throws ValidationException if the signature or its content is null
     */
    private static void validateSignature(DdiSignature signature, String context) {
        if (Objects.isNull(signature) || Objects.isNull(signature.getSignature())) {
            LOG.error("Missing {} signature details", context);
            throw new ValidationException("Invalid or null " + context + " signature details provided");
        }
    }

    /**
     * @param inventory device inventory in encoded form
     * @return DeviceInventoryDetails object
     * @throws JsonProcessingException if object cannot be mapped to DeviceInventoryDetails
     */
    public static DeviceInventoryDetails decodeAndCreateDeviceInventory(DdiDeviceInventory inventory) throws JsonProcessingException {
        // TODO: Using com.nimbusds.jose.util.Base64 library which needs to be addressed later.
        byte[] decodedBytes = new com.nimbusds.jose.util.Base64(inventory.getInventoryDetails()).decode();
        String decodedString = new String(decodedBytes);
        return mapper.readValue(decodedString, DeviceInventoryDetails.class);
    }

    /**
     * @param inventoryDetails which will be validated
     * @param controllerId
     * @return Map<String, String>
     * @throws ValidationException
     */
    public static Map<String, String> validateDeviceInventoryAndCreateTargetAttributes(DdiDeviceInventory inventory, DeviceInventoryDetails inventoryDetails, String controllerId)
    {

        Map<String, String> targetAttributes = initializeTargetAttributes(inventory);
        validateInventoryDetails(inventoryDetails, controllerId);
        validateEcuListAndPopulateAttributes(inventoryDetails.getEcuList(), targetAttributes);
        return targetAttributes;
    }

    private static Map<String, String> initializeTargetAttributes(DdiDeviceInventory inventory) {

        Map<String, String> targetAttributes = new HashMap<>();
        targetAttributes.put("inventory.signature", inventory.getInventorySignature().getSignature());
        targetAttributes.put("inventory.signatureType", inventory.getInventorySignature().getSignatureType());
        if(inventory.getStaticInventoryHash() != null && inventory.getStaticInventorySignature() != null){
            targetAttributes.put("staticInventoryHash", inventory.getStaticInventoryHash());
            targetAttributes.put("staticInventory.signature", inventory.getStaticInventorySignature().getSignature());
            targetAttributes.put("staticInventory.signatureType", inventory.getStaticInventorySignature().getSignatureType());
        }
        if (inventory.getRawInventorySignature() != null) {
            targetAttributes.put("rawInventory.signature", inventory.getRawInventorySignature().getSignature());
            targetAttributes.put("rawInventory.signatureType", inventory.getRawInventorySignature().getSignatureType());
        }
        return targetAttributes;
    }

    private static void validateInventoryDetails(DeviceInventoryDetails inventoryDetails, String controllerId) {
        if (inventoryDetails == null) {
            throw new ValidationException("Device Inventory Details cannot be null");
        }
        if (!controllerId.equals(inventoryDetails.getVin())) {
            LOG.error("VIN provided in the inventory does not match the controller ID");
            throw new ValidationException("Vin provided in the inventory not same as controller id");
        }
    }

    private static void validateEcuListAndPopulateAttributes(List<Ecu> ecuList, Map<String, String> targetAttributes) {
        if (ecuList == null || ecuList.isEmpty()) {
            throw new ValidationException("Inventory must contain minimum 1 ECU in ecuList");
        }

        for (Ecu ecu : ecuList) {
            validateEcu(ecu);
            populateEcuAttributes(ecu, targetAttributes);
        }
    }

    private static void validateEcu(Ecu ecu) {
        // Validate basic ECU fields
        if (isNullOrBlank(ecu.getNodeAddress()) || isNullOrBlank(ecu.getPartNumber()) || isNullOrBlank(ecu.getHwVersion()) || ecu.getScomos() == null) {
            throw new ValidationException("Node Address/ Part Number/ Hardware version / Scomos are missing in ECU list");
        }

        // Validate Scomos list
        if (ecu.getScomos().isEmpty()) {
            throw new ValidationException("Scomos for ECU cannot be empty");
        }

        // Validate Scomos fields
        for (Scomos scomos : ecu.getScomos()) {
            if (isNullOrBlank(scomos.getScomoId()) || isNullOrBlank(scomos.getSwVersion())) {
                throw new ValidationException("ScomoId or SwVersion missing");
            }
        }
    }

    private static void populateEcuAttributes(Ecu ecu, Map<String, String> targetAttributes) {
        targetAttributes.compute(ECU_NODE_ADDR, (k, v) -> appendToAttribute(v, ecu.getNodeAddress()));
        targetAttributes.compute(ECU_PART_NUMBER, (k, v) -> appendToAttribute(v, ecu.getPartNumber()));
        targetAttributes.compute(ECU_HW_VERSION, (k, v) -> appendToAttribute(v, ecu.getHwVersion()));
        targetAttributes.compute(ECU_SERIAL_NUMBER, (k, v) -> appendToAttribute(v, ecu.getSerialNumber()));

        for (Scomos scomos : ecu.getScomos()) {
            targetAttributes.compute(ECU_SCOMOS_SCOMO_ID, (k, v) -> appendToAttribute(v, scomos.getScomoId()));
            targetAttributes.compute(ECU_SCOMOS_SW_VERSION, (k, v) -> appendToAttribute(v, scomos.getSwVersion()));
        }
    }

    private static String appendToAttribute(String existingValue, String newValue) {
        return StringUtils.hasText(existingValue) ? existingValue + "," + newValue : newValue;
    }

    /**
     * Check feedback status is Closed with Success or Failure
     * i.e. {@link DeviceActionStatus# FINISHED}/{@link DeviceActionStatus#ERROR}
     *
     * @param status {@link DeviceActionStatus#FINISHED}/{@link DeviceActionStatus#ERROR}
     * @return boolean
     */
    public static boolean isStatusFinishedOrError(DeviceActionStatus status) {
        return DeviceActionStatus.FINISHED_FAILURE.equals(status) || DeviceActionStatus.ERROR_RESPONSE_CODE.equals(status) || DeviceActionStatus.FINISHED_SUCCESS.equals(status);
    }

    /**
     * Check whether deployment log collection should be requested or not.
     * The validation is based on {@link DeviceActionStatus#FINISHED}/{@link DeviceActionStatus#ERROR} and {@link Rollout}'s log collection limits.
     * Log won't be collected for null {@link Rollout}'s actions.
     *
     * @param rollout              the {@link Rollout}
     * @param status               the {@link DeviceActionStatus#FINISHED}/{@link DeviceActionStatus#ERROR}
     * @param controllerManagement the {@link ControllerManagement#countActionStatusByRolloutIdAndStatus(Long, DeviceActionStatus)}
     * @return boolean
     */
    public static boolean isRequestDeploymentLog(Rollout rollout, DeviceActionStatus status, ControllerManagement controllerManagement) {
        if (rollout == null) {
            LOG.debug("Rollout is null, returning false");
            return false;
        }
        if (Boolean.FALSE.equals(rollout.isLogCollectionRequired())) {
            LOG.debug("Log collection is not required for rollout id: {}", rollout.getId());
            return false;
        }
        Integer logStatusVinMax = (DeviceActionStatus.FINISHED_SUCCESS.equals(status) ? rollout.getLogMaxSuccessVin() : rollout.getLogMaxFailureVin());
        Long countOfActionStatusVin = controllerManagement.countActionStatusByRolloutIdAndStatus(rollout.getId(), status);
        LOG.debug("Rollout id: {}, Status: {}, LogStatusVinMax: {}, CountOfActionStatusVin: {}", rollout.getId(), status, logStatusVinMax, countOfActionStatusVin);
        return countOfActionStatusVin <= logStatusVinMax;
    }


    /**
     * Encodes the given deployment descriptor by:
     * <ul>
     *   <li>Serializing it to JSON</li>
     *   <li>Encoding the JSON as Base64</li>
     *   <li>Computing a SHA-256 hash of the Base64 string</li>
     *   <li>Base64 encoding the resulting hash</li>
     * </ul>
     *
     * @param deploymentDescriptor the {@link DdiDeploymentDescriptor} to encode
     * @return the Base64-encoded SHA-256 hash of the Base64-encoded JSON representation
     * @throws CosmosSignatureException if encoding or hashing fails
     */
    public static String encodeDeploymentDescriptor(DdiDeploymentDescriptor deploymentDescriptor) {
        try {
            // Step 1: Serialize deploymentDescriptor to JSON and then to byte array.
            String deploymentDescriptorJson = mapper.writeValueAsString(deploymentDescriptor);
            byte[] deploymentDescriptorJsonBytes = deploymentDescriptorJson.getBytes(StandardCharsets.UTF_8);

            // Step 2: Encode the byte array using Base64.
            String base64EncodedDescriptorJson = Base64.getEncoder().encodeToString(deploymentDescriptorJsonBytes);

            // Step 3: Compute SHA-256 hash of the Base64-encoded string.
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] sha256Hash = digest.digest(base64EncodedDescriptorJson.getBytes(StandardCharsets.UTF_8));

            // Step 4: Base64 encode the SHA-256 hash and return as String.
            return Base64.getEncoder().encodeToString(sha256Hash);
        } catch (JsonProcessingException e) {
            LOG.error("Error while encoding deployment descriptor", e);
            throw new CosmosSignatureException("Error while encoding deployment descriptor", e);
        } catch (NoSuchAlgorithmException e) {
            throw new CosmosSignatureException("Algorithm not found", e);
        }
    }

    /**
     * Utility method for null/blank check
     * @param str String to be validated
     * @return boolean
     */
    private static boolean isNullOrBlank(String str) {
        return !StringUtils.hasText(str);
    }
}
