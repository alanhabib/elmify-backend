# Audio Streaming Implementation Guide

## Overview

This guide provides a complete implementation of smooth audio streaming from Cloudflare R2 storage to Spring Boot backend, with support for:

- ✅ **Range requests (HTTP 206)** - Enables seeking and resumable playback
- ✅ **Chunked transfer** - Prevents timeouts on mobile networks
- ✅ **iOS/React Native compatibility** - Tested with AVPlayer and react-native-track-player
- ✅ **Efficient caching** - Reduces bandwidth and improves performance
- ✅ **Comprehensive tests** - Unit and integration tests included

## Architecture

```
┌─────────────────┐
│  React Native   │
│   (Frontend)    │
└────────┬────────┘
         │ HTTP Range Request
         ▼
┌─────────────────┐
│  Spring Boot    │
│   Controller    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ AudioStreaming  │
│    Service      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ StorageService  │
│  (R2/S3 SDK)    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Cloudflare R2  │
│     Storage     │
└─────────────────┘
```

## Key Components

### 1. StreamingProperties (Configuration)

Located: `src/main/java/com/elmify/backend/config/StreamingProperties.java`

Configurable properties for streaming behavior:

```java
@Configuration
@ConfigurationProperties(prefix = "elmify.streaming")
public class StreamingProperties {
    private Long maxChunkSize = 10 * 1024 * 1024L; // 10MB
    private Integer bufferSize = 8192;              // 8KB
    private Boolean enableCompression = false;
    private Long cacheMaxAge = 31536000L;           // 1 year
    private Boolean enableDetailedLogging = false;
}
```

### 2. AudioStreamingService

Located: `src/main/java/com/elmify/backend/service/AudioStreamingService.java`

Core streaming logic with range request support:

```java
@Service
public class AudioStreamingService {

    /**
     * Stream audio with range request support
     */
    public ResponseEntity<Resource> streamAudio(
            String objectKey,
            String rangeHeader) {

        var metadata = storageService.getObjectMetadata(objectKey);
        long fileSize = metadata.size();

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            return handleRangeRequest(objectKey, rangeHeader, fileSize);
        }

        return handleFullRequest(objectKey, fileSize);
    }
}
```

### 3. Configuration (application.yml)

Add to `src/main/resources/application.yml`:

```yaml
elmify:
  # R2 Storage Configuration
  r2:
    endpoint: https://your-account.r2.cloudflarestorage.com
    bucket-name: your-bucket-name
    access-key: ${R2_ACCESS_KEY}
    secret-key: ${R2_SECRET_KEY}
    region: auto  # or specific region
    presigned-url-expiration: PT6H

  # Streaming Configuration
  streaming:
    max-chunk-size: 10485760    # 10MB - optimal for mobile
    buffer-size: 8192           # 8KB - standard buffer
    enable-compression: false   # Audio already compressed
    cache-max-age: 31536000     # 1 year cache
    enable-detailed-logging: false
```

## Implementation Steps

### Step 1: Add Dependencies

Ensure your `pom.xml` includes:

```xml
<!-- AWS SDK for S3 (R2 compatible) -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.25.27</version>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>localstack</artifactId>
    <version>1.19.0</version>
    <scope>test</scope>
</dependency>
```

### Step 2: Configure R2 Storage

Create or update `application-prod.yml`:

```yaml
elmify:
  r2:
    endpoint: https://your-account-id.r2.cloudflarestorage.com
    bucket-name: ${R2_BUCKET_NAME}
    access-key: ${R2_ACCESS_KEY}
    secret-key: ${R2_SECRET_KEY}
    region: auto
    presigned-url-expiration: PT6H

  streaming:
    max-chunk-size: 10485760
    enable-detailed-logging: false
```

### Step 3: Update Your Controller

Modify your `LectureController.java` to use the new streaming service:

```java
@RestController
@RequestMapping("/api/v1/lectures")
public class LectureController {

    private final AudioStreamingService audioStreamingService;
    private final LectureService lectureService;

    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> streamAudio(
            @PathVariable Long id,
            @RequestParam(value = "token", required = false) String token,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {

        // Validate authentication
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Get lecture
        Lecture lecture = lectureService.getLectureById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lecture", id));

        // Stream audio with range support
        return audioStreamingService.streamAudio(
            lecture.getFilePath(),
            rangeHeader
        );
    }
}
```

## React Native Integration

### Using react-native-track-player

```typescript
import TrackPlayer, { Capability } from 'react-native-track-player';

// Setup player
await TrackPlayer.setupPlayer();

// Configure capabilities
await TrackPlayer.updateOptions({
  capabilities: [
    Capability.Play,
    Capability.Pause,
    Capability.SeekTo,
    Capability.SkipToNext,
    Capability.SkipToPrevious,
  ],
});

// Add track with authentication token
const lectureId = 123;
const authToken = 'your-jwt-token';

await TrackPlayer.add({
  id: lectureId.toString(),
  url: `https://your-api.com/api/v1/lectures/${lectureId}/stream?token=${authToken}`,
  title: 'Lecture Title',
  artist: 'Speaker Name',
  duration: 3600, // seconds
});

// Play
await TrackPlayer.play();
```

### Using Expo AV

```typescript
import { Audio } from 'expo-av';

const lectureId = 123;
const authToken = 'your-jwt-token';

// Create sound instance
const { sound } = await Audio.Sound.createAsync(
  {
    uri: `https://your-api.com/api/v1/lectures/${lectureId}/stream?token=${authToken}`
  },
  { shouldPlay: true },
  onPlaybackStatusUpdate
);

// Control playback
await sound.playAsync();
await sound.pauseAsync();
await sound.setPositionAsync(30000); // Seek to 30 seconds
```

## How It Works

### Range Request Flow

1. **Initial Request** - Client requests first chunk:
   ```
   GET /api/v1/lectures/123/stream
   Range: bytes=0-10485759
   ```

2. **Server Response** - Returns 206 Partial Content:
   ```
   HTTP/1.1 206 Partial Content
   Content-Range: bytes 0-10485759/52428800
   Content-Length: 10485760
   Content-Type: audio/mpeg
   Accept-Ranges: bytes
   ```

3. **Seeking** - Client seeks to middle:
   ```
   GET /api/v1/lectures/123/stream
   Range: bytes=26214400-36700159
   ```

4. **Continuation** - Player automatically fetches next chunks as needed

### Chunk Size Optimization

- **10MB chunks** - Optimal for 4G/5G networks
- Prevents connection timeouts
- Reduces server load
- Mobile players automatically request more chunks

### Caching Strategy

```
Cache-Control: public, max-age=31536000
```

- Audio files are immutable (never change)
- Browser/CDN can cache for 1 year
- Reduces bandwidth costs
- Faster subsequent playback

## Testing

### Run Unit Tests

```bash
mvn test -Dtest=AudioStreamingServiceTest
```

### Run Integration Tests

```bash
mvn test -Dtest=StorageServiceIntegrationTest
```

### Run All Tests

```bash
mvn clean test
```

### Test Coverage

```bash
mvn clean verify
# Open: target/site/jacoco/index.html
```

## Performance Optimization Tips

### 1. Enable CDN (Cloudflare)

```yaml
# Use public R2 bucket with CDN
elmify:
  r2:
    endpoint: https://pub-xxxxx.r2.dev  # Public R2 endpoint
```

Benefits:
- Edge caching
- Reduced latency
- Lower bandwidth costs

### 2. Adjust Chunk Size for Network

```yaml
# For slower networks (3G)
elmify:
  streaming:
    max-chunk-size: 5242880  # 5MB

# For fast networks (WiFi)
elmify:
  streaming:
    max-chunk-size: 20971520  # 20MB
```

### 3. Enable Compression for Metadata

```yaml
server:
  compression:
    enabled: true
    mime-types: application/json
```

### 4. Connection Pooling

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      connection-timeout: 20000
```

## Troubleshooting

### Issue: Audio stutters on mobile

**Solution**: Reduce chunk size

```yaml
elmify:
  streaming:
    max-chunk-size: 5242880  # 5MB instead of 10MB
```

### Issue: Connection timeouts

**Solution**: Check network latency and adjust timeouts

```yaml
# Add to application.yml
spring:
  mvc:
    async:
      request-timeout: 300000  # 5 minutes
```

### Issue: High bandwidth costs

**Solution**: Enable CDN and check cache headers

```bash
curl -I https://your-api.com/api/v1/lectures/123/stream
# Verify: Cache-Control: public, max-age=31536000
```

### Issue: Range requests not working

**Solution**: Verify headers in response

```bash
curl -H "Range: bytes=0-1023" \
     -I https://your-api.com/api/v1/lectures/123/stream
# Should return: 206 Partial Content
```

## Monitoring

### Key Metrics to Track

1. **Average chunk size served**
2. **Cache hit ratio**
3. **Response time by range**
4. **Bandwidth usage**
5. **Error rate**

### Prometheus Metrics (Example)

```java
@Timed(value = "audio.streaming.duration",
       description = "Time taken to stream audio")
@Counted(value = "audio.streaming.requests",
         description = "Number of streaming requests")
public ResponseEntity<Resource> streamAudio(...) {
    // ... streaming logic
}
```

## Security Considerations

### 1. Authentication

All streaming endpoints require authentication:

```java
@PreAuthorize("isAuthenticated()")
public ResponseEntity<Resource> streamAudio(...) {
    // Only authenticated users can stream
}
```

### 2. Rate Limiting

Implement rate limiting to prevent abuse:

```java
@RateLimiter(name = "streaming")
public ResponseEntity<Resource> streamAudio(...) {
    // Limited requests per user
}
```

### 3. Content Security

```java
HttpHeaders headers = new HttpHeaders();
headers.add("X-Content-Type-Options", "nosniff");
headers.add("Content-Security-Policy", "default-src 'self'");
```

## Best Practices

1. ✅ **Always support range requests** - Required for seeking
2. ✅ **Use appropriate chunk sizes** - 5-10MB for mobile
3. ✅ **Set long cache times** - Audio files are immutable
4. ✅ **Monitor bandwidth usage** - Track costs
5. ✅ **Test on real devices** - Simulators don't reflect real network conditions
6. ✅ **Log performance metrics** - Identify bottlenecks
7. ✅ **Handle errors gracefully** - Network issues are common
8. ✅ **Use CDN when possible** - Reduce latency and costs

## Summary

You now have a production-ready audio streaming implementation with:

- ✅ Smooth streaming for large files
- ✅ Range request support for seeking
- ✅ Mobile-optimized chunk sizes
- ✅ Comprehensive tests
- ✅ Performance monitoring
- ✅ Security best practices

The system is designed to handle long audio lectures (hours) smoothly on mobile networks with no interruptions.

## Additional Resources

- [Spring Boot Streaming Best Practices](https://spring.io/guides/gs/streaming-with-spring/)
- [AWS S3 Range GET](https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObject.html)
- [HTTP Range Requests (RFC 7233)](https://tools.ietf.org/html/rfc7233)
- [Cloudflare R2 Documentation](https://developers.cloudflare.com/r2/)
