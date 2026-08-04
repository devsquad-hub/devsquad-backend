package com.devsquad.shared.config;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.net.URI;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.LegacyMd5Plugin;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@ApplicationScoped
public class S3Configuration {

  @Produces
  @Singleton
  S3Client s3Client(
      @ConfigProperty(name = "app.storage.endpoint") URI endpoint,
      @ConfigProperty(name = "app.storage.region") String region,
      @ConfigProperty(name = "app.storage.access-key") String accessKey,
      @ConfigProperty(name = "app.storage.secret-key") String secretKey) {
    return S3Client.builder()
        .endpointOverride(endpoint)
        .region(Region.of(region))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
        .httpClientBuilder(UrlConnectionHttpClient.builder())
        .addPlugin(LegacyMd5Plugin.create())
        .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
        .serviceConfiguration(pathStyle())
        .build();
  }

  @Produces
  @Singleton
  S3Presigner s3Presigner(
      @ConfigProperty(name = "app.storage.public-endpoint") URI endpoint,
      @ConfigProperty(name = "app.storage.region") String region,
      @ConfigProperty(name = "app.storage.access-key") String accessKey,
      @ConfigProperty(name = "app.storage.secret-key") String secretKey) {
    return S3Presigner.builder()
        .endpointOverride(endpoint)
        .region(Region.of(region))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
        .serviceConfiguration(pathStyle())
        .build();
  }

  void ensureStorageBucket(
      @Observes StartupEvent event,
      S3Client client,
      @ConfigProperty(name = "app.storage.bucket") String bucket,
      @ConfigProperty(name = "app.storage.initialize-bucket") boolean initialize) {
    if (!initialize) return;
    try {
      client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
    } catch (S3Exception exception) {
      if (exception.statusCode() != 404) throw exception;
      client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
    }
  }

  void closeClient(@Disposes S3Client client) {
    client.close();
  }

  void closePresigner(@Disposes S3Presigner presigner) {
    presigner.close();
  }

  private static software.amazon.awssdk.services.s3.S3Configuration pathStyle() {
    return software.amazon.awssdk.services.s3.S3Configuration.builder()
        .pathStyleAccessEnabled(true)
        .chunkedEncodingEnabled(false)
        .build();
  }
}
