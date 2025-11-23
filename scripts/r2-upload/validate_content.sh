#!/bin/bash

################################################################################
# validate_content.sh - Validate speaker/lecture directory structure and files
################################################################################
#
# PURPOSE:
#   This script validates the content directory structure for speakers,
#   collections, and lectures before uploading to R2 storage.
#
# EXPECTED STRUCTURE:
#   content/
#   ├── Speaker Name/
#   │   ├── speaker.jpg (or .png)
#   │   ├── speaker_small.jpg (or .png)
#   │   └── Collection Name/
#   │       ├── collection.jpg (or .png)
#   │       ├── collection_small.jpg (or .png)
#   │       ├── 01 - Lecture Title.mp3
#   │       └── 02 - Another Lecture.mp3
#
# USAGE:
#   ./validate_content.sh [OPTIONS] <content_directory>
#
# OPTIONS:
#   -v, --verbose          Show detailed validation output
#   -f, --fix-suggestions  Show suggestions for fixing issues
#   -s, --strict           Treat warnings as errors
#   -h, --help             Show this help message
#
# EXIT CODES:
#   0 - All validations passed
#   1 - Critical errors found (missing required files, invalid structure)
#   2 - Warnings found (optional files missing, naming issues)
#
# EXAMPLES:
#   # Basic validation
#   ./validate_content.sh /path/to/content
#
#   # Verbose mode with fix suggestions
#   ./validate_content.sh -v -f /path/to/content
#
#   # Strict mode (warnings = errors)
#   ./validate_content.sh --strict /path/to/content
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

# Command-line options
VERBOSE=false
FIX_SUGGESTIONS=false
STRICT_MODE=false
CONTENT_DIR=""

# Counters for validation results
ERROR_COUNT=0
WARNING_COUNT=0
SPEAKER_COUNT=0
COLLECTION_COUNT=0
LECTURE_COUNT=0

# Supported file formats
SUPPORTED_AUDIO_FORMATS=("mp3" "m4a" "wav" "flac" "aac" "ogg")
SUPPORTED_IMAGE_FORMATS=("jpg" "jpeg" "png" "webp")

# Color codes for terminal output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color

# Script metadata
readonly SCRIPT_NAME=$(basename "$0")

################################################################################
# HELPER FUNCTIONS
################################################################################

# Print colored output to stderr
log_info() {
    echo -e "${BLUE}[INFO]${NC} $*" >&2
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $*" >&2
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $*" >&2
    ((WARNING_COUNT++))
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $*" >&2
    ((ERROR_COUNT++))
}

log_verbose() {
    if [[ "$VERBOSE" == "true" ]]; then
        echo -e "${BLUE}[VERBOSE]${NC} $*" >&2
    fi
}

# Show fix suggestion if enabled
suggest_fix() {
    if [[ "$FIX_SUGGESTIONS" == "true" ]]; then
        echo -e "${GREEN}[FIX]${NC} $*" >&2
    fi
}

# Show usage information
show_usage() {
    cat << EOF
Usage: $SCRIPT_NAME [OPTIONS] <content_directory>

Validate speaker/lecture directory structure and files.

OPTIONS:
    -v, --verbose          Show detailed validation output
    -f, --fix-suggestions  Show suggestions for fixing issues
    -s, --strict           Treat warnings as errors
    -h, --help             Show this help message

EXIT CODES:
    0 - All validations passed
    1 - Critical errors found
    2 - Warnings found (only if --strict is used)

EXAMPLES:
    # Basic validation
    $SCRIPT_NAME /path/to/content

    # Verbose with fix suggestions
    $SCRIPT_NAME -v -f /path/to/content

    # Strict mode
    $SCRIPT_NAME --strict /path/to/content

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

    # Check for ffprobe (from ffmpeg)
    if ! command_exists ffprobe; then
        missing_deps+=("ffprobe")
    fi

    # Check for file command
    if ! command_exists file; then
        missing_deps+=("file")
    fi

    if [[ ${#missing_deps[@]} -gt 0 ]]; then
        log_error "Missing required dependencies: ${missing_deps[*]}"
        echo ""
        echo "Installation instructions for macOS:"
        echo ""

        for dep in "${missing_deps[@]}"; do
            case "$dep" in
                ffprobe)
                    echo "  # Install ffmpeg (includes ffprobe)"
                    echo "  brew install ffmpeg"
                    echo ""
                    ;;
                file)
                    echo "  # file command is usually pre-installed on macOS"
                    echo "  # If missing, install via Xcode Command Line Tools:"
                    echo "  xcode-select --install"
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
# FILE VALIDATION FUNCTIONS
################################################################################

# Check if a file has a valid extension
has_valid_extension() {
    local file="$1"
    local formats_name="$2"

    local extension="${file##*.}"
    extension=$(echo "$extension" | tr '[:upper:]' '[:lower:]')

    # Use eval to access array by name (compatible with Bash 3.2)
    eval "local formats=(\"\${${formats_name}[@]}\")"

    for format in "${formats[@]}"; do
        if [[ "$extension" == "$format" ]]; then
            return 0
        fi
    done

    return 1
}

# Validate audio file using ffprobe
validate_audio_file() {
    local file="$1"
    local relative_path="$2"

    log_verbose "Validating audio file: $relative_path"

    # Check if file exists
    if [[ ! -f "$file" ]]; then
        log_error "Audio file not found: $relative_path"
        return 1
    fi

    # Check file extension
    if ! has_valid_extension "$file" SUPPORTED_AUDIO_FORMATS; then
        log_error "Invalid audio format: $relative_path (supported: ${SUPPORTED_AUDIO_FORMATS[*]})"
        return 1
    fi

    # Validate using ffprobe
    if ! ffprobe -v error -show_format -show_streams "$file" &>/dev/null; then
        log_error "Corrupted or invalid audio file: $relative_path"
        suggest_fix "Try re-encoding: ffmpeg -i \"$file\" -c:a libmp3lame -b:a 128k \"${file%.mp3}_fixed.mp3\""
        return 1
    fi

    # Extract metadata
    local duration
    duration=$(ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 "$file" 2>/dev/null)

    if [[ -z "$duration" ]] || [[ "$duration" == "N/A" ]]; then
        log_warning "Could not extract duration from: $relative_path"
    else
        log_verbose "  Duration: ${duration%.*} seconds"
    fi

    return 0
}

# Validate image file
validate_image_file() {
    local file="$1"
    local relative_path="$2"

    log_verbose "Validating image file: $relative_path"

    # Check if file exists
    if [[ ! -f "$file" ]]; then
        return 1
    fi

    # Check file extension
    if ! has_valid_extension "$file" SUPPORTED_IMAGE_FORMATS; then
        log_warning "Invalid image format: $relative_path (supported: ${SUPPORTED_IMAGE_FORMATS[*]})"
        return 1
    fi

    # Validate using file command
    local file_type
    file_type=$(file -b --mime-type "$file")

    if [[ ! "$file_type" =~ ^image/ ]]; then
        log_error "File is not a valid image: $relative_path (detected: $file_type)"
        return 1
    fi

    return 0
}

# Validate lecture filename format (must be "NN - Title.mp3")
validate_lecture_filename() {
    local filename="$1"
    local relative_path="$2"

    # Expected pattern: 01 - Title.mp3 (number must be 01-999)
    if [[ ! "$filename" =~ ^[0-9]{2,3}\ -\ .+\.(mp3|m4a|wav|flac|aac|ogg)$ ]]; then
        log_warning "Invalid lecture filename format: $relative_path"
        log_warning "  Expected: 'NN - Title.ext' (e.g., '01 - Introduction.mp3' or '114 - Surah Name.mp3')"

        # Try to suggest a fix
        if [[ "$filename" =~ ^[0-9]+[^0-9] ]]; then
            # Has a number but wrong format
            suggest_fix "Rename to proper format: '01 - ${filename#*[0-9]}'"
        else
            suggest_fix "Add lecture number prefix: '01 - $filename'"
        fi

        return 1
    fi

    return 0
}

################################################################################
# DIRECTORY VALIDATION FUNCTIONS
################################################################################

# Validate speaker directory structure
validate_speaker_directory() {
    local speaker_dir="$1"
    local speaker_name=$(basename "$speaker_dir")

    log_info "Validating speaker: $speaker_name"
    ((SPEAKER_COUNT++))

    local has_error=false

    # Check for speaker images
    local speaker_image=""
    local speaker_small_image=""

    for ext in "${SUPPORTED_IMAGE_FORMATS[@]}"; do
        if [[ -f "$speaker_dir/speaker.$ext" ]]; then
            speaker_image="$speaker_dir/speaker.$ext"
        fi
        if [[ -f "$speaker_dir/speaker_small.$ext" ]]; then
            speaker_small_image="$speaker_dir/speaker_small.$ext"
        fi
    done

    # Validate speaker image (required)
    if [[ -z "$speaker_image" ]]; then
        log_error "Missing speaker image: $speaker_name/speaker.{jpg,png}"
        suggest_fix "Add speaker image or run fix_content.sh to generate placeholder"
        has_error=true
    else
        validate_image_file "$speaker_image" "$speaker_name/$(basename "$speaker_image")"
    fi

    # Validate speaker_small image (required)
    if [[ -z "$speaker_small_image" ]]; then
        log_error "Missing speaker thumbnail: $speaker_name/speaker_small.{jpg,png}"
        suggest_fix "Add thumbnail or run fix_content.sh to generate from main image"
        has_error=true
    else
        validate_image_file "$speaker_small_image" "$speaker_name/$(basename "$speaker_small_image")"
    fi

    # Find and validate collections
    local collection_count=0
    while IFS= read -r -d '' collection_dir; do
        validate_collection_directory "$collection_dir" "$speaker_name"
        ((collection_count++))
    done < <(find "$speaker_dir" -mindepth 1 -maxdepth 1 -type d -print0)

    if [[ $collection_count -eq 0 ]]; then
        log_warning "Speaker has no collections: $speaker_name"
        suggest_fix "Add at least one collection directory"
    fi

    if [[ "$has_error" == "true" ]]; then
        return 1
    fi

    return 0
}

# Validate collection directory structure
validate_collection_directory() {
    local collection_dir="$1"
    local speaker_name="$2"
    local collection_name=$(basename "$collection_dir")

    log_info "  Validating collection: $speaker_name/$collection_name"
    ((COLLECTION_COUNT++))

    local has_error=false

    # Check for collection images
    local collection_image=""
    local collection_small_image=""

    for ext in "${SUPPORTED_IMAGE_FORMATS[@]}"; do
        if [[ -f "$collection_dir/collection.$ext" ]]; then
            collection_image="$collection_dir/collection.$ext"
        fi
        if [[ -f "$collection_dir/collection_small.$ext" ]]; then
            collection_small_image="$collection_dir/collection_small.$ext"
        fi
    done

    # Validate collection image (required)
    if [[ -z "$collection_image" ]]; then
        log_error "Missing collection cover: $speaker_name/$collection_name/collection.{jpg,png}"
        suggest_fix "Add cover image or run fix_content.sh to generate placeholder"
        has_error=true
    else
        validate_image_file "$collection_image" "$speaker_name/$collection_name/$(basename "$collection_image")"
    fi

    # Validate collection_small image (required)
    if [[ -z "$collection_small_image" ]]; then
        log_error "Missing collection thumbnail: $speaker_name/$collection_name/collection_small.{jpg,png}"
        suggest_fix "Add thumbnail or run fix_content.sh to generate from cover"
        has_error=true
    else
        validate_image_file "$collection_small_image" "$speaker_name/$collection_name/$(basename "$collection_small_image")"
    fi

    # Find and validate lecture files
    local lecture_count=0
    while IFS= read -r -d '' lecture_file; do
        local filename=$(basename "$lecture_file")

        # Validate filename format
        validate_lecture_filename "$filename" "$speaker_name/$collection_name/$filename"

        # Validate audio file
        validate_audio_file "$lecture_file" "$speaker_name/$collection_name/$filename"

        ((lecture_count++))
        ((LECTURE_COUNT++))
    done < <(find "$collection_dir" -maxdepth 1 -type f \( -iname "*.mp3" -o -iname "*.m4a" -o -iname "*.wav" -o -iname "*.flac" -o -iname "*.aac" -o -iname "*.ogg" \) -print0)

    if [[ $lecture_count -eq 0 ]]; then
        log_error "Collection has no lecture files: $speaker_name/$collection_name"
        suggest_fix "Add at least one audio file (mp3, m4a, wav, flac, aac, ogg)"
        has_error=true
    else
        log_verbose "    Found $lecture_count lectures"
    fi

    if [[ "$has_error" == "true" ]]; then
        return 1
    fi

    return 0
}

################################################################################
# MAIN VALIDATION FUNCTION
################################################################################

validate_content_directory() {
    local content_dir="$1"

    log_info "Starting validation of: $content_dir"
    echo ""

    # Check if directory exists
    if [[ ! -d "$content_dir" ]]; then
        log_error "Content directory not found: $content_dir"
        return 1
    fi

    # Find and validate all speaker directories
    while IFS= read -r -d '' speaker_dir; do
        validate_speaker_directory "$speaker_dir"
        echo ""
    done < <(find "$content_dir" -mindepth 1 -maxdepth 1 -type d -print0)

    if [[ $SPEAKER_COUNT -eq 0 ]]; then
        log_error "No speaker directories found in: $content_dir"
        return 1
    fi

    return 0
}

################################################################################
# MAIN EXECUTION
################################################################################

main() {
    # Parse command-line arguments
    while [[ $# -gt 0 ]]; do
        case "$1" in
            -v|--verbose)
                VERBOSE=true
                shift
                ;;
            -f|--fix-suggestions)
                FIX_SUGGESTIONS=true
                shift
                ;;
            -s|--strict)
                STRICT_MODE=true
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

    # Check if content directory was provided
    if [[ -z "$CONTENT_DIR" ]]; then
        log_error "Content directory not specified"
        show_usage
        exit 1
    fi

    # Print header
    echo ""
    log_info "=========================================="
    log_info "  Elmify Content Validator"
    log_info "=========================================="
    echo ""

    # Validate dependencies
    if ! validate_dependencies; then
        exit 1
    fi
    echo ""

    # Validate content directory
    validate_content_directory "$CONTENT_DIR"

    # Print summary
    echo ""
    log_info "=========================================="
    log_info "  Validation Summary"
    log_info "=========================================="
    echo ""
    log_info "Total Speakers:    $SPEAKER_COUNT"
    log_info "Total Collections: $COLLECTION_COUNT"
    log_info "Total Lectures:    $LECTURE_COUNT"
    echo ""

    if [[ $ERROR_COUNT -eq 0 && $WARNING_COUNT -eq 0 ]]; then
        log_success "✅ All validations passed! Content is ready for upload."
        exit 0
    elif [[ $ERROR_COUNT -eq 0 ]]; then
        log_warning "⚠️  Validation completed with $WARNING_COUNT warnings"

        if [[ "$STRICT_MODE" == "true" ]]; then
            log_error "Strict mode enabled: treating warnings as errors"
            exit 2
        else
            log_info "Content can be uploaded, but warnings should be reviewed"
            exit 0
        fi
    else
        log_error "❌ Validation failed with $ERROR_COUNT errors and $WARNING_COUNT warnings"
        log_error "Please fix the errors before uploading"

        if [[ "$FIX_SUGGESTIONS" == "true" ]]; then
            echo ""
            log_info "Run fix_content.sh to automatically fix some issues"
        fi

        exit 1
    fi
}

# Run main function with all script arguments
main "$@"
