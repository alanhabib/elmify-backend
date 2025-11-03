# Production Deployment Guide

## Overview
This guide covers the production deployment of the AudibleClone backend with security best practices and monitoring.

## Environment Variables Required

### Database Configuration
```bash
DATABASE_URL=jdbc:postgresql://your-db-host:5432/your-db-name
DB_USERNAME=your-db-user
DB_PASSWORD=your-secure-password
```

### Clerk Configuration
```bash
CLERK_SECRET_KEY=sk_live_your-clerk-secret-key
CLERK_PUBLISHABLE_KEY=pk_live_your-clerk-publishable-key
CLERK_JWT_ISSUER=https://your-clerk-domain.clerk.accounts.dev
```

### Storage Configuration (R2/S3)
```bash
R2_ENDPOINT=https://your-account-id.r2.cloudflarestorage.com
R2_ACCESS_KEY=your-r2-access-key
R2_SECRET_KEY=your-r2-secret-key
R2_REGION=auto
```

### Security Configuration
```bash
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://app.yourdomain.com
ADMIN_EMAILS=admin@yourdomain.com,admin2@yourdomain.com
```

### Server Configuration
```bash
PORT=8080
```

## Pre-Deployment Checklist

### 1. Database Setup
- [ ] PostgreSQL database created
- [ ] Database user with appropriate permissions
- [ ] Flyway migrations tested
- [ ] Database connection tested

### 2. Security Setup
- [ ] All environment variables configured
- [ ] HTTPS/TLS certificates configured
- [ ] CORS origins restricted to production domains
- [ ] Admin emails configured
- [ ] JWT issuer verified

### 3. Storage Setup
- [ ] R2/S3 bucket created
- [ ] Access keys configured with minimal required permissions
- [ ] Bucket CORS policy configured
- [ ] Test file upload/download working

### 4. Monitoring Setup
- [ ] Log aggregation configured
- [ ] Health check endpoints accessible
- [ ] Metrics collection enabled (Prometheus)
- [ ] Alerting configured

## Deployment Steps

### 1. Build Application
```bash
./mvnw clean package -Pprod
```

### 2. Run with Production Profile
```bash
java -jar -Dspring.profiles.active=prod target/audibleclone-backend-*.jar
```

### 3. Verify Health Checks
```bash
curl https://your-domain.com/actuator/health
curl https://your-domain.com/actuator/health/clerk
curl https://your-domain.com/actuator/health/storage
```

## Docker Deployment

### Dockerfile Example
```dockerfile
FROM openjdk:21-jre-slim

WORKDIR /app

COPY target/audibleclone-backend-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
```

### Docker Compose Example
```yaml
version: '3.8'

services:
  audibleclone-backend:
    build: .
    ports:
      - "8080:8080"
    environment:
      - DATABASE_URL=${DATABASE_URL}
      - DB_USERNAME=${DB_USERNAME}
      - DB_PASSWORD=${DB_PASSWORD}
      - CLERK_SECRET_KEY=${CLERK_SECRET_KEY}
      - CLERK_PUBLISHABLE_KEY=${CLERK_PUBLISHABLE_KEY}
      - CLERK_JWT_ISSUER=${CLERK_JWT_ISSUER}
      - R2_ENDPOINT=${R2_ENDPOINT}
      - R2_ACCESS_KEY=${R2_ACCESS_KEY}
      - R2_SECRET_KEY=${R2_SECRET_KEY}
      - CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS}
      - ADMIN_EMAILS=${ADMIN_EMAILS}
    depends_on:
      - postgres
    restart: unless-stopped

  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: audibleclone
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: unless-stopped

volumes:
  postgres_data:
```

## Security Features Enabled

### 1. Authentication & Authorization
- JWT-based authentication with Clerk
- Role-based access control (USER, ADMIN)
- Method-level security annotations

### 2. Rate Limiting
- IP-based rate limiting per endpoint type
- Different limits for streaming, analytics, and admin endpoints
- Configurable via `audibleclone.security.rate-limiting.enabled`

### 3. Security Headers
- HSTS (HTTP Strict Transport Security)
- Content Type Options
- Frame Options
- Referrer Policy
- Permissions Policy

### 4. Input Validation
- Bean validation on DTOs
- Path parameter validation
- Global exception handling with structured error responses

### 5. Logging & Monitoring
- Structured logging with trace IDs
- Health check endpoints for dependencies
- Metrics collection with Micrometer/Prometheus
- Actuator endpoints secured

## Performance Optimizations

### 1. Database
- Connection pooling with HikariCP
- Optimized JPA/Hibernate settings
- Batch processing enabled

### 2. Caching
- Second-level Hibernate cache enabled
- Query result caching

### 3. HTTP
- GZIP compression enabled
- HTTP/2 support
- Optimized thread pool settings

## Monitoring Endpoints

- **Health**: `/actuator/health` - Overall application health
- **Database**: `/actuator/health/db` - Database connectivity
- **Clerk**: `/actuator/health/clerk` - JWT issuer connectivity
- **Storage**: `/actuator/health/storage` - R2/S3 connectivity
- **Metrics**: `/actuator/metrics` - Application metrics
- **Info**: `/actuator/info` - Application information

## Troubleshooting

### Common Issues

1. **JWT Validation Failures**
   - Verify `CLERK_JWT_ISSUER` matches your Clerk instance
   - Check network connectivity to Clerk's JWKS endpoint
   - Validate JWT token format from frontend

2. **Database Connection Issues**
   - Verify database URL, username, and password
   - Check network connectivity to database
   - Ensure database user has required permissions

3. **Storage Issues**
   - Verify R2/S3 credentials and permissions
   - Check bucket name and region configuration
   - Test presigned URL generation

4. **CORS Issues**
   - Verify `CORS_ALLOWED_ORIGINS` includes your frontend domain
   - Check for proper HTTPS configuration
   - Validate request headers from frontend

### Log Analysis
- All errors include trace IDs for correlation
- Structured JSON logging for easy parsing
- Security events logged with appropriate detail level
- Performance metrics logged for optimization

## Security Considerations

1. **Never commit secrets to version control**
2. **Use environment variables for all sensitive data**
3. **Regularly rotate access keys and passwords**
4. **Monitor logs for suspicious activity**
5. **Keep dependencies updated**
6. **Use HTTPS everywhere**
7. **Implement proper backup and recovery procedures**

## Post-Deployment Verification

1. **Functional Tests**
   ```bash
   # Test authentication
   curl -H "Authorization: Bearer $JWT_TOKEN" https://your-domain.com/api/v1/users/me
   
   # Test streaming
   curl -H "Authorization: Bearer $JWT_TOKEN" https://your-domain.com/api/v1/lectures/1/stream-url
   ```

2. **Security Tests**
   ```bash
   # Test rate limiting
   for i in {1..150}; do curl https://your-domain.com/api/v1/lectures; done
   
   # Test CORS
   curl -H "Origin: https://malicious-site.com" https://your-domain.com/api/v1/lectures
   ```

3. **Performance Tests**
   - Load testing with realistic traffic patterns
   - Database performance under load
   - Memory usage monitoring
   - Response time analysis