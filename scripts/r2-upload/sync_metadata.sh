#!/bin/bash

################################################################################
# sync_metadata.sh - Sync metadata between R2, PostgreSQL, and local files
################################################################################
#
# PURPOSE:
#   Unified script to manage metadata synchronization between:
#   - Local content directory
#   - Cloudflare R2 storage
#   - PostgreSQL database (Railway or local)
#
# USAGE:
#   ./sync_metadata.sh [COMMAND] [OPTIONS]
#
# COMMANDS:
#   push        Upload local content to R2 and sync to database
#   pull        Download database metadata to local manifest
#   scan        Scan R2 bucket and generate metadata from files
#   status      Show current sync status
#   diff        Compare local vs database
#
# OPTIONS:
#   -d, --dir DIR          Content directory (required for push)
#   -o, --output FILE      Output file for pull (default: exported_manifest.json)
#   -e, --env ENV          Environment: local or production (default: production)
#   -h, --help             Show this help message
#
# EXAMPLES:
#   # Push new content to R2 and sync to Railway database
#   ./sync_metadata.sh push --dir ~/content
#
#   # Pull current database state
#   ./sync_metadata.sh pull
#
#   # Pull from local database
#   ./sync_metadata.sh pull --env local
#
#   # Scan R2 and generate metadata
#   ./sync_metadata.sh scan
#
#   # Compare local vs database
#   ./sync_metadata.sh diff --dir ~/content
#
# ENVIRONMENT VARIABLES:
#   DATABASE_URL           PostgreSQL connection string (for Railway)
#   R2_BUCKET_NAME         Cloudflare R2 bucket name
#
# AUTHOR:  Elmify Team
# VERSION: 1.0.0
#
################################################################################

set -euo pipefail

################################################################################
# CONFIGURATION
################################################################################

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMMAND=""
CONTENT_DIR=""
OUTPUT_FILE="exported_manifest.json"
ENVIRONMENT="production"

# Color codes
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly CYAN='\033[0;36m'
readonly NC='\033[0m'

################################################################################
# HELPER FUNCTIONS
################################################################################

log_info() {
    echo -e "${BLUE}[INFO]${NC} $*"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $*"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $*"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $*" >&2
}

show_help() {
    sed -n '/^# PURPOSE:/,/^################################################################################$/p' "$0" | sed 's/^# //; s/^#//'
    exit 0
}

################################################################################
# COMMAND FUNCTIONS
################################################################################

cmd_push() {
    if [[ -z "$CONTENT_DIR" ]]; then
        log_error "Content directory required for push command"
        echo "Usage: ./sync_metadata.sh push --dir <content_directory>"
        exit 1
    fi

    if [[ ! -d "$CONTENT_DIR" ]]; then
        log_error "Content directory not found: $CONTENT_DIR"
        exit 1
    fi

    log_info "Starting push workflow..."
    echo ""

    # Step 1: Validate content
    log_info "Step 1/4: Validating content structure..."
    if ! "$SCRIPT_DIR/validate_content.sh" "$CONTENT_DIR"; then
        log_error "Content validation failed. Fix issues and try again."
        exit 1
    fi
    log_success "Content validated"
    echo ""

    # Step 2: Upload to R2
    log_info "Step 2/4: Uploading to R2 storage..."
    if ! "$SCRIPT_DIR/upload_to_r2.sh" "$CONTENT_DIR"; then
        log_error "R2 upload failed"
        exit 1
    fi
    log_success "Uploaded to R2"
    echo ""

    # Step 3: Generate manifest
    log_info "Step 3/4: Generating manifest..."
    if ! node "$SCRIPT_DIR/generate_manifest.js" "$CONTENT_DIR" "manifest.json"; then
        log_error "Manifest generation failed"
        exit 1
    fi
    log_success "Manifest generated"
    echo ""

    # Step 4: Import to database
    log_info "Step 4/4: Importing to PostgreSQL..."
    if [[ "$ENVIRONMENT" == "production" ]]; then
        if [[ -z "${DATABASE_URL:-}" ]]; then
            log_error "DATABASE_URL environment variable required for production"
            log_info "Set it with: export DATABASE_URL=\"postgresql://...\""
            exit 1
        fi
        log_info "Importing to Railway production database..."
    else
        log_info "Importing to local database..."
    fi

    if ! node "$SCRIPT_DIR/import_manifest.js" "manifest.json"; then
        log_error "Database import failed"
        exit 1
    fi
    log_success "Imported to database"
    echo ""

    log_success "✅ Push complete! Content is now live."
}

cmd_pull() {
    log_info "Pulling metadata from PostgreSQL..."
    echo ""

    if [[ "$ENVIRONMENT" == "production" ]]; then
        if [[ -z "${DATABASE_URL:-}" ]]; then
            log_error "DATABASE_URL environment variable required for production"
            log_info "Set it with: export DATABASE_URL=\"postgresql://...\""
            exit 1
        fi
        log_info "Exporting from Railway production database..."
    else
        log_info "Exporting from local database..."
    fi

    if ! node "$SCRIPT_DIR/export_from_db.js" "$OUTPUT_FILE"; then
        log_error "Database export failed"
        exit 1
    fi

    echo ""
    log_success "✅ Export complete!"
    log_info "Metadata saved to: $OUTPUT_FILE"
}

cmd_status() {
    log_info "Checking sync status..."
    echo ""

    # Check R2 bucket
    log_info "R2 Storage:"
    if command -v wrangler &> /dev/null; then
        local bucket_name="${R2_BUCKET_NAME:-elmify-audio}"
        local file_count=$(wrangler r2 object list "$bucket_name" 2>/dev/null | grep -c "^" || echo "unknown")
        echo "  Bucket: $bucket_name"
        echo "  Files: $file_count"
    else
        log_warning "  Wrangler not installed - cannot check R2"
    fi
    echo ""

    # Check database
    log_info "PostgreSQL Database:"
    if [[ "$ENVIRONMENT" == "production" ]]; then
        if [[ -z "${DATABASE_URL:-}" ]]; then
            log_warning "  DATABASE_URL not set - cannot check production database"
        else
            echo "  Environment: Production (Railway)"
            # Could add psql query here to count records
        fi
    else
        echo "  Environment: Local"
        # Could add local psql query here
    fi
    echo ""

    # Check local files
    if [[ -f "manifest.json" ]]; then
        log_info "Local manifest.json found"
        local speaker_count=$(node -e "const m = require('./manifest.json'); console.log(m.speakers?.length || 0)")
        echo "  Speakers: $speaker_count"
    else
        log_warning "No local manifest.json found"
    fi
}

cmd_diff() {
    if [[ -z "$CONTENT_DIR" ]]; then
        log_error "Content directory required for diff command"
        echo "Usage: ./sync_metadata.sh diff --dir <content_directory>"
        exit 1
    fi

    log_info "Comparing local content vs database..."
    echo ""

    # Generate local manifest
    log_info "Generating local manifest..."
    node "$SCRIPT_DIR/generate_manifest.js" "$CONTENT_DIR" "local_manifest.json"

    # Export database
    log_info "Exporting database..."
    node "$SCRIPT_DIR/export_from_db.js" "db_manifest.json"

    echo ""
    log_info "Comparison:"
    
    local local_speakers=$(node -e "const m = require('./local_manifest.json'); console.log(m.speakers?.length || 0)")
    local db_speakers=$(node -e "const m = require('./db_manifest.json'); console.log(m.speakers?.length || 0)")

    echo "  Local speakers:    $local_speakers"
    echo "  Database speakers: $db_speakers"
    
    if [[ "$local_speakers" -eq "$db_speakers" ]]; then
        log_success "Speaker counts match ✓"
    else
        log_warning "Speaker counts differ!"
    fi

    echo ""
    log_info "Detailed manifests saved:"
    echo "  Local:    local_manifest.json"
    echo "  Database: db_manifest.json"
}

cmd_scan() {
    log_info "Scanning R2 bucket for existing files..."
    echo ""

    if ! node "$SCRIPT_DIR/scan_r2_and_sync.js"; then
        log_error "R2 scan failed"
        exit 1
    fi

    echo ""
    log_success "✅ Scan complete!"
    echo ""
    log_info "Next steps:"
    echo "  1. Review r2_scanned_manifest.json"
    echo "  2. Add missing metadata (bios, descriptions, etc.)"
    echo "  3. Import to database:"
    echo "     DATABASE_URL=\"...\" node import_manifest.js r2_scanned_manifest.json"
}

################################################################################
# ARGUMENT PARSING
################################################################################

if [[ $# -eq 0 ]]; then
    show_help
fi

COMMAND="$1"
shift

while [[ $# -gt 0 ]]; do
    case $1 in
        -d|--dir)
            CONTENT_DIR="$2"
            shift 2
            ;;
        -o|--output)
            OUTPUT_FILE="$2"
            shift 2
            ;;
        -e|--env)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -h|--help)
            show_help
            ;;
        *)
            log_error "Unknown option: $1"
            echo ""
            show_help
            ;;
    esac
done

################################################################################
# MAIN
################################################################################

case "$COMMAND" in
    push)
        cmd_push
        ;;
    pull)
        cmd_pull
        ;;
    scan)
        cmd_scan
        ;;
    status)
        cmd_status
        ;;
    diff)
        cmd_diff
        ;;
    help)
        show_help
        ;;
    *)
        log_error "Unknown command: $COMMAND"
        echo ""
        echo "Valid commands: push, pull, scan, status, diff"
        echo "Run './sync_metadata.sh help' for more information"
        exit 1
        ;;
esac
