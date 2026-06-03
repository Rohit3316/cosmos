package org.cosmos.kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KafkaApplicationStart {

    /**
     * Main method to start the spring-boot application.
     *
     * @param args
     *            the VM arguments.
     */
    public static void main(final String[] args) {
        SpringApplication.run(KafkaApplicationStart.class, args);
    }
}
