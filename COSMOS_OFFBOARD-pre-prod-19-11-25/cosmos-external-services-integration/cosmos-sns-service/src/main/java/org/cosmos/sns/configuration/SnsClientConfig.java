// package org.cosmos.sns.configuration;


// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

// import software.amazon.awssdk.services.sns.SnsAsyncClient;

// @Configuration
// public class SnsClientConfig {


//     @Bean
//     public SnsAsyncClient amazonSNS() {
//         return SnsAsyncClient.create();
//     }

// }


//Rohit Salunkhe

package org.cosmos.sns.configuration;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsAsyncClient;

@Configuration
public class SnsClientConfig {

@Primary
@Bean(name = "snsAsyncClient")
public SnsAsyncClient amazonSNS() {

    SnsAsyncClient client = SnsAsyncClient.builder()
            .endpointOverride(URI.create("http://localhost:4566"))
            .region(Region.AP_SOUTH_1)
            .credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("test", "test")
                    )
            )
            .build();

    client.listTopics().join();

    System.out.println("SNS CONNECTED SUCCESSFULLY");

    return client;
}
}

//Rohit Salunkhe