# Understanding S3-Compatible Storage & Presigned URLs

## Table of Contents
1. [What is S3-Compatible Storage?](#what-is-s3-compatible-storage)
2. [MinIO vs Cloudflare R2](#minio-vs-cloudflare-r2)
3. [Why Not Store Files in the Database?](#why-not-store-files-in-the-database)
4. [Object Storage Concepts](#object-storage-concepts)
5. [Your Application's Storage Setup](#your-applications-storage-setup)
6. [What are Presigned URLs?](#what-are-presigned-urls)
7. [How Presigned URLs Work](#how-presigned-urls-work)
8. [Your StorageService Implementation](#your-storageservice-implementation)
9. [Security with Presigned URLs](#security-with-presigned-urls)
10. [Configuration by Environment](#configuration-by-environment)
11. [Best Practices](#best-practices)

---

## What is S3-Compatible Storage?

**S3** stands for **Simple Storage Service**, originally created by Amazon Web Services (AWS). It's a way to store files (objects) in the cloud.

**S3-Compatible** means the storage service uses the **same API** as Amazon S3, so you can swap providers without changing your code.

### The File Cabinet Analogy

Think of S3 storage like a giant file cabinet:

| Physical File Cabinet | S3 Storage |
|----------------------|------------|
| **Drawer** | Bucket (e.g., `elmify-audio`) |
| **Folder** | Prefix/Path (e.g., `lectures/`) |
| **File** | Object (e.g., `lecture-5.mp3`) |
| **File Path** | Object Key (e.g., `lectures/lecture-5.mp3`) |

You organize files into **buckets** (top-level containers), and within buckets you can use prefixes to create a folder-like structure.

---

## MinIO vs Cloudflare R2

Your application uses **two different S3-compatible storage providers** depending on the environment:

### MinIO (Development)

**What is MinIO?**
- Open-source S3-compatible storage server
- Runs **locally** on your computer (like running a database locally)
- Free and lightweight
- Perfect for development and testing

**Setup:**
```bash
# Run MinIO in Docker
docker run -p 9000:9000 -p 9001:9001 \
  -e "MINIO_ROOT_USER=minioadmin" \
  -e "MINIO_ROOT_PASSWORD=minioadmin" \
  minio/minio server /data --console-address ":9001"
```

**Access:**
- **API Endpoint:** `http://127.0.0.1:9000` (your app uses this)
- **Web Console:** `http://127.0.0.1:9001` (browser UI to manage files)
- **Default Credentials:** `minioadmin` / `minioadmin`

---

### Cloudflare R2 (Production)

**What is Cloudflare R2?**
- Cloudflare's S3-compatible cloud storage
- Globally distributed (fast worldwide)
- **No egress fees** (free bandwidth - huge cost savings!)
- Production-grade reliability

**Why Cloudflare R2 over AWS S3?**

| Feature | AWS S3 | Cloudflare R2 |
|---------|--------|---------------|
| **Storage Cost** | $0.023/GB/month | $0.015/GB/month |
| **Egress (Download)** | $0.09/GB | **FREE** 🎉 |
| **Example: 1TB storage + 10TB downloads** | $23 + $900 = **$923/month** | $15 + $0 = **$15/month** |

For audio streaming apps (lots of downloads), R2 can save **thousands of dollars** per month.

---

### Why Use Both?

```
Development (Local)          Production (Cloud)
      ↓                             ↓
   MinIO                     Cloudflare R2
http://127.0.0.1:9000    https://...r2.cloudflarestorage.com

    Free                    Pay-as-you-go
  Local files              Global CDN
Fast (no internet)        Reliable & scalable
```

**Spring Profiles make this seamless:**
- Run with `dev` profile → Uses MinIO
- Run with `prod` profile → Uses Cloudflare R2
- **Same code, different config!**

---

## Why Not Store Files in the Database?

You might wonder: "Why not just store audio files in PostgreSQL?"

### ❌ Problems with Storing Files in Database

1. **Database Bloat**
   ```sql
   -- A 50MB audio file stored as BYTEA
   INSERT INTO lectures (title, audio_data) VALUES ('Lecture 1', <50MB of binary data>);
   ```
   - Makes database huge (hundreds of GB)
   - Slows down backups (backing up 500GB takes hours)
   - Increases costs (database storage is expensive)

2. **Performance Issues**
   ```sql
   SELECT audio_data FROM lectures WHERE id = 5;
   -- Returns 50MB over database connection (slow!)
   ```
   - Every playback request loads entire file through database
   - Database connections are limited (PostgreSQL default: 100)
   - Audio streaming would exhaust connections

3. **Scaling Difficulties**
   - Can't use CDN (content delivery network) for fast global access
   - Can't offload bandwidth to storage provider
   - Database becomes a bottleneck

---

### ✅ Why Object Storage is Better

1. **Designed for Large Files**
   - Optimized for storing and streaming media
   - Handles GB-sized files easily

2. **Cheap and Scalable**
   - Storage is cheap ($0.015/GB)
   - Bandwidth is free (Cloudflare R2)

3. **Global Distribution**
   - Files cached worldwide (low latency)
   - Fast downloads from anywhere

4. **Direct Streaming**
   - Mobile app downloads directly from storage (not through your server)
   - Your backend just generates URLs

---

## Object Storage Concepts

### Buckets

A **bucket** is a top-level container for objects (like a database in PostgreSQL).

```
Your R2/MinIO Account
│
├── Bucket: elmify-audio        ← All your audio files
│   ├── lectures/lecture-1.mp3
│   ├── lectures/lecture-2.mp3
│   └── thumbnails/thumb-1.jpg
│
├── Bucket: elmify-backups      ← Backups
│   └── backup-2025-01-20.sql
│
└── Bucket: elmify-logs         ← Application logs
    └── app-2025-01-20.log
```

**Your Configuration:**
```yaml
audibleclone:
  r2:
    bucket-name: elmify-audio
```

---

### Objects and Keys

An **object** is a file. Each object has:
- **Key** - The unique identifier (path + filename)
- **Data** - The actual file content
- **Metadata** - Additional info (content type, size, etc.)

**Example:**
```
Key:          lectures/philosophy-101/lecture-5.mp3
Data:         <50MB of MP3 audio>
Content-Type: audio/mpeg
Size:         52,428,800 bytes
```

---

### Keys are Like File Paths

Keys can include `/` to create a folder-like structure:

```
elmify-audio/
├── lectures/
│   ├── philosophy/
│   │   ├── intro-to-plato.mp3
│   │   └── intro-to-aristotle.mp3
│   └── history/
│       └── world-war-2.mp3
├── thumbnails/
│   ├── plato-thumb.jpg
│   └── aristotle-thumb.jpg
└── waveforms/
    └── plato-waveform.json
```

**Important:** These aren't real folders - they're just key prefixes. The object key is the full path:
```
lectures/philosophy/intro-to-plato.mp3
```

---

### Metadata

Each object can have metadata:

```java
HeadObjectResponse metadata = s3Client.headObject(request);

metadata.contentLength();    // 52428800 (50MB)
metadata.contentType();       // "audio/mpeg"
metadata.lastModified();      // 2025-01-15T10:30:00Z
metadata.eTag();              // "abc123def456" (MD5 hash)
```

**Your Implementation:**
```java
public record ObjectMetadata(
    String key,
    Long size,
    String contentType,
    java.time.Instant lastModified
) {}

public ObjectMetadata getObjectMetadata(String objectKey) {
    HeadObjectRequest request = HeadObjectRequest.builder()
        .bucket(bucketName)
        .key(objectKey)
        .build();

    HeadObjectResponse response = s3Client.headObject(request);

    return new ObjectMetadata(
        objectKey,
        response.contentLength(),
        response.contentType(),
        response.lastModified()
    );
}
```

---

## Your Application's Storage Setup

### Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                    React Native App                           │
│  1. User taps "Play" on lecture                              │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│               Spring Boot Backend API                         │
│  2. GET /api/v1/lectures/5/stream-url                        │
│  3. LectureController receives request                       │
│  4. Calls StorageService.generatePresignedUrl()              │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│            MinIO (Dev) or Cloudflare R2 (Prod)               │
│  5. Generate presigned URL with 1-hour expiration           │
│  6. Returns: https://storage.../lecture-5.mp3?signature=...  │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│                    Spring Boot Backend                        │
│  7. Returns: { "url": "https://..." }                        │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│                    React Native App                           │
│  8. Downloads/streams audio directly from storage URL        │
│     (Bypasses backend - saves bandwidth!)                    │
└──────────────────────────────────────────────────────────────┘
```

**Key Point:** The audio file **never goes through your backend**. Your backend only generates a secure URL.

---

### File Upload Flow (Admin/Import)

```
┌──────────────────────────────────────────────────────────────┐
│                  Import Script / Admin Tool                   │
│  1. Has audio file: lecture-5.mp3                           │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│            MinIO (Dev) or Cloudflare R2 (Prod)               │
│  2. Upload directly to storage                               │
│     PUT lectures/lecture-5.mp3                               │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│               PostgreSQL Database (Metadata)                  │
│  3. INSERT INTO lectures (title, file_path, ...)            │
│     VALUES ('Lecture 5', 'lectures/lecture-5.mp3', ...)     │
└──────────────────────────────────────────────────────────────┘
```

**Storage:** Holds the actual audio file
**Database:** Holds metadata (title, speaker, file path, duration, etc.)

---

## What are Presigned URLs?

A **presigned URL** is a temporary, secure URL that grants access to a private object without requiring authentication.

### The Hotel Key Card Analogy

```
Regular URL (Public):
https://storage.example.com/lectures/lecture-5.mp3
↑ Like a hotel with no locks - anyone can enter

Presigned URL (Temporary Access):
https://storage.example.com/lectures/lecture-5.mp3
  ?X-Amz-Algorithm=AWS4-HMAC-SHA256
  &X-Amz-Credential=...
  &X-Amz-Date=20250120T143000Z
  &X-Amz-Expires=3600
  &X-Amz-Signature=abc123def456...
↑ Like a hotel key card that:
  - Opens only room 5 (this specific file)
  - Expires in 1 hour
  - Can't be used after checkout
```

---

### Components of a Presigned URL

```
https://storage.example.com/lectures/lecture-5.mp3
  ?X-Amz-Algorithm=AWS4-HMAC-SHA256
  &X-Amz-Credential=AKIAIOSFODNN7EXAMPLE/20250120/auto/s3/aws4_request
  &X-Amz-Date=20250120T143000Z
  &X-Amz-Expires=3600
  &X-Amz-SignedHeaders=host
  &X-Amz-Signature=abc123def456789...
```

| Parameter | Purpose |
|-----------|---------|
| **X-Amz-Algorithm** | Signature algorithm (AWS4-HMAC-SHA256) |
| **X-Amz-Credential** | Access key + date + region + service |
| **X-Amz-Date** | When the URL was generated |
| **X-Amz-Expires** | How long the URL is valid (seconds) |
| **X-Amz-Signature** | Cryptographic signature proving authenticity |

**The signature is like a tamper-proof seal:**
- If someone changes the URL (tries to access a different file), the signature becomes invalid
- If the expiration time passes, the signature becomes invalid
- Only someone with the secret key (your backend) can generate valid signatures

---

## How Presigned URLs Work

### The Security Flow

```
Step 1: Backend Has Secret Key
┌────────────────────────────────┐
│ StorageService                 │
│ Access Key: AKIAIOSFODNN7...   │
│ Secret Key: wJalrXUtnFEMI...   │ ← Secret! Never shared!
└────────────────────────────────┘

Step 2: Generate URL with Signature
┌────────────────────────────────┐
│ StorageService.generatePresignedUrl("lectures/lecture-5.mp3") │
│ Creates:                       │
│ 1. URL = base + key            │
│ 2. Expiration = now + 1 hour   │
│ 3. Signature = HMAC-SHA256(    │
│      SecretKey,                │
│      "GET\nlectures/lecture-5.mp3\n20250120T143000Z\n3600" │
│    )                           │
└────────────────────────────────┘

Step 3: Return URL to Client
{
  "url": "https://storage.../lecture-5.mp3?...&X-Amz-Signature=abc123"
}

Step 4: Client Downloads with URL
┌────────────────────────────────┐
│ React Native App               │
│ fetch(url)                     │
│   ↓                            │
│ Sends to MinIO/R2              │
└────────────────────────────────┘

Step 5: Storage Validates Signature
┌────────────────────────────────┐
│ MinIO / Cloudflare R2          │
│ 1. Receives request            │
│ 2. Extracts signature from URL │
│ 3. Recalculates signature with │
│    its copy of secret key      │
│ 4. Compares signatures         │
│    ✓ Match → Allow access      │
│    ✗ No match → 403 Forbidden  │
│ 5. Check expiration            │
│    ✓ Not expired → Continue    │
│    ✗ Expired → 403 Forbidden   │
└────────────────────────────────┘

Step 6: Stream Audio to Client
┌────────────────────────────────┐
│ Client receives audio stream   │
│ Plays in media player          │
└────────────────────────────────┘
```

---

### Why This is Secure

1. **Private by Default**
   ```
   GET https://storage.example.com/lectures/lecture-5.mp3
   → 403 Forbidden (no signature)
   ```

2. **Signature Required**
   ```
   GET https://storage.example.com/lectures/lecture-5.mp3?X-Amz-Signature=abc123
   → 200 OK (valid signature)
   ```

3. **Can't Be Tampered With**
   ```
   GET https://storage.example.com/lectures/lecture-999.mp3?X-Amz-Signature=abc123
   → 403 Forbidden (signature doesn't match new URL)
   ```

4. **Time-Limited**
   ```
   GET https://storage.example.com/lectures/lecture-5.mp3?X-Amz-Signature=abc123&X-Amz-Expires=3600
   After 1 hour:
   → 403 Forbidden (expired)
   ```

---

## Your StorageService Implementation

### Configuration

```java
@Service
public class StorageService {

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

        // S3Client for direct operations (upload, delete, etc.)
        this.s3Client = S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(credentialsProvider)
            .region(Region.of(region))
            .forcePathStyle(true)  // Required for MinIO
            .build();

        // S3Presigner for generating presigned URLs
        this.s3Presigner = S3Presigner.builder()
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(credentialsProvider)
            .region(Region.of(region))
            .build();
    }
}
```

**Key Points:**

1. **Two Clients:**
   - `S3Client` - For direct operations (upload, delete, check existence)
   - `S3Presigner` - Specifically for generating presigned URLs

2. **Force Path Style:**
   ```java
   .forcePathStyle(true)
   ```
   This is **critical for MinIO**. It changes URL format from:
   ```
   Virtual-hosted style: https://elmify-audio.storage.com/lecture.mp3
   Path style:          https://storage.com/elmify-audio/lecture.mp3
   ```
   MinIO requires path style.

---

### Generating Presigned URLs

```java
public String generatePresignedUrl(String objectKey) {
    try {
        // 1. Create a GetObjectRequest (what we want to access)
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(bucketName)           // elmify-audio
            .key(objectKey)               // lectures/lecture-5.mp3
            .build();

        // 2. Create a presign request with expiration
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(presignedUrlExpiration)  // 1 hour
            .getObjectRequest(getObjectRequest)
            .build();

        // 3. Generate the presigned URL
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        String url = presignedRequest.url().toString();

        logger.debug("Generated presigned URL for key: {}", objectKey);
        return url;
    } catch (Exception e) {
        logger.error("Failed to generate presigned URL for key: {}", objectKey, e);
        throw new RuntimeException("Failed to generate presigned URL", e);
    }
}
```

**Usage in Controller:**

```java
@GetMapping("/{id}/stream-url")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<Map<String, String>> getStreamUrl(@PathVariable Long id) {
    Lecture lecture = lectureService.getLectureById(id)
        .orElseThrow(() -> new RuntimeException("Lecture not found"));

    // lecture.getFilePath() = "lectures/lecture-5.mp3"
    String presignedUrl = storageService.generatePresignedUrl(lecture.getFilePath());
    // presignedUrl = "http://127.0.0.1:9000/elmify-audio/lectures/lecture-5.mp3?X-Amz-..."

    return ResponseEntity.ok(Map.of("url", presignedUrl));
}
```

---

### Checking if Object Exists

```java
public boolean objectExists(String objectKey) {
    try {
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
            .bucket(bucketName)
            .key(objectKey)
            .build();

        s3Client.headObject(headObjectRequest);
        return true;  // Object exists
    } catch (NoSuchKeyException e) {
        return false;  // Object not found
    } catch (Exception e) {
        logger.error("Error checking if object exists: {}", objectKey, e);
        return false;
    }
}
```

**Use Case:** Validate that a file exists before generating a presigned URL.

---

### Listing Objects

```java
public List<String> listObjects(String prefix) {
    try {
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix(prefix)  // "lectures/" or "thumbnails/"
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
```

**Example:**
```java
List<String> lectures = storageService.listObjects("lectures/");
// Returns: ["lectures/lecture-1.mp3", "lectures/lecture-2.mp3", ...]
```

---

### Downloading Object Stream (Internal Use)

```java
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
```

**Use Case:** If you need to process the file in your backend (e.g., generate waveform, extract metadata).

---

## Security with Presigned URLs

### Access Control

```java
@GetMapping("/{id}/stream-url")
@PreAuthorize("isAuthenticated()")  // ← Must be logged in
public ResponseEntity<Map<String, String>> getStreamUrl(
    @AuthenticationPrincipal Jwt jwt,
    @PathVariable Long id) {

    String clerkId = jwt.getSubject();

    // Optional: Check if user has access to this lecture
    if (!lectureService.canUserAccess(clerkId, id)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of("error", "Access denied"));
    }

    Lecture lecture = lectureService.getLectureById(id)
        .orElseThrow(() -> new RuntimeException("Lecture not found"));

    String presignedUrl = storageService.generatePresignedUrl(lecture.getFilePath());

    return ResponseEntity.ok(Map.of("url", presignedUrl));
}
```

**Security Layers:**

1. **Authentication:** User must have valid JWT
2. **Authorization:** (Optional) Check if user has permission for this lecture
3. **Presigned URL:** Time-limited, signed access to the file
4. **No Direct Access:** Can't access files without going through API first

---

### Expiration Times

**Your Configuration:**
```yaml
audibleclone:
  r2:
    presigned-url-expiration: PT1H  # 1 hour
```

**Duration Format (ISO-8601):**
```
PT1H     → 1 hour
PT30M    → 30 minutes
PT2H30M  → 2 hours 30 minutes
P1D      → 1 day
```

**Choosing Expiration Time:**

| Use Case | Recommended Expiration |
|----------|------------------------|
| **Streaming audio** | 1-2 hours (enough for longest lecture) |
| **Download** | 15-30 minutes (quick download) |
| **Thumbnail images** | 24 hours (can be cached longer) |
| **Admin tools** | 5-10 minutes (short-lived) |

**Trade-offs:**

- **Short expiration (15 min):**
  - ✅ More secure (less time for URL to leak)
  - ❌ User might get error if they pause for too long

- **Long expiration (24 hours):**
  - ✅ Better UX (URL works all day)
  - ❌ If URL leaks, it's valid for 24 hours

**Your Choice (1 hour):** Good balance for audio streaming.

---

### Preventing URL Sharing

**Problem:** User gets a presigned URL and shares it with friends.

**Mitigation Strategies:**

1. **Short Expiration** (you do this)
   - URL only works for 1 hour
   - Limits damage if shared

2. **IP Whitelisting** (advanced)
   ```java
   // Generate URL that only works from user's IP
   presignRequest.putCustomQueryParameter("ip", userIpAddress);
   ```

3. **Rate Limiting** (recommended)
   ```java
   @RateLimiter(name = "streamUrl", fallbackMethod = "streamUrlFallback")
   public ResponseEntity<?> getStreamUrl(...) { ... }
   ```
   Limit to 10 requests per minute per user.

4. **Analytics** (detection)
   - Log all presigned URL generations
   - Alert if single user generates 100+ URLs/hour

---

## Configuration by Environment

### Development (MinIO)

**application-dev.yml:**
```yaml
audibleclone:
  r2:
    endpoint: http://127.0.0.1:9000
    access-key: minioadmin
    secret-key: minioadmin
    region: us-east-1
    bucket-name: elmify-audio
    presigned-url-expiration: PT1H
```

**Generated URL:**
```
http://127.0.0.1:9000/elmify-audio/lectures/lecture-5.mp3
  ?X-Amz-Algorithm=AWS4-HMAC-SHA256
  &X-Amz-Credential=minioadmin/20250120/us-east-1/s3/aws4_request
  &X-Amz-Date=20250120T143000Z
  &X-Amz-Expires=3600
  &X-Amz-Signature=abc123...
```

---

### Production (Cloudflare R2)

**application-prod.yml:**
```yaml
audibleclone:
  r2:
    endpoint: https://b995be98e08909685abfca00c971e79e.r2.cloudflarestorage.com
    access-key: ${R2_ACCESS_KEY}      # From environment variable
    secret-key: ${R2_SECRET_KEY}      # From environment variable
    region: auto
    bucket-name: elmify-audio
    presigned-url-expiration: PT1H
```

**Environment Variables (on server):**
```bash
export R2_ACCESS_KEY="your-cloudflare-access-key"
export R2_SECRET_KEY="your-cloudflare-secret-key"
```

**Generated URL:**
```
https://b995be98e08909685abfca00c971e79e.r2.cloudflarestorage.com/elmify-audio/lectures/lecture-5.mp3
  ?X-Amz-Algorithm=AWS4-HMAC-SHA256
  &X-Amz-Credential=your-access-key/20250120/auto/s3/aws4_request
  &X-Amz-Date=20250120T143000Z
  &X-Amz-Expires=3600
  &X-Amz-Signature=xyz789...
```

---

### Seamless Switching

**Your Code:**
```java
// This works with BOTH MinIO and R2!
String url = storageService.generatePresignedUrl("lectures/lecture-5.mp3");
```

**Spring Profile Magic:**
```bash
# Development
./mvnw spring-boot:run -Dspring.profiles.active=dev
# Uses MinIO (application-dev.yml)

# Production
./mvnw spring-boot:run -Dspring.profiles.active=prod
# Uses Cloudflare R2 (application-prod.yml)
```

Same code, different storage backend. This is the power of **configuration over code**.

---

## Best Practices

### 1. Never Store Credentials in Code

❌ **WRONG:**
```java
@Service
public class StorageService {
    private static final String ACCESS_KEY = "minioadmin";  // Hardcoded!
    private static final String SECRET_KEY = "minioadmin";  // Exposed in Git!
}
```

✅ **CORRECT:**
```yaml
# application-prod.yml
audibleclone:
  r2:
    access-key: ${R2_ACCESS_KEY}  # From environment variable
    secret-key: ${R2_SECRET_KEY}
```

```bash
# Set on server (not in Git)
export R2_ACCESS_KEY="actual-key"
export R2_SECRET_KEY="actual-secret"
```

---

### 2. Use Appropriate Expiration Times

```java
// Streaming audio - 1-2 hours
presigned-url-expiration: PT1H

// Quick downloads - 15 minutes
presigned-url-expiration: PT15M

// Long-lived thumbnails - 24 hours
presigned-url-expiration: PT24H
```

---

### 3. Validate Files Exist Before Generating URLs

```java
@GetMapping("/{id}/stream-url")
public ResponseEntity<?> getStreamUrl(@PathVariable Long id) {
    Lecture lecture = lectureService.getLectureById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Lecture", id));

    // Check if file exists in storage
    if (!storageService.objectExists(lecture.getFilePath())) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Audio file not found in storage"));
    }

    String url = storageService.generatePresignedUrl(lecture.getFilePath());
    return ResponseEntity.ok(Map.of("url", url));
}
```

---

### 4. Handle Errors Gracefully

```java
public String generatePresignedUrl(String objectKey) {
    try {
        // ... generate URL
    } catch (NoSuchKeyException e) {
        throw new ResourceNotFoundException("File not found: " + objectKey);
    } catch (S3Exception e) {
        logger.error("S3 error for key {}: {}", objectKey, e.awsErrorDetails().errorMessage());
        throw new RuntimeException("Storage service error", e);
    } catch (Exception e) {
        logger.error("Unexpected error generating URL for key: {}", objectKey, e);
        throw new RuntimeException("Failed to generate presigned URL", e);
    }
}
```

---

### 5. Use Consistent Naming Conventions

```
✅ GOOD:
lectures/philosophy/intro-to-plato.mp3
thumbnails/philosophy/intro-to-plato.jpg
waveforms/philosophy/intro-to-plato.json

❌ BAD:
audio/plato.mp3
img/thumb1.jpg
waveform_data.json
```

Benefits of consistent naming:
- Easy to find related files
- Can bulk-delete by prefix
- Organized for backups

---

### 6. Monitor Storage Usage

```java
public long getTotalStorageSize() {
    List<String> allKeys = listObjects("");
    return allKeys.stream()
        .mapToLong(key -> getObjectMetadata(key).size())
        .sum();
}

public Map<String, Long> getStorageSizeByPrefix() {
    return Map.of(
        "lectures", listObjects("lectures/").stream()
            .mapToLong(key -> getObjectMetadata(key).size()).sum(),
        "thumbnails", listObjects("thumbnails/").stream()
            .mapToLong(key -> getObjectMetadata(key).size()).sum()
    );
}
```

---

### 7. Implement Caching for URLs

**Problem:** Generating presigned URLs for every request is slow.

**Solution:** Cache URLs until they're about to expire.

```java
@Cacheable(value = "presignedUrls", key = "#objectKey")
public String generatePresignedUrl(String objectKey) {
    // This result is cached for 50 minutes (before 1-hour expiration)
}
```

**Cache Configuration:**
```yaml
spring:
  cache:
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=50m
```

---

## Key Takeaways

### What You Learned

1. **S3-Compatible Storage**
   - Standard API for object storage (files)
   - MinIO (dev) and Cloudflare R2 (prod) both use S3 API
   - Same code works with both providers

2. **Why Object Storage > Database**
   - Designed for large files
   - Cheap and scalable
   - Global distribution
   - Direct streaming (bypasses backend)

3. **Presigned URLs**
   - Temporary, secure URLs for private files
   - Cryptographically signed with secret key
   - Time-limited (1 hour in your app)
   - Can't be tampered with

4. **Your Architecture**
   - Backend generates presigned URLs
   - Mobile app downloads directly from storage
   - Saves backend bandwidth
   - Better performance

5. **Security Layers**
   - Authentication (JWT required to get URL)
   - Presigned URL (time-limited, signed)
   - No direct storage access
   - Short expiration limits sharing

6. **Environment Switching**
   - Dev: MinIO locally (free, fast)
   - Prod: Cloudflare R2 (global, reliable, free egress)
   - Spring Profiles make switching seamless

### How This Connects to Your Application

```
Database (PostgreSQL)
  ↓ Stores: lecture metadata, file paths
JPA Entities (Lecture)
  ↓ field: filePath = "lectures/lecture-5.mp3"
Service Layer (LectureService)
  ↓ Retrieves lecture from database
StorageService
  ↓ Generates presigned URL for filePath
MinIO / Cloudflare R2
  ↓ Validates signature, streams file
React Native App
  ↓ Downloads and plays audio
```

### Next Steps

- [Understanding Spring Boot Configuration & Profiles](./understanding-spring-boot-configuration.md) - How dev/prod configs work
- [Understanding RESTful API Design](./understanding-restful-api-design.md) - How the `/stream-url` endpoint follows REST
- [Understanding DTOs](./understanding-dtos-pattern.md) - How `LectureDto.fromEntity()` transforms URLs to presigned URLs

---

**Congratulations!** You now understand how object storage works, why presigned URLs are secure, and how your application seamlessly switches between local MinIO and production Cloudflare R2.
