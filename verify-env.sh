#!/bin/bash

# Railway Environment Variable Verification Script
# Run this to check if all required variables are set

echo "🔍 Verifying Railway Environment Variables..."
echo ""

# Required variables
REQUIRED_VARS=(
  "DATABASE_URL"
  "DB_USERNAME"
  "DB_PASSWORD"
  "CLERK_SECRET_KEY"
  "CLERK_PUBLISHABLE_KEY"
  "CLERK_JWT_ISSUER"
  "R2_ENDPOINT"
  "R2_BUCKET_NAME"
  "R2_ACCESS_KEY"
  "R2_SECRET_KEY"
  "PORT"
  "SPRING_PROFILES_ACTIVE"
)

# Optional variables
OPTIONAL_VARS=(
  "REDIS_HOST"
  "REDIS_PORT"
  "REDIS_PASSWORD"
  "ADMIN_EMAILS"
  "CORS_ALLOWED_ORIGINS"
)

MISSING_REQUIRED=()
MISSING_OPTIONAL=()

# Check required variables
echo "📋 Required Variables:"
for var in "${REQUIRED_VARS[@]}"; do
  railway variables get "$var" &> /dev/null
  if [ $? -eq 0 ]; then
    echo "  ✅ $var is set"
  else
    echo "  ❌ $var is MISSING"
    MISSING_REQUIRED+=("$var")
  fi
done

echo ""
echo "📋 Optional Variables:"
for var in "${OPTIONAL_VARS[@]}"; do
  railway variables get "$var" &> /dev/null
  if [ $? -eq 0 ]; then
    echo "  ✅ $var is set"
  else
    echo "  ⚠️  $var is not set (optional)"
    MISSING_OPTIONAL+=("$var")
  fi
done

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ ${#MISSING_REQUIRED[@]} -eq 0 ]; then
  echo "✅ All required variables are set!"
  echo ""
  echo "You can now deploy:"
  echo "  railway up"

  if [ ${#MISSING_OPTIONAL[@]} -gt 0 ]; then
    echo ""
    echo "⚠️  Missing optional variables:"
    for var in "${MISSING_OPTIONAL[@]}"; do
      echo "  - $var"
    done
    echo ""
    echo "The app will work without these, but:"
    echo "  - No REDIS_* = slower playlist loading (still works)"
    echo "  - No ADMIN_EMAILS = default admin only"
  fi
else
  echo "❌ Missing required variables:"
  for var in "${MISSING_REQUIRED[@]}"; do
    echo "  - $var"
  done
  echo ""
  echo "Set missing variables with:"
  echo '  railway variables set VARIABLE_NAME="value"'
  echo ""
  echo "See RAILWAY_DEPLOYMENT.md for detailed instructions"
fi

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
