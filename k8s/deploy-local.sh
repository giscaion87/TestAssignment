#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
K8S_DIR="${ROOT_DIR}/k8s"

STRIMZI_INSTALL_URL="${STRIMZI_INSTALL_URL:-https://strimzi.io/install/latest?namespace=sporty}"
INGRESS_NGINX_URL="${INGRESS_NGINX_URL:-https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.11.2/deploy/static/provider/cloud/deploy.yaml}"
START_PORT_FORWARD="${START_PORT_FORWARD:-0}"
ROCKETMQ_HELM_RELEASE="${ROCKETMQ_HELM_RELEASE:-sporty-rocketmq}"
ROCKETMQ_HELM_CHART_DIR="${ROCKETMQ_HELM_CHART_DIR:-${K8S_DIR}/rocketmq-helm/rocketmq}"
ROCKETMQ_HELM_VALUES="${ROCKETMQ_HELM_VALUES:-${K8S_DIR}/rocketmq-helm/values.yaml}"

check_crd() {
  local crd="$1"
  kubectl get crd "${crd}" >/dev/null 2>&1
}

ensure_ingress() {
  if ! kubectl get namespace ingress-nginx >/dev/null 2>&1; then
    echo "Installing ingress-nginx..."
    kubectl apply -f "${INGRESS_NGINX_URL}"
    kubectl -n ingress-nginx wait --for=condition=Available deployment/ingress-nginx-controller --timeout=180s || true
  fi
}

ensure_operators() {
  local has_crd=0
  local has_deploy=0
  if check_crd kafkas.kafka.strimzi.io; then
    has_crd=1
  fi
  if kubectl -n sporty get deploy strimzi-cluster-operator >/dev/null 2>&1; then
    has_deploy=1
  fi

  if [[ "${has_crd}" -eq 0 || "${has_deploy}" -eq 0 ]]; then
    echo "Installing Strimzi operator..."
    kubectl apply -f "${STRIMZI_INSTALL_URL}"
  fi

  if kubectl -n sporty get deploy strimzi-cluster-operator >/dev/null 2>&1; then
    kubectl -n sporty rollout status deploy/strimzi-cluster-operator --timeout=180s || true
  fi
}

port_forward_gateway() {
  echo "Starting port-forward: svc/sporty-rest-gateway -> localhost:8080"
  kubectl -n sporty port-forward svc/sporty-rest-gateway 8080:8080 >/tmp/sporty-rest-gateway-portforward.log 2>&1 &
  echo $! > /tmp/sporty-rest-gateway-portforward.pid
  echo "Port-forward PID: $(cat /tmp/sporty-rest-gateway-portforward.pid)"
}

echo "== Build local images =="
docker build -t sporty-rest-producer:latest "${ROOT_DIR}/sporty-rest-producer"
docker build -t sporty-rest-gateway:latest "${ROOT_DIR}/sporty-rest-gateway"
docker build -t sporty-consumer:latest "${ROOT_DIR}/sporty-consumer"

echo "== Apply namespace =="
kubectl apply -f "${K8S_DIR}/namespace.yaml"

echo "== Ensure ingress-nginx =="
ensure_ingress

echo "== Ensure operators =="
ensure_operators

# Kafka (Strimzi)
kubectl apply -f "${K8S_DIR}/strimzi-kafka.yaml"
kubectl -n sporty wait kafka/sporty --for=condition=Ready --timeout=300s || true

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

kubectl -n sporty wait --for=condition=Ready pod -l app.kubernetes.io/name=broker --timeout=180s || true
kubectl -n sporty wait --for=condition=Ready pod -l app.kubernetes.io/name=nameserver --timeout=180s || true

# Apps
kubectl apply -f "${K8S_DIR}/sporty-rest-producer-deployment.yaml"
kubectl apply -f "${K8S_DIR}/sporty-rest-producer-service.yaml"

kubectl apply -f "${K8S_DIR}/sporty-rest-gateway-deployment.yaml"
kubectl apply -f "${K8S_DIR}/sporty-rest-gateway-service.yaml"
kubectl apply -f "${K8S_DIR}/ingress.yaml"

kubectl apply -f "${K8S_DIR}/sporty-consumer-deployment.yaml"
kubectl apply -f "${K8S_DIR}/sporty-consumer-service.yaml"
kubectl apply -f "${K8S_DIR}/sporty-consumer-hpa.yaml"

if [[ "${START_PORT_FORWARD}" == "1" ]]; then
  port_forward_gateway
fi

cat <<'INFO'

Done.

Notes:
- Docker Desktop Kubernetes uses local images directly.
- Add to /etc/hosts for ingress:
  127.0.0.1 api.test.com
- Optional port-forward:
  START_PORT_FORWARD=1 ./k8s/deploy-local.sh
INFO
