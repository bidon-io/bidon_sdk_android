#!/bin/bash

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Configuration
TAURUSX_BUILD_FILE="${SCRIPT_DIR}/build.gradle.kts"
BITBUCKET_REPO="https://bitbucket.org/sdkcenter/sdkcenter/raw/release"
ARTIFACTORY_URL="https://artifactory.bidon.org/artifactory/remote-taurusx"
TEMP_DIR="$(mktemp -d)"

# Logging functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Cleanup
cleanup() {
    [ -d "$TEMP_DIR" ] && rm -rf "$TEMP_DIR"
}
trap cleanup EXIT

# Check credentials
if [ -z "$BDN_USERNAME" ] || [ -z "$BDN_USERPASSWORD" ]; then
    log_error "BDN_USERNAME and BDN_USERPASSWORD environment variables must be set"
    exit 1
fi

log_info "TaurusX Artifact Migration Script"
log_info "=================================="

# Get current version from build.gradle.kts
CURRENT_VERSION=$(grep 'val adapterSdkVersion = ' "$TAURUSX_BUILD_FILE" | sed -E 's/.*"(.+)".*/\1/' 2>/dev/null || echo "unknown")
log_info "Current version in build.gradle.kts: $CURRENT_VERSION"

# Define artifacts to sync
declare -a ARTIFACTS=(
    "com.taurusx.tax:ads"
    "com.taurusx.omsdk:core"
)

# Convert Maven coordinates to path
maven_path() {
    local group_id=$1
    local artifact_id=$2
    local version=$3
    local extension=$4
    local group_path=$(echo "$group_id" | tr '.' '/')
    echo "${group_path}/${artifact_id}/${version}/${artifact_id}-${version}.${extension}"
}

# Download and upload artifact
migrate_artifact() {
    local group_id=$1
    local artifact_id=$2
    local version=$3
    local extension=$4

    local path=$(maven_path "$group_id" "$artifact_id" "$version" "$extension")
    local file="${TEMP_DIR}/$(basename "$path")"
    local download_url="${BITBUCKET_REPO}/${path}"
    local upload_url="${ARTIFACTORY_URL}/${path}"

    # Add parameter to disable POM generation for AAR files
    [ "$extension" = "aar" ] && upload_url="${upload_url};generate.pom=false"

    if ! curl -f -L -s -o "$file" "$download_url" 2>/dev/null; then
        log_warning "Failed to download: $(basename "$path")"
        return 1
    fi

    local http_code=$(curl -s -w "%{http_code}" -u "${BDN_USERNAME}:${BDN_USERPASSWORD}" \
        -T "$file" "$upload_url" -o /dev/null)

    # Clean up downloaded file immediately after upload
    rm -f "$file"

    if [ "$http_code" = "201" ] || [ "$http_code" = "200" ]; then
        log_info "✓ Uploaded: $(basename "$path")"
        return 0
    elif [ "$http_code" = "409" ]; then
        log_info "  Skipped: $(basename "$path") (already exists)"
        return 0
    else
        log_error "✗ Failed: $(basename "$path") (HTTP $http_code)"
        return 1
    fi
}

# Fetch available versions from maven-metadata.xml
fetch_versions() {
    local group_id=$1
    local artifact_id=$2
    local group_path=$(echo "$group_id" | tr '.' '/')
    local metadata_url="${BITBUCKET_REPO}/${group_path}/${artifact_id}/maven-metadata.xml"

    >&2 echo -e "${GREEN}[INFO]${NC} Fetching versions from maven-metadata.xml for ${group_id}:${artifact_id}..."

    # Download and parse maven-metadata.xml
    local versions=$(curl -s -L "$metadata_url" 2>/dev/null | \
        grep -oE '<version>[^<]+</version>' | \
        sed -E 's/<\/?version>//g' | \
        sort -V | uniq)

    if [ -z "$versions" ]; then
        >&2 echo -e "${YELLOW}[WARNING]${NC} No versions found in maven-metadata.xml for ${group_id}:${artifact_id}"
        return 1
    fi

    echo "$versions"
}

# Migrate all versions of an artifact
migrate_all_versions() {
    local group_id=$1
    local artifact_id=$2

    log_info ""
    log_info "Migrating ${group_id}:${artifact_id}"
    log_info "--------------------------------------"

    local versions=$(fetch_versions "$group_id" "$artifact_id")
    if [ $? -ne 0 ]; then
        return 1
    fi

    local total=0
    local success=0

    for version in $versions; do
        log_info "Processing version: $version"

        # Migrate POM first, then AAR
        if migrate_artifact "$group_id" "$artifact_id" "$version" "pom"; then
            ((success++))
        fi
        ((total++))

        if migrate_artifact "$group_id" "$artifact_id" "$version" "aar"; then
            ((success++))
        fi
        ((total++))
    done

    log_info "Completed ${group_id}:${artifact_id}: $success/$total files uploaded"
}

# Main migration loop
for artifact in "${ARTIFACTS[@]}"; do
    IFS=':' read -r group_id artifact_id <<< "$artifact"
    migrate_all_versions "$group_id" "$artifact_id"
done

log_info ""
log_info "=================================="
log_info "Migration completed successfully!"
