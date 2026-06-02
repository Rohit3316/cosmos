package org.eclipse.hawkbit.ddi.rest.resource.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * Config class for the DDI Inventory API.
 */

@Component
@PropertySource("classpath:/ddi-resource-default.properties")
@ConfigurationProperties(prefix = "hawkbit.server.ddi.inventory")
@Data
public class InventoryConfig {
    private String publicKey;
    private String signingAlgorithm;
}
