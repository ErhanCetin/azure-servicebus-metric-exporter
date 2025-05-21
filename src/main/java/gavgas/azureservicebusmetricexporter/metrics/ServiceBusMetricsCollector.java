package gavgas.azureservicebusmetricexporter.metrics;

import gavgas.azureservicebusmetricexporter.config.ServiceBusProperties;
import gavgas.azureservicebusmetricexporter.model.NamespaceMetric;
import gavgas.azureservicebusmetricexporter.model.QueueMetric;
import gavgas.azureservicebusmetricexporter.model.SubscriptionMetric;
import gavgas.azureservicebusmetricexporter.model.TopicMetric;
import gavgas.azureservicebusmetricexporter.service.ServiceBusClientService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ServiceBusMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final ServiceBusClientService serviceBusClientService;
    private final ServiceBusProperties serviceBusProperties;

    // Pattern to extract environment from entity name (e.g., dev-*, qa-*, prod-*)
    private static final Pattern ENV_PATTERN = Pattern.compile("^(dev\\d*|qa|prod|test|uat|stage|staging)-(.*)$");

    // Keep track of registered gauge functions to avoid duplicates
    private final Map<String, Object> registeredMetrics = new HashMap<>();

    public ServiceBusMetricsCollector(MeterRegistry meterRegistry,
                                      ServiceBusClientService serviceBusClientService,
                                      ServiceBusProperties serviceBusProperties) {
        this.meterRegistry = meterRegistry;
        this.serviceBusClientService = serviceBusClientService;
        this.serviceBusProperties = serviceBusProperties;

        log.info("ServiceBusMetricsCollector initialized with environment: {}", serviceBusProperties.getEnvironment());
    }

    @PostConstruct
    public void init() {
        log.info("Initializing ServiceBusMetricsCollector");

        // First, initialize a simple test gauge
        Gauge.builder("azure_servicebus_test", () -> 42)
             .description("Test metric to verify Prometheus integration")
             .register(meterRegistry);

        log.info("Test gauge registered");

        // Initial fetch of metrics
        serviceBusClientService.collectMetrics();

        // Register all metrics with proper functions
        registerAllMetrics();

        log.info("All metrics registered");
    }

    @Scheduled(fixedDelayString = "${azure.servicebus.metrics.scrape-interval:60000}")
    public void collectMetrics() {
        log.info("Scheduled metric collection started");
        serviceBusClientService.collectMetrics();
        log.info("Metrics collected successfully.");
    }


    private void registerAllMetrics() {
        // Register queue metrics
        for (QueueMetric queue : serviceBusClientService.getQueueMetrics()) {
            String queueName = queue.getName();
            String namespace = queue.getNamespace();

            Tags tags = Tags.of(
                "entity_type", "queue",
                "entity_name", queueName,
                "namespace", namespace,
                "environment", serviceBusProperties.getEnvironment()
            );

            String metricId = "queue_active_" + namespace + "_" + queueName;
            if (!registeredMetrics.containsKey(metricId)) {
                Gauge.builder("azure_servicebus_active_messages", () -> {
                         return serviceBusClientService.getQueueMetrics().stream()
                                                       .filter(q -> q.getName().equals(queueName) && q.getNamespace().equals(namespace))
                                                       .mapToLong(QueueMetric::getActiveMessages)
                                                       .findFirst()
                                                       .orElse(0);
                     })
                     .tags(tags)
                     .description("Number of active messages in the queue")
                     .register(meterRegistry);
                registeredMetrics.put(metricId, true);
            }

            metricId = "queue_deadletter_" + namespace + "_" + queueName;
            if (!registeredMetrics.containsKey(metricId)) {
                Gauge.builder("azure_servicebus_dead_letter_messages", () -> {
                         return serviceBusClientService.getQueueMetrics().stream()
                                                       .filter(q -> q.getName().equals(queueName) && q.getNamespace().equals(namespace))
                                                       .mapToLong(QueueMetric::getDeadLetterMessages)
                                                       .findFirst()
                                                       .orElse(0);
                     })
                     .tags(tags)
                     .description("Number of dead letter messages in the queue")
                     .register(meterRegistry);
                registeredMetrics.put(metricId, true);
            }

            metricId = "queue_scheduled_" + namespace + "_" + queueName;
            if (!registeredMetrics.containsKey(metricId)) {
                Gauge.builder("azure_servicebus_scheduled_messages", () -> {
                         return serviceBusClientService.getQueueMetrics().stream()
                                                       .filter(q -> q.getName().equals(queueName) && q.getNamespace().equals(namespace))
                                                       .mapToLong(QueueMetric::getScheduledMessages)
                                                       .findFirst()
                                                       .orElse(0);
                     })
                     .tags(tags)
                     .description("Number of scheduled messages in the queue")
                     .register(meterRegistry);
                registeredMetrics.put(metricId, true);
            }

            metricId = "queue_size_" + namespace + "_" + queueName;
            if (!registeredMetrics.containsKey(metricId)) {
                Gauge.builder("azure_servicebus_size_bytes", () -> {
                         return serviceBusClientService.getQueueMetrics().stream()
                                                       .filter(q -> q.getName().equals(queueName) && q.getNamespace().equals(namespace))
                                                       .mapToLong(QueueMetric::getSizeBytes)
                                                       .findFirst()
                                                       .orElse(0);
                     })
                     .tags(tags)
                     .description("Size of the queue in bytes")
                     .register(meterRegistry);
                registeredMetrics.put(metricId, true);
            }

            metricId = "queue_total_" + namespace + "_" + queueName;
            if (!registeredMetrics.containsKey(metricId)) {
                Gauge.builder("azure_servicebus_total_messages", () -> {
                         return serviceBusClientService.getQueueMetrics().stream()
                                                       .filter(q -> q.getName().equals(queueName) && q.getNamespace().equals(namespace))
                                                       .mapToLong(QueueMetric::getTotalMessages)
                                                       .findFirst()
                                                       .orElse(0);
                     })
                     .tags(tags)
                     .description("Total number of messages in the queue")
                     .register(meterRegistry);
                registeredMetrics.put(metricId, true);
            }

            log.info("Registered metrics for queue: {}", queueName);
        }

        // Register topic metrics
        for (TopicMetric topic : serviceBusClientService.getTopicMetrics()) {
            String topicName = topic.getName();
            String namespace = topic.getNamespace();

            Tags tags = Tags.of(
                "entity_type", "topic",
                "entity_name", topicName,
                "namespace", namespace,
                "environment", serviceBusProperties.getEnvironment()
            );

            String metricId = "topic_size_" + namespace + "_" + topicName;
            if (!registeredMetrics.containsKey(metricId)) {
                Gauge.builder("azure_servicebus_size_bytes", () -> {
                         return serviceBusClientService.getTopicMetrics().stream()
                                                       .filter(t -> t.getName().equals(topicName) && t.getNamespace().equals(namespace))
                                                       .mapToLong(TopicMetric::getSizeBytes)
                                                       .findFirst()
                                                       .orElse(0);
                     })
                     .tags(tags)
                     .description("Size of the topic in bytes")
                     .register(meterRegistry);
                registeredMetrics.put(metricId, true);
            }

            metricId = "topic_subscription_count_" + namespace + "_" + topicName;
            if (!registeredMetrics.containsKey(metricId)) {
                Gauge.builder("azure_servicebus_subscription_count", () -> {
                         return serviceBusClientService.getTopicMetrics().stream()
                                                       .filter(t -> t.getName().equals(topicName) && t.getNamespace().equals(namespace))
                                                       .mapToLong(TopicMetric::getSubscriptionCount)
                                                       .findFirst()
                                                       .orElse(0);
                     })
                     .tags(tags)
                     .description("Number of subscriptions for the topic")
                     .register(meterRegistry);
                registeredMetrics.put(metricId, true);
            }

            log.info("Registered metrics for topic: {}", topicName);
        }

        // Register subscription metrics
        for (SubscriptionMetric sub : serviceBusClientService.getSubscriptionMetrics()) {
            String topicName = sub.getTopicName();
            String subscriptionName = sub.getName();
            String namespace = sub.getNamespace();
            String entityName = topicName + "/" + subscriptionName;

            Tags tags = Tags.of(
                "entity_type", "subscription",
                "entity_name", entityName,
                "namespace", namespace,
                "topic_name", topicName,
                "subscription_name", subscriptionName,
                "environment", serviceBusProperties.getEnvironment()
            );

            String metricId = "sub_active_" + namespace + "_" + topicName + "_" + subscriptionName;
            if (!registeredMetrics.containsKey(metricId)) {
                Gauge.builder("azure_servicebus_active_messages", () -> {
                         return serviceBusClientService.getSubscriptionMetrics().stream()
                                                       .filter(s -> s.getTopicName().equals(topicName) &&
                                                           s.getName().equals(subscriptionName) &&
                                                           s.getNamespace().equals(namespace))
                                                       .mapToLong(SubscriptionMetric::getActiveMessages)
                                                       .findFirst()
                                                       .orElse(0);
                     })
                     .tags(tags)
                     .description("Number of active messages in the subscription")
                     .register(meterRegistry);
                registeredMetrics.put(metricId, true);
            }

            metricId = "sub_deadletter_" + namespace + "_" + topicName + "_" + subscriptionName;
            if (!registeredMetrics.containsKey(metricId)) {
                Gauge.builder("azure_servicebus_dead_letter_messages", () -> {
                         return serviceBusClientService.getSubscriptionMetrics().stream()
                                                       .filter(s -> s.getTopicName().equals(topicName) &&
                                                           s.getName().equals(subscriptionName) &&
                                                           s.getNamespace().equals(namespace))
                                                       .mapToLong(SubscriptionMetric::getDeadLetterMessages)
                                                       .findFirst()
                                                       .orElse(0);
                     })
                     .tags(tags)
                     .description("Number of dead letter messages in the subscription")
                     .register(meterRegistry);
                registeredMetrics.put(metricId, true);
            }

            log.info("Registered metrics for subscription: {}/{}", topicName, subscriptionName);
        }

        // Register namespace metrics
        for (NamespaceMetric ns : serviceBusClientService.getNamespaceMetrics()) {
            String namespace = ns.getNamespace();

            // For namespace, use the configured environment
            String environment = serviceBusProperties.getEnvironment();

            Tags tags = Tags.of(
                "namespace", namespace,
                "environment", environment
            );

            String metricId = "namespace_connections_" + namespace;
            if (!registeredMetrics.containsKey(metricId)) {
                Gauge.builder("azure_servicebus_active_connections", () -> {
                         return serviceBusClientService.getNamespaceMetrics().stream()
                                                       .filter(n -> n.getNamespace().equals(namespace))
                                                       .mapToLong(NamespaceMetric::getActiveConnections)
                                                       .findFirst()
                                                       .orElse(0);
                     })
                     .tags(tags)
                     .description("Number of active connections")
                     .register(meterRegistry);
                registeredMetrics.put(metricId, true);
            }

            // Register quota metrics
            for (Map.Entry<String, Double> entry : ns.getQuotaUsage().entrySet()) {
                String quotaName = entry.getKey();

                Tags quotaTags = Tags.of(
                    "namespace", namespace,
                    "quota_name", quotaName,
                    "environment", environment
                );

                metricId = "namespace_quota_" + namespace + "_" + quotaName;
                if (!registeredMetrics.containsKey(metricId)) {
                    Gauge.builder("azure_servicebus_quota_usage_percentage", () -> {
                             return serviceBusClientService.getNamespaceMetrics().stream()
                                                           .filter(n -> n.getNamespace().equals(namespace))
                                                           .filter(n -> n.getQuotaUsage().containsKey(quotaName))
                                                           .map(n -> n.getQuotaUsage().get(quotaName))
                                                           .findFirst()
                                                           .orElse(0.0);
                         })
                         .tags(quotaTags)
                         .description("Percentage of quota used")
                         .register(meterRegistry);
                    registeredMetrics.put(metricId, true);
                }
            }

            log.info("Registered metrics for namespace: {}", namespace);
        }
    }
}