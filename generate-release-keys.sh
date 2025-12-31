#!/bin/bash

# GitNote Release Key Generation Script
# This script generates a signing keystore for Android release builds

set -e

echo "GitNote Release Key Generation"
echo "=============================="

# Configuration
KEYSTORE_FILE="app/key.jks"
KEY_ALIAS="gitnote_release_key"
KEY_PASSWORD="GitNoteStore2025!"  # Must be same as STORE_PASSWORD for PKCS12
STORE_PASSWORD="GitNoteStore2025!"

# Check if keystore already exists
if [ -f "$KEYSTORE_FILE" ]; then
    echo "Warning: $KEYSTORE_FILE already exists!"
    read -p "Do you want to overwrite it? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Aborting."
        exit 1
    fi
    rm -f "$KEYSTORE_FILE"
fi

echo "Generating keystore..."
echo "Keystore file: $KEYSTORE_FILE"
echo "Key alias: $KEY_ALIAS"

# Generate keystore
keytool -genkeypair \
    -v \
    -keystore "$KEYSTORE_FILE" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass "$STORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -dname "CN=GitNote, OU=Development, O=GitNote, L=Unknown, ST=Unknown, C=US"

echo ""
echo "Keystore generated successfully!"
echo ""
echo "Environment variables for release builds:"
echo "=========================================="
echo "export KEY_ALIAS=\"$KEY_ALIAS\""
echo "export KEY_PASSWORD=\"$KEY_PASSWORD\""
echo "export STORE_PASSWORD=\"$STORE_PASSWORD\""
echo ""
echo "You can also create a .env file with these variables:"
echo "KEY_ALIAS=$KEY_ALIAS"
echo "KEY_PASSWORD=$KEY_PASSWORD"
echo "STORE_PASSWORD=$STORE_PASSWORD"
echo ""
echo "To build the release APK, run:"
echo "./gradlew :app:assembleRelease"
echo ""
echo "To build and install the release APK:"
echo "./gradlew :app:assembleRelease :app:installRelease"
echo ""
echo "Security Note: Keep your keystore file secure and never commit it to version control!"