# TaurusX Artifact Migration

This document describes the process of synchronizing TaurusX SDK artifacts from Bitbucket to our Artifactory repository.

## Overview

Due to accessibility issues with Bitbucket (https://bitbucket.org/sdkcenter/sdkcenter/raw/release), we've created an automated script to synchronize **all versions** of TaurusX artifacts to our Artifactory repository at `https://artifactory.bidon.org/artifactory/remote-taurusx`.

The script synchronizes two dependencies:
- `com.taurusx.tax:ads` - Main TaurusX SDK
- `com.taurusx.omsdk:core` - Open Measurement SDK dependency

## Prerequisites

Before running the migration script, ensure you have:

1. **Environment Variables Set:**
   ```bash
   export BDN_USERNAME="your_username"
   export BDN_USERPASSWORD="your_password"
   ```

2. **Required Tools:**
   - `curl` - for downloading and uploading artifacts
   - `bash` - version 4.0 or higher

## Usage

The script automatically synchronizes **all available versions** of TaurusX artifacts from Bitbucket. It reads version information from `maven-metadata.xml` files on Bitbucket.

### Running the Synchronization

From the `adapter/taurusx/` directory:

```bash
cd adapter/taurusx
./migrate-taurusx.sh
```

Or from the project root:

```bash
./adapter/taurusx/migrate-taurusx.sh
```

### What Gets Synchronized

The script automatically discovers all available versions from `maven-metadata.xml` and synchronizes:

**For `com.taurusx.tax:ads`:**
- All versions: POM and AAR files

**For `com.taurusx.omsdk:core`:**
- All versions: POM and AAR files

**Note:**
- Javadoc and sources are not available in the Bitbucket repository
- Already uploaded artifacts are automatically skipped (no re-upload)
- Temporary files are cleaned up immediately after each upload to save disk space

### Example Output

```
[INFO] TaurusX Artifact Migration Script
[INFO] ==================================
[INFO] Current version in build.gradle.kts: 1.11.2
[INFO]
[INFO] Migrating com.taurusx.tax:ads
[INFO] --------------------------------------
[INFO] Fetching versions from maven-metadata.xml for com.taurusx.tax:ads...
[INFO] Processing version: 1.9.0
[INFO] ✓ Uploaded: ads-1.9.0.pom
[INFO] ✓ Uploaded: ads-1.9.0.aar
[INFO] Processing version: 1.10.0
[INFO]   Skipped: ads-1.10.0.pom (already exists)
[INFO]   Skipped: ads-1.10.0.aar (already exists)
[INFO] Processing version: 1.11.2
[INFO] ✓ Uploaded: ads-1.11.2.pom
[INFO] ✓ Uploaded: ads-1.11.2.aar
[INFO] Completed com.taurusx.tax:ads: 6/8 files uploaded
[INFO]
[INFO] Migrating com.taurusx.omsdk:core
[INFO] --------------------------------------
[INFO] Fetching versions from maven-metadata.xml for com.taurusx.omsdk:core...
[INFO] Processing version: 1.4.12
[INFO] ✓ Uploaded: core-1.4.12.pom
[INFO] ✓ Uploaded: core-1.4.12.aar
[INFO] Completed com.taurusx.omsdk:core: 2/2 files uploaded
[INFO]
[INFO] ==================================
[INFO] Migration completed successfully!
```

## Adding New TaurusX Versions

When a new TaurusX SDK version is released:

1. Simply run the synchronization script:
   ```bash
   cd adapter/taurusx
   ./migrate-taurusx.sh
   ```

2. The script will automatically:
   - Fetch the latest version list from `maven-metadata.xml`
   - Detect new versions not yet in Artifactory
   - Download and upload only the new artifacts
   - Skip already synchronized versions

3. Update your `build.gradle.kts` to use the new version:
   ```kotlin
   val adapterSdkVersion = "1.12.0" // New version
   ```

## Repository Configuration

The TaurusX artifacts are now served from our Artifactory. The repository is configured in `settings.gradle.kts`:

```kotlin
maven(url = "https://artifactory.bidon.org/artifactory/remote-taurusx")
```

The old Bitbucket repository reference:
```kotlin
// maven(url = "https://bitbucket.org/sdkcenter/sdkcenter/raw/release")
```

## Troubleshooting

### Authentication Failed
Ensure `BDN_USERNAME` and `BDN_USERPASSWORD` environment variables are correctly set:
```bash
echo $BDN_USERNAME
echo $BDN_USERPASSWORD
```

### Artifact Already Exists
If you see warnings about existing artifacts:
```
[WARNING] Already exists: ads-1.11.2.pom
```
This is normal - the script skips already uploaded artifacts. To force re-upload, delete the version folder in Artifactory UI first.

### Download Failed
If artifacts cannot be downloaded from Bitbucket:
- Check if the version exists on Bitbucket at `https://bitbucket.org/sdkcenter/sdkcenter/src/release/com/taurusx/tax/ads/<version>/`
- Verify network connectivity to Bitbucket

### Upload Failed
If upload to Artifactory fails:
- Verify credentials have write permissions to the repository
- Ensure Artifactory is accessible from your network

## Script Configuration

You can modify these configuration variables in `migrate-taurusx.sh`:

```bash
BITBUCKET_REPO="https://bitbucket.org/sdkcenter/sdkcenter/raw/release"
ARTIFACTORY_URL="https://artifactory.bidon.org/artifactory/remote-taurusx"

# Define artifacts to sync
declare -a ARTIFACTS=(
    "com.taurusx.tax:ads"
    "com.taurusx.omsdk:core"
)
```

To add more dependencies, simply add them to the `ARTIFACTS` array.

## CI/CD Integration

You can integrate this script into your CI/CD pipeline to automatically synchronize new TaurusX versions:

```yaml
# Example GitHub Actions workflow - runs on schedule or manual trigger
name: Sync TaurusX Artifacts

on:
  schedule:
    - cron: '0 0 * * 1'  # Weekly on Monday
  workflow_dispatch:  # Manual trigger

jobs:
  sync:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v3

      - name: Sync TaurusX Artifacts
        env:
          BDN_USERNAME: ${{ secrets.BDN_USERNAME }}
          BDN_USERPASSWORD: ${{ secrets.BDN_USERPASSWORD }}
        run: ./adapter/taurusx/migrate-taurusx.sh
```

The script is idempotent - it safely skips already synchronized artifacts, so it can be run repeatedly without issues.