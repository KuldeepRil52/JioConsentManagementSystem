#!/bin/bash
# =============================================================================
# JCMS Backend Deploy Script
# Builds Docker images and deploys all (or selected) backend services to K8s.
#
# Usage:
#   ./backendDeploy.sh [OPTIONS] [SERVICE...]
#
# Options:
#   -r, --registry  REGISTRY   Docker registry prefix (e.g. registry.example.com)
#   -t, --tag       TAG        Docker image tag            (default: latest)
#   -n, --namespace NAMESPACE  Kubernetes namespace        (default: default)
#   -b, --build-only           Build images but skip kubectl apply
#   -d, --deploy-only          Skip docker build, only run kubectl apply
#   -D, --dry-run              Print commands without executing
#   -h, --help                 Show this help message
#
# Examples:
#   ./backendDeploy.sh                           # build & deploy all services
#   ./backendDeploy.sh auth-apis vault-apis      # build & deploy specific services
#   ./backendDeploy.sh -r registry.example.com   # push to registry then deploy
#   ./backendDeploy.sh -n dpdp-cms               # deploy to a specific namespace
#   ./backendDeploy.sh --deploy-only             # re-deploy without rebuilding
#   ./backendDeploy.sh --dry-run                 # preview commands only
# =============================================================================

set -euo pipefail

# ── Defaults ─────────────────────────────────────────────────────────────────
REGISTRY=""
TAG="latest"
NAMESPACE="default"
BUILD=true
DEPLOY=true
DRY_RUN=false

ALL_SERVICES=(
  "audit-module-apis"
  "auth-apis"
  "consent-core-apis"
  "cookie-consent-apis"
  "grivance-module-apis"
  "notification-consumer-apis"
  "notification-module-apis"
  "partner-portal-apis"
  "schedular-apis"
  "system-registry-apis"
  "translator-apis"
  "vault-apis"
  "wso2-cred-generator-apis"
)

SELECTED_SERVICES=()
FAILED_SERVICES=()

# ── Helpers ───────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; RESET='\033[0m'
log_info()    { echo -e "${CYAN}[INFO]${RESET}  $*"; }
log_ok()      { echo -e "${GREEN}[OK]${RESET}    $*"; }
log_warn()    { echo -e "${YELLOW}[WARN]${RESET}  $*"; }
log_error()   { echo -e "${RED}[ERROR]${RESET} $*"; }
log_section() { echo -e "\n${CYAN}══════════════════════════════════════════${RESET}"; echo -e "${CYAN}  $*${RESET}"; echo -e "${CYAN}══════════════════════════════════════════${RESET}"; }

run() {
  if $DRY_RUN; then
    echo -e "${YELLOW}[DRY-RUN]${RESET} $*"
  else
    "$@"
  fi
}

usage() {
  sed -n '/^# Usage:/,/^# ====/p' "$0" | sed 's/^# \?//'
  exit 0
}

# ── Argument parsing ──────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case "$1" in
    -r|--registry)    REGISTRY="$2";  shift 2 ;;
    -t|--tag)         TAG="$2";       shift 2 ;;
    -n|--namespace)   NAMESPACE="$2"; shift 2 ;;
    -b|--build-only)  DEPLOY=false;   shift   ;;
    -d|--deploy-only) BUILD=false;    shift   ;;
    -D|--dry-run)     DRY_RUN=true;   shift   ;;
    -h|--help)        usage ;;
    -*)  log_error "Unknown option: $1"; usage ;;
    *)   SELECTED_SERVICES+=("$1"); shift ;;
  esac
done

# Use all services if none specified
SERVICES=( "${SELECTED_SERVICES[@]:-${ALL_SERVICES[@]}}" )
if [[ ${#SELECTED_SERVICES[@]} -eq 0 ]]; then
  SERVICES=("${ALL_SERVICES[@]}")
fi

# ── Validate selected service names ───────────────────────────────────────────
for svc in "${SERVICES[@]}"; do
  valid=false
  for known in "${ALL_SERVICES[@]}"; do
    [[ "$svc" == "$known" ]] && valid=true && break
  done
  if ! $valid; then
    log_error "Unknown service: '$svc'"
    log_info  "Valid services: ${ALL_SERVICES[*]}"
    exit 1
  fi
done

# ── Preflight checks ──────────────────────────────────────────────────────────
log_section "Preflight checks"

if ! $DRY_RUN; then
  if $BUILD && ! command -v docker &>/dev/null; then
    log_error "docker not found in PATH"; exit 1
  fi
  if $DEPLOY && ! command -v kubectl &>/dev/null; then
    log_error "kubectl not found in PATH"; exit 1
  fi
fi

log_info "Registry  : ${REGISTRY:-'(local only)'}"
log_info "Tag       : $TAG"
log_info "Namespace : $NAMESPACE"
log_info "Build     : $BUILD"
log_info "Deploy    : $DEPLOY"
log_info "Dry-run   : $DRY_RUN"
log_info "Services  : ${SERVICES[*]}"

# ── Apply ConfigMap first ─────────────────────────────────────────────────────
if $DEPLOY; then
  log_section "Applying ConfigMap"
  if [[ ! -f "./configmap.yaml" ]]; then
    log_warn "configmap.yaml not found — skipping. Deployments may fail if they reference it."
  else
    run kubectl apply -f "./configmap.yaml" --namespace "$NAMESPACE"
    log_ok "ConfigMap applied."
  fi
fi

# ── Build & Deploy loop ───────────────────────────────────────────────────────
log_section "Processing ${#SERVICES[@]} service(s)"

for SERVICE in "${SERVICES[@]}"; do
  echo ""
  log_info "── $SERVICE ──────────────────────────────"

  DOCKERFILE="./$SERVICE/deployment/PublicDockerfile"
  DEPLOY_YAML="./$SERVICE/deployment/deployment.yaml"

  # ── Build ──────────────────────────────────────────────────────────────────
  # FINAL_IMAGE is what gets written into the deployment.yaml before apply.
  # Local-only:  <service>:<tag>
  # With registry: <registry>/<service>:<tag>
  LOCAL_IMAGE="$SERVICE:$TAG"
  FINAL_IMAGE="${REGISTRY:+$REGISTRY/}${SERVICE}:${TAG}"

  if $BUILD; then
    if [[ ! -f "$DOCKERFILE" ]]; then
      log_error "Dockerfile not found: $DOCKERFILE — skipping $SERVICE"
      FAILED_SERVICES+=("$SERVICE (missing Dockerfile)")
      continue
    fi

    log_info "Building image: $LOCAL_IMAGE"
    if run docker build -t "$LOCAL_IMAGE" -f "$DOCKERFILE" "./$SERVICE"; then
      log_ok "Image built: $LOCAL_IMAGE"
    else
      log_error "Build failed for $SERVICE"
      FAILED_SERVICES+=("$SERVICE (build failed)")
      continue
    fi

    if [[ -n "$REGISTRY" ]]; then
      log_info "Tagging:  $LOCAL_IMAGE  →  $FINAL_IMAGE"
      run docker tag "$LOCAL_IMAGE" "$FINAL_IMAGE"
      log_info "Pushing: $FINAL_IMAGE"
      if run docker push "$FINAL_IMAGE"; then
        log_ok "Pushed: $FINAL_IMAGE"
      else
        log_error "Push failed for $SERVICE"
        FAILED_SERVICES+=("$SERVICE (push failed)")
        continue
      fi
    fi
  fi

  # ── Deploy ─────────────────────────────────────────────────────────────────
  if $DEPLOY; then
    if [[ ! -f "$DEPLOY_YAML" ]]; then
      log_error "deployment.yaml not found: $DEPLOY_YAML — skipping deploy for $SERVICE"
      FAILED_SERVICES+=("$SERVICE (missing deployment.yaml)")
      continue
    fi

    # Patch the image line in-memory before applying so the cluster always
    # pulls the image that was just built/pushed (correct registry + tag).
    # The yaml on disk is never modified.
    log_info "Applying: $DEPLOY_YAML  (image → $FINAL_IMAGE)"
    PATCHED_YAML=$(sed "s|image: ${SERVICE}:.*|image: ${FINAL_IMAGE}|g" "$DEPLOY_YAML")

    if $DRY_RUN; then
      echo -e "${YELLOW}[DRY-RUN]${RESET} kubectl apply (patched yaml) --namespace $NAMESPACE"
      echo "$PATCHED_YAML" | grep "image:"
    else
      if echo "$PATCHED_YAML" | kubectl apply -f - --namespace "$NAMESPACE"; then
        log_ok "Deployed: $SERVICE"
      else
        log_error "kubectl apply failed for $SERVICE"
        FAILED_SERVICES+=("$SERVICE (deploy failed)")
        continue
      fi
    fi

    # Wait for rollout (skip in dry-run, non-blocking with timeout)
    if ! $DRY_RUN; then
      log_info "Waiting for rollout: $SERVICE (timeout 120s)..."
      if kubectl rollout status deployment/"$SERVICE" --namespace "$NAMESPACE" --timeout=120s 2>/dev/null; then
        log_ok "Rollout complete: $SERVICE"
      else
        log_warn "Rollout did not complete within 120s for $SERVICE — check pod status manually."
      fi
    fi
  fi
done

# ── Summary ───────────────────────────────────────────────────────────────────
log_section "Summary"

TOTAL=${#SERVICES[@]}
FAILED=${#FAILED_SERVICES[@]}
PASSED=$(( TOTAL - FAILED ))

if [[ $FAILED -eq 0 ]]; then
  log_ok "All $TOTAL service(s) processed successfully."
else
  log_warn "$PASSED/$TOTAL succeeded.  $FAILED failed:"
  for f in "${FAILED_SERVICES[@]}"; do
    log_error "  ✗ $f"
  done
fi

if $DEPLOY && ! $DRY_RUN; then
  echo ""
  log_info "Current pod status (namespace: $NAMESPACE):"
  kubectl get pods --namespace "$NAMESPACE"
fi

[[ $FAILED -eq 0 ]] && exit 0 || exit 1
