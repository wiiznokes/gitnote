#!/bin/bash

# GitNote Release Key Generation Script
# This script generates a signing keystore for Android release builds
# and stores the generated keys in release-keys.env

set -e

echo "GitNote Release Key Generation"
echo "=============================="

# Configuration
KEYSTORE_FILE="app/key.jks"
KEY_ALIAS="gitnote_release_key"
ENV_FILE="release-keys.env"

# Generate random passwords
STORE_PASSWORD=$(openssl rand -base64 32)
KEY_PASSWORD="$STORE_PASSWORD"

echo "Generated secure random passwords for keystore..."

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

# Store the keys in the environment file
cat > "$ENV_FILE" << EOF
# GitNote Release Keys - Generated on $(date)
# This file contains sensitive information and should never be committed to version control
KEY_ALIAS=$KEY_ALIAS
KEY_PASSWORD=$KEY_PASSWORD
STORE_PASSWORD=$STORE_PASSWORD
EOF

echo ""
echo "Keystore generated successfully!"
echo "Keys stored in: $ENV_FILE"
echo ""
echo "Environment variables for release builds:"
echo "=========================================="
echo "KEY_ALIAS=$KEY_ALIAS"
echo "KEY_PASSWORD=$KEY_PASSWORD"
echo "STORE_PASSWORD=$STORE_PASSWORD"
echo ""
echo "To load these variables automatically, run:"
echo "source $ENV_FILE"
echo ""
echo "To build the release APK, run:"
echo "./gradlew :app:assembleRelease"
echo ""
echo "To build and install the release APK:"
echo "./gradlew :app:assembleRelease :app:installRelease"
echo ""
echo "Security Note: Keep your keystore file and $ENV_FILE secure and never commit them to version control!"