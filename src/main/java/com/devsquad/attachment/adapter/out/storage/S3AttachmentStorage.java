package com.devsquad.attachment.adapter.out.storage;

import com.devsquad.attachment.application.port.AttachmentStorage;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ApplicationScoped
public class S3AttachmentStorage implements AttachmentStorage {
  private static final Duration TICKET_DURATION = Duration.ofMinutes(10);

  private final S3Client client;
  private final S3Presigner presigner;
  private final String bucket;
  private final long maxFileSize;

  public S3AttachmentStorage(
      S3Client client,
      S3Presigner presigner,
      @ConfigProperty(name = "app.storage.bucket") String bucket,
      @ConfigProperty(name = "app.storage.max-file-size") long maxFileSize) {
    this.client = client;
    this.presigner = presigner;
    this.bucket = bucket;
    this.maxFileSize = maxFileSize;
  }

  @Override
  public long maxFileSize() {
    return maxFileSize;
  }

  @Override
  public SignedUpload signUpload(String objectKey, String contentType, long sizeBytes) {
    var request =
        PutObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .contentType(contentType)
            .contentLength(sizeBytes)
            .build();
    var signed =
        presigner.presignPutObject(
            PutObjectPresignRequest.builder()
                .signatureDuration(TICKET_DURATION)
                .putObjectRequest(request)
                .build());
    return new SignedUpload(signed.url().toString(), signed.signedHeaders());
  }

  @Override
  public long objectSize(String objectKey) {
    return client
        .headObject(HeadObjectRequest.builder().bucket(bucket).key(objectKey).build())
        .contentLength();
  }

  @Override
  public String signDownload(String objectKey) {
    var signed =
        presigner.presignGetObject(
            GetObjectPresignRequest.builder()
                .signatureDuration(TICKET_DURATION)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(objectKey).build())
                .build());
    return signed.url().toString();
  }
}
