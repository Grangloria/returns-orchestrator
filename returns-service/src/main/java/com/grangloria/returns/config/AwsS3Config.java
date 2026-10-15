package com.grangloria.returns.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class AwsS3Config {

    @Value("${aws.s3.endpoint:}")
    private String s3Endpoint;

    @Value("${aws.region:us-east-2}")
    private String region;

    @Bean
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(region));

        if (s3Endpoint != null && !s3Endpoint.isBlank() && s3Endpoint.contains("localhost")) {
            // LocalStack development mode
            builder.endpointOverride(URI.create(s3Endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("test", "test")))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build());
        } else {
            // AWS Production: Inherit ECS task role credentials and use native S3 endpoints
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }
}