#!/bin/bash

################################################################################
# convert_existing_structure.sh - Convert existing content to expected format
################################################################################
#
# PURPOSE:
#   Convert your existing batch content structure to match the expected format
#   for the upload scripts.
#
# TRANSFORMATIONS:
#   - speaker-image.jpg → speaker.jpg
#   - cover.jpg → collection.jpg
#   - Generate speaker_small.jpg from speaker.jpg
#   - Generate collection_small.jpg from collection.jpg
#   - Add lecture numbers to MP3 files (01 - Title.mp3)
#   - Remove .json files (not needed)
#   - Convert underscore names to Title Case directories
#
# USAGE:
#   ./convert_existing_structure.sh [OPTIONS] <source_dir> <output_dir>
#
# OPTIONS:
#   -d, --dry-run          Preview changes without applying them
#   -v, --verbose          Show detailed output
#   -h, --help             Show this help message
#
# EXAMPLES:
#   # Preview conversion
#   ./convert_existing_structure.sh --dry-run \
#     ~/Desktop/hobby_projects/batch/_staging \
#     ~/Desktop/hobby_projects/batch/content
#
#   # Apply conversion
#   ./convert_existing_structure.sh \
#     ~/Desktop/hobby_projects/batch/_staging \
#     ~/Desktop/hobby_projects/batch/content
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

DRY_RUN=false
VERBOSE=false
SOURCE_DIR=""
OUTPUT_DIR=""

# Counters
SPEAKERS_CONVERTED=0
COLLECTIONS_CONVERTED=0
LECTURES_CONVERTED=0
ERRORS=0

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
    ((ERRORS++))
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
Usage: $SCRIPT_NAME [OPTIONS] <source_dir> <output_dir>

Convert existing batch content to expected format.

OPTIONS:
    -d, --dry-run          Preview changes without applying them
    -v, --verbose          Show detailed output
    -h, --help             Show this help message

EXAMPLES:
    # Preview conversion
    $SCRIPT_NAME --dry-run ~/batch/_staging ~/batch/content

    # Apply conversion
    $SCRIPT_NAME ~/batch/_staging ~/batch/content

EOF
}

################################################################################
# UTILITY FUNCTIONS
################################################################################

# Convert underscore_name to Title Case
to_title_case() {
    local text="$1"

    # Replace underscores with spaces
    text=$(echo "$text" | sed 's/_/ /g')

    # Convert to title case (capitalize first letter of each word)
    text=$(echo "$text" | awk '{for(i=1;i<=NF;i++) $i=toupper(substr($i,1,1)) tolower(substr($i,2))}1')

    echo "$text"
}

# Generate thumbnail from image
generate_thumbnail() {
    local source_image="$1"
    local output_file="$2"

    if [[ "$DRY_RUN" == "true" ]]; then
        log_dry_run "Would generate thumbnail: $output_file"
        return 0
    fi

    log_verbose "Generating thumbnail: $output_file"

    convert "$source_image" \
        -resize "300x300^" \
        -gravity center \
        -extent "300x300" \
        "$output_file" 2>/dev/null || {
        log_error "Failed to generate thumbnail: $output_file"
        return 1
    }

    return 0
}

################################################################################
# CONVERSION FUNCTIONS
################################################################################

# Convert speaker directory
convert_speaker() {
    local source_speaker_dir="$1"
    local speaker_slug=$(basename "$source_speaker_dir")
    local speaker_name=$(to_title_case "$speaker_slug")

    log_info "Converting speaker: $speaker_slug → $speaker_name"

    # Create output directory
    local output_speaker_dir="$OUTPUT_DIR/$speaker_name"

    if [[ "$DRY_RUN" == "false" ]]; then
        mkdir -p "$output_speaker_dir"
    else
        log_dry_run "Would create: $output_speaker_dir"
    fi

    # Convert speaker image
    local speaker_image=""

    # Check for speaker-image.jpg (your current format)
    if [[ -f "$source_speaker_dir/speaker-image.jpg" ]]; then
        speaker_image="$source_speaker_dir/speaker-image.jpg"
    elif [[ -f "$source_speaker_dir/speaker.jpg" ]]; then
        speaker_image="$source_speaker_dir/speaker.jpg"
    elif [[ -f "$source_speaker_dir/speaker.png" ]]; then
        speaker_image="$source_speaker_dir/speaker.png"
    fi

    if [[ -n "$speaker_image" ]]; then
        if [[ "$DRY_RUN" == "false" ]]; then
            cp "$speaker_image" "$output_speaker_dir/speaker.jpg"
            log_success "  Copied speaker image"

            # Generate thumbnail
            generate_thumbnail "$output_speaker_dir/speaker.jpg" "$output_speaker_dir/speaker_small.jpg"
        else
            log_dry_run "  Would copy: speaker image → speaker.jpg"
            log_dry_run "  Would generate: speaker_small.jpg"
        fi
    else
        log_warning "  No speaker image found for: $speaker_name"
    fi

    # Convert collections
    while IFS= read -r -d '' collection_dir; do
        convert_collection "$collection_dir" "$output_speaker_dir"
    done < <(find "$source_speaker_dir" -mindepth 1 -maxdepth 1 -type d -print0)

    ((SPEAKERS_CONVERTED++))
}

# Convert collection directory
convert_collection() {
    local source_collection_dir="$1"
    local output_speaker_dir="$2"
    local collection_slug=$(basename "$source_collection_dir")
    local collection_name=$(to_title_case "$collection_slug")

    log_info "  Converting collection: $collection_slug → $collection_name"

    # Create output directory
    local output_collection_dir="$output_speaker_dir/$collection_name"

    if [[ "$DRY_RUN" == "false" ]]; then
        mkdir -p "$output_collection_dir"
    else
        log_dry_run "  Would create: $output_collection_dir"
    fi

    # Convert collection image
    local collection_image=""

    # Check for cover.jpg (your current format)
    if [[ -f "$source_collection_dir/cover.jpg" ]]; then
        collection_image="$source_collection_dir/cover.jpg"
    elif [[ -f "$source_collection_dir/collection.jpg" ]]; then
        collection_image="$source_collection_dir/collection.jpg"
    elif [[ -f "$source_collection_dir/collection.png" ]]; then
        collection_image="$source_collection_dir/collection.png"
    fi

    if [[ -n "$collection_image" ]]; then
        if [[ "$DRY_RUN" == "false" ]]; then
            cp "$collection_image" "$output_collection_dir/collection.jpg"
            log_success "    Copied collection cover"

            # Generate thumbnail
            generate_thumbnail "$output_collection_dir/collection.jpg" "$output_collection_dir/collection_small.jpg"
        else
            log_dry_run "    Would copy: cover → collection.jpg"
            log_dry_run "    Would generate: collection_small.jpg"
        fi
    else
        log_warning "    No collection cover found for: $collection_name"
    fi

    # Convert lectures
    local lecture_number=1

    while IFS= read -r -d '' lecture_file; do
        convert_lecture "$lecture_file" "$output_collection_dir" "$lecture_number"
        ((lecture_number++))
    done < <(find "$source_collection_dir" -maxdepth 1 -type f \( -iname "*.mp3" -o -iname "*.m4a" \) -print0 | sort -z)

    ((COLLECTIONS_CONVERTED++))
}

# Convert lecture file
convert_lecture() {
    local source_file="$1"
    local output_collection_dir="$2"
    local lecture_number="$3"

    local filename=$(basename "$source_file")
    local extension="${filename##*.}"
    local title="${filename%.*}"

    # Format lecture number (01, 02, etc.)
    local formatted_number=$(printf "%02d" "$lecture_number")

    # Generate new filename: "01 - Title.mp3"
    local new_filename="${formatted_number} - ${title}.${extension}"

    log_verbose "    Converting lecture: $filename → $new_filename"

    if [[ "$DRY_RUN" == "false" ]]; then
        cp "$source_file" "$output_collection_dir/$new_filename"
    else
        log_dry_run "    Would copy: $filename → $new_filename"
    fi

    ((LECTURES_CONVERTED++))
}

################################################################################
# DEPENDENCY VALIDATION
################################################################################

command_exists() {
    command -v "$1" >/dev/null 2>&1
}

validate_dependencies() {
    log_info "Validating dependencies..."

    if ! command_exists convert; then
        log_error "Missing ImageMagick (convert command)"
        echo ""
        echo "Install with: brew install imagemagick"
        echo ""
        return 1
    fi

    log_success "All dependencies are installed"
    return 0
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
                if [[ -z "$SOURCE_DIR" ]]; then
                    SOURCE_DIR="$1"
                elif [[ -z "$OUTPUT_DIR" ]]; then
                    OUTPUT_DIR="$1"
                else
                    log_error "Too many arguments"
                    show_usage
                    exit 1
                fi
                shift
                ;;
        esac
    done

    # Validate arguments
    if [[ -z "$SOURCE_DIR" ]] || [[ -z "$OUTPUT_DIR" ]]; then
        log_error "Missing required arguments"
        show_usage
        exit 1
    fi

    # Print header
    echo ""
    log_info "=========================================="
    log_info "  Elmify Structure Converter"
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

    # Check if source directory exists
    if [[ ! -d "$SOURCE_DIR" ]]; then
        log_error "Source directory not found: $SOURCE_DIR"
        exit 1
    fi

    # Create output directory
    if [[ "$DRY_RUN" == "false" ]]; then
        mkdir -p "$OUTPUT_DIR"
    fi

    # Show configuration
    log_info "Configuration:"
    log_info "  Source:  $SOURCE_DIR"
    log_info "  Output:  $OUTPUT_DIR"
    log_info "  Dry-run: $DRY_RUN"
    log_info "  Verbose: $VERBOSE"
    echo ""

    # Process all speakers
    log_info "Starting conversion..."
    echo ""

    while IFS= read -r -d '' speaker_dir; do
        # Skip directories starting with _ or .
        local dirname=$(basename "$speaker_dir")
        if [[ "$dirname" == _* ]] || [[ "$dirname" == .* ]]; then
            log_verbose "Skipping: $dirname"
            continue
        fi

        convert_speaker "$speaker_dir"
        echo ""
    done < <(find "$SOURCE_DIR" -mindepth 1 -maxdepth 1 -type d -print0)

    # Print summary
    echo ""
    log_info "=========================================="
    log_info "  Conversion Summary"
    log_info "=========================================="
    echo ""
    log_info "Speakers Converted:    $SPEAKERS_CONVERTED"
    log_info "Collections Converted: $COLLECTIONS_CONVERTED"
    log_info "Lectures Converted:    $LECTURES_CONVERTED"
    log_info "Errors:                $ERRORS"
    echo ""

    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "Dry-run complete. Run without --dry-run to apply changes."
    elif [[ $ERRORS -eq 0 ]]; then
        log_success "✅ Conversion completed successfully!"
        echo ""
        log_info "Next steps:"
        log_info "  1. Validate: ./scripts/validate_content.sh \"$OUTPUT_DIR\""
        log_info "  2. Fix: ./scripts/fix_content.sh \"$OUTPUT_DIR\""
        log_info "  3. Upload: ./scripts/upload_to_r2.sh \"$OUTPUT_DIR\""
    else
        log_warning "⚠️  Completed with $ERRORS errors"
    fi

    exit 0
}

# Run main function
main "$@"
