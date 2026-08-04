package com.devsquad.shared.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.CORSConfiguration;
import software.amazon.awssdk.services.s3.model.CORSRule;
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration(proxyBeanMethods = false)
public class S3Configuration {

    @Bean
    S3Client s3Client(
            @Value("${app.storage.endpoint}") URI endpoint,
            @Value("${app.storage.region}") String region,
            @Value("${app.storage.access-key}") String accessKey,
            @Value("${app.storage.secret-key}") String secretKey) {
        return S3Client.builder().endpointOverride(endpoint).region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .serviceConfiguration(pathStyle()).build();
    }

    @Bean
    S3Presigner s3Presigner(
            @Value("${app.storage.public-endpoint}") URI endpoint,
            @Value("${app.storage.region}") String region,
            @Value("${app.storage.access-key}") String accessKey,
            @Value("${app.storage.secret-key}") String secretKey) {
        return S3Presigner.builder().endpointOverride(endpoint).region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(pathStyle()).build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.storage.initialize-bucket", havingValue = "true", matchIfMissing = true)
    ApplicationRunner ensureStorageBucket(
            S3Client client,
            @Value("${app.storage.bucket}") String bucket,
            @Value("${app.security.allowed-origins:http://localhost:3000}") String allowedOrigins) {
        return arguments -> {
            try {
                client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            } catch (S3Exception exception) {
                if (exception.statusCode() != 404) throw exception;
                client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            }
            var rule = CORSRule.builder()
                    .allowedMethods("GET", "PUT")
                    .allowedHeaders("*")
                    .allowedOrigins(java.util.Arrays.stream(allowedOrigins.split(","))
                            .map(String::trim).filter(value -> !value.isBlank()).toList())
                    .exposeHeaders("ETag")
                    .maxAgeSeconds(3_600)
                    .build();
            client.putBucketCors(PutBucketCorsRequest.builder().bucket(bucket)
                    .corsConfiguration(CORSConfiguration.builder().corsRules(rule).build()).build());
        };
    }

    private static software.amazon.awssdk.services.s3.S3Configuration pathStyle() {
        return software.amazon.awssdk.services.s3.S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .chunkedEncodingEnabled(false)
                .build();
    }
}
