#!/bin/bash

# Startup Debug Script - Run this in Railway to diagnose startup issues
# Add this as Railway's start command temporarily for debugging

echo "=================================================="
echo "🔍 Railway Startup Diagnostics"
echo "=================================================="
echo ""

echo "📋 Environment Check:"
echo "  PORT: ${PORT:-NOT_SET}"
echo "  SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-NOT_SET}"
echo "  DATABASE_URL: ${DATABASE_URL:0:30}... (${#DATABASE_URL} chars)"
echo "  CLERK_SECRET_KEY: ${CLERK_SECRET_KEY:0:10}... (${#CLERK_SECRET_KEY} chars)"
echo "  R2_ENDPOINT: ${R2_ENDPOINT:-NOT_SET}"
echo ""

echo "🔍 Java Version:"
java -version
echo ""

echo "🔍 Memory Limits:"
echo "  Max Heap: $(java -XX:+PrintFlagsFinal -version 2>&1 | grep MaxHeapSize | awk '{print $4/1024/1024 "MB"}')"
echo ""

echo "🔍 Application JAR:"
if [ -f app.jar ]; then
  echo "  ✅ app.jar exists ($(du -h app.jar | cut -f1))"
  echo "  JAR contents:"
  jar tf app.jar | grep -E "(application.*\.yml|BOOT-INF/classes/db/migration)" | head -10
else
  echo "  ❌ app.jar NOT FOUND!"
  exit 1
fi
echo ""

echo "🔍 Required Environment Variables:"
MISSING_VARS=()

[ -z "$DATABASE_URL" ] && MISSING_VARS+=("DATABASE_URL")
[ -z "$DB_USERNAME" ] && MISSING_VARS+=("DB_USERNAME")
[ -z "$DB_PASSWORD" ] && MISSING_VARS+=("DB_PASSWORD")
[ -z "$CLERK_SECRET_KEY" ] && MISSING_VARS+=("CLERK_SECRET_KEY")
[ -z "$CLERK_PUBLISHABLE_KEY" ] && MISSING_VARS+=("CLERK_PUBLISHABLE_KEY")
[ -z "$CLERK_JWT_ISSUER" ] && MISSING_VARS+=("CLERK_JWT_ISSUER")
[ -z "$R2_ENDPOINT" ] && MISSING_VARS+=("R2_ENDPOINT")
[ -z "$R2_BUCKET_NAME" ] && MISSING_VARS+=("R2_BUCKET_NAME")
[ -z "$R2_ACCESS_KEY" ] && MISSING_VARS+=("R2_ACCESS_KEY")
[ -z "$R2_SECRET_KEY" ] && MISSING_VARS+=("R2_SECRET_KEY")

if [ ${#MISSING_VARS[@]} -eq 0 ]; then
  echo "  ✅ All required variables are set"
else
  echo "  ❌ Missing variables:"
  for var in "${MISSING_VARS[@]}"; do
    echo "    - $var"
  done
  echo ""
  echo "  ⚠️  Application will likely fail to start!"
  echo "  See RAILWAY_DEPLOYMENT.md for setup instructions"
fi
echo ""

echo "🔍 Optional Variables:"
[ -n "$REDIS_HOST" ] && echo "  ✅ Redis configured" || echo "  ⚠️  Redis not configured (slower performance)"
echo ""

echo "=================================================="
echo "🚀 Starting Application..."
echo "=================================================="
echo ""

# Start the application with detailed logging
exec java \
  -Xmx400m \
  -Xms200m \
  -XX:+UseContainerSupport \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} \
  -Dlogging.level.root=INFO \
  -Dlogging.level.org.springframework.boot=INFO \
  -Dlogging.level.com.elmify=DEBUG \
  -jar app.jar
