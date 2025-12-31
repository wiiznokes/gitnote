#!/bin/bash

# GitNote Release Environment Setup
# This script exports environment variables for release builds

# Configuration - Update these if you changed them during key generation
KEY_ALIAS="gitnote_release_key"
KEY_PASSWORD="GitNoteStore2025!"  # Must be same as STORE_PASSWORD for PKCS12
STORE_PASSWORD="GitNoteStore2025!"

echo "Setting up GitNote release environment variables..."
echo "=================================================="

export KEY_ALIAS="$KEY_ALIAS"
export KEY_PASSWORD="$KEY_PASSWORD"
export STORE_PASSWORD="$STORE_PASSWORD"

echo "Environment variables set:"
echo "KEY_ALIAS=$KEY_ALIAS"
echo "KEY_PASSWORD=$KEY_PASSWORD"
echo "STORE_PASSWORD=$STORE_PASSWORD"
echo ""
echo "You can now run release builds:"
echo "./gradlew :app:assembleRelease"
echo ""
echo "Or build and install:"
echo "./gradlew :app:assembleRelease :app:installRelease"