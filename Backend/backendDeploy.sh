#!/bin/bash
set -e

# Leave empty if running locally
REGISTRY=""
TAG="latest"

# List of services
SERVICES=("audit-module-apis" "auth-apis" "consent-core-apis" "cookie-consent-apis" "grivance-module-apis" "notification-consumer-apis" "notification-module-apis" "patner-portal-apis" "schedular-apis" "translator-apis" "vault-apis" "wso2-cred-generator-apis" "system-registry-apis")

for SERVICE in "${SERVICES[@]}"; do
    echo "--------------------------------------------------"
    echo "Building Docker image for $SERVICE..."
    sudo docker build -t "$SERVICE:$TAG" -f "./$SERVICE/deployment/PublicDockerfile" "./$SERVICE"

    if [ -n "$REGISTRY" ]; then
        echo "Tagging image for registry..."
        sudo docker tag "$SERVICE:$TAG" "$REGISTRY/$SERVICE:$TAG"

        echo "Pushing image to registry $REGISTRY..."
        sudo docker push "$REGISTRY/$SERVICE:$TAG"
    fi

    echo "Deploying $SERVICE to Kubernetes..."
    kubectl apply -f "./$SERVICE/deployment/deployment.yaml"
done

echo "--------------------------------------------------"
echo "All services built and deployed locally successfully!"
kubectl get pods
