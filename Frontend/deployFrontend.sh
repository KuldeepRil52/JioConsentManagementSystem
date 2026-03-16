#!/bin/bash
set -e  # Stop script on any error

# Configuration
BUILD_ENV="dev"                   # dev or prod
DEPLOY_PATH=""                    # Optional deployment path, leave empty if not needed

# List of React services
SERVICES=(
    "partner-portal-frontend"
    "user-portal-frontend"
    "consent-popup"
)

# Arrays to store results
BuildSuccess=()
BuildFailed=()

for SERVICE in "${SERVICES[@]}"; do
    echo "--------------------------------------------------"
    echo "Building React service: $SERVICE"

    SERVICE_PATH="./$SERVICE"

    if [ ! -d "$SERVICE_PATH" ]; then
        echo "⚠ Service folder not found for $SERVICE, skipping..."
        BuildFailed+=("$SERVICE")
        continue
    fi

    # ----- Clean dependencies -----
    echo "Removing node_modules and package-lock.json..."
    rm -rf "$SERVICE_PATH/node_modules" "$SERVICE_PATH/package-lock.json"

    # ----- Clean npm cache and fix config -----
    echo "Cleaning npm cache..."
    npm cache clean --force

    # ----- Install dependencies -----
    echo "Installing dependencies..."
    (cd "$SERVICE_PATH" && npm install)

    # ----- Build React app -----
    echo "Building React app for environment '$BUILD_ENV'..."
    (cd "$SERVICE_PATH" && npm run "build:$BUILD_ENV")

    # ----- Optional deployment -----
    if [ -n "$DEPLOY_PATH" ]; then
        SOURCE_BUILD="$SERVICE_PATH/build"
        TARGET_DEPLOY="$DEPLOY_PATH/$SERVICE"
        echo "Copying build to deployment path: $TARGET_DEPLOY"
        rm -rf "$TARGET_DEPLOY"
        mkdir -p "$TARGET_DEPLOY"
        cp -r "$SOURCE_BUILD/"* "$TARGET_DEPLOY/"
    fi

    # Mark success
    BuildSuccess+=("$SERVICE")
done

echo "--------------------------------------------------"
echo "Build summary:"

if [ ${#BuildSuccess[@]} -gt 0 ]; then
    echo "✅ Successfully built:"
    for s in "${BuildSuccess[@]}"; do
        echo " - $s"
    done
fi

if [ ${#BuildFailed[@]} -gt 0 ]; then
    echo "❌ Failed to build:"
    for f in "${BuildFailed[@]}"; do
        echo " - $f"
    done
fi

