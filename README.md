# Azure Service Bus Prometheus Exporter

A comprehensive Spring Boot application that collects and exports Azure Service Bus metrics to Prometheus format for monitoring and alerting.

## Features

- **Comprehensive Metrics Collection**: Collects detailed metrics for queues, topics, and subscriptions
- **Prometheus Integration**: Exposes metrics in Prometheus format via Spring Boot Actuator
- **Real-time Monitoring**: Configurable scrape intervals for up-to-date metrics
- **Environment Detection**: Automatic environment tagging based on entity naming patterns
- **Efficient Caching**: Internal metric caching to reduce API calls to Azure
- **Containerized Deployment**: Docker and Kubernetes ready with sample configurations
- **Grafana Dashboards**: Pre-built Grafana dashboards for visualization
- **CI/CD Support**: Sample pipelines for GitLab and Azure DevOps

## Metrics Exported

### Queue Metrics
- Active Messages Count
- Dead Letter Messages Count
- Scheduled Messages Count
- Transfer Messages Count
- Size in Bytes
- Total Messages Count

### Topic Metrics
- Size in Bytes
- Subscription Count

### Subscription Metrics
- Active Messages Count
- Dead Letter Messages Count
- Transfer Messages Count
- Transfer Dead Letter Messages

### Namespace Metrics
- Active Connections
- Quota Usage Percentages

## Getting Started

### Prerequisites

- Java 21 or higher
- Azure Service Bus namespace with proper access rights
- Docker (for containerized deployment)
- Prometheus (for metrics collection)
- Grafana (for visualization)

### Configuration

Configure the application through `application.yaml` or environment variables:

```yaml
azure:
  servicebus:
    # Environment name for tagging metrics
    environment: ${ENVIRONMENT:default}
    auth:
      mode: connection_string
      connection-string: ${AZURE_SERVICEBUS_CONNECTION_STRING}
    entities:
      # RegEx pattern to filter entities
      filter: ".*"
      # Entity types to collect metrics for
      types:
        - queue
        - topic
        - subscription
    # Include namespace-level metrics
    include-namespace-metrics: true
    metrics:
      # Cache duration to reduce API calls
      cache-duration: 60s
      # Collection interval
      scrape-interval: 60s
```

### Running Locally

Build and run the application:

```bash
./gradlew clean build
java -jar build/libs/azure-servicebus-metric-exporter-*.jar
```

### Docker Deployment

Use the included Docker Compose setup for a complete monitoring stack:

```bash
# Set your connection string
export AZURE_SERVICEBUS_CONNECTION_STRING="Endpoint=sb://namespace.servicebus.windows.net/;SharedAccessKeyName=key;SharedAccessKey=value"
export ENVIRONMENT="dev"

# Deploy with Docker Compose
docker-compose up -d
```

This will start:
- The Azure Service Bus Exporter
- Prometheus for metrics collection
- Grafana for visualization

### Kubernetes Deployment

Deploy to Kubernetes using either raw manifests or Helm:

```bash
# Using raw manifests
kubectl create namespace monitoring
kubectl -n monitoring create secret generic azure-servicebus-exporter-secret \
  --from-literal=AZURE_SERVICEBUS_CONNECTION_STRING="your-connection-string"
kubectl apply -f kubernetes/deployment.yaml

# Using Helm
helm upgrade --install azure-sb-exporter ./helm \
  --namespace monitoring \
  --set env.ENVIRONMENT=prod \
  --set azureServiceBus.connectionString="your-connection-string"
```

## Accessing Metrics

After deployment, the following endpoints are available:

- **Prometheus Metrics**: http://localhost:8080/actuator/prometheus
- **Health Status**: http://localhost:8080/actuator/health
- **Application Info**: http://localhost:8080/actuator/info
- **Custom Probe Metrics**: http://localhost:8080/probe/metrics
- **Entity List**: http://localhost:8080/probe/metrics/list
- **Specific Entity Metrics**: http://localhost:8080/probe/metrics/resource?type=queue&name=yourQueueName
- **Application Status**: http://localhost:8080/status
- **Query UI**: http://localhost:8080/query

## Environment Detection

The exporter automatically detects environments from entity naming patterns:

- Entity names like `dev-entity-name` will be tagged with environment `dev`
- Entity names like `prod-entity-name` will be tagged with environment `prod`
- Entity names without recognized patterns will use the default environment from configuration

## Grafana Dashboard

A pre-built Grafana dashboard is included in the `grafana` directory. Import the JSON file to get:

- Overview of all Service Bus metrics
- Entity-specific visualizations
- Environment-based filtering
- Active and dead letter message tracking
- Size monitoring

## CI/CD Integration

Sample CI/CD pipelines are provided for:

- **GitLab**: See `ci_cd/.gitlab-ci.yml`
- **Azure DevOps**: See `ci_cd/azure/azure-pipelines.yml`

These pipelines include:
- Building and testing the application
- Creating and pushing Docker images
- Deploying to Kubernetes clusters
- Environment-specific configurations

## Development

### Project Structure

```
azure-servicebus-metric-exporter/
├── src/
│   ├── main/
│   │   ├── java/gavgas/azureservicebusmetricexporter/
│   │   │   ├── AzureServicebusMetricExporterApplication.java
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── health/
│   │   │   ├── metrics/
│   │   │   ├── model/
│   │   │   └── service/
│   │   └── resources/
│   └── test/
├── helm/                 # Helm chart for Kubernetes deployment
├── kubernetes/           # Raw Kubernetes manifests
├── ci_cd/                # CI/CD pipelines for GitLab and Azure
├── grafana/              # Grafana dashboards and documentation
├── Dockerfile            # Multi-stage Docker build
├── docker-compose.yaml   # Complete monitoring stack
└── README.md
```

### Key Components

- **ServiceBusClientService**: Collects metrics from Azure Service Bus
- **ServiceBusMetricsCollector**: Registers metrics with Prometheus registry
- **ProbeController**: Provides JSON API for metrics
- **StatusController**: Application status and UI for testing

### Building

```bash
# Build JAR
./gradlew clean build

# Build Docker image
docker build -t yourregistry/azure-servicebus-metric-exporter:latest .

# Push Docker image
docker push yourregistry/azure-servicebus-metric-exporter:latest
```

## Extending the Project

### Adding New Metrics

To add new metrics:

1. Update the model classes in `model/` directory
2. Modify `ServiceBusClientService` to collect the new metrics
3. Update `ServiceBusMetricsCollector` to register the new metrics with Prometheus

### Custom Dashboards

Additional Grafana dashboards can be created by:

1. Using the Query UI at `/query` to experiment with metrics
2. Building custom panels in Grafana
3. Exporting the dashboard as JSON

## Troubleshooting

### Common Issues

- **No metrics appearing**: Check connection string and access permissions
- **Missing entities**: Verify entity filter regex in configuration
- **Connection errors**: Check network access to Azure Service Bus namespace
- **Performance issues**: Adjust cache duration and scrape interval

### Checking Status

```bash
# Check application status
curl http://localhost:8080/actuator/health

# View Docker logs
docker logs azure-servicebus-exporter

# Check Prometheus targets
curl http://localhost:9090/targets
```

## Sample Scenarios for Environment Filter

Example Scenarios

### Scenario 1: entity.filter=".*" (All entities)

Environment: "dev"
Entity Filter: ".*" (matches everything)
Environment Filter: "^dev.*" (only those starting with "dev")
Result: Only entities starting with "dev" will be collected

### Scenario 2: entity.filter="cdp-.*" (Only those starting with cdp-)

Environment: "dev"
Entity Filter: "cdp-.*"
Environment Filter: "^dev.*"
Result: No entity metrics will be collected because an entity cannot start with both "dev" and "cdp-"

### Scenario 3: entity.filter=".-cdp-." (Those containing -cdp-)

Environment: "dev"
Entity Filter: ".-cdp-."
Environment Filter: "^dev.*"
Result: Entities matching the "dev-cdp-*" pattern will be collected (e.g., "dev-cdp-event", "dev1-cdp-batch")

### Scenario 4: entity.filter=".event$" (Those ending with event)

Environment: "qa"
Entity Filter: ".*event$"
Environment Filter: "^qa.*"
Result: Entities matching the "qa*event" pattern (e.g., "qa-cdp-event", "qa1-test-event")

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Azure Messaging SDK for Java
- Spring Boot and Spring Actuator
- Micrometer and Prometheus
- Grafana for visualization

---

*Further documentation and examples are available in the project's wiki.*