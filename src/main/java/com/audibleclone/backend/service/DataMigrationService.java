package com.audibleclone.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for migrating data from Cloudflare R2 to local MinIO. This service should be used to
 * migrate existing audio files during development setup.
 */
@Service
public class DataMigrationService {

  private static final Logger logger = LoggerFactory.getLogger(DataMigrationService.class);

  private final S3Client sourceClient; // R2 client
  private final S3Client targetClient; // MinIO client
  private final String sourceBucket;
  private final String targetBucket;
  private final ExecutorService executorService;

  public DataMigrationService(
      @Value("${audibleclone.migration.source.endpoint}") String sourceEndpoint,
      @Value("${audibleclone.migration.source.access-key:#{null}}") String sourceAccessKey,
      @Value("${audibleclone.migration.source.secret-key:#{null}}") String sourceSecretKey,
      @Value("${audibleclone.migration.source.bucket:elmify-audio}") String sourceBucket,
      @Value("${audibleclone.r2.endpoint}") String targetEndpoint,
      @Value("${audibleclone.r2.access-key}") String targetAccessKey,
      @Value("${audibleclone.r2.secret-key}") String targetSecretKey,
      @Value("${audibleclone.r2.bucket-name}") String targetBucket) {

    this.sourceBucket = sourceBucket;
    this.targetBucket = targetBucket;
    this.executorService = Executors.newFixedThreadPool(4); // 4 concurrent transfers

    // Initialize source client (R2) - only if credentials are provided
    if (sourceAccessKey != null && !sourceAccessKey.isBlank() && 
        sourceSecretKey != null && !sourceSecretKey.isBlank() &&
        !sourceAccessKey.equals("dummy-access-key")) {
      AwsBasicCredentials sourceCredentials =
          AwsBasicCredentials.create(sourceAccessKey, sourceSecretKey);
      this.sourceClient =
          S3Client.builder()
              .endpointOverride(URI.create(sourceEndpoint))
              .credentialsProvider(StaticCredentialsProvider.create(sourceCredentials))
              .region(Region.of("auto"))
              .forcePathStyle(true)
              .build();
      logger.info("Source R2 client initialized for migration: {}", sourceEndpoint);
    } else {
      this.sourceClient = null;
      logger.warn("Source R2 credentials not provided - migration disabled");
    }

    // Initialize target client (MinIO) - only if credentials are valid
    if (targetAccessKey != null && !targetAccessKey.isBlank() && 
        targetSecretKey != null && !targetSecretKey.isBlank()) {
      AwsBasicCredentials targetCredentials =
          AwsBasicCredentials.create(targetAccessKey, targetSecretKey);
      this.targetClient =
          S3Client.builder()
              .endpointOverride(URI.create(targetEndpoint))
              .credentialsProvider(StaticCredentialsProvider.create(targetCredentials))
              .region(Region.of("us-east-1"))
              .forcePathStyle(true)
              .build();
      logger.info("Target MinIO client initialized: {}", targetEndpoint);
    } else {
      this.targetClient = null;
      logger.warn("Target MinIO credentials not provided - MinIO functionality disabled");
    }
  }

  /** Migrate all objects from R2 to MinIO */
  public MigrationResult migrateAllData() {
    if (sourceClient == null) {
      throw new IllegalStateException(
          "Source R2 client not configured - please provide R2 credentials");
    }
    if (targetClient == null) {
      throw new IllegalStateException(
          "Target MinIO client not configured - please provide MinIO credentials");
    }

    logger.info(
        "Starting migration from R2 bucket '{}' to MinIO bucket '{}'", sourceBucket, targetBucket);

    try {
      // Ensure target bucket exists
      createBucketIfNotExists();

      // List all objects in source bucket
      List<String> objectKeys = listAllObjects();
      logger.info("Found {} objects to migrate", objectKeys.size());

      if (objectKeys.isEmpty()) {
        return new MigrationResult(0, 0, 0, "No objects found in source bucket");
      }

      // Start migration
      long startTime = System.currentTimeMillis();
      int successful = 0;
      int failed = 0;

      for (String objectKey : objectKeys) {
        try {
          if (migrateObject(objectKey)) {
            successful++;
            logger.debug("Successfully migrated: {}", objectKey);
          } else {
            failed++;
            logger.warn("Failed to migrate: {}", objectKey);
          }
        } catch (Exception e) {
          failed++;
          logger.error("Error migrating object {}: {}", objectKey, e.getMessage());
        }
      }

      long duration = System.currentTimeMillis() - startTime;
      String message =
          String.format(
              "Migration completed in %d ms. %d successful, %d failed",
              duration, successful, failed);
      logger.info(message);

      return new MigrationResult(objectKeys.size(), successful, failed, message);

    } catch (Exception e) {
      logger.error("Migration failed: {}", e.getMessage(), e);
      throw new RuntimeException("Migration failed", e);
    }
  }

  /** Migrate a specific object */
  private boolean migrateObject(String objectKey) {
    if (targetClient == null) {
      logger.error("Target MinIO client not configured");
      return false;
    }
    try {
      // Check if object already exists in target
      if (objectExistsInTarget(objectKey)) {
        logger.debug("Object already exists in target, skipping: {}", objectKey);
        return true;
      }

      // Get object from source
      GetObjectRequest getRequest =
          GetObjectRequest.builder().bucket(sourceBucket).key(objectKey).build();

      ResponseInputStream<GetObjectResponse> sourceStream = sourceClient.getObject(getRequest);
      GetObjectResponse sourceResponse = sourceStream.response();

      // Put object to target
      PutObjectRequest putRequest =
          PutObjectRequest.builder()
              .bucket(targetBucket)
              .key(objectKey)
              .contentType(sourceResponse.contentType())
              .contentLength(sourceResponse.contentLength())
              .build();

      targetClient.putObject(
          putRequest, RequestBody.fromInputStream(sourceStream, sourceResponse.contentLength()));
      sourceStream.close();

      return true;
    } catch (Exception e) {
      logger.error("Failed to migrate object {}: {}", objectKey, e.getMessage());
      return false;
    }
  }

  /** Check if object exists in target bucket */
  private boolean objectExistsInTarget(String objectKey) {
    if (targetClient == null) {
      return false;
    }
    try {
      HeadObjectRequest headRequest =
          HeadObjectRequest.builder().bucket(targetBucket).key(objectKey).build();
      targetClient.headObject(headRequest);
      return true;
    } catch (NoSuchKeyException e) {
      return false;
    } catch (Exception e) {
      logger.warn("Error checking object existence in target: {}", e.getMessage());
      return false;
    }
  }

  /** Create target bucket if it doesn't exist */
  private void createBucketIfNotExists() {
    if (targetClient == null) {
      throw new IllegalStateException("Target MinIO client not configured");
    }
    try {
      HeadBucketRequest headRequest = HeadBucketRequest.builder().bucket(targetBucket).build();
      targetClient.headBucket(headRequest);
      logger.debug("Target bucket '{}' already exists", targetBucket);
    } catch (NoSuchBucketException e) {
      logger.info("Creating target bucket: {}", targetBucket);
      CreateBucketRequest createRequest =
          CreateBucketRequest.builder().bucket(targetBucket).build();
      targetClient.createBucket(createRequest);
      logger.info("Target bucket '{}' created successfully", targetBucket);
    }
  }

  /** List all objects in source bucket */
  private List<String> listAllObjects() {
    try {
      ListObjectsV2Request listRequest =
          ListObjectsV2Request.builder().bucket(sourceBucket).build();

      ListObjectsV2Response response = sourceClient.listObjectsV2(listRequest);
      return response.contents().stream().map(S3Object::key).toList();
    } catch (Exception e) {
      logger.error("Failed to list objects from source bucket: {}", e.getMessage());
      throw new RuntimeException("Failed to list source objects", e);
    }
  }

  /** Get migration status */
  public MigrationStatus getMigrationStatus() {
    try {
      if (sourceClient == null) {
        return new MigrationStatus(false, "Source R2 client not configured", 0, 0);
      }

      // Count objects in source and target
      int sourceCount = listAllObjects().size();
      int targetCount = listTargetObjects().size();

      boolean completed = sourceCount > 0 && sourceCount == targetCount;
      String status =
          completed
              ? "Migration completed"
              : String.format("Source: %d objects, Target: %d objects", sourceCount, targetCount);

      return new MigrationStatus(completed, status, sourceCount, targetCount);
    } catch (Exception e) {
      return new MigrationStatus(false, "Error checking migration status: " + e.getMessage(), 0, 0);
    }
  }

  /** List objects in target bucket */
  private List<String> listTargetObjects() {
    if (targetClient == null) {
      return List.of();
    }
    try {
      ListObjectsV2Request listRequest =
          ListObjectsV2Request.builder().bucket(targetBucket).build();

      ListObjectsV2Response response = targetClient.listObjectsV2(listRequest);
      return response.contents().stream().map(S3Object::key).toList();
    } catch (Exception e) {
      logger.warn("Failed to list objects from target bucket: {}", e.getMessage());
      return List.of();
    }
  }

  public record MigrationResult(int totalObjects, int successful, int failed, String message) {}

  public record MigrationStatus(
      boolean completed, String status, int sourceCount, int targetCount) {}
}
