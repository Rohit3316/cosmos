package org.cosmos.kafka.config;

import java.util.HashMap;
import java.util.Map;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * Kafka configuration class that sets up the ProducerFactory for Kafka.
 * It uses properties defined in KafkaProperties to configure the producer.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
@ConditionalOnProperty(name = "cosmos.server.kafka.enabled", havingValue = "true", matchIfMissing = false)
@ConditionalOnClass({KafkaConfig.class})
public class KafkaConfig {

    /**
     * KafkaProperties holds the configuration properties for Kafka.
     * It is injected into this configuration class to access the necessary properties.
     */
    private final KafkaProperties kafkaProperties;

    /**
     * Property to allow auto-creation of topics in Kafka.
     * This is set to false to prevent automatic topic creation.
     */
    public static final String ALLOW_AUTO_CREATE_TOPICS = "allow.auto.create.topics";

    public KafkaConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    /**
     * ProducerFactory configuration using reusable serializer properties.
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    /**
     * KafkaTemplate bean for sending messages.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * ConsumerFactory configuration using reusable deserializer properties.
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }

    /**
     * KafkaListener factory setup using the reusable consumer factory.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    /**
     * Reusable Kafka producer configuration.
     */
    private Map<String, Object> producerConfigs() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ALLOW_AUTO_CREATE_TOPICS, false);
        return config;
    }

    /**
     * Reusable Kafka consumer configuration.
     */
    private Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ALLOW_AUTO_CREATE_TOPICS, false);
        // Note: Don't hardcode group.id here — let each consumer set dynamically
        return props;
    }

    /**
     * Logs a message when the Kafka configuration is loaded.
     * This method is called after the bean is constructed and all properties are set.
     */
    @PostConstruct
    public void logKafkaStartup() {
        log.info("KafkaConfig loaded: Kafka integration is ENABLED.");
    }
}