# Docker Volumes & Data Persistence Guide

## Understanding the Problem You're Facing

You're re-uploading 10GB of data repeatedly because you're likely doing one of these:

1. ❌ Running MinIO without volumes → Data lost on restart
2. ❌ Running `docker-compose down -v` → Deletes volumes
3. ❌ Deleting containers without understanding volumes

## How Docker Volumes Work

### What is a Docker Volume?

Think of a Docker volume as a **persistent USB drive** attached to your container:

```
┌─────────────────────────┐
│   Docker Container      │
│   (MinIO running here)  │
│                         │
│   When container stops  │
│   or is deleted, data   │
│   in container is LOST  │
└────────┬────────────────┘
         │
         │ BUT...
         │
┌────────▼────────────────┐
│   Docker Volume         │
│   (Persistent Storage)  │
│                         │
│   Data survives:        │
│   ✅ Container restart  │
│   ✅ Container delete   │
│   ✅ System reboot      │
│   ✅ Docker restart     │
└─────────────────────────┘
```

### Your docker-compose.yml Configuration

```yaml
# This section creates two persistent volumes
volumes:
  postgres-data:      # PostgreSQL data persists here
    driver: local
  minio-data:         # MinIO data (your 10GB) persists here
    driver: local
```

```yaml
# MinIO service uses the volume
minio:
  volumes:
    - minio-data:/data   # Maps volume to /data inside container
```

## Data Persistence Lifecycle

### ✅ What KEEPS Your Data:

```bash
# Stop containers (data is SAFE)
docker-compose stop

# Delete containers (data is SAFE)
docker-compose down

# Restart containers (data is SAFE)
docker-compose restart

# Reboot computer (data is SAFE)
sudo reboot

# Update docker-compose.yml (data is SAFE)
docker-compose up -d
```

### ❌ What DELETES Your Data:

```bash
# Delete volumes explicitly
docker-compose down -v   # ⚠️ -v flag = delete volumes

# Delete volume manually
docker volume rm elmify-backend_minio-data

# Prune all unused volumes
docker volume prune
```

## Managing Your MinIO Data

### Check Volume Status

```bash
# List all volumes
docker volume ls

# Inspect minio volume
docker volume inspect elmify-backend_minio-data

# Check volume size
docker system df -v
```

### Where is Data Stored?

Your 10GB of MinIO data is stored in a Docker volume located at:

**Mac/Linux:**
```
/var/lib/docker/volumes/elmify-backend_minio-data/_data
```

**Mac (Docker Desktop):**
Inside the Docker Desktop VM - not directly accessible, but persistent!

### View Your Data

```bash
# Access MinIO Console (web UI)
open http://localhost:9001

# Login:
# Username: minioadmin
# Password: minioadmin

# Or use MinIO Client (mc)
docker exec -it elmify-minio mc ls /data/elmify-audio
```

## Common Scenarios

### Scenario 1: Daily Development

```bash
# Morning - Start services
docker-compose up -d
./mvnw spring-boot:run

# Evening - Stop services
docker-compose stop

# Result: ✅ All data persists (10GB still there)
```

### Scenario 2: Change docker-compose.yml

```bash
# Stop services
docker-compose down

# Edit docker-compose.yml
vim docker-compose.yml

# Restart with new config
docker-compose up -d

# Result: ✅ All data persists
```

### Scenario 3: Full Reset (Start Fresh)

```bash
# WARNING: This deletes ALL data
docker-compose down -v

# Start fresh
docker-compose up -d

# Result: ❌ Data deleted, need to re-upload 10GB
```

### Scenario 4: Update MinIO Version

```bash
# Pull new image
docker-compose pull minio

# Recreate container with new image
docker-compose up -d

# Result: ✅ Data persists (volume untouched)
```

## Backup Your MinIO Data

Since you have 10GB of data, you should back it up:

### Method 1: Volume Backup (Recommended)

```bash
# Create backup directory
mkdir -p ~/backups/elmify-minio

# Backup volume to tar file
docker run --rm \
  -v elmify-backend_minio-data:/data \
  -v ~/backups/elmify-minio:/backup \
  alpine \
  tar czf /backup/minio-data-$(date +%Y%m%d).tar.gz -C /data .

# Restore from backup (if needed)
docker run --rm \
  -v elmify-backend_minio-data:/data \
  -v ~/backups/elmify-minio:/backup \
  alpine \
  tar xzf /backup/minio-data-YYYYMMDD.tar.gz -C /data
```

### Method 2: MinIO Mirror (Easier)

```bash
# Install MinIO Client
brew install minio/stable/mc

# Configure alias
mc alias set local http://localhost:9000 minioadmin minioadmin

# Backup entire bucket
mc mirror local/elmify-audio ~/backups/elmify-audio

# Restore bucket
mc mirror ~/backups/elmify-audio local/elmify-audio
```

### Method 3: Automated Backup Script

```bash
#!/bin/bash
# backup-minio.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="$HOME/backups/elmify-minio"

mkdir -p "$BACKUP_DIR"

docker run --rm \
  -v elmify-backend_minio-data:/data \
  -v "$BACKUP_DIR":/backup \
  alpine \
  tar czf "/backup/minio-backup-$DATE.tar.gz" -C /data .

echo "✅ Backup created: $BACKUP_DIR/minio-backup-$DATE.tar.gz"

# Keep only last 7 backups
ls -t "$BACKUP_DIR"/minio-backup-*.tar.gz | tail -n +8 | xargs rm -f
```

## Best Practices

### 1. **Never use `-v` flag unless you want to delete data**

```bash
# Good
docker-compose down           # Stops containers, keeps volumes

# Dangerous
docker-compose down -v        # Stops AND deletes volumes
```

### 2. **Regular Backups**

Schedule weekly backups of your MinIO data:

```bash
# Add to crontab
crontab -e

# Run backup every Sunday at 2 AM
0 2 * * 0 /path/to/backup-minio.sh
```

### 3. **Monitor Volume Size**

```bash
# Check volume usage
docker system df -v | grep minio

# Clean up if needed (but keeps your data)
docker system prune
```

### 4. **Separate Volumes for Different Projects**

Docker Compose automatically names volumes based on directory:

```
elmify-backend_minio-data    ← Your project
other-project_minio-data     ← Another project
```

They don't interfere with each other!

## Troubleshooting

### "My data disappeared!"

**Check if volume still exists:**
```bash
docker volume ls | grep minio
```

**If volume exists but appears empty:**
```bash
# Check if container is using correct volume
docker inspect elmify-minio | grep -A 10 Mounts
```

### "Volume is taking too much space"

```bash
# Check size
docker system df -v

# If you want to start fresh
docker-compose down -v  # ⚠️ Deletes data
docker-compose up -d
```

### "Can't access MinIO after restart"

```bash
# Check if container is running
docker ps | grep minio

# Check logs
docker logs elmify-minio

# Restart container
docker-compose restart minio
```

## Migration: From Local MinIO to Docker MinIO

If you currently have MinIO running locally (not Docker):

```bash
# 1. Export data from local MinIO
mc mirror local-minio/elmify-audio ~/temp-backup

# 2. Start Docker MinIO
docker-compose up -d minio

# 3. Import to Docker MinIO
mc alias set docker-minio http://localhost:9000 minioadmin minioadmin
mc mirror ~/temp-backup docker-minio/elmify-audio

# 4. Verify
mc ls docker-minio/elmify-audio

# 5. Stop local MinIO (if you want)
brew services stop minio
```

## Summary: Why You're Re-uploading

You're probably running one of these commands that **deletes volumes**:

```bash
docker-compose down -v        # ⚠️ Deletes volumes
docker volume prune           # ⚠️ Deletes unused volumes
docker system prune -a --volumes  # ⚠️ Nuclear option
```

**Solution:** Just use `docker-compose down` without `-v` flag!

## Quick Reference

```bash
# Start (data persists)
docker-compose up -d

# Stop (data persists)
docker-compose down

# Restart (data persists)
docker-compose restart

# View data
open http://localhost:9001

# Backup data
mc mirror local/elmify-audio ~/backup

# Check volume
docker volume inspect elmify-backend_minio-data
```

Your 10GB is **safe** as long as you don't use the `-v` flag! 🎉
