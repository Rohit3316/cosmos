package org.eclipse.hawkbit.repository;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Component
@ConfigurationProperties(prefix = "log")
@Setter
public class DeploymentLogPropertiesConfig {

    private String collectionRequired;
    private String maxSuccessVin;
    private String maxFailureVin;
    private String maxLogAllFileSize;
    private String maxLogEachFileSize;
    private String maxNumberOfFiles;

}
