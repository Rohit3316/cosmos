package com.cosmos.apm.config;

import com.inetpsa.mpsdk.common.constants.Constants;
import com.inetpsa.mpsdk.registry.MonitorPortalSdkRegister;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration class for integrating Elastic APM with Monitor Portal SDK.
 */
@AutoConfiguration
@EnableConfigurationProperties(ApmAutoConfiguration.ApmProps.class)
@ConditionalOnClass(name = "com.inetpsa.mpsdk.registry.MonitorPortalSdkRegister")
@ConditionalOnProperty(prefix = "elastic.apm", name = "enabled", havingValue = "true")
public class ApmAutoConfiguration {

    @Bean("apmConfig")
    @ConditionalOnMissingBean(name = "apmConfig")
    public Map<String, String> apmConfig(ApmProps p) {
        Map<String, String> m = new HashMap<>();
        m.put(Constants.SERVICE_NAME, p.serviceName());
        m.put(Constants.APM_SERVER_URL, p.serverUrl());
        m.put(Constants.APM_SERVER_ENV, p.environment());
        m.put(Constants.APM_SECRET_TOKEN, p.secretToken());
        m.put(Constants.APM_VERIFY_SERVER_CERT, Boolean.toString(p.verifyServerCert()));
        m.put(Constants.APPLICATION_PACKAGES, p.applicationPackage());
        m.put(Constants.DISABLE_MP_SDK, Boolean.toString(p.disableMpSdk()));
        MonitorPortalSdkRegister.attachMpSdk(m);
        return m;
    }

    // Nested properties holder
    @ConfigurationProperties(prefix = "elastic.apm")
    public static record ApmProps(
            String serviceName,
            String serverUrl,
            String environment,
            String secretToken,
            boolean verifyServerCert,
            String applicationPackage,
            boolean disableMpSdk,
            boolean enabled
    ) {
    }

}
