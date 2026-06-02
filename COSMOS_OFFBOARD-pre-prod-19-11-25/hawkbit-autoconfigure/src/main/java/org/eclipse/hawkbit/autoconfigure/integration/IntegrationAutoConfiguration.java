package org.eclipse.hawkbit.autoconfigure.integration;

import org.cosmos.kafka.config.KafkaConfig;
import org.cosmos.s3.configuration.S3Configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;
import org.cosmos.sns.configuration.SnsClientConfig;

/**
 * External Services Integration configuration.
 */
@Configuration
@EnableIntegration
@ComponentScan(basePackages = {"com.stellantis.cosmos.sqs.app","org.cosmos.kafka"})
@IntegrationComponentScan(basePackageClasses = {S3Configuration.class, SnsClientConfig.class, KafkaConfig.class})
@Import({S3Configuration.class, SnsClientConfig.class, KafkaConfig.class})
public class IntegrationAutoConfiguration {

}
