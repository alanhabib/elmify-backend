package com.elmify.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StorageService {

    private static final Logger logger = LoggerFactory.getLogger(StorageService.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final Duration presignedUrlExpiration;

    public StorageService(
            @Value("${elmify.r2.endpoint}") String endpoint,
            @Value("${elmify.r2.access-key}") String accessKey,
            @Value("${elmify.r2.secret-key}") String secretKey,
            @Value("${elmify.r2.region}") String region,
            @Value("${elmify.r2.bucket-name}") String bucketName,
            @Value("${elmify.r2.presigned-url-expiration}") Duration presignedUrlExpiration) {

        this.bucketName = bucketName;
        this.presignedUrlExpiration = presignedUrlExpiration;

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(credentialsProvider)
                .region(Region.of(region))
                .forcePathStyle(true) // Important for MinIO compatibility
                .build();

        this.s3Presigner = S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(credentialsProvider)
                .region(Region.of(region))
                .serviceConfiguration(config -> config.pathStyleAccessEnabled(true))
                .build();

        logger.info("Storage service initialized with endpoint: {}, bucket: {}", endpoint, bucketName);
    }

    /**
     * Generate a presigned URL for streaming audio files
     * Optimized for network streaming with range request support
     */
    public String generatePresignedUrl(String objectKey) {
        try {
            // Build GetObject request with optimizations for streaming
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    // Add response headers to optimize streaming
                    .responseContentType("audio/mpeg") // Explicit content type
                    .responseCacheControl("public, max-age=31536000") // Cache for 1 year
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(presignedUrlExpiration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String url = presignedRequest.url().toString();

            logger.debug("Generated presigned URL for key: {} (expires in {})", objectKey, presignedUrlExpiration);
            return url;
        } catch (Exception e) {
            logger.error("Failed to generate presigned URL for key: {}", objectKey, e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    /**
     * Check if an object exists in storage
     */
    public boolean objectExists(String objectKey) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            logger.error("Error checking if object exists: {}", objectKey, e);
            return false;
        }
    }

    /**
     * Get object metadata
     */
    public ObjectMetadata getObjectMetadata(String objectKey) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            HeadObjectResponse response = s3Client.headObject(headObjectRequest);
            
            return new ObjectMetadata(
                    objectKey,
                    response.contentLength(),
                    response.contentType(),
                    response.lastModified()
            );
        } catch (Exception e) {
            logger.error("Failed to get metadata for object: {}", objectKey, e);
            throw new RuntimeException("Failed to get object metadata", e);
        }
    }

    /**
     * List all objects in the bucket with a prefix
     */
    public List<String> listObjects(String prefix) {
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);
            
            return response.contents().stream()
                    .map(S3Object::key)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Failed to list objects with prefix: {}", prefix, e);
            throw new RuntimeException("Failed to list objects", e);
        }
    }

    /**
     * Get the direct stream of an object (for internal processing)
     */
    public ResponseInputStream<GetObjectResponse> getObjectStream(String objectKey) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            return s3Client.getObject(getObjectRequest);
        } catch (Exception e) {
            logger.error("Failed to get object stream for key: {}", objectKey, e);
            throw new RuntimeException("Failed to get object stream", e);
        }
    }

    /**
     * Get a partial stream of an object for range requests
     */
    public ResponseInputStream<GetObjectResponse> getObjectStreamRange(String objectKey, Long start, Long end) {
        try {
            GetObjectRequest.Builder requestBuilder = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey);
            
            // Add range header if start and end are provided
            if (start != null && end != null) {
                requestBuilder.range("bytes=" + start + "-" + end);
            } else if (start != null) {
                requestBuilder.range("bytes=" + start + "-");
            }

            return s3Client.getObject(requestBuilder.build());
        } catch (Exception e) {
            logger.error("Failed to get object stream range for key: {}", objectKey, e);
            throw new RuntimeException("Failed to get object stream range", e);
        }
    }

    public record ObjectMetadata(
            String key,
            Long size,
            String contentType,
            java.time.Instant lastModified
    ) {}
}