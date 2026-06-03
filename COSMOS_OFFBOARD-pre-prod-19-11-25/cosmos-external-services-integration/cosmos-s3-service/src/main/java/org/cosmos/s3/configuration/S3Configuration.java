// package org.cosmos.s3.configuration;

// import org.cosmos.s3.ChecksumCalculator;
// import org.cosmos.s3.DataSizeConverter;
// import org.cosmos.s3.S3MultipartFileUpload;
// import org.cosmos.s3.S3Repository;
// import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.scheduling.annotation.EnableAsync;

// import software.amazon.awssdk.services.s3.S3Client;

// @Configuration
// @ConditionalOnClass({S3Configuration.class})
// @EnableAsync
// public class S3Configuration {
//     /**
//      * Bean creation of S3 repository for S3 service.
//      *
//      * @return {@link S3Repository} the repository.
//      */
//     @Bean
//     public S3Repository s3Repository() {
//         return new S3Repository();
//     }

//     // @Bean
//     // public S3MultipartFileUpload s3MutipartFileUpload(){
//     //     return new S3MultipartFileUpload(s3Client);
//     // }

//     //Rohit Salunkhe
//     @Bean
// public S3MultipartFileUpload s3MultipartFileUpload(S3Client s3Client) {
//     return new S3MultipartFileUpload(s3Client);
// }
// //Rohit Salunkhe

//     @Bean
//     public DataSizeConverter dataSizeConverter(){
//         return new DataSizeConverter();
//     }

//     @Bean
//     public ChecksumCalculator checksumCalculator() {
//         return new ChecksumCalculator();
//     }
    

// }


//Rohit Salunkhe
package org.cosmos.s3.configuration;

import java.net.URI;

import org.cosmos.s3.ChecksumCalculator;
import org.cosmos.s3.DataSizeConverter;
import org.cosmos.s3.S3MultipartFileUpload;
import org.cosmos.s3.S3Repository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@ConditionalOnClass({S3Configuration.class})
@EnableAsync
public class S3Configuration {

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create("http://localhost:4566"))
                .region(Region.AP_SOUTH_1)
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create("test", "test")
                        )
                )
                .forcePathStyle(true)
                .build();
    }

    @Bean
    public S3Repository s3Repository() {
        return new S3Repository();
    }

    @Bean
    public S3MultipartFileUpload s3MultipartFileUpload(S3Client s3Client) {
        return new S3MultipartFileUpload(s3Client);
    }

    @Bean
    public DataSizeConverter dataSizeConverter() {
        return new DataSizeConverter();
    }

    @Bean
    public ChecksumCalculator checksumCalculator() {
        return new ChecksumCalculator();
    }
}

//Rohit Salunkhe
