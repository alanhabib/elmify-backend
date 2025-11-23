#!/bin/bash

################################################################################
# fix_content.sh - Auto-fix common issues in content directory
################################################################################
#
# PURPOSE:
#   This script automatically fixes common issues found during validation:
#   - Generates missing placeholder images (speaker, collection covers)
#   - Renames audio files to match naming convention (NN - Title.ext)
#   - Removes special characters and normalizes filenames
#   - Creates backups before making changes
#
# USAGE:
#   ./fix_content.sh [OPTIONS] <content_directory>
#
# OPTIONS:
#   -d, --dry-run          Preview changes without applying them
#   -b, --backup DIR       Backup directory (default: ./backup_TIMESTAMP)
#   -s, --skip-backup      Skip creating backup (dangerous!)
#   -i, --skip-images      Skip generating missing images
#   -r, --skip-rename      Skip renaming files
#   -v, --verbose          Show detailed output
#   -h, --help             Show this help message
#
# EXAMPLES:
#   # Preview changes (safe)
#   ./fix_content.sh --dry-run /path/to/content
#
#   # Fix all issues with backup
#   ./fix_content.sh /path/to/content
#
#   # Only rename files, skip image generation
#   ./fix_content.sh --skip-images /path/to/content
#
#   # Custom backup location
#   ./fix_content.sh --backup /path/to/backup /path/to/content
#
# DEPENDENCIES:
#   - imagemagick (convert command) - for generating placeholder images
#   - ffmpeg (ffprobe command) - for audio metadata extraction
#
# AUTHOR:  Elmify Team
# VERSION: 1.0.0
# DATE:    2025-01-12
#
################################################################################

set -euo pipefail

################################################################################
# CONFIGURATION & GLOBAL VARIABLES
################################################################################

# Command-line options
DRY_RUN=false
SKIP_BACKUP=false
SKIP_IMAGES=false
SKIP_RENAME=false
VERBOSE=false
CONTENT_DIR=""
BACKUP_DIR=""

# Counters for operations
IMAGES_GENERATED=0
FILES_RENAMED=0
ERRORS_ENCOUNTERED=0

# Color codes
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly CYAN='\033[0;36m'
readonly NC='\033[0m'

readonly SCRIPT_NAME=$(basename "$0")

################################################################################
# HELPER FUNCTIONS
################################################################################

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
    ((ERRORS_ENCOUNTERED++))
}

log_verbose() {
    if [[ "$VERBOSE" == "true" ]]; then
        echo -e "${CYAN}[VERBOSE]${NC} $*" >&2
    fi
}

log_dry_run() {
    if [[ "$DRY_RUN" == "true" ]]; then
        echo -e "${YELLOW}[DRY-RUN]${NC} $*" >&2
    fi
}

show_usage() {
    cat << EOF
Usage: $SCRIPT_NAME [OPTIONS] <content_directory>

Auto-fix common issues in content directory structure.

OPTIONS:
    -d, --dry-run          Preview changes without applying them
    -b, --backup DIR       Backup directory (default: ./backup_TIMESTAMP)
    -s, --skip-backup      Skip creating backup (dangerous!)
    -i, --skip-images      Skip generating missing images
    -r, --skip-rename      Skip renaming files
    -v, --verbose          Show detailed output
    -h, --help             Show this help message

EXAMPLES:
    # Preview changes (recommended first)
    $SCRIPT_NAME --dry-run /path/to/content

    # Fix all issues with backup
    $SCRIPT_NAME /path/to/content

    # Only rename files
    $SCRIPT_NAME --skip-images /path/to/content

    # Custom backup location
    $SCRIPT_NAME --backup /path/to/backup /path/to/content

EOF
}

################################################################################
# DEPENDENCY VALIDATION
################################################################################

command_exists() {
    command -v "$1" >/dev/null 2>&1
}

validate_dependencies() {
    log_info "Validating dependencies..."

    local missing_deps=()

    if [[ "$SKIP_IMAGES" == "false" ]] && ! command_exists convert; then
        missing_deps+=("imagemagick")
    fi

    if ! command_exists ffprobe; then
        missing_deps+=("ffmpeg")
    fi

    if [[ ${#missing_deps[@]} -gt 0 ]]; then
        log_error "Missing required dependencies: ${missing_deps[*]}"
        echo ""
        echo "Installation instructions for macOS:"
        echo ""

        for dep in "${missing_deps[@]}"; do
            case "$dep" in
                imagemagick)
                    echo "  # Install ImageMagick"
                    echo "  brew install imagemagick"
                    echo ""
                    ;;
                ffmpeg)
                    echo "  # Install ffmpeg"
                    echo "  brew install ffmpeg"
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
# BACKUP FUNCTIONS
################################################################################

create_backup() {
    local source_dir="$1"
    local backup_dir="$2"

    if [[ "$SKIP_BACKUP" == "true" ]]; then
        log_warning "Skipping backup (--skip-backup enabled)"
        return 0
    fi

    if [[ "$DRY_RUN" == "true" ]]; then
        log_dry_run "Would create backup at: $backup_dir"
        return 0
    fi

    log_info "Creating backup..."

    if ! mkdir -p "$backup_dir"; then
        log_error "Failed to create backup directory: $backup_dir"
        return 1
    fi

    if ! rsync -a "$source_dir/" "$backup_dir/"; then
        log_error "Failed to create backup"
        return 1
    fi

    log_success "Backup created at: $backup_dir"
    return 0
}

################################################################################
# IMAGE GENERATION FUNCTIONS
################################################################################

# Generate a placeholder image with text overlay
generate_placeholder_image() {
    local output_file="$1"
    local text="$2"
    local width="${3:-600}"
    local height="${4:-600}"

    if [[ "$DRY_RUN" == "true" ]]; then
        log_dry_run "Would generate placeholder: $output_file"
        return 0
    fi

    log_verbose "Generating placeholder: $output_file"

    # Generate a gradient placeholder with text
    convert -size "${width}x${height}" \
        gradient:'#4A5568-#2D3748' \
        -gravity center \
        -pointsize 40 \
        -fill white \
        -annotate +0+0 "$text" \
        "$output_file" 2>/dev/null || {
        log_error "Failed to generate placeholder: $output_file"
        return 1
    }

    ((IMAGES_GENERATED++))
    return 0
}

# Generate thumbnail from existing image
generate_thumbnail() {
    local source_image="$1"
    local output_file="$2"
    local size="${3:-300x300}"

    if [[ "$DRY_RUN" == "true" ]]; then
        log_dry_run "Would generate thumbnail: $output_file"
        return 0
    fi

    log_verbose "Generating thumbnail: $output_file"

    # Resize image to thumbnail size
    convert "$source_image" \
        -resize "$size^" \
        -gravity center \
        -extent "$size" \
        "$output_file" 2>/dev/null || {
        log_error "Failed to generate thumbnail: $output_file"
        return 1
    }

    ((IMAGES_GENERATED++))
    return 0
}

# Fix missing speaker images
fix_speaker_images() {
    local speaker_dir="$1"
    local speaker_name=$(basename "$speaker_dir")

    if [[ "$SKIP_IMAGES" == "true" ]]; then
        return 0
    fi

    log_verbose "Checking speaker images for: $speaker_name"

    # Check for main speaker image
    local speaker_image=""
    for ext in jpg jpeg png; do
        if [[ -f "$speaker_dir/speaker.$ext" ]]; then
            speaker_image="$speaker_dir/speaker.$ext"
            break
        fi
    done

    # Generate main image if missing
    if [[ -z "$speaker_image" ]]; then
        log_warning "Missing speaker.jpg for: $speaker_name"
        speaker_image="$speaker_dir/speaker.jpg"
        generate_placeholder_image "$speaker_image" "$speaker_name" 600 600
    fi

    # Check for thumbnail
    local speaker_small=""
    for ext in jpg jpeg png; do
        if [[ -f "$speaker_dir/speaker_small.$ext" ]]; then
            speaker_small="$speaker_dir/speaker_small.$ext"
            break
        fi
    done

    # Generate thumbnail if missing
    if [[ -z "$speaker_small" ]] && [[ -n "$speaker_image" ]]; then
        log_warning "Missing speaker_small.jpg for: $speaker_name"
        speaker_small="$speaker_dir/speaker_small.jpg"
        generate_thumbnail "$speaker_image" "$speaker_small" "300x300"
    fi
}

# Fix missing collection images
fix_collection_images() {
    local collection_dir="$1"
    local speaker_name="$2"
    local collection_name=$(basename "$collection_dir")

    if [[ "$SKIP_IMAGES" == "true" ]]; then
        return 0
    fi

    log_verbose "Checking collection images for: $speaker_name/$collection_name"

    # Check for main collection image
    local collection_image=""
    for ext in jpg jpeg png; do
        if [[ -f "$collection_dir/collection.$ext" ]]; then
            collection_image="$collection_dir/collection.$ext"
            break
        fi
    done

    # Generate main image if missing
    if [[ -z "$collection_image" ]]; then
        log_warning "Missing collection.jpg for: $speaker_name/$collection_name"
        collection_image="$collection_dir/collection.jpg"
        generate_placeholder_image "$collection_image" "$collection_name" 600 600
    fi

    # Check for thumbnail
    local collection_small=""
    for ext in jpg jpeg png; do
        if [[ -f "$collection_dir/collection_small.$ext" ]]; then
            collection_small="$collection_dir/collection_small.$ext"
            break
        fi
    done

    # Generate thumbnail if missing
    if [[ -z "$collection_small" ]] && [[ -n "$collection_image" ]]; then
        log_warning "Missing collection_small.jpg for: $speaker_name/$collection_name"
        collection_small="$collection_dir/collection_small.jpg"
        generate_thumbnail "$collection_image" "$collection_small" "300x300"
    fi
}

################################################################################
# FILE RENAMING FUNCTIONS
################################################################################

# Slugify text (convert to lowercase-hyphenated format)
slugify() {
    local text="$1"

    # Convert to lowercase
    text=$(echo "$text" | tr '[:upper:]' '[:lower:]')

    # Remove special characters, keep alphanumeric and spaces
    text=$(echo "$text" | sed 's/[^a-z0-9 ]//g')

    # Replace multiple spaces with single space
    text=$(echo "$text" | sed 's/  */ /g')

    # Trim leading/trailing spaces
    text=$(echo "$text" | sed 's/^ *//;s/ *$//')

    # Replace spaces with hyphens
    text=$(echo "$text" | sed 's/ /-/g')

    echo "$text"
}

# Fix lecture filename to match convention
fix_lecture_filename() {
    local file="$1"
    local collection_dir=$(dirname "$file")
    local filename=$(basename "$file")
    local extension="${filename##*.}"

    if [[ "$SKIP_RENAME" == "true" ]]; then
        return 0
    fi

    # Check if already matches pattern
    if [[ "$filename" =~ ^[0-9]{2}\ -\ .+\..+$ ]]; then
        log_verbose "Filename already correct: $filename"
        return 0
    fi

    log_warning "Fixing lecture filename: $filename"

    # Try to extract number from filename
    local number=""
    if [[ "$filename" =~ ^([0-9]+) ]]; then
        number="${BASH_REMATCH[1]}"
        # Pad to 2 digits
        number=$(printf "%02d" "$number")
    else
        # No number found, assign next available
        number=$(find "$collection_dir" -maxdepth 1 -type f -name "*.${extension}" | wc -l | tr -d ' ')
        number=$(printf "%02d" $((number + 1)))
    fi

    # Extract title (remove number and clean up)
    local title="${filename%.*}"
    title=$(echo "$title" | sed 's/^[0-9]*[^a-zA-Z]*//')  # Remove leading numbers/special chars
    title=$(echo "$title" | sed 's/[_-]/ /g')  # Replace underscores/hyphens with spaces
    title=$(echo "$title" | sed 's/  */ /g')  # Normalize spaces

    # Generate new filename
    local new_filename="${number} - ${title}.${extension}"
    local new_file="$collection_dir/$new_filename"

    if [[ "$DRY_RUN" == "true" ]]; then
        log_dry_run "Would rename: $filename -> $new_filename"
        return 0
    fi

    # Rename file
    if mv "$file" "$new_file" 2>/dev/null; then
        log_success "Renamed: $filename -> $new_filename"
        ((FILES_RENAMED++))
    else
        log_error "Failed to rename: $filename"
        return 1
    fi

    return 0
}

################################################################################
# DIRECTORY PROCESSING
################################################################################

process_collection() {
    local collection_dir="$1"
    local speaker_name="$2"

    # Fix collection images
    fix_collection_images "$collection_dir" "$speaker_name"

    # Fix lecture filenames
    while IFS= read -r -d '' lecture_file; do
        fix_lecture_filename "$lecture_file"
    done < <(find "$collection_dir" -maxdepth 1 -type f \( -iname "*.mp3" -o -iname "*.m4a" -o -iname "*.wav" -o -iname "*.flac" -o -iname "*.aac" -o -iname "*.ogg" \) -print0)
}

process_speaker() {
    local speaker_dir="$1"
    local speaker_name=$(basename "$speaker_dir")

    log_info "Processing speaker: $speaker_name"

    # Fix speaker images
    fix_speaker_images "$speaker_dir"

    # Process collections
    while IFS= read -r -d '' collection_dir; do
        process_collection "$collection_dir" "$speaker_name"
    done < <(find "$speaker_dir" -mindepth 1 -maxdepth 1 -type d -print0)
}

################################################################################
# MAIN EXECUTION
################################################################################

main() {
    # Parse command-line arguments
    while [[ $# -gt 0 ]]; do
        case "$1" in
            -d|--dry-run)
                DRY_RUN=true
                shift
                ;;
            -b|--backup)
                BACKUP_DIR="$2"
                shift 2
                ;;
            -s|--skip-backup)
                SKIP_BACKUP=true
                shift
                ;;
            -i|--skip-images)
                SKIP_IMAGES=true
                shift
                ;;
            -r|--skip-rename)
                SKIP_RENAME=true
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
            -*)
                log_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
            *)
                CONTENT_DIR="$1"
                shift
                ;;
        esac
    done

    # Validate content directory argument
    if [[ -z "$CONTENT_DIR" ]]; then
        log_error "Content directory not specified"
        show_usage
        exit 1
    fi

    # Set default backup directory if not specified
    if [[ -z "$BACKUP_DIR" ]] && [[ "$SKIP_BACKUP" == "false" ]]; then
        BACKUP_DIR="./backup_$(date +%Y%m%d_%H%M%S)"
    fi

    # Print header
    echo ""
    log_info "=========================================="
    log_info "  Elmify Content Fixer"
    log_info "=========================================="
    echo ""

    if [[ "$DRY_RUN" == "true" ]]; then
        log_warning "DRY-RUN MODE: No changes will be made"
        echo ""
    fi

    # Validate dependencies
    if ! validate_dependencies; then
        exit 1
    fi
    echo ""

    # Check if content directory exists
    if [[ ! -d "$CONTENT_DIR" ]]; then
        log_error "Content directory not found: $CONTENT_DIR"
        exit 1
    fi

    # Create backup
    if [[ "$DRY_RUN" == "false" ]] && [[ "$SKIP_BACKUP" == "false" ]]; then
        if ! create_backup "$CONTENT_DIR" "$BACKUP_DIR"; then
            log_error "Backup failed, aborting"
            exit 1
        fi
        echo ""
    fi

    # Process all speakers
    while IFS= read -r -d '' speaker_dir; do
        process_speaker "$speaker_dir"
        echo ""
    done < <(find "$CONTENT_DIR" -mindepth 1 -maxdepth 1 -type d -print0)

    # Print summary
    echo ""
    log_info "=========================================="
    log_info "  Fix Summary"
    log_info "=========================================="
    echo ""
    log_info "Images Generated: $IMAGES_GENERATED"
    log_info "Files Renamed:    $FILES_RENAMED"
    log_info "Errors:           $ERRORS_ENCOUNTERED"
    echo ""

    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "Dry-run complete. Run without --dry-run to apply changes."
    elif [[ $ERRORS_ENCOUNTERED -eq 0 ]]; then
        log_success "✅ All fixes applied successfully!"

        if [[ "$SKIP_BACKUP" == "false" ]]; then
            log_info "Backup saved at: $BACKUP_DIR"
        fi
    else
        log_warning "⚠️  Completed with $ERRORS_ENCOUNTERED errors"
    fi

    exit 0
}

# Run main function
main "$@"
