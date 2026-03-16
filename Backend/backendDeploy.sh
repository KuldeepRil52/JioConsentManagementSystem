#!/bin/bash
set -e

# Leave empty if running locally
REGISTRY=""
TAG="latest"

# List of services
SERVICES=("audit-module-apis" "auth-apis" "consent-core-apis" "cookie-consent-apis" "grivance-module-apis" "notification-consumer-apis" "notification-module-apis" "patner-portal-apis" "schedular-apis" "translator-apis" "vault-apis" "wso2-cred-generator-apis")

for SERVICE in "${SERVICES[@]}"; do
    echo "--------------------------------------------------"
    echo "Building Docker image for $SERVICE..."
    docker build -t "$SERVICE:$TAG" "./$SERVICE/deployment"

    if [ -n "$REGISTRY" ]; then
        echo "Tagging image for registry..."
        docker tag "$SERVICE:$TAG" "$REGISTRY/$SERVICE:$TAG"

        echo "Pushing image to registry $REGISTRY..."
        docker push "$REGISTRY/$SERVICE:$TAG"
    fi

    echo "Deploying $SERVICE to Kubernetes..."
    kubectl apply -f "./$SERVICE/deployment/jio-dl-deployment.yaml"
done

echo "--------------------------------------------------"
echo "All services built and deployed locally successfully!"
kubectl get pods
