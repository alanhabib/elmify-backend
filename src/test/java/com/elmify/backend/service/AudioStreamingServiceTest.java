package com.elmify.backend.service;

import com.elmify.backend.config.StreamingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AudioStreamingService Tests")
class AudioStreamingServiceTest {

    @Mock
    private StorageService storageService;

    @Mock
    private StreamingProperties streamingProperties;

    private AudioStreamingService audioStreamingService;

    private static final String TEST_OBJECT_KEY = "lectures/test-lecture.mp3";
    private static final long TEST_FILE_SIZE = 1024 * 1024; // 1MB
    private static final String TEST_CONTENT_TYPE = "audio/mpeg";

    @BeforeEach
    void setUp() {
        // Configure default streaming properties
        when(streamingProperties.getMaxChunkSize()).thenReturn(10 * 1024 * 1024L); // 10MB
        when(streamingProperties.getBufferSize()).thenReturn(8192);
        when(streamingProperties.getCacheMaxAge()).thenReturn(31536000L);
        when(streamingProperties.getEnableDetailedLogging()).thenReturn(false);

        audioStreamingService = new AudioStreamingService(storageService, streamingProperties);
    }

    @Test
    @DisplayName("Should stream full audio file when no range header provided")
    void shouldStreamFullAudioFile() {
        // Arrange
        StorageService.ObjectMetadata metadata = new StorageService.ObjectMetadata(
            TEST_OBJECT_KEY,
            TEST_FILE_SIZE,
            TEST_CONTENT_TYPE,
            Instant.now()
        );

        byte[] testData = new byte[1024];
        ResponseInputStream<GetObjectResponse> mockStream = createMockStream(testData);

        when(storageService.getObjectMetadata(TEST_OBJECT_KEY)).thenReturn(metadata);
        when(storageService.getObjectStream(TEST_OBJECT_KEY)).thenReturn(mockStream);

        // Act
        ResponseEntity<Resource> response = audioStreamingService.streamAudio(TEST_OBJECT_KEY, null);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        HttpHeaders headers = response.getHeaders();
        assertEquals(TEST_CONTENT_TYPE, headers.getFirst(HttpHeaders.CONTENT_TYPE));
        assertEquals("bytes", headers.getFirst(HttpHeaders.ACCEPT_RANGES));
        assertEquals(String.valueOf(TEST_FILE_SIZE), headers.getFirst(HttpHeaders.CONTENT_LENGTH));
        assertTrue(headers.getFirst(HttpHeaders.CACHE_CONTROL).contains("public"));

        verify(storageService).getObjectMetadata(TEST_OBJECT_KEY);
        verify(storageService).getObjectStream(TEST_OBJECT_KEY);
    }

    @Test
    @DisplayName("Should handle range request with start and end")
    void shouldHandleRangeRequestWithStartAndEnd() {
        // Arrange
        StorageService.ObjectMetadata metadata = new StorageService.ObjectMetadata(
            TEST_OBJECT_KEY,
            TEST_FILE_SIZE,
            TEST_CONTENT_TYPE,
            Instant.now()
        );

        String rangeHeader = "bytes=0-1023";
        byte[] testData = new byte[1024];
        ResponseInputStream<GetObjectResponse> mockStream = createMockStream(testData);

        when(storageService.getObjectMetadata(TEST_OBJECT_KEY)).thenReturn(metadata);
        when(storageService.getObjectStreamRange(eq(TEST_OBJECT_KEY), eq(0L), eq(1023L)))
            .thenReturn(mockStream);

        // Act
        ResponseEntity<Resource> response = audioStreamingService.streamAudio(TEST_OBJECT_KEY, rangeHeader);

        // Assert
        assertEquals(HttpStatus.PARTIAL_CONTENT, response.getStatusCode());
        assertNotNull(response.getBody());

        HttpHeaders headers = response.getHeaders();
        assertEquals("bytes 0-1023/" + TEST_FILE_SIZE, headers.getFirst(HttpHeaders.CONTENT_RANGE));
        assertEquals("1024", headers.getFirst(HttpHeaders.CONTENT_LENGTH));

        verify(storageService).getObjectStreamRange(TEST_OBJECT_KEY, 0L, 1023L);
    }

    @Test
    @DisplayName("Should handle range request with only start")
    void shouldHandleRangeRequestWithOnlyStart() {
        // Arrange
        StorageService.ObjectMetadata metadata = new StorageService.ObjectMetadata(
            TEST_OBJECT_KEY,
            TEST_FILE_SIZE,
            TEST_CONTENT_TYPE,
            Instant.now()
        );

        String rangeHeader = "bytes=1024-";
        long expectedEnd = Math.min(1024 + 10 * 1024 * 1024L - 1, TEST_FILE_SIZE - 1);
        byte[] testData = new byte[(int)(expectedEnd - 1024 + 1)];
        ResponseInputStream<GetObjectResponse> mockStream = createMockStream(testData);

        when(storageService.getObjectMetadata(TEST_OBJECT_KEY)).thenReturn(metadata);
        when(storageService.getObjectStreamRange(eq(TEST_OBJECT_KEY), eq(1024L), eq(expectedEnd)))
            .thenReturn(mockStream);

        // Act
        ResponseEntity<Resource> response = audioStreamingService.streamAudio(TEST_OBJECT_KEY, rangeHeader);

        // Assert
        assertEquals(HttpStatus.PARTIAL_CONTENT, response.getStatusCode());
        verify(storageService).getObjectStreamRange(eq(TEST_OBJECT_KEY), eq(1024L), eq(expectedEnd));
    }

    @Test
    @DisplayName("Should limit chunk size to max configured value")
    void shouldLimitChunkSizeToMaxConfiguredValue() {
        // Arrange
        long maxChunkSize = 1024L; // 1KB for testing
        when(streamingProperties.getMaxChunkSize()).thenReturn(maxChunkSize);

        StorageService.ObjectMetadata metadata = new StorageService.ObjectMetadata(
            TEST_OBJECT_KEY,
            TEST_FILE_SIZE,
            TEST_CONTENT_TYPE,
            Instant.now()
        );

        // Request a large range (0 to 10MB) but expect it to be limited to 1KB
        String rangeHeader = "bytes=0-10485760";
        byte[] testData = new byte[1024];
        ResponseInputStream<GetObjectResponse> mockStream = createMockStream(testData);

        when(storageService.getObjectMetadata(TEST_OBJECT_KEY)).thenReturn(metadata);
        when(storageService.getObjectStreamRange(eq(TEST_OBJECT_KEY), eq(0L), eq(1023L)))
            .thenReturn(mockStream);

        // Act
        ResponseEntity<Resource> response = audioStreamingService.streamAudio(TEST_OBJECT_KEY, rangeHeader);

        // Assert
        assertEquals(HttpStatus.PARTIAL_CONTENT, response.getStatusCode());
        // Verify that the end was limited to maxChunkSize - 1
        verify(storageService).getObjectStreamRange(TEST_OBJECT_KEY, 0L, 1023L);
    }

    @Test
    @DisplayName("Should handle range request at end of file")
    void shouldHandleRangeRequestAtEndOfFile() {
        // Arrange
        StorageService.ObjectMetadata metadata = new StorageService.ObjectMetadata(
            TEST_OBJECT_KEY,
            TEST_FILE_SIZE,
            TEST_CONTENT_TYPE,
            Instant.now()
        );

        long start = TEST_FILE_SIZE - 100;
        long end = TEST_FILE_SIZE - 1;
        String rangeHeader = "bytes=" + start + "-" + end;
        byte[] testData = new byte[100];
        ResponseInputStream<GetObjectResponse> mockStream = createMockStream(testData);

        when(storageService.getObjectMetadata(TEST_OBJECT_KEY)).thenReturn(metadata);
        when(storageService.getObjectStreamRange(eq(TEST_OBJECT_KEY), eq(start), eq(end)))
            .thenReturn(mockStream);

        // Act
        ResponseEntity<Resource> response = audioStreamingService.streamAudio(TEST_OBJECT_KEY, rangeHeader);

        // Assert
        assertEquals(HttpStatus.PARTIAL_CONTENT, response.getStatusCode());
        assertEquals("bytes " + start + "-" + end + "/" + TEST_FILE_SIZE,
            response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE));
        assertEquals("100", response.getHeaders().getFirst(HttpHeaders.CONTENT_LENGTH));
    }

    @Test
    @DisplayName("Should throw exception for invalid range")
    void shouldThrowExceptionForInvalidRange() {
        // Arrange
        StorageService.ObjectMetadata metadata = new StorageService.ObjectMetadata(
            TEST_OBJECT_KEY,
            TEST_FILE_SIZE,
            TEST_CONTENT_TYPE,
            Instant.now()
        );

        // Range beyond file size
        String rangeHeader = "bytes=" + (TEST_FILE_SIZE + 100) + "-" + (TEST_FILE_SIZE + 200);

        when(storageService.getObjectMetadata(TEST_OBJECT_KEY)).thenReturn(metadata);

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            audioStreamingService.streamAudio(TEST_OBJECT_KEY, rangeHeader)
        );
    }

    @Test
    @DisplayName("Should use default content type when metadata has none")
    void shouldUseDefaultContentTypeWhenMetadataHasNone() {
        // Arrange
        StorageService.ObjectMetadata metadata = new StorageService.ObjectMetadata(
            TEST_OBJECT_KEY,
            TEST_FILE_SIZE,
            null, // No content type
            Instant.now()
        );

        byte[] testData = new byte[1024];
        ResponseInputStream<GetObjectResponse> mockStream = createMockStream(testData);

        when(storageService.getObjectMetadata(TEST_OBJECT_KEY)).thenReturn(metadata);
        when(storageService.getObjectStream(TEST_OBJECT_KEY)).thenReturn(mockStream);

        // Act
        ResponseEntity<Resource> response = audioStreamingService.streamAudio(TEST_OBJECT_KEY, null);

        // Assert
        assertEquals("audio/mpeg", response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
    }

    @Test
    @DisplayName("Should get streaming metadata without downloading file")
    void shouldGetStreamingMetadataWithoutDownloadingFile() {
        // Arrange
        StorageService.ObjectMetadata metadata = new StorageService.ObjectMetadata(
            TEST_OBJECT_KEY,
            TEST_FILE_SIZE,
            TEST_CONTENT_TYPE,
            Instant.now()
        );

        when(storageService.getObjectMetadata(TEST_OBJECT_KEY)).thenReturn(metadata);

        // Act
        AudioStreamingService.StreamingMetadata streamingMetadata =
            audioStreamingService.getStreamingMetadata(TEST_OBJECT_KEY);

        // Assert
        assertNotNull(streamingMetadata);
        assertEquals(TEST_OBJECT_KEY, streamingMetadata.objectKey());
        assertEquals(TEST_FILE_SIZE, streamingMetadata.fileSize());
        assertEquals(TEST_CONTENT_TYPE, streamingMetadata.contentType());
        assertEquals(10 * 1024 * 1024L, streamingMetadata.maxChunkSize());

        // Verify only metadata was fetched, not the file content
        verify(storageService).getObjectMetadata(TEST_OBJECT_KEY);
        verify(storageService, never()).getObjectStream(any());
        verify(storageService, never()).getObjectStreamRange(any(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("Should include security headers in response")
    void shouldIncludeSecurityHeadersInResponse() {
        // Arrange
        StorageService.ObjectMetadata metadata = new StorageService.ObjectMetadata(
            TEST_OBJECT_KEY,
            TEST_FILE_SIZE,
            TEST_CONTENT_TYPE,
            Instant.now()
        );

        byte[] testData = new byte[1024];
        ResponseInputStream<GetObjectResponse> mockStream = createMockStream(testData);

        when(storageService.getObjectMetadata(TEST_OBJECT_KEY)).thenReturn(metadata);
        when(storageService.getObjectStream(TEST_OBJECT_KEY)).thenReturn(mockStream);

        // Act
        ResponseEntity<Resource> response = audioStreamingService.streamAudio(TEST_OBJECT_KEY, null);

        // Assert
        assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
    }

    @Test
    @DisplayName("Should handle storage service exceptions gracefully")
    void shouldHandleStorageServiceExceptionsGracefully() {
        // Arrange
        when(storageService.getObjectMetadata(TEST_OBJECT_KEY))
            .thenThrow(new RuntimeException("Storage service error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            audioStreamingService.streamAudio(TEST_OBJECT_KEY, null)
        );
    }

    // Helper method to create mock ResponseInputStream
    private ResponseInputStream<GetObjectResponse> createMockStream(byte[] data) {
        GetObjectResponse response = GetObjectResponse.builder()
            .contentLength((long) data.length)
            .contentType(TEST_CONTENT_TYPE)
            .build();

        return new ResponseInputStream<>(response, new ByteArrayInputStream(data));
    }
}
