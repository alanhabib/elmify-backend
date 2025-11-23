#!/bin/bash

################################################################################
# generate_small_images.sh - Auto-generate missing _small images
################################################################################
#
# PURPOSE:
#   Creates _small.jpg versions of speaker.jpg and collection.jpg files
#   that are missing in the content directory.
#
# USAGE:
#   ./generate_small_images.sh <content_directory>
#
# REQUIRES:
#   - ImageMagick (brew install imagemagick)
#   - sips (built-in on macOS)
#
################################################################################

set -euo pipefail

# Colors
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m'

# Configuration
THUMBNAIL_SIZE=300  # 300px width for small images

# Counters
GENERATED_COUNT=0
SKIPPED_COUNT=0
ERROR_COUNT=0

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
    echo -e "${RED}[ERROR]${NC} $*"
}

# Check if sips is available (macOS built-in)
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Generate small image using sips (macOS)
generate_thumbnail() {
    local source="$1"
    local destination="$2"
    local width="$3"

    if command_exists sips; then
        sips -Z "$width" "$source" --out "$destination" >/dev/null 2>&1
        return $?
    elif command_exists convert; then
        # ImageMagick fallback
        convert "$source" -resize "${width}x" "$destination" 2>/dev/null
        return $?
    else
        log_error "Neither sips nor ImageMagick (convert) found!"
        return 1
    fi
}

# Process a directory (speaker or collection)
process_directory() {
    local dir="$1"
    local prefix="$2"  # "speaker" or "collection"

    # Find regular image
    local source_image=""
    for ext in jpg jpeg png; do
        if [[ -f "$dir/${prefix}.$ext" ]]; then
            source_image="$dir/${prefix}.$ext"
            break
        fi
    done

    # Check if source exists
    if [[ -z "$source_image" ]]; then
        log_warning "No ${prefix} image found in: $dir"
        return 0
    fi

    # Check if small version already exists
    local ext="${source_image##*.}"
    local small_image="$dir/${prefix}_small.$ext"

    if [[ -f "$small_image" ]]; then
        log_info "✓ Small image already exists: $(basename "$dir")/${prefix}_small.$ext"
        SKIPPED_COUNT=$((SKIPPED_COUNT + 1))
        return 0
    fi

    # Generate small version
    log_info "Generating: $(basename "$dir")/${prefix}_small.$ext"

    if generate_thumbnail "$source_image" "$small_image" "$THUMBNAIL_SIZE"; then
        log_success "✅ Created: $(basename "$dir")/${prefix}_small.$ext"
        GENERATED_COUNT=$((GENERATED_COUNT + 1))
    else
        log_error "❌ Failed to create: $(basename "$dir")/${prefix}_small.$ext"
        ERROR_COUNT=$((ERROR_COUNT + 1))
    fi
}

# Main function
main() {
    if [[ $# -eq 0 ]]; then
        echo "Usage: $0 <content_directory>"
        exit 1
    fi

    local content_dir="$1"

    if [[ ! -d "$content_dir" ]]; then
        log_error "Directory does not exist: $content_dir"
        exit 1
    fi

    echo ""
    log_info "=========================================="
    log_info "  Elmify Small Image Generator"
    log_info "=========================================="
    echo ""

    # Check dependencies
    if ! command_exists sips && ! command_exists convert; then
        log_error "Neither sips nor ImageMagick found!"
        echo ""
        echo "Install ImageMagick:"
        echo "  brew install imagemagick"
        echo ""
        exit 1
    fi

    log_info "Processing: $content_dir"
    log_info "Thumbnail size: ${THUMBNAIL_SIZE}px"
    echo ""

    # Process all speaker directories
    for speaker_dir in "$content_dir"/*; do
        [[ ! -d "$speaker_dir" ]] && continue
        [[ "$(basename "$speaker_dir")" == "." ]] && continue
        [[ "$(basename "$speaker_dir")" == ".." ]] && continue
        [[ "$(basename "$speaker_dir")" == ".DS_Store" ]] && continue

        log_info "📂 Speaker: $(basename "$speaker_dir")"

        # Process speaker image
        process_directory "$speaker_dir" "speaker"

        # Process collection directories
        for collection_dir in "$speaker_dir"/*; do
            [[ ! -d "$collection_dir" ]] && continue
            [[ "$(basename "$collection_dir")" == "." ]] && continue
            [[ "$(basename "$collection_dir")" == ".." ]] && continue
            [[ "$(basename "$collection_dir")" == ".DS_Store" ]] && continue

            log_info "  📁 Collection: $(basename "$collection_dir")"
            process_directory "$collection_dir" "collection"
        done

        echo ""
    done

    # Summary
    echo ""
    log_info "=========================================="
    log_info "  Summary"
    log_info "=========================================="
    log_success "✅ Generated: $GENERATED_COUNT images"
    log_info "⏭️  Skipped: $SKIPPED_COUNT images (already exist)"

    if [[ $ERROR_COUNT -gt 0 ]]; then
        log_error "❌ Errors: $ERROR_COUNT images"
        exit 1
    fi

    echo ""
    log_success "🎉 All small images generated successfully!"
    exit 0
}

main "$@"
