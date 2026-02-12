#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
K8S_DIR="${ROOT_DIR}/k8s"

REGISTRY="${REGISTRY:-}"
TAG="${TAG:-latest}"
ROCKETMQ_HELM_RELEASE="${ROCKETMQ_HELM_RELEASE:-sporty-rocketmq}"
ROCKETMQ_HELM_CHART_DIR="${ROCKETMQ_HELM_CHART_DIR:-${K8S_DIR}/rocketmq-helm/rocketmq}"
ROCKETMQ_HELM_VALUES="${ROCKETMQ_HELM_VALUES:-${K8S_DIR}/rocketmq-helm/values.yaml}"

if [[ -z "${REGISTRY}" ]]; then
  echo "REGISTRY is not set. Example: REGISTRY=ghcr.io/your-org"
  exit 1
fi

build_and_push() {
  local name="$1"
  local path="$2"
  local image="${REGISTRY}/${name}:${TAG}"
  echo "Building ${image}"
  docker build -t "${image}" "${path}"
  echo "Pushing ${image}"
  docker push "${image}"
}

apply_with_image() {
  local file="$1"
  sed "s|image: sporty-rest-producer:latest|image: ${REGISTRY}/sporty-rest-producer:${TAG}|g; \
       s|image: sporty-rest-gateway:latest|image: ${REGISTRY}/sporty-rest-gateway:${TAG}|g; \
       s|image: sporty-consumer:latest|image: ${REGISTRY}/sporty-consumer:${TAG}|g" \
    "${file}" | kubectl apply -f -
}

echo "== Build & push images =="
build_and_push sporty-rest-producer "${ROOT_DIR}/sporty-rest-producer"
build_and_push sporty-rest-gateway "${ROOT_DIR}/sporty-rest-gateway"
build_and_push sporty-consumer "${ROOT_DIR}/sporty-consumer"

echo "== Apply k8s manifests =="
kubectl apply -f "${K8S_DIR}/namespace.yaml"

# Kafka (Strimzi operator must already be installed)
kubectl apply -f "${K8S_DIR}/strimzi-kafka.yaml"

# RocketMQ (Helm)
if ! command -v helm >/dev/null 2>&1; then
  echo "Helm is required to install RocketMQ. Install Helm 3.7+ and re-run."
  exit 1
fi

if [[ ! -d "${ROCKETMQ_HELM_CHART_DIR}" ]]; then
  cat <<'MISSING'
RocketMQ Helm chart not found in repo.

Run these commands and place the chart under:
  k8s/rocketmq-helm/rocketmq

  helm pull oci://registry-1.docker.io/apache/rocketmq --version 0.0.1
  tar -zxvf rocketmq-0.0.1.tgz
  mkdir -p k8s/rocketmq-helm
  mv rocketmq k8s/rocketmq-helm/rocketmq

Then re-run this script.
MISSING
  exit 1
fi

helm upgrade --install "${ROCKETMQ_HELM_RELEASE}" "${ROCKETMQ_HELM_CHART_DIR}" \
  -n sporty --create-namespace \
  -f "${ROCKETMQ_HELM_VALUES}"

apply_with_image "${K8S_DIR}/sporty-rest-producer-deployment.yaml"
kubectl apply -f "${K8S_DIR}/sporty-rest-producer-service.yaml"

apply_with_image "${K8S_DIR}/sporty-rest-gateway-deployment.yaml"
kubectl apply -f "${K8S_DIR}/sporty-rest-gateway-service.yaml"
kubectl apply -f "${K8S_DIR}/ingress.yaml"

apply_with_image "${K8S_DIR}/sporty-consumer-deployment.yaml"
kubectl apply -f "${K8S_DIR}/sporty-consumer-service.yaml"
kubectl apply -f "${K8S_DIR}/sporty-consumer-hpa.yaml"

cat <<'INFO'

Done.

Usage:
  REGISTRY=ghcr.io/your-org TAG=latest ./k8s/deploy.sh

Notes:
- Strimzi operator must already be installed in the cluster.
- Helm must be available for RocketMQ install.
- Update k8s/ingress.yaml host if needed.
INFO
