package com.elmify.backend.controller;

import com.elmify.backend.entity.Lecture;
import com.elmify.backend.service.AudioStreamingService;
import com.elmify.backend.service.LectureService;
import com.elmify.backend.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for audio streaming endpoints.
 * Tests the full HTTP request/response cycle for streaming operations.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Audio Streaming Controller Tests")
class AudioStreamingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LectureService lectureService;

    @MockBean
    private AudioStreamingService audioStreamingService;

    @MockBean
    private StorageService storageService;

    private Lecture testLecture;
    private static final Long TEST_LECTURE_ID = 1L;
    private static final String TEST_FILE_PATH = "lectures/test-lecture.mp3";
    private static final long TEST_FILE_SIZE = 1024 * 1024; // 1MB

    @BeforeEach
    void setUp() {
        testLecture = new Lecture();
        testLecture.setId(TEST_LECTURE_ID);
        testLecture.setFilePath(TEST_FILE_PATH);
        testLecture.setTitle("Test Lecture");
        testLecture.setCreatedAt(Instant.now());
    }

    @Test
    @WithMockUser
    @DisplayName("Should get stream URL for authenticated user")
    void shouldGetStreamUrlForAuthenticatedUser() throws Exception {
        when(lectureService.getLectureById(TEST_LECTURE_ID))
            .thenReturn(Optional.of(testLecture));

        mockMvc.perform(get("/api/v1/lectures/{id}/stream-url", TEST_LECTURE_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.url").value("/api/v1/lectures/" + TEST_LECTURE_ID + "/stream"));

        verify(lectureService).getLectureById(TEST_LECTURE_ID);
        verify(lectureService).incrementPlayCount(TEST_LECTURE_ID);
    }

    @Test
    @DisplayName("Should reject stream URL request for unauthenticated user")
    void shouldRejectStreamUrlRequestForUnauthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/v1/lectures/{id}/stream-url", TEST_LECTURE_ID))
            .andExpect(status().isUnauthorized());

        verify(lectureService, never()).getLectureById(any());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 when lecture not found for stream URL")
    void shouldReturn404WhenLectureNotFoundForStreamUrl() throws Exception {
        when(lectureService.getLectureById(TEST_LECTURE_ID))
            .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/lectures/{id}/stream-url", TEST_LECTURE_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("Should stream full audio file without range header")
    void shouldStreamFullAudioFileWithoutRangeHeader() throws Exception {
        byte[] testData = "test audio content".getBytes();
        Resource resource = new ByteArrayResource(testData);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "audio/mpeg");
        headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(testData.length));

        ResponseEntity<Resource> mockResponse = new ResponseEntity<>(resource, headers, HttpStatus.OK);

        when(lectureService.getLectureById(TEST_LECTURE_ID))
            .thenReturn(Optional.of(testLecture));
        when(audioStreamingService.streamAudio(eq(TEST_FILE_PATH), isNull()))
            .thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/lectures/{id}/stream", TEST_LECTURE_ID))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "audio/mpeg"))
            .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
            .andExpect(content().bytes(testData));

        verify(audioStreamingService).streamAudio(TEST_FILE_PATH, null);
    }

    @Test
    @WithMockUser
    @DisplayName("Should stream audio with range header (206 Partial Content)")
    void shouldStreamAudioWithRangeHeader() throws Exception {
        String rangeHeader = "bytes=0-1023";
        byte[] testData = new byte[1024];
        Resource resource = new ByteArrayResource(testData);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "audio/mpeg");
        headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.add(HttpHeaders.CONTENT_RANGE, "bytes 0-1023/" + TEST_FILE_SIZE);
        headers.add(HttpHeaders.CONTENT_LENGTH, "1024");

        ResponseEntity<Resource> mockResponse =
            new ResponseEntity<>(resource, headers, HttpStatus.PARTIAL_CONTENT);

        when(lectureService.getLectureById(TEST_LECTURE_ID))
            .thenReturn(Optional.of(testLecture));
        when(audioStreamingService.streamAudio(eq(TEST_FILE_PATH), eq(rangeHeader)))
            .thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/lectures/{id}/stream", TEST_LECTURE_ID)
                .header(HttpHeaders.RANGE, rangeHeader))
            .andExpect(status().isPartialContent())
            .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes 0-1023/" + TEST_FILE_SIZE))
            .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, "1024"));

        verify(audioStreamingService).streamAudio(TEST_FILE_PATH, rangeHeader);
    }

    @Test
    @DisplayName("Should stream audio with token parameter (iOS compatibility)")
    void shouldStreamAudioWithTokenParameter() throws Exception {
        byte[] testData = "test audio content".getBytes();
        Resource resource = new ByteArrayResource(testData);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "audio/mpeg");

        ResponseEntity<Resource> mockResponse = new ResponseEntity<>(resource, headers, HttpStatus.OK);

        when(lectureService.getLectureById(TEST_LECTURE_ID))
            .thenReturn(Optional.of(testLecture));
        when(audioStreamingService.streamAudio(eq(TEST_FILE_PATH), isNull()))
            .thenReturn(mockResponse);

        // Token authentication instead of Authorization header
        mockMvc.perform(get("/api/v1/lectures/{id}/stream", TEST_LECTURE_ID)
                .param("token", "test-jwt-token"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should reject stream request without authentication")
    void shouldRejectStreamRequestWithoutAuthentication() throws Exception {
        // No token parameter and no Authorization header
        mockMvc.perform(get("/api/v1/lectures/{id}/stream", TEST_LECTURE_ID))
            .andExpect(status().isUnauthorized());

        verify(audioStreamingService, never()).streamAudio(any(), any());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 when lecture not found for streaming")
    void shouldReturn404WhenLectureNotFoundForStreaming() throws Exception {
        when(lectureService.getLectureById(TEST_LECTURE_ID))
            .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/lectures/{id}/stream", TEST_LECTURE_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("Should handle multiple range requests (seeking simulation)")
    void shouldHandleMultipleRangeRequests() throws Exception {
        // Simulate a user seeking through the audio file
        String[] ranges = {
            "bytes=0-1023",           // Initial load
            "bytes=102400-103423",    // Seek to middle
            "bytes=1048000-1048575"   // Seek near end
        };

        when(lectureService.getLectureById(TEST_LECTURE_ID))
            .thenReturn(Optional.of(testLecture));

        for (String range : ranges) {
            byte[] testData = new byte[1024];
            Resource resource = new ByteArrayResource(testData);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, "audio/mpeg");

            ResponseEntity<Resource> mockResponse =
                new ResponseEntity<>(resource, headers, HttpStatus.PARTIAL_CONTENT);

            when(audioStreamingService.streamAudio(eq(TEST_FILE_PATH), eq(range)))
                .thenReturn(mockResponse);

            mockMvc.perform(get("/api/v1/lectures/{id}/stream", TEST_LECTURE_ID)
                    .header(HttpHeaders.RANGE, range))
                .andExpect(status().isPartialContent());

            verify(audioStreamingService).streamAudio(TEST_FILE_PATH, range);
        }
    }

    @Test
    @WithMockUser
    @DisplayName("Should include cache headers for efficient client caching")
    void shouldIncludeCacheHeadersForEfficientClientCaching() throws Exception {
        byte[] testData = "test audio content".getBytes();
        Resource resource = new ByteArrayResource(testData);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "audio/mpeg");
        headers.add(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000");

        ResponseEntity<Resource> mockResponse = new ResponseEntity<>(resource, headers, HttpStatus.OK);

        when(lectureService.getLectureById(TEST_LECTURE_ID))
            .thenReturn(Optional.of(testLecture));
        when(audioStreamingService.streamAudio(eq(TEST_FILE_PATH), isNull()))
            .thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/lectures/{id}/stream", TEST_LECTURE_ID))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000"));
    }
}
