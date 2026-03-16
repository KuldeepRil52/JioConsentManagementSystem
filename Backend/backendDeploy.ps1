$ErrorActionPreference = "Stop"

$REGISTRY = ""   # Leave empty for local deployment
$TAG = "local"

# List of services
$SERVICES = @("audit-module-apis", "auth-apis", "consent-core-apis", "grivance-module-apis", "notification-consumer-apis", "notification-module-apis", "patner-portal-apis", "schedular-apis", "translator-apis", "vault-apis", "wso2-cred-generator-apis", "cookie-consent-apis")  # Add more services as needed


# Arrays to store results
$BuildSuccess = @()
$BuildFailed  = @()

foreach ($SERVICE in $SERVICES) {
    Write-Host "--------------------------------------------------"
    Write-Host "Building Docker image for ${SERVICE}..."

    $dockerfilePath = ".\$SERVICE\deployment\Dockerfile"

    # ----- Check if Dockerfile exists -----
    if (-not (Test-Path $dockerfilePath)) {
        Write-Warning "Dockerfile not found for ${SERVICE}, skipping..."
        $BuildFailed += $SERVICE
        continue
    }

    try {
        # ----- Check base image -----
        $dockerfileContent = Get-Content $dockerfilePath
        $baseImageLine = $dockerfileContent | Where-Object { $_ -match "^FROM\s+" } | Select-Object -First 1
        if ($baseImageLine -match "FROM\s+(\S+)") { $baseImage = $Matches[1] }

        # Try pulling the base image
        Write-Host "Checking if base image '${baseImage}' is available..."
        docker pull $baseImage | Out-Null
    } catch {
        Write-Warning "Cannot pull base image '${baseImage}' for ${SERVICE}. Skipping build."
        $BuildFailed += $SERVICE
        continue
    }

    try {
        # Build Docker image
        docker build --no-cache -t "${SERVICE}:${TAG}" ".\$SERVICE" -f $dockerfilePath

        # Tag and push if registry is set
        if ($REGISTRY -ne "") {
            docker tag "${SERVICE}:${TAG}" "${REGISTRY}/${SERVICE}:${TAG}"
            docker push "${REGISTRY}/${SERVICE}:${TAG}"
        }

        # Deploy to Kubernetes if YAML exists
        $yamlPath = ".\$SERVICE\deployment\jio-dl-deployment.yaml"
        if (Test-Path $yamlPath) {
            Write-Host "Deploying ${SERVICE} to Kubernetes..."
            kubectl apply -f $yamlPath
        } else {
            Write-Warning "Deployment YAML not found for ${SERVICE}, skipping Kubernetes deployment."
        }

        $BuildSuccess += $SERVICE
    } catch {
        Write-Warning "Build failed for ${SERVICE}: $_"
        $BuildFailed += $SERVICE
    }
}

Write-Host "--------------------------------------------------"
Write-Host "Deployment summary:"

if ($BuildSuccess.Count -gt 0) {
    Write-Host "✅ Successfully built & deployed:"
    $BuildSuccess | ForEach-Object { Write-Host " - $_" }
}

if ($BuildFailed.Count -gt 0) {
    Write-Host "❌ Failed to build/deploy:"
    $BuildFailed | ForEach-Object { Write-Host " - $_" }
}

Write-Host "--------------------------------------------------"
kubectl get pods -n dpdp-consent-st
