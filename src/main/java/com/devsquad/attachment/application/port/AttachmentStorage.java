package com.devsquad.attachment.application.port;

import java.util.List;
import java.util.Map;

public interface AttachmentStorage {
    long maxFileSize();

    SignedUpload signUpload(String objectKey, String contentType, long sizeBytes);

    long objectSize(String objectKey);

    String signDownload(String objectKey);

    record SignedUpload(String url, Map<String, List<String>> headers) {}
}
