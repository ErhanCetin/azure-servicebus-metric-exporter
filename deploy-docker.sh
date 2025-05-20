#!/bin/bash
## IMPORTANT !!!!!!    prometheus.yml file should be in the same directory of deploy-docker.sh
# Azure Service Bus Metric Exporter Docker deployment script
set -e

# Default parameters
ACTION="deploy"
ENV="local"
TAG="latest"
CONNECTION_STRING=""
DOCKER_REGISTRY=""

# Usage function
function usage {
  echo "Usage: $0 [OPTIONS]"
  echo "Options:"
  echo "  -a, --action ACTION       Action to perform: deploy, undeploy, build, push (default: deploy)"
  echo "  -e, --environment ENV     Environment to deploy to (default: local)"
  echo "  -t, --tag TAG             Docker image tag (default: latest)"
  echo "  -c, --connection STRING   Azure Service Bus connection string (required for deploy)"
  echo "  -r, --registry REGISTRY   Docker registry (required for push)"
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
    -t|--tag)
      TAG="$2"
      shift 2
      ;;
    -c|--connection)
      CONNECTION_STRING="$2"
      shift 2
      ;;
    -r|--registry)
      DOCKER_REGISTRY="$2"
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

# Build the project
function build_project {
  echo "Building the project..."
  ./gradlew clean build
}

# Build Docker image
function build_docker {
  echo "Building Docker image with tag: $TAG"
  docker build -t azure-servicebus-metric-exporter:$TAG .
}

# Push Docker image
function push_docker {
  if [ -z "$DOCKER_REGISTRY" ]; then
    echo "Error: Docker registry is required for push action"
    usage
  fi
  
  echo "Tagging and pushing Docker image to registry: $DOCKER_REGISTRY"
  docker tag azure-servicebus-metric-exporter:$TAG $DOCKER_REGISTRY/azure-servicebus-metric-exporter:$TAG
  docker push $DOCKER_REGISTRY/azure-servicebus-metric-exporter:$TAG
}

# Deploy with Docker Compose
function deploy_compose {
  if [ -z "$CONNECTION_STRING" ]; then
    echo "Error: Azure Service Bus connection string is required for deploy action"
    usage
  fi
  
  echo "Deploying with Docker Compose to environment: $ENV"
  
  # Export environment variables for Docker Compose
  export ENVIRONMENT=$ENV
  export AZURE_SERVICEBUS_CONNECTION_STRING=$CONNECTION_STRING
  
  # Build and start Docker Compose services
  docker-compose build
  docker-compose up -d
  
  echo "Deployment completed successfully!"
  echo "Access the application at: http://localhost:8080"
  echo "Access Prometheus at: http://localhost:9090"
  echo "Access Grafana at: http://localhost:3000 (admin/admin)"
}

# Undeploy with Docker Compose
function undeploy_compose {
  echo "Stopping and removing containers..."
  docker-compose down
  
  # Optionally remove volumes
  read -p "Do you want to remove volumes (prometheus & grafana data)? (y/n): " remove_volumes
  if [[ $remove_volumes == "y" || $remove_volumes == "Y" ]]; then
    docker-compose down -v
    echo "Volumes removed."
  fi
  
  echo "Undeployment completed successfully!"
}

# Main execution
case $ACTION in
  build)
    build_project
    build_docker
    ;;
  push)
    build_docker
    push_docker
    ;;
  deploy)
    build_project
    build_docker
    deploy_compose
    ;;
  undeploy)
    undeploy_compose
    ;;
  *)
    echo "Unknown action: $ACTION"
    usage
    ;;
esac