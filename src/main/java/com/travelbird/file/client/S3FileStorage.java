package com.travelbird.file.client;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * S3 Presigned URL 발급/실제 업로드 객체 조회. 기능명세서 3.5.1 — WebP 최적화/EXIF 제거 등
 * 이미지 가공은 프론트엔드 책임이고, 이 클래스는 S3 오브젝트 자체만 다룬다.
 */
@Component
public class S3FileStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public S3FileStorage(S3Client s3Client, S3Presigner s3Presigner, @Value("${aws.s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
    }

    public record PresignedUpload(URL url, Instant expiresAt) {
    }

    public PresignedUpload createPresignedPutUrl(String objectKey, String contentType, Duration validity) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(validity)
                .putObjectRequest(putObjectRequest)
                .build();

        var presigned = s3Presigner.presignPutObject(presignRequest);
        return new PresignedUpload(presigned.url(), Instant.now().plus(validity));
    }

    public boolean exists(String objectKey) {
        try {
            headObject(objectKey);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    public HeadObjectResponse headObject(String objectKey) {
        return s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(objectKey).build());
    }

    public byte[] getObjectBytes(String objectKey) {
        return s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(objectKey).build())
                .asByteArray();
    }

    public void delete(String objectKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
    }
}
