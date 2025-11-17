package com.elmify.backend.service;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

/**
 * Integration tests for StorageService using LocalStack (S3-compatible storage).
 * These tests verify real interactions with an S3-compatible storage system.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("StorageService Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StorageServiceIntegrationTest {

    private static final String TEST_BUCKET = "test-bucket";
    private static final String TEST_OBJECT_KEY = "test-audio.mp3";
    private static final String TEST_CONTENT = "This is test audio content that simulates an MP3 file";

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:latest"))
        .withServices(S3);

    private static S3Client s3Client;

    @Autowired
    private StorageService storageService;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("elmify.r2.endpoint", () -> localStack.getEndpointOverride(S3).toString());
        registry.add("elmify.r2.access-key", () -> localStack.getAccessKey());
        registry.add("elmify.r2.secret-key", () -> localStack.getSecretKey());
        registry.add("elmify.r2.region", () -> localStack.getRegion());
        registry.add("elmify.r2.bucket-name", () -> TEST_BUCKET);
        registry.add("elmify.r2.presigned-url-expiration", () -> "PT1H");
    }

    @BeforeAll
    static void setUpAll() {
        // Create S3 client for test setup
        s3Client = S3Client.builder()
            .endpointOverride(localStack.getEndpointOverride(S3))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())))
            .region(Region.of(localStack.getRegion()))
            .forcePathStyle(true)
            .build();

        // Create test bucket
        s3Client.createBucket(CreateBucketRequest.builder()
            .bucket(TEST_BUCKET)
            .build());
    }

    @BeforeEach
    void setUp() {
        // Upload test object before each test
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(TEST_BUCKET)
                .key(TEST_OBJECT_KEY)
                .contentType("audio/mpeg")
                .build(),
            RequestBody.fromString(TEST_CONTENT)
        );
    }

    @AfterEach
    void tearDown() {
        // Clean up test objects
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(TEST_BUCKET)
                .key(TEST_OBJECT_KEY)
                .build());
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should check if object exists")
    void shouldCheckIfObjectExists() {
        assertTrue(storageService.objectExists(TEST_OBJECT_KEY));
        assertFalse(storageService.objectExists("non-existent-file.mp3"));
    }

    @Test
    @Order(2)
    @DisplayName("Should get object metadata")
    void shouldGetObjectMetadata() {
        StorageService.ObjectMetadata metadata = storageService.getObjectMetadata(TEST_OBJECT_KEY);

        assertNotNull(metadata);
        assertEquals(TEST_OBJECT_KEY, metadata.key());
        assertEquals(TEST_CONTENT.length(), metadata.size());
        assertEquals("audio/mpeg", metadata.contentType());
        assertNotNull(metadata.lastModified());
    }

    @Test
    @Order(3)
    @DisplayName("Should list objects with prefix")
    void shouldListObjectsWithPrefix() {
        // Upload additional test objects
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(TEST_BUCKET)
                .key("test-audio-2.mp3")
                .build(),
            RequestBody.fromString("Test content 2")
        );

        List<String> objects = storageService.listObjects("test-");

        assertNotNull(objects);
        assertTrue(objects.size() >= 2);
        assertTrue(objects.contains(TEST_OBJECT_KEY));
        assertTrue(objects.contains("test-audio-2.mp3"));

        // Cleanup
        s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(TEST_BUCKET)
            .key("test-audio-2.mp3")
            .build());
    }

    @Test
    @Order(4)
    @DisplayName("Should stream full object")
    void shouldStreamFullObject() throws IOException {
        ResponseInputStream<GetObjectResponse> stream = storageService.getObjectStream(TEST_OBJECT_KEY);

        assertNotNull(stream);
        String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(TEST_CONTENT, content);
        assertEquals("audio/mpeg", stream.response().contentType());
    }

    @Test
    @Order(5)
    @DisplayName("Should stream object range")
    void shouldStreamObjectRange() throws IOException {
        // Request first 10 bytes
        ResponseInputStream<GetObjectResponse> stream =
            storageService.getObjectStreamRange(TEST_OBJECT_KEY, 0L, 9L);

        assertNotNull(stream);
        String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(TEST_CONTENT.substring(0, 10), content);
        assertEquals(10, content.length());
    }

    @Test
    @Order(6)
    @DisplayName("Should stream object range from middle")
    void shouldStreamObjectRangeFromMiddle() throws IOException {
        // Request bytes 5-14 (10 bytes)
        ResponseInputStream<GetObjectResponse> stream =
            storageService.getObjectStreamRange(TEST_OBJECT_KEY, 5L, 14L);

        assertNotNull(stream);
        String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(TEST_CONTENT.substring(5, 15), content);
        assertEquals(10, content.length());
    }

    @Test
    @Order(7)
    @DisplayName("Should stream object range to end")
    void shouldStreamObjectRangeToEnd() throws IOException {
        long start = TEST_CONTENT.length() - 10L;
        ResponseInputStream<GetObjectResponse> stream =
            storageService.getObjectStreamRange(TEST_OBJECT_KEY, start, null);

        assertNotNull(stream);
        String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(TEST_CONTENT.substring((int) start), content);
    }

    @Test
    @Order(8)
    @DisplayName("Should generate presigned URL")
    void shouldGeneratePresignedUrl() {
        String presignedUrl = storageService.generatePresignedUrl(TEST_OBJECT_KEY);

        assertNotNull(presignedUrl);
        assertTrue(presignedUrl.contains(TEST_BUCKET));
        assertTrue(presignedUrl.contains(TEST_OBJECT_KEY));
        assertTrue(presignedUrl.contains("X-Amz-Algorithm"));
        assertTrue(presignedUrl.contains("X-Amz-Signature"));
    }

    @Test
    @Order(9)
    @DisplayName("Should throw exception for non-existent object metadata")
    void shouldThrowExceptionForNonExistentObjectMetadata() {
        assertThrows(RuntimeException.class, () ->
            storageService.getObjectMetadata("non-existent-file.mp3")
        );
    }

    @Test
    @Order(10)
    @DisplayName("Should throw exception for non-existent object stream")
    void shouldThrowExceptionForNonExistentObjectStream() {
        assertThrows(RuntimeException.class, () ->
            storageService.getObjectStream("non-existent-file.mp3")
        );
    }
}
