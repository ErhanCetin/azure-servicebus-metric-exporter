# Azure Service Bus Prometheus Exporter

A Spring Boot application that exports Azure Service Bus metrics to Prometheus.

## Features

- Collects Service Bus metrics via Azure SDK for Java
- Exposes metrics in Prometheus format via Spring Boot Actuator
- Supports queues, topics, and subscriptions
- Entity filtering with regex patterns
- Metric caching to reduce API calls
- Ready-to-use Kubernetes deployment and Helm chart
- Support for both Azure Kubernetes Service (AKS) and Google Kubernetes Engine (GKE)

## Metrics

The exporter collects the following Azure Service Bus metrics:

### Queue Metrics
- Active Messages
- Dead-lettered Messages
- Scheduled Messages
- Transfer Messages
- Transfer Dead-lettered Messages
- Size in Bytes
- Total Messages
- Queue Creation Time
- Queue Update Time
- Queue Access Time

### Topic Metrics
- Size in Bytes
- Subscription Count

### Subscription Metrics
- Active Messages
- Dead-lettered Messages
- Transfer Messages
- Transfer Dead-lettered Messages

## Configuration

The application can be configured using the `application.yml` file or environment variables.

### Configuration Properties

```yaml
azure:
  servicebus:
    auth:
      mode: connection_string
      connection-string: ${SB_CONNECTION_STRING:}
    entities:
      filter: ".*"
      types: queue, topic, subscription
    include-namespace-metrics: true
    metrics:
      namespace: azure_servicebus
      cache-duration: 60s
      scrape-interval: 60s
    namespaces:
      - defaultNamespace
```

### Environment Variables

- `SB_CONNECTION_STRING`: Azure Service Bus connection string

## API Endpoints

- `/actuator/prometheus`: Prometheus metrics endpoint
- `/actuator/health`: Health check endpoint
- `/probe/metrics`: Raw metrics in JSON format
- `/probe/metrics/list`: List of available entities
- `/probe/metrics/resource`: Metrics for a specific entity
- `/status`: Application status information
- `/query`: Development UI for testing

## Usage

### Using Docker

```bash
docker run -p 8080:8080 \
  -e SB_CONNECTION_STRING="Endpoint=sb://namespace.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=yourkeyhere" \
  yourregistry/azure-servicebus-exporter:latest
```

### Using Kubernetes

```bash
# Create a secret with your connection string
kubectl create secret generic azure-servicebus-exporter \
  --from-literal=connection-string="Endpoint=sb://namespace.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=yourkeyhere"

# Apply the Kubernetes manifests
kubectl apply -f kubernetes/

# Or use Helm
helm install azure-servicebus-exporter ./helm/azure-servicebus-exporter
```

### Local Development

```bash
# Build the application
./gradlew clean build

# Run the application
java -jar build/libs/azure-servicebus-exporter-1.0.0.jar

# Run with Gradle bootRun
./gradlew bootRun
```

## Building

### Building the JAR

```bash
./gradlew clean build
```

### Building the Docker Image

```bash
./gradlew bootBuildImage --imageName=yourregistry/azure-servicebus-exporter:latest
```

## Prometheus Configuration

```yaml
scrape_configs:
  - job_name: 'azure-servicebus'
    scrape_interval: 1m
    static_configs:
      - targets: ['azure-servicebus-exporter:8080']
    metrics_path: /actuator/prometheus
```

## Project Structure

```
azure-servicebus-exporter/
├── src/
│   └── main/
│       ├── java/com/example/azureservicebusexporter/
│       │   ├── AzureServiceBusExporterApplication.java
│       │   ├── config/
│       │   │   ├── ServiceBusClientConfig.java
│       │   │   └── ServiceBusProperties.java
│       │   ├── controller/
│       │   │   ├── ProbeController.java
│       │   │   └── StatusController.java
│       │   ├── health/
│       │   │   └── ServiceBusHealthIndicator.java
│       │   ├── metrics/
│       │   │   └── ServiceBusMetricsCollector.java
│       │   ├── model/
│       │   │   ├── NamespaceMetric.java
│       │   │   ├── QueueMetric.java
│       │   │   ├── SubscriptionMetric.java
│       │   │   └── TopicMetric.java
│       │   └── service/
│       │       └── ServiceBusClientService.java
│       └── resources/
│           └── application.yml
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
├── Dockerfile
└── kubernetes/
    ├── configmap.yaml
    ├── deployment.yaml
    ├── secret.yaml
    ├── service.yaml
    └── serviceaccount.yaml
```

## License

This project is licensed under the MIT License.


Doğru endpoint'lere erişmek için aşağıdaki URL'leri kullanın:

Prometheus Metrikleri için: http://localhost:8080/actuator/prometheus
Sağlık Durumu için: http://localhost:8080/actuator/health
Uygulama Bilgileri için: http://localhost:8080/actuator/info

Ayrıca, özel API endpoint'leri için:

Özel API Metrikleri için: http://localhost:8080/probe/metrics
Varlık Listesi için: http://localhost:8080/probe/metrics/list
Belirli Kaynak için: http://localhost:8080/probe/metrics/resource?type=queue&name=yourQueueName
Durum Bilgisi için: http://localhost:8080/status
Sorgu UI için: http://localhost:8080/query



İleri Adımlar
Şimdi herşey düzgün çalışıyor, ama daha da geliştirebilirsiniz:

Metrik Dokümantasyonu: Her metriğin ne anlama geldiğini açıklayan bir doküman hazırlayın.
Grafana Dashboard Template: Kullanıcılar için önceden hazırlanmış bir Grafana dashboard template'i oluşturun.
Özel Metrikler: İşletme ihtiyaçlarınıza göre özel metrikler ekleyin (örn. kuyruk mesaj yaşı, işleme süreleri, hata oranları).
Konfigürasyon Seçenekleri: Daha fazla yapılandırma seçeneği ekleyin (örn. hangi metriklerin toplanacağını seçme).
PromQL Örnekleri: Yaygın kullanım senaryoları için PromQL sorgu örnekleri sağlayın.

Tebrikler! Azure Service Bus metriklerini Prometheus formatında sunmayı başarıyla tamamladınız. Artık bu metrikleri izleyebilir, görselleştirebilir ve alarmlar oluşturabilirsiniz.RetryClaude can make mistakes. Please double-check responses.