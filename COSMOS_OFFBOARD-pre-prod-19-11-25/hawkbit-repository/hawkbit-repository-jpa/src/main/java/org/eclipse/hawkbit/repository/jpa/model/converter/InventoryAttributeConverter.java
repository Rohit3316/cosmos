package org.eclipse.hawkbit.repository.jpa.model.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetInventory;
import org.cosmos.models.ddi.DeviceInventoryDetails;
import org.eclipse.persistence.exceptions.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converter class used to convert Inventory Details from {@link DeviceInventoryDetails} in {@link JpaTargetInventory} to
 * Text type inventory column in sp_target_inventory table
 */
@Converter
public class InventoryAttributeConverter implements AttributeConverter<DeviceInventoryDetails, String> {
    private static final Logger LOG = LoggerFactory.getLogger(InventoryAttributeConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Convert from {@link DeviceInventoryDetails} to Text Column
     *
     * @param deviceInventoryDetails {@link DeviceInventoryDetails}
     * @return String, the inventory representation in JSON String
     */
    @Override
    public String convertToDatabaseColumn(DeviceInventoryDetails deviceInventoryDetails) {
        try {
            return objectMapper.writeValueAsString(deviceInventoryDetails);
        } catch (JsonProcessingException jpe) {
            LOG.error("Cannot convert DeviceInventoryDetails into JSON String");
            throw ValidationException.cannotCastToClass(deviceInventoryDetails, DeviceInventoryDetails.class, String.class);
        }
    }

    /**
     * Convert from Text Column to {@link DeviceInventoryDetails}
     *
     * @param value String, the inventory representation in JSON String
     * @return {@link DeviceInventoryDetails}
     */
    @Override
    public DeviceInventoryDetails convertToEntityAttribute(String value) {
        try {
            return objectMapper.readValue(value, DeviceInventoryDetails.class);
        } catch (JsonProcessingException e) {
            LOG.error("Cannot convert JSON String into DeviceInventoryDetails");
            throw ValidationException.cannotCastToClass(value, String.class, DeviceInventoryDetails.class);
        }
    }
}
