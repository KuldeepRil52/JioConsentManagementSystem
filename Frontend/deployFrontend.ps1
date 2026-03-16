$ErrorActionPreference = "Stop"

$BUILD_ENV="dev"
$DEPLOY_PATH = ""

# List of React services
$SERVICES = @(
    "partner-portal-frontend",
    "user-portal-frontend",
    "consent-popup-frontend"
)

# Arrays to store results
$BuildSuccess = @()
$BuildFailed  = @()

foreach ($SERVICE in $SERVICES) {
    Write-Host "--------------------------------------------------"
    Write-Host "Building React service: ${SERVICE}..."

    $servicePath = ".\$SERVICE"

    if (-not (Test-Path $servicePath)) {
        Write-Warning "Service folder not found for ${SERVICE}, skipping..."
        $BuildFailed += $SERVICE
        continue
    }

    try {
        # ----- Clean dependencies -----
        Write-Host "Removing node_modules and package-lock.json..."
        Remove-Item "$servicePath\node_modules" -Recurse -Force -ErrorAction SilentlyContinue
        Remove-Item "$servicePath\package-lock.json" -Force -ErrorAction SilentlyContinue

        Write-Host "Cleaning npm cache and fixing config..."
        npm cache clean --force
        npm config fix

        # ----- Install dependencies -----
        Write-Host "Installing dependencies..."
        Push-Location $servicePath
        npm install
        Pop-Location

        # ----- Build React app -----
        Write-Host "Building React app for environment '$BUILD_ENV'..."
        Push-Location $servicePath
        npm run "build:$BUILD_ENV"
        Pop-Location

        # ----- Optional deployment -----
        if ($DEPLOY_PATH -ne "") {
            $sourceBuild = "$servicePath\build"
            $targetDeploy = Join-Path $DEPLOY_PATH $SERVICE
            Write-Host "Copying build to deployment path: $targetDeploy"
            Remove-Item $targetDeploy -Recurse -Force -ErrorAction SilentlyContinue
            Copy-Item $sourceBuild -Destination $targetDeploy -Recurse
        }

        $BuildSuccess += $SERVICE
    } catch {
        Write-Warning "Build failed for ${SERVICE}: $_"
        $BuildFailed += $SERVICE
    }
}

Write-Host "--------------------------------------------------"
Write-Host "Build summary:"

if ($BuildSuccess.Count -gt 0) {
    Write-Host "✅ Successfully built:"
    $BuildSuccess | ForEach-Object { Write-Host " - $_" }
}

if ($BuildFailed.Count -gt 0) {
    Write-Host "❌ Failed to build:"
    $BuildFailed | ForEach-Object { Write-Host " - $_" }
}

