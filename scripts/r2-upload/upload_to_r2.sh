#!/bin/bash

################################################################################
# upload_to_r2.sh - Upload speaker/lecture content to Cloudflare R2 storage
################################################################################
#
# PURPOSE:
#   This script uploads validated content to Cloudflare R2 storage using
#   Wrangler CLI, extracts metadata, and generates a JSON manifest for
#   database import.
#
# R2 PATH STRUCTURE:
#   speakers/{speaker-slug}/images/speaker.jpg
#   speakers/{speaker-slug}/images/speaker_small.jpg
#   speakers/{speaker-slug}/collections/{collection-slug}/images/cover.jpg
#   speakers/{speaker-slug}/collections/{collection-slug}/images/cover_small.jpg
#   speakers/{speaker-slug}/collections/{collection-slug}/lectures/01-title.mp3
#
# USAGE:
#   ./upload_to_r2.sh [OPTIONS] <content_directory>
#
# OPTIONS:
#   -b, --bucket BUCKET    R2 bucket name (default: elmify-audio)
#   -o, --output FILE      Output manifest file (default: manifest.json)
#   -r, --resume           Resume upload (skip existing files)
#   -f, --force            Force re-upload all files
#   -v, --verbose          Show detailed output
#   -h, --help             Show this help message
#
# ENVIRONMENT VARIABLES:
#   R2_BUCKET_NAME         Cloudflare R2 bucket name
#
# EXAMPLES:
#   # Upload content
#   ./upload_to_r2.sh /path/to/content
#
#   # Resume interrupted upload
#   ./upload_to_r2.sh --resume /path/to/content
#
#   # Custom bucket and output
#   ./upload_to_r2.sh --bucket my-bucket --output data.json /path/to/content
#
# OUTPUT:
#   Generates a JSON manifest file with all metadata for database import.
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
BUCKET_NAME="${R2_BUCKET_NAME:-elmify-audio}"
OUTPUT_FILE="manifest.json"
RESUME_MODE=false
FORCE_UPLOAD=false
VERBOSE=false
CONTENT_DIR=""

# Counters
SPEAKERS_UPLOADED=0
COLLECTIONS_UPLOADED=0
LECTURES_UPLOADED=0
FILES_SKIPPED=0
UPLOAD_ERRORS=0

# Manifest data structure (will be JSON)
MANIFEST_DATA="[]"

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
    ((UPLOAD_ERRORS++))
}

log_verbose() {
    if [[ "$VERBOSE" == "true" ]]; then
        echo -e "${CYAN}[VERBOSE]${NC} $*" >&2
    fi
}

show_usage() {
    cat << EOF
Usage: $SCRIPT_NAME [OPTIONS] <content_directory>

Upload speaker/lecture content to Cloudflare R2 storage.

OPTIONS:
    -b, --bucket BUCKET    R2 bucket name (default: $BUCKET_NAME)
    -o, --output FILE      Output manifest file (default: $OUTPUT_FILE)
    -r, --resume           Resume upload (skip existing files)
    -f, --force            Force re-upload all files
    -v, --verbose          Show detailed output
    -h, --help             Show this help message

ENVIRONMENT VARIABLES:
    R2_BUCKET_NAME         Cloudflare R2 bucket name

EXAMPLES:
    # Upload content
    $SCRIPT_NAME /path/to/content

    # Resume interrupted upload
    $SCRIPT_NAME --resume /path/to/content

    # Custom bucket and output
    $SCRIPT_NAME --bucket my-bucket --output data.json /path/to/content

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

    if ! command_exists wrangler; then
        missing_deps+=("wrangler")
    fi

    if ! command_exists ffprobe; then
        missing_deps+=("ffmpeg")
    fi

    if ! command_exists jq; then
        missing_deps+=("jq")
    fi

    if ! command_exists shasum; then
        missing_deps+=("shasum")
    fi

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
                ffmpeg)
                    echo "  # Install ffmpeg (includes ffprobe)"
                    echo "  brew install ffmpeg"
                    echo ""
                    ;;
                jq)
                    echo "  # Install jq"
                    echo "  brew install jq"
                    echo ""
                    ;;
                shasum)
                    echo "  # shasum is usually pre-installed on macOS"
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
# UTILITY FUNCTIONS
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

# Calculate SHA-256 hash of file
calculate_hash() {
    local file="$1"

    shasum -a 256 "$file" 2>/dev/null | awk '{print $1}'
}

# Get file size in bytes
get_file_size() {
    local file="$1"

    stat -f%z "$file" 2>/dev/null || stat -c%s "$file" 2>/dev/null
}

# JSON escape string
json_escape() {
    local string="$1"

    # Escape special characters for JSON
    echo "$string" | jq -R -s '.'
}

################################################################################
# R2 OPERATIONS
################################################################################

# Check if object exists in R2
object_exists() {
    local bucket="$1"
    local object_key="$2"

    log_verbose "Checking if exists: $object_key"

    # Try to get object metadata
    wrangler r2 object get "$bucket" "$object_key" --json >/dev/null 2>&1
}

# Upload file to R2
upload_file() {
    local local_file="$1"
    local bucket="$2"
    local object_key="$3"

    log_verbose "Uploading: $object_key"

    # Check if file should be skipped (resume mode)
    if [[ "$RESUME_MODE" == "true" ]] && object_exists "$bucket" "$object_key"; then
        log_verbose "Skipping (already exists): $object_key"
        ((FILES_SKIPPED++))
        return 0
    fi

    # Upload using wrangler (--remote flag uploads to Cloudflare R2, not local)
    if wrangler r2 object put "$bucket/$object_key" --file "$local_file" --remote >/dev/null 2>&1; then
        log_success "Uploaded: $object_key"
        return 0
    else
        log_error "Failed to upload: $object_key"
        return 1
    fi
}

################################################################################
# METADATA EXTRACTION
################################################################################

# Extract audio metadata using ffprobe
extract_audio_metadata() {
    local file="$1"

    # Extract duration in seconds (rounded to integer)
    local duration
    duration=$(ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 "$file" 2>/dev/null)
    duration=$(printf "%.0f" "$duration" 2>/dev/null || echo "0")

    # Extract bitrate (in bits per second)
    local bitrate
    bitrate=$(ffprobe -v error -show_entries format=bit_rate -of default=noprint_wrappers=1:nokey=1 "$file" 2>/dev/null)
    bitrate=${bitrate:-0}

    # Extract sample rate
    local sample_rate
    sample_rate=$(ffprobe -v error -select_streams a:0 -show_entries stream=sample_rate -of default=noprint_wrappers=1:nokey=1 "$file" 2>/dev/null)
    sample_rate=${sample_rate:-0}

    # Return JSON object
    echo "{\"duration\":$duration,\"bitrate\":$bitrate,\"sampleRate\":$sample_rate}"
}

################################################################################
# UPLOAD FUNCTIONS
################################################################################

# Upload speaker images and return metadata
upload_speaker_images() {
    local speaker_dir="$1"
    local speaker_slug="$2"
    local speaker_name="$3"

    log_verbose "Uploading speaker images: $speaker_name"

    local image_url=""
    local image_small_url=""

    # Upload main speaker image
    for ext in jpg jpeg png; do
        if [[ -f "$speaker_dir/speaker.$ext" ]]; then
            local object_key="speakers/$speaker_slug/images/speaker.jpg"
            upload_file "$speaker_dir/speaker.$ext" "$BUCKET_NAME" "$object_key"
            image_url="$object_key"
            break
        fi
    done

    # Upload speaker thumbnail
    for ext in jpg jpeg png; do
        if [[ -f "$speaker_dir/speaker_small.$ext" ]]; then
            local object_key="speakers/$speaker_slug/images/speaker_small.jpg"
            upload_file "$speaker_dir/speaker_small.$ext" "$BUCKET_NAME" "$object_key"
            image_small_url="$object_key"
            break
        fi
    done

    echo "{\"imageUrl\":$(json_escape "$image_url"),\"imageSmallUrl\":$(json_escape "$image_small_url")}"
}

# Upload collection images and return metadata
upload_collection_images() {
    local collection_dir="$1"
    local speaker_slug="$2"
    local collection_slug="$3"
    local collection_name="$4"

    log_verbose "Uploading collection images: $collection_name"

    local cover_url=""
    local cover_small_url=""

    # Upload collection cover
    for ext in jpg jpeg png; do
        if [[ -f "$collection_dir/collection.$ext" ]]; then
            local object_key="speakers/$speaker_slug/collections/$collection_slug/images/cover.jpg"
            upload_file "$collection_dir/collection.$ext" "$BUCKET_NAME" "$object_key"
            cover_url="$object_key"
            break
        fi
    done

    # Upload collection thumbnail
    for ext in jpg jpeg png; do
        if [[ -f "$collection_dir/collection_small.$ext" ]]; then
            local object_key="speakers/$speaker_slug/collections/$collection_slug/images/cover_small.jpg"
            upload_file "$collection_dir/collection_small.$ext" "$BUCKET_NAME" "$object_key"
            cover_small_url="$object_key"
            break
        fi
    done

    echo "{\"coverImageUrl\":$(json_escape "$cover_url"),\"coverImageSmallUrl\":$(json_escape "$cover_small_url")}"
}

# Upload lecture and return metadata
upload_lecture() {
    local lecture_file="$1"
    local speaker_slug="$2"
    local collection_slug="$3"
    local lecture_number="$4"

    local filename=$(basename "$lecture_file")
    local extension="${filename##*.}"
    local title="${filename%.*}"

    # Remove lecture number prefix from title (e.g., "01 - " )
    title=$(echo "$title" | sed 's/^[0-9]* - //')

    log_info "    Uploading lecture: $filename"

    # Generate slugified filename
    local title_slug=$(slugify "$title")
    local file_name=$(printf "%02d-%s.%s" "$lecture_number" "$title_slug" "$extension")

    # Generate R2 object key
    local object_key="speakers/$speaker_slug/collections/$collection_slug/lectures/$file_name"

    # Upload file
    if ! upload_file "$lecture_file" "$BUCKET_NAME" "$object_key"; then
        return 1
    fi

    # Extract metadata
    local audio_metadata
    audio_metadata=$(extract_audio_metadata "$lecture_file")

    local duration=$(echo "$audio_metadata" | jq -r '.duration')
    local bitrate=$(echo "$audio_metadata" | jq -r '.bitrate')
    local sample_rate=$(echo "$audio_metadata" | jq -r '.sampleRate')

    # Get file size
    local file_size
    file_size=$(get_file_size "$lecture_file")

    # Calculate file hash
    local file_hash
    file_hash=$(calculate_hash "$lecture_file")

    # Read lecture description from matching JSON file
    local description=""
    local lecture_json_file="${lecture_file%.*}.json"
    if [[ -f "$lecture_json_file" ]]; then
        description=$(jq -r '.description // ""' "$lecture_json_file" 2>/dev/null || echo "")
    fi

    ((LECTURES_UPLOADED++))

    # Return JSON object
    cat <<EOF
{
  "title": $(json_escape "$title"),
  "description": $(json_escape "$description"),
  "lectureNumber": $lecture_number,
  "fileName": $(json_escape "$file_name"),
  "filePath": $(json_escape "$object_key"),
  "duration": $duration,
  "fileSize": $file_size,
  "fileFormat": $(json_escape "$extension"),
  "bitrate": $bitrate,
  "sampleRate": $sample_rate,
  "fileHash": $(json_escape "$file_hash")
}
EOF
}

# Process collection directory
process_collection() {
    local collection_dir="$1"
    local speaker_slug="$2"
    local speaker_name="$3"

    local collection_name=$(basename "$collection_dir")
    local collection_slug=$(slugify "$collection_name")

    log_info "  Processing collection: $collection_name"

    # Upload collection images
    local image_metadata
    image_metadata=$(upload_collection_images "$collection_dir" "$speaker_slug" "$collection_slug" "$collection_name")

    local cover_image_url=$(echo "$image_metadata" | jq -r '.coverImageUrl')
    local cover_image_small_url=$(echo "$image_metadata" | jq -r '.coverImageSmallUrl')

    # Read collection metadata from collection.json
    local collection_description=""
    local collection_year="null"
    local collection_json_file="$collection_dir/collection.json"
    if [[ -f "$collection_json_file" ]]; then
        collection_description=$(jq -r '.description // ""' "$collection_json_file" 2>/dev/null || echo "")
        collection_year=$(jq -r '.year // null' "$collection_json_file" 2>/dev/null || echo "null")
    fi

    # Process lectures
    local lectures_json="[]"
    local lecture_number=1

    while IFS= read -r -d '' lecture_file; do
        local lecture_json
        lecture_json=$(upload_lecture "$lecture_file" "$speaker_slug" "$collection_slug" "$lecture_number")

        # Add to lectures array
        lectures_json=$(echo "$lectures_json" | jq --argjson lecture "$lecture_json" '. += [$lecture]')

        ((lecture_number++))
    done < <(find "$collection_dir" -maxdepth 1 -type f \( -iname "*.mp3" -o -iname "*.m4a" -o -iname "*.wav" -o -iname "*.flac" -o -iname "*.aac" -o -iname "*.ogg" \) -print0 | sort -z)

    ((COLLECTIONS_UPLOADED++))

    # Return collection JSON
    cat <<EOF
{
  "title": $(json_escape "$collection_name"),
  "description": $(json_escape "$collection_description"),
  "year": $collection_year,
  "coverImageUrl": $(json_escape "$cover_image_url"),
  "coverImageSmallUrl": $(json_escape "$cover_image_small_url"),
  "lectures": $lectures_json
}
EOF
}

# Process speaker directory
process_speaker() {
    local speaker_dir="$1"
    local speaker_name=$(basename "$speaker_dir")
    local speaker_slug=$(slugify "$speaker_name")

    log_info "Processing speaker: $speaker_name"

    # Upload speaker images
    local image_metadata
    image_metadata=$(upload_speaker_images "$speaker_dir" "$speaker_slug" "$speaker_name")

    local image_url=$(echo "$image_metadata" | jq -r '.imageUrl')
    local image_small_url=$(echo "$image_metadata" | jq -r '.imageSmallUrl')

    # Read speaker metadata from speaker.json
    local speaker_bio=""
    local speaker_is_premium="false"
    local speaker_json_file="$speaker_dir/speaker.json"
    if [[ -f "$speaker_json_file" ]]; then
        speaker_bio=$(jq -r '.bio // ""' "$speaker_json_file" 2>/dev/null || echo "")
        speaker_is_premium=$(jq -r '.isPremium // false' "$speaker_json_file" 2>/dev/null || echo "false")
    fi

    # Process collections
    local collections_json="[]"

    while IFS= read -r -d '' collection_dir; do
        local collection_json
        collection_json=$(process_collection "$collection_dir" "$speaker_slug" "$speaker_name")

        # Add to collections array
        collections_json=$(echo "$collections_json" | jq --argjson collection "$collection_json" '. += [$collection]')

        echo ""
    done < <(find "$speaker_dir" -mindepth 1 -maxdepth 1 -type d -print0 | sort -z)

    ((SPEAKERS_UPLOADED++))

    # Return speaker JSON
    cat <<EOF
{
  "name": $(json_escape "$speaker_name"),
  "bio": $(json_escape "$speaker_bio"),
  "imageUrl": $(json_escape "$image_url"),
  "imageSmallUrl": $(json_escape "$image_small_url"),
  "isPremium": $speaker_is_premium,
  "collections": $collections_json
}
EOF
}

################################################################################
# MAIN EXECUTION
################################################################################

main() {
    # Parse command-line arguments
    while [[ $# -gt 0 ]]; do
        case "$1" in
            -b|--bucket)
                BUCKET_NAME="$2"
                shift 2
                ;;
            -o|--output)
                OUTPUT_FILE="$2"
                shift 2
                ;;
            -r|--resume)
                RESUME_MODE=true
                shift
                ;;
            -f|--force)
                FORCE_UPLOAD=true
                RESUME_MODE=false
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

    # Print header
    echo ""
    log_info "=========================================="
    log_info "  Elmify R2 Uploader"
    log_info "=========================================="
    echo ""

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

    # Show configuration
    log_info "Configuration:"
    log_info "  Content Dir: $CONTENT_DIR"
    log_info "  Bucket:      $BUCKET_NAME"
    log_info "  Output:      $OUTPUT_FILE"
    log_info "  Resume:      $RESUME_MODE"
    log_info "  Verbose:     $VERBOSE"
    echo ""

    # Process all speakers
    local speakers_json="[]"

    log_info "Starting upload process..."
    echo ""

    while IFS= read -r -d '' speaker_dir; do
        local speaker_json
        speaker_json=$(process_speaker "$speaker_dir")

        # Add to speakers array
        speakers_json=$(echo "$speakers_json" | jq --argjson speaker "$speaker_json" '. += [$speaker]')

        echo ""
    done < <(find "$CONTENT_DIR" -mindepth 1 -maxdepth 1 -type d -print0 | sort -z)

    # Generate final manifest
    local manifest
    manifest=$(jq -n --argjson speakers "$speakers_json" '{speakers: $speakers}')

    # Write manifest to file
    echo "$manifest" > "$OUTPUT_FILE"

    # Print summary
    echo ""
    log_info "=========================================="
    log_info "  Upload Summary"
    log_info "=========================================="
    echo ""
    log_info "Speakers Uploaded:    $SPEAKERS_UPLOADED"
    log_info "Collections Uploaded: $COLLECTIONS_UPLOADED"
    log_info "Lectures Uploaded:    $LECTURES_UPLOADED"
    log_info "Files Skipped:        $FILES_SKIPPED"
    log_info "Errors:               $UPLOAD_ERRORS"
    echo ""
    log_success "Manifest saved to: $OUTPUT_FILE"
    echo ""

    if [[ $UPLOAD_ERRORS -eq 0 ]]; then
        log_success "✅ Upload completed successfully!"
        log_info "Next steps:"
        log_info "  1. Review the manifest file: $OUTPUT_FILE"
        log_info "  2. Import data to database using the manifest"
        exit 0
    else
        log_error "❌ Upload completed with $UPLOAD_ERRORS errors"
        exit 1
    fi
}

# Run main function
main "$@"
