# Elmify Backend - Quick Start Guide

## 🎯 TL;DR - Get Running in 2 Commands

```bash
make db-up    # Start PostgreSQL + MinIO (keeps your 10GB!)
make start    # Start Spring Boot backend
```

That's it! Your data is safe and persistent.

---

## 📚 What You Need to Know

### 1. Your 10GB MinIO Data is SAFE! 🎉

✅ **Data persists when you:**
- Stop containers: `make stop`
- Restart computer
- Update docker-compose.yml
- Restart Docker Desktop

❌ **Data is deleted ONLY when you:**
- Run `make clean` (explicitly deletes volumes)
- Run `docker-compose down -v` (the `-v` flag deletes volumes)

### 2. Docker Compose Handles Everything

Your `docker-compose.yml` includes:
- **PostgreSQL** with auto-created `elmify_db` database
- **MinIO** with auto-created `elmify-audio` bucket
- **Persistent volumes** for both (data survives restarts)
- **Health checks** to ensure services are ready
- **Automatic bucket creation** (no manual setup)

### 3. Use the Makefile (It's Super Easy!)

```bash
make help        # See all commands
make start       # Start everything
make stop        # Stop (data persists!)
make backup      # Backup your 10GB
make volume-info # Check volume status
```

---

## 🚀 First-Time Setup

### Prerequisites

- Docker Desktop installed
- Make installed (comes with macOS)
- Java 17+ installed

### Steps

```bash
# 1. Navigate to project
cd ~/Desktop/hobby_projects/elmify-backend

# 2. Start database services (one-time setup)
make db-up

# 3. Wait for services to be ready (~10 seconds)

# 4. Start Spring Boot
make start
```

That's it! The database `elmify_db` is created automatically.

---

## 📅 Daily Development Workflow

### Morning (Start Work)

```bash
cd elmify-backend
make start
```

This:
1. ✅ Starts PostgreSQL
2. ✅ Starts MinIO (with your 10GB data intact)
3. ✅ Starts Spring Boot
4. ✅ Runs Flyway migrations (if any new ones)

### Evening (Stop Work)

```bash
make stop
```

This:
1. ✅ Stops Spring Boot
2. ✅ Stops Docker containers
3. ✅ **Keeps all data** (PostgreSQL + MinIO)

### Next Morning

```bash
make start
```

Your 10GB is still there! 🎉

---

## 💾 Backup Your 10GB Data

### Create Backup

```bash
make backup
```

This creates a timestamped backup in `~/backups/elmify-minio/`:
```
minio-backup-20241106_120000.tar.gz  (10GB compressed)
```

### Restore from Backup

```bash
make restore
# Follow the prompts to select a backup
```

### Check Backups

```bash
ls -lh ~/backups/elmify-minio/
```

---

## 📊 Monitor Your Volumes

### Check Volume Status

```bash
make volume-info
```

Shows:
- Volume names
- Volume sizes
- Mount points
- Volume details

### Manually Check

```bash
# List volumes
docker volume ls

# Check size
docker system df -v

# Inspect MinIO volume
docker volume inspect elmify-backend_minio-data
```

---

## 🔧 Common Tasks

### View MinIO Console (Web UI)

```bash
open http://localhost:9001
```

Login:
- Username: `minioadmin`
- Password: `minioadmin`

### View Logs

```bash
make logs
```

Or for specific service:
```bash
docker logs elmify-postgres
docker logs elmify-minio
```

### Restart Services

```bash
make restart
```

### Check Service Health

```bash
docker ps
```

You should see:
- `elmify-postgres` (healthy)
- `elmify-minio` (healthy)

---

## 🆘 Troubleshooting

### "Database elmify_db does not exist"

**Solution:**
```bash
# Restart database services
make db-down
make db-up
make start
```

The database is created automatically on first start.

### "My 10GB data is gone!"

**Check if volume exists:**
```bash
docker volume ls | grep minio
```

**If volume exists but empty:**
```bash
# Restore from backup
make restore
```

**If you don't have a backup:**
Sorry, you need to re-upload. But now you know to run `make backup` regularly!

### "Port 5432 or 9000 already in use"

**You have local PostgreSQL or MinIO running:**
```bash
# Stop local PostgreSQL
brew services stop postgresql

# Stop local MinIO
brew services stop minio
```

Or change ports in `docker-compose.yml`.

### "Docker is not running"

**Start Docker Desktop:**
```bash
open -a Docker
# Wait for Docker to start, then retry
```

---

## 🔥 Full Reset (Nuclear Option)

**⚠️ WARNING: This deletes ALL data (PostgreSQL + MinIO)**

```bash
make clean
```

Use this when:
- You want to start completely fresh
- Your database schema is corrupted
- You're testing migration scripts

After `make clean`, you need to:
1. Re-upload your 10GB to MinIO
2. Restart services: `make start`

---

## 📖 Deep Dive Guides

For more details, see:

- **[DATABASE_SETUP.md](DATABASE_SETUP.md)** - Database configuration details
- **[DOCKER_VOLUMES_GUIDE.md](DOCKER_VOLUMES_GUIDE.md)** - Complete guide on data persistence
- **[BACKEND_INTEGRATION.md](../elmify-frontend/BACKEND_INTEGRATION.md)** - Frontend integration

---

## 🎓 Key Concepts

### Docker Volumes = Persistent Storage

```
Container (temporary) → Volume (persistent)
```

When you stop/delete a container, the volume remains.

### docker-compose down vs docker-compose down -v

```bash
docker-compose down     # ✅ Keeps volumes (data safe)
docker-compose down -v  # ❌ Deletes volumes (data lost)
```

**Rule of thumb:** Never use `-v` unless you want to delete data!

### Named Volumes

Your volumes are named:
- `elmify-backend_postgres-data` - PostgreSQL data
- `elmify-backend_minio-data` - Your 10GB data

They're stored by Docker and persist across restarts.

---

## 🎯 Production Deployment (Railway)

On Railway:
- PostgreSQL is a managed service (auto-created database)
- MinIO is replaced with Cloudflare R2
- Use environment variables from Railway
- No Docker needed (Railway handles containers)

See [PRODUCTION_DEPLOYMENT.md](PRODUCTION_DEPLOYMENT.md) for details.

---

## ✅ Checklist

Daily workflow:
- [ ] `make start` - Start services
- [ ] Develop your code
- [ ] `make stop` - Stop services
- [ ] Data is automatically saved

Weekly:
- [ ] `make backup` - Backup your 10GB

Before major changes:
- [ ] `make backup` - Just in case!

---

## 🤝 Need Help?

1. Run `make help` to see all commands
2. Check the logs: `make logs`
3. Verify volumes: `make volume-info`
4. Read the guides in this folder

---

**Remember:** Your data is safe as long as you don't use `docker-compose down -v`! 🎉
