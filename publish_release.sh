#!/bin/bash
set -e

TOKEN="${1:-$GITHUB_TOKEN}"
REPO="Vipulbaldha23/Nexa"
TAG="v1.0.0"
APK_PATH="NexaPlayer-v1.0.0.apk"

if [ -z "$TOKEN" ]; then
  echo "Error: GitHub Access Token is required."
  echo "Usage: ./publish_release.sh <YOUR_GITHUB_TOKEN_WITH_WRITE_ACCESS>"
  exit 1
fi

echo "Creating release $TAG on $REPO..."
RELEASE_RESPONSE=$(curl -s -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "Content-Type: application/json" \
  https://api.github.com/repos/$REPO/releases \
  -d '{
    "tag_name": "'"$TAG"'",
    "target_commitish": "main",
    "name": "Nexa Player v1.0.0",
    "body": "### Nexa Player v1.0.0 - Release\n\n- Futuristic Liquid Glass UI & AMOLED Dark Theme\n- Media3 ExoPlayer with gesture controls\n- Brightness/Volume swipe gestures\n- Multi-track Audio & Subtitles support\n- Picture-in-Picture (PiP)\n- MediaStore scanning & SAF file picker",
    "draft": false,
    "prerelease": false
  }')

RELEASE_ID=$(echo "$RELEASE_RESPONSE" | grep -o '"id": [0-9]*' | head -1 | awk '{print $2}')

if [ -z "$RELEASE_ID" ]; then
  echo "Failed to create release. Response from GitHub:"
  echo "$RELEASE_RESPONSE"
  exit 1
fi

echo "Release created successfully (ID: $RELEASE_ID)!"
echo "Uploading APK: $APK_PATH..."

UPLOAD_URL="https://uploads.github.com/repos/$REPO/releases/$RELEASE_ID/assets?name=NexaPlayer-v1.0.0.apk"

UPLOAD_RESPONSE=$(curl -s -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/vnd.android.package-archive" \
  --data-binary "@$APK_PATH" \
  "$UPLOAD_URL")

echo "Upload response:"
echo "$UPLOAD_RESPONSE"
echo ""
echo "Done! Release is live at: https://github.com/$REPO/releases/tag/$TAG"
