#!/bin/bash

################################################################################
# create_speaker.sh - Quick speaker template creator
################################################################################
#
# PURPOSE:
#   Quickly create a new speaker directory structure with placeholder images
#   and JSON metadata files. Saves time when adding new speakers to your
#   content library.
#
# USAGE:
#   ./create_speaker.sh <speaker_name> <collection_name> [content_dir]
#
# ARGUMENTS:
#   speaker_name      Name of the speaker (e.g., "Jordan Peterson")
#   collection_name   Name of the first collection (e.g., "Maps of Meaning")
#   content_dir       Optional: Path to content directory
#                     (default: ~/Desktop/hobby_projects/batch/content)
#
# EXAMPLES:
#   # Create new speaker in default content directory
#   ./create_speaker.sh "Naval Ravikant" "Almanack"
#
#   # Create new speaker in specific directory
#   ./create_speaker.sh "Jordan Peterson" "Personality" ~/batch/content
#
# OUTPUT:
#   Creates directory structure:
#   content/
#   └── Speaker Name/
#       ├── speaker.jpg (placeholder with speaker name)
#       ├── speaker_small.jpg (300x300 thumbnail)
#       ├── speaker.json (metadata: name, image)
#       └── Collection Name/
#           ├── collection.jpg (placeholder with collection name)
#           ├── collection_small.jpg (300x300 thumbnail)
#           └── collection.json (metadata: title, cover)
#
# AUTHOR:  Elmify Team
# VERSION: 2.0.0
# DATE:    2025-01-12
#
################################################################################

set -euo pipefail

################################################################################
# CONFIGURATION
################################################################################

SPEAKER_NAME="${1:-}"
COLLECTION_NAME="${2:-}"
CONTENT_DIR="${3:-$HOME/Desktop/hobby_projects/batch/content}"

# Color codes
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m'

################################################################################
# HELPER FUNCTIONS
################################################################################

log_info() {
    echo -e "${BLUE}[INFO]${NC} $*" >&2
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $*" >&2
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $*" >&2
}

show_usage() {
    cat << EOF
Usage: $(basename "$0") <speaker_name> <collection_name> [content_dir]

Create a new speaker template with placeholder images and JSON metadata.

ARGUMENTS:
    speaker_name      Name of the speaker (e.g., "Jordan Peterson")
    collection_name   Name of the collection (e.g., "Maps of Meaning")
    content_dir       Optional: Content directory
                      (default: ~/Desktop/hobby_projects/batch/content)

EXAMPLES:
    # Create in default content directory
    $(basename "$0") "Naval Ravikant" "Almanack"

    # Create in specific directory
    $(basename "$0") "Jordan Peterson" "Personality" ~/batch/content

EOF
}

log_missing_argument() {
    local arg_name="$1"
    local arg_example="$2"

    log_error "Missing required argument: $arg_name"
    echo ""
    echo "Expected: $(basename "$0") <speaker_name> <collection_name> [content_dir]"
    echo ""
    echo "Example for $arg_name:"
    echo "  $arg_example"
    echo ""
}

################################################################################
# VALIDATION
################################################################################

# Check if ImageMagick is installed
if ! command -v convert >/dev/null 2>&1; then
    log_error "ImageMagick not installed"
    echo ""
    echo "Install with: brew install imagemagick"
    echo ""
    exit 1
fi

# Validate arguments with specific error messages
if [[ -z "$SPEAKER_NAME" ]] && [[ -z "$COLLECTION_NAME" ]]; then
    log_error "Missing required arguments: speaker_name and collection_name"
    echo ""
    echo "Expected: $(basename "$0") <speaker_name> <collection_name> [content_dir]"
    echo ""
    echo "Examples:"
    echo "  $(basename "$0") \"Jordan Peterson\" \"Maps of Meaning\""
    echo "  $(basename "$0") \"Naval Ravikant\" \"Almanack\" ~/batch/content"
    echo ""
    exit 1
elif [[ -z "$SPEAKER_NAME" ]]; then
    log_missing_argument "speaker_name" "$(basename "$0") \"Jordan Peterson\" \"Maps of Meaning\""
    exit 1
elif [[ -z "$COLLECTION_NAME" ]]; then
    log_missing_argument "collection_name" "$(basename "$0") \"$SPEAKER_NAME\" \"Maps of Meaning\""
    exit 1
fi

################################################################################
# MAIN LOGIC
################################################################################

main() {
    log_info "Creating speaker template..."
    echo ""

    # Create directories
    local speaker_dir="$CONTENT_DIR/$SPEAKER_NAME"
    local collection_dir="$speaker_dir/$COLLECTION_NAME"

    if [[ -d "$speaker_dir" ]]; then
        log_error "Speaker already exists: $SPEAKER_NAME"
        echo ""
        echo "To add a new collection to existing speaker:"
        echo "  mkdir \"$speaker_dir/New Collection Name\""
        echo ""
        exit 1
    fi

    log_info "Creating directory structure..."
    mkdir -p "$collection_dir"
    log_success "Created: $collection_dir"
    echo ""

    # Create speaker placeholder image
    log_info "Generating speaker images..."
    convert -size 600x600 \
        gradient:'#4A5568-#2D3748' \
        -gravity center \
        -pointsize 40 \
        -fill white \
        -annotate +0+0 "$SPEAKER_NAME" \
        "$speaker_dir/speaker.jpg" 2>/dev/null || {
        log_error "Failed to generate speaker.jpg"
        exit 1
    }
    log_success "Generated: speaker.jpg (600x600)"

    # Create speaker thumbnail
    convert "$speaker_dir/speaker.jpg" \
        -resize 300x300^ \
        -gravity center \
        -extent 300x300 \
        "$speaker_dir/speaker_small.jpg" 2>/dev/null || {
        log_error "Failed to generate speaker_small.jpg"
        exit 1
    }
    log_success "Generated: speaker_small.jpg (300x300)"
    echo ""

    # Create collection placeholder image
    log_info "Generating collection images..."
    convert -size 600x600 \
        gradient:'#2D3748-#1A202C' \
        -gravity center \
        -pointsize 36 \
        -fill white \
        -annotate +0-50 "$SPEAKER_NAME" \
        -pointsize 28 \
        -annotate +0+50 "$COLLECTION_NAME" \
        "$collection_dir/collection.jpg" 2>/dev/null || {
        log_error "Failed to generate collection.jpg"
        exit 1
    }
    log_success "Generated: collection.jpg (600x600)"

    # Create collection thumbnail
    convert "$collection_dir/collection.jpg" \
        -resize 300x300^ \
        -gravity center \
        -extent 300x300 \
        "$collection_dir/collection_small.jpg" 2>/dev/null || {
        log_error "Failed to generate collection_small.jpg"
        exit 1
    }
    log_success "Generated: collection_small.jpg (300x300)"
    echo ""

    # Create speaker.json with all fields from Speaker entity
    log_info "Generating speaker metadata..."
    cat > "$speaker_dir/speaker.json" <<EOF
{
  "name": "$SPEAKER_NAME",
  "bio": "TODO: Add speaker biography here. Describe who they are, their background, and what they're known for.",
  "image": "speaker.jpg",
  "imageSmall": "speaker_small.jpg",
  "isPremium": false
}
EOF
    log_success "Generated: speaker.json"

    # Create collection.json with all fields from Collection entity
    log_info "Generating collection metadata..."
    cat > "$collection_dir/collection.json" <<EOF
{
  "title": "$COLLECTION_NAME",
  "description": "TODO: Add collection description here. Explain what topics are covered and what listeners will learn.",
  "year": null,
  "cover": "collection.jpg",
  "coverSmall": "collection_small.jpg"
}
EOF
    log_success "Generated: collection.json"
    echo ""

    # Print summary
    log_success "✅ Created template for: $SPEAKER_NAME / $COLLECTION_NAME"
    echo ""
    echo "📁 Location: $speaker_dir"
    echo ""
    echo "📝 Structure created:"
    echo "   $SPEAKER_NAME/"
    echo "   ├── speaker.jpg (placeholder)"
    echo "   ├── speaker_small.jpg (placeholder)"
    echo "   ├── speaker.json (metadata)"
    echo "   └── $COLLECTION_NAME/"
    echo "       ├── collection.jpg (placeholder)"
    echo "       ├── collection_small.jpg (placeholder)"
    echo "       └── collection.json (metadata)"
    echo ""
    echo "🎯 Next steps:"
    echo "   1. Edit speaker.json and add the speaker biography"
    echo "   2. Edit collection.json and add the collection description"
    echo "   3. Replace placeholder images with real images (optional)"
    echo "   4. Add lecture files in format: NN - Title.mp3"
    echo "      Example: cp /path/to/lecture.mp3 \"$collection_dir/01 - Introduction.mp3\""
    echo "   5. Create matching .json files for each lecture with description"
    echo "      Example: echo '{\"description\": \"Lecture description here\"}' > \"$collection_dir/01 - Introduction.json\""
    echo "   6. Validate: ./scripts/validate_content.sh \"$speaker_dir\""
    echo ""
    echo "📚 Quick commands:"
    echo "   # Add a lecture"
    echo "   cp /path/to/file.mp3 \"$collection_dir/01 - Lecture Title.mp3\""
    echo ""
    echo "   # Add another collection"
    echo "   mkdir \"$speaker_dir/Another Collection\""
    echo "   ./scripts/fix_content.sh \"$speaker_dir/Another Collection\""
    echo ""
    echo "   # Validate speaker"
    echo "   ./scripts/validate_content.sh \"$speaker_dir\""
    echo ""
}

# Run main function
main

exit 0
