#!/bin/bash

# GitNote Release Environment Setup
# This script loads environment variables from release-keys.env for release builds

ENV_FILE="release-keys.env"

echo "Setting up GitNote release environment variables..."
echo "=================================================="

# Check if the environment file exists
if [ ! -f "$ENV_FILE" ]; then
    echo "Error: $ENV_FILE not found!"
    echo "Please run ./generate-release-keys.sh first to generate the release keys."
    exit 1
fi

# Load the environment variables
source "$ENV_FILE"

# Export the variables so they're available to subprocesses
export KEY_ALIAS
export KEY_PASSWORD  
export STORE_PASSWORD

echo "Environment variables loaded from $ENV_FILE:"
echo "KEY_ALIAS=$KEY_ALIAS"
echo "KEY_PASSWORD=[HIDDEN]"
echo "STORE_PASSWORD=[HIDDEN]"
echo ""
echo "You can now run release builds:"
echo "./gradlew :app:assembleRelease"
echo ""
echo "Or build and install:"
echo "./gradlew :app:assembleRelease :app:installRelease"