# Elmify Backend - Development Makefile
# Makes common tasks easier

.PHONY: help start stop restart logs clean db-up db-down db-reset build test backup restore volume-info

# Default target
help:
	@echo "Elmify Backend - Available Commands:"
	@echo ""
	@echo "  make start       - Start database services + Spring Boot"
	@echo "  make stop        - Stop all services (data persists)"
	@echo "  make restart     - Restart all services"
	@echo "  make logs        - View Docker Compose logs"
	@echo "  make clean       - Stop services and remove volumes (⚠️  deletes data)"
	@echo ""
	@echo "  make db-up       - Start only database services"
	@echo "  make db-down     - Stop database services"
	@echo "  make db-reset    - Reset database (delete all data)"
	@echo ""
	@echo "  make backup      - Backup MinIO data (10GB safe!)"
	@echo "  make restore     - Restore MinIO data from backup"
	@echo "  make volume-info - Show volume information and size"
	@echo ""
	@echo "  make build       - Build Spring Boot application"
	@echo "  make test        - Run tests"
	@echo ""

# Start everything
start: db-up
	@echo "✅ Starting Spring Boot..."
	./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Stop everything
stop:
	@echo "🛑 Stopping services..."
	docker-compose down

# Restart services
restart: stop start

# View logs
logs:
	docker-compose logs -f

# Clean everything (removes volumes)
clean:
	@echo "🧹 Cleaning up..."
	docker-compose down -v
	@echo "✅ All services stopped and volumes removed"

# Database services only
db-up:
	@echo "🚀 Starting database services..."
	docker-compose up -d
	@echo "⏳ Waiting for database to be ready..."
	@sleep 5
	@echo "✅ Database services ready"

db-down:
	@echo "🛑 Stopping database services..."
	docker-compose down

# Reset database (dangerous!)
db-reset:
	@echo "⚠️  Resetting database (all data will be lost)..."
	@read -p "Are you sure? [y/N] " -n 1 -r; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		echo ""; \
		docker-compose down -v; \
		docker-compose up -d; \
		echo "✅ Database reset complete"; \
	else \
		echo ""; \
		echo "❌ Cancelled"; \
	fi

# Build application
build:
	@echo "🔨 Building application..."
	./mvnw clean package -DskipTests

# Run tests
test:
	@echo "🧪 Running tests..."
	./mvnw test

# Backup MinIO data
backup:
	@echo "💾 Backing up MinIO data..."
	@mkdir -p ~/backups/elmify-minio
	@BACKUP_FILE=~/backups/elmify-minio/minio-backup-$$(date +%Y%m%d_%H%M%S).tar.gz; \
	docker run --rm \
		-v elmify-backend_minio-data:/data \
		-v ~/backups/elmify-minio:/backup \
		alpine tar czf /backup/$$(basename $$BACKUP_FILE) -C /data . && \
	echo "✅ Backup created: $$BACKUP_FILE" && \
	du -h $$BACKUP_FILE

# Restore MinIO data
restore:
	@echo "📦 Available backups:"
	@ls -lh ~/backups/elmify-minio/*.tar.gz 2>/dev/null || echo "No backups found"
	@echo ""
	@read -p "Enter backup filename (just the name, e.g., minio-backup-20241106_120000.tar.gz): " BACKUP; \
	if [ -f ~/backups/elmify-minio/$$BACKUP ]; then \
		echo "🔄 Restoring from $$BACKUP..."; \
		docker run --rm \
			-v elmify-backend_minio-data:/data \
			-v ~/backups/elmify-minio:/backup \
			alpine sh -c "rm -rf /data/* && tar xzf /backup/$$BACKUP -C /data"; \
		echo "✅ Restore complete"; \
	else \
		echo "❌ Backup file not found"; \
	fi

# Show volume information
volume-info:
	@echo "📊 Docker Volume Information:"
	@echo ""
	@docker volume ls | grep elmify || echo "No elmify volumes found"
	@echo ""
	@echo "💾 Volume Sizes:"
	@docker system df -v | grep -A 1 "VOLUME NAME" | grep -E "(VOLUME NAME|elmify)" || echo "No volume info"
	@echo ""
	@echo "📂 MinIO Volume Details:"
	@docker volume inspect elmify-backend_minio-data 2>/dev/null || echo "MinIO volume not found"
