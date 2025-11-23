#!/bin/bash

################################################################################
# clear_r2_storage.sh - Clear all objects from Cloudflare R2 storage
################################################################################
#
# PURPOSE:
#   This script removes ALL objects from the specified R2 bucket.
#   Use this before uploading new content structure to start fresh.
#
# USAGE:
#   ./clear_r2_storage.sh [OPTIONS]
#
# OPTIONS:
#   -b, --bucket BUCKET    R2 bucket name (default: from env or 'elmify-audio')
#   -y, --yes              Skip confirmation prompt (dangerous!)
#   -v, --verbose          Show detailed output
#   -h, --help             Show this help message
#
# ENVIRONMENT VARIABLES:
#   R2_BUCKET_NAME         Cloudflare R2 bucket name
#
# DEPENDENCIES:
#   - wrangler CLI (Cloudflare Workers CLI)
#   - jq (JSON processor)
#
# EXAMPLES:
#   # Interactive mode (recommended)
#   ./clear_r2_storage.sh
#
#   # Auto-confirm deletion (dangerous!)
#   ./clear_r2_storage.sh --yes
#
#   # Specify different bucket
#   ./clear_r2_storage.sh --bucket my-other-bucket
#
# AUTHOR:  Elmify Team
# VERSION: 1.0.0
# DATE:    2025-01-12
#
################################################################################

set -euo pipefail  # Exit on error, undefined vars, pipe failures

################################################################################
# CONFIGURATION & GLOBAL VARIABLES
################################################################################

# Default configuration
BUCKET_NAME="${R2_BUCKET_NAME:-elmify-audio}"
VERBOSE=false
AUTO_CONFIRM=false

# Color codes for terminal output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color

# Script metadata
readonly SCRIPT_NAME=$(basename "$0")
readonly SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)

################################################################################
# HELPER FUNCTIONS
################################################################################

# Print colored output to stderr (so it doesn't interfere with data output)
log_info() {
    echo -e "${BLUE}[INFO]${NC} $*" >&2
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $*" >&2
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $*" >&2
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $*" >&2
}

# Print verbose messages only if VERBOSE=true
log_verbose() {
    if [[ "$VERBOSE" == "true" ]]; then
        echo -e "${BLUE}[VERBOSE]${NC} $*" >&2
    fi
}

# Show usage information
show_usage() {
    cat << EOF
Usage: $SCRIPT_NAME [OPTIONS]

Clear all objects from Cloudflare R2 storage bucket.

OPTIONS:
    -b, --bucket BUCKET    R2 bucket name (default: $BUCKET_NAME)
    -y, --yes              Skip confirmation prompt (dangerous!)
    -v, --verbose          Show detailed output
    -h, --help             Show this help message

ENVIRONMENT VARIABLES:
    R2_BUCKET_NAME         Cloudflare R2 bucket name

EXAMPLES:
    # Interactive mode (recommended)
    $SCRIPT_NAME

    # Auto-confirm deletion
    $SCRIPT_NAME --yes

    # Specify different bucket
    $SCRIPT_NAME --bucket my-other-bucket

EOF
}

################################################################################
# DEPENDENCY VALIDATION
################################################################################

# Check if a command exists in PATH
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Validate all required dependencies are installed
validate_dependencies() {
    log_info "Validating dependencies..."

    local missing_deps=()

    # Check for wrangler CLI
    if ! command_exists wrangler; then
        missing_deps+=("wrangler")
    fi

    # Check for jq (JSON processor)
    if ! command_exists jq; then
        missing_deps+=("jq")
    fi

    # If any dependencies are missing, show installation instructions
    if [[ ${#missing_deps[@]} -gt 0 ]]; then
        log_error "Missing required dependencies: ${missing_deps[*]}"
        echo ""
        echo "Installation instructions for macOS:"
        echo ""

        for dep in "${missing_deps[@]}"; do
            case "$dep" in
                wrangler)
                    echo "  # Install Wrangler CLI"
                    echo "  npm install -g wrangler"
                    echo ""
                    ;;
                jq)
                    echo "  # Install jq"
                    echo "  brew install jq"
                    echo ""
                    ;;
            esac
        done

        return 1
    fi

    log_success "All dependencies are installed"
    return 0
}

################################################################################
# R2 OPERATIONS
################################################################################

# List all objects in the R2 bucket
list_objects() {
    local bucket="$1"

    log_verbose "Listing objects in bucket: $bucket"

    # Use wrangler to list objects
    # Note: wrangler r2 object list returns objects one per line
    wrangler r2 object list "$bucket" 2>/dev/null || {
        log_error "Failed to list objects in bucket: $bucket"
        return 1
    }
}

# Count total objects in bucket
count_objects() {
    local bucket="$1"

    log_verbose "Counting objects in bucket: $bucket"

    # Count lines from wrangler output
    local count
    count=$(wrangler r2 object list "$bucket" 2>/dev/null | wc -l | tr -d ' ')

    echo "$count"
}

# Delete a single object from R2
delete_object() {
    local bucket="$1"
    local object_key="$2"

    log_verbose "Deleting object: $object_key"

    # Use wrangler to delete the object
    wrangler r2 object delete "$bucket" "$object_key" 2>/dev/null || {
        log_warning "Failed to delete object: $object_key"
        return 1
    }

    return 0
}

# Delete all objects from the bucket
clear_bucket() {
    local bucket="$1"

    log_info "Fetching object list from bucket: $bucket"

    # Get list of all objects
    local objects
    objects=$(list_objects "$bucket")

    # Check if bucket is empty
    if [[ -z "$objects" ]]; then
        log_info "Bucket is already empty!"
        return 0
    fi

    # Count total objects
    local total_count
    total_count=$(echo "$objects" | wc -l | tr -d ' ')

    log_info "Found $total_count objects to delete"

    # Counter for deleted objects
    local deleted_count=0
    local failed_count=0

    # Delete each object
    echo "$objects" | while IFS= read -r object_key; do
        # Skip empty lines
        [[ -z "$object_key" ]] && continue

        # Delete the object
        if delete_object "$bucket" "$object_key"; then
            ((deleted_count++))

            # Show progress every 10 objects
            if [[ $((deleted_count % 10)) -eq 0 ]]; then
                log_info "Progress: $deleted_count/$total_count objects deleted"
            fi
        else
            ((failed_count++))
        fi
    done

    # Final summary
    echo ""
    log_success "Deletion complete!"
    log_info "Deleted: $deleted_count objects"

    if [[ $failed_count -gt 0 ]]; then
        log_warning "Failed: $failed_count objects"
    fi
}

################################################################################
# CONFIRMATION PROMPT
################################################################################

# Ask user to confirm deletion
confirm_deletion() {
    local bucket="$1"
    local count="$2"

    echo ""
    log_warning "⚠️  WARNING: This will PERMANENTLY DELETE all objects from bucket: $bucket"
    log_warning "⚠️  Total objects to be deleted: $count"
    echo ""

    # If auto-confirm is enabled, skip prompt
    if [[ "$AUTO_CONFIRM" == "true" ]]; then
        log_info "Auto-confirm enabled, proceeding with deletion..."
        return 0
    fi

    # Prompt user for confirmation
    read -p "Are you sure you want to continue? Type 'yes' to confirm: " -r
    echo ""

    if [[ $REPLY == "yes" ]]; then
        return 0
    else
        log_info "Deletion cancelled by user"
        return 1
    fi
}

################################################################################
# MAIN EXECUTION
################################################################################

main() {
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case "$1" in
            -b|--bucket)
                BUCKET_NAME="$2"
                shift 2
                ;;
            -y|--yes)
                AUTO_CONFIRM=true
                shift
                ;;
            -v|--verbose)
                VERBOSE=true
                shift
                ;;
            -h|--help)
                show_usage
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
        esac
    done

    # Print header
    echo ""
    log_info "=========================================="
    log_info "  Elmify R2 Storage Cleaner"
    log_info "=========================================="
    echo ""

    # Validate dependencies
    if ! validate_dependencies; then
        exit 1
    fi

    # Show configuration
    log_info "Configuration:"
    log_info "  Bucket: $BUCKET_NAME"
    log_info "  Verbose: $VERBOSE"
    log_info "  Auto-confirm: $AUTO_CONFIRM"
    echo ""

    # Count objects in bucket
    log_info "Checking bucket contents..."
    local object_count
    object_count=$(count_objects "$BUCKET_NAME")

    # Check if bucket is empty
    if [[ "$object_count" -eq 0 ]]; then
        log_info "Bucket is already empty, nothing to delete!"
        exit 0
    fi

    log_info "Found $object_count objects in bucket"

    # Ask for confirmation
    if ! confirm_deletion "$BUCKET_NAME" "$object_count"; then
        exit 0
    fi

    # Clear the bucket
    log_info "Starting deletion process..."
    echo ""

    if clear_bucket "$BUCKET_NAME"; then
        echo ""
        log_success "✅ Bucket cleared successfully!"
        exit 0
    else
        echo ""
        log_error "❌ Bucket clearing failed!"
        exit 1
    fi
}

# Run main function with all script arguments
main "$@"
