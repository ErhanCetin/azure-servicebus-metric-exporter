#!/bin/bash

# Azure Service Bus Metric Exporter Kubernetes deployment script
set -e

# Default parameters
ACTION="deploy"
ENV="production"
NAMESPACE="monitoring"
CONNECTION_STRING=""
CONTEXT=""
HELM_RELEASE="azure-sb-exporter"

# Usage function
function usage {
  echo "Usage: $0 [OPTIONS]"
  echo "Options:"
  echo "  -a, --action ACTION       Action to perform: deploy, undeploy, upgrade (default: deploy)"
  echo "  -e, --environment ENV     Environment to deploy to (default: production)"
  echo "  -n, --namespace NS        Kubernetes namespace (default: monitoring)"
  echo "  -c, --connection STRING   Azure Service Bus connection string (required for deploy/upgrade)"
  echo "  -k, --kube-context CTX    Kubernetes context (optional)"
  echo "  -r, --release NAME        Helm release name (default: azure-sb-exporter)"
  echo "  -h, --help                Display this help message"
  exit 1
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  key="$1"
  case $key in
    -a|--action)
      ACTION="$2"
      shift 2
      ;;
    -e|--environment)
      ENV="$2"
      shift 2
      ;;
    -n|--namespace)
      NAMESPACE="$2"
      shift 2
      ;;
    -c|--connection)
      CONNECTION_STRING="$2"
      shift 2
      ;;
    -k|--kube-context)
      CONTEXT="$2"
      shift 2
      ;;
    -r|--release)
      HELM_RELEASE="$2"
      shift 2
      ;;
    -h|--help)
      usage
      ;;
    *)
      echo "Unknown option: $1"
      usage
      ;;
  esac
done

# Set kubectl context if provided
if [ -n "$CONTEXT" ]; then
  kubectl config use-context $CONTEXT
fi

# Deploy with Kubernetes YAML files
function deploy_kubernetes {
  if [ -z "$CONNECTION_STRING" ]; then
    echo "Error: Azure Service Bus connection string is required for deploy action"
    usage
  fi

  echo "Deploying to Kubernetes namespace: $NAMESPACE, environment: $ENV"

  # Create namespace if it doesn't exist
  kubectl get namespace $NAMESPACE > /dev/null 2>&1 || kubectl create namespace $NAMESPACE

  # Create ConfigMap with environment
  kubectl -n $NAMESPACE create configmap azure-servicebus-exporter-env \
    --from-literal=ENVIRONMENT=$ENV \
    --dry-run=client -o yaml | kubectl apply -f -

  # Create Secret with connection string
  kubectl -n $NAMESPACE create secret generic azure-servicebus-exporter-secret \
    --from-literal=AZURE_SERVICEBUS_CONNECTION_STRING="$CONNECTION_STRING" \
    --dry-run=client -o yaml | kubectl apply -f -

  # Apply Kubernetes manifests
  kubectl apply -f kubernetes/deployment.yaml -n $NAMESPACE

  echo "Deployment completed successfully!"
  echo "To get the service URL: kubectl -n $NAMESPACE get svc azure-servicebus-exporter"
}

# Undeploy from Kubernetes
function undeploy_kubernetes {
  echo "Removing deployment from Kubernetes namespace: $NAMESPACE"

  # Delete Kubernetes resources
  kubectl delete -f kubernetes/deployment.yaml -n $NAMESPACE --ignore-not-found
  kubectl -n $NAMESPACE delete secret azure-servicebus-exporter-secret --ignore-not-found
  kubectl -n $NAMESPACE delete configmap azure-servicebus-exporter-env --ignore-not-found

  echo "Undeployment completed successfully!"
}

# Deploy with Helm
function deploy_helm {
  if [ -z "$CONNECTION_STRING" ]; then
    echo "Error: Azure Service Bus connection string is required for deploy action"
    usage
  fi

  echo "Deploying with Helm to namespace: $NAMESPACE, environment: $ENV"

  # Create namespace if it doesn't exist
  kubectl get namespace $NAMESPACE > /dev/null 2>&1 || kubectl create namespace $NAMESPACE

  # Install or upgrade Helm chart
  helm upgrade --install $HELM_RELEASE ./helm \
    --namespace $NAMESPACE \
    --set env.ENVIRONMENT=$ENV \
    --set azureServiceBus.connectionString="$CONNECTION_STRING"

  echo "Helm deployment completed successfully!"
  echo "To get the service URL: kubectl -n $NAMESPACE get svc $HELM_RELEASE-azure-servicebus-metric-exporter"
}

# Undeploy with Helm
function undeploy_helm {
  echo "Uninstalling Helm release: $HELM_RELEASE from namespace: $NAMESPACE"

  helm uninstall $HELM_RELEASE -n $NAMESPACE

  echo "Helm uninstall completed successfully!"
}

# Main execution
case $ACTION in
  deploy)
    if [[ -d "./helm" ]]; then
      deploy_helm
    else
      deploy_kubernetes
    fi
    ;;
  upgrade)
    if [[ -d "./helm" ]]; then
      deploy_helm
    else
      deploy_kubernetes
    fi
    ;;
  undeploy)
    if [[ -d "./helm" ]]; then
      undeploy_helm
    else
      undeploy_kubernetes
    fi
    ;;
  *)
    echo "Unknown action: $ACTION"
    usage
    ;;
esac