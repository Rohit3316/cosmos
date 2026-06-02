package org.eclipse.hawkbit.repository.file.supportpackage.configuration;

import org.eclipse.hawkbit.repository.file.supportpackage.SupportsPackageFileSystemRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * This class provides configurations for the {@link SupportsPackageFileSystemRepository} and a custom {@link RestTemplate} for file downloads.
 *
 * @author Tabnine
 */
@Configuration
@EnableConfigurationProperties(SupportPackageFileSystemProperties.class)
public class SupportPackageFileSystemConfiguration {

    /**
     * Creates a new instance of {@link SupportsPackageFileSystemRepository} using the provided properties and a {@link RestTemplate}.
     *
     * @param supportPackageFileSystemProperties The properties for configuring the repository.
     * @param restTemplate The {@link RestTemplate} for making HTTP requests.
     * @return A new instance of {@link SupportsPackageFileSystemRepository}.
     */
    @Bean
    public SupportsPackageFileSystemRepository getSupportsPackageFileSystemRepository(final SupportPackageFileSystemProperties supportPackageFileSystemProperties, final  RestTemplate restTemplate) {
        return new SupportsPackageFileSystemRepository(supportPackageFileSystemProperties ,restTemplate);
    }

    /**
     * Creates a new instance of {@link RestTemplate} with a longer connect timeout for file downloads.
     *
     * @param builder The {@link RestTemplateBuilder} for configuring the {@link RestTemplate}.
     * @return A new instance of {@link RestTemplate} with a longer connect timeout.
     */
    @Bean
    public RestTemplate fileDownloadRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))  // Longer timeout for file downloads
                .build();
    }
}