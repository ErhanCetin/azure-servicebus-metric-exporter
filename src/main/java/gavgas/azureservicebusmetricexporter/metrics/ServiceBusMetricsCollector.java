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
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.ToLongFunction;

@Slf4j
@Component
public class ServiceBusMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final ServiceBusClientService serviceBusClientService;
    private final ServiceBusProperties serviceBusProperties;

    // Keep track of registered gauge functions to avoid duplicates
    private final Map<String, Object> registeredMetrics = new HashMap<>();

    public ServiceBusMetricsCollector(MeterRegistry meterRegistry,
                                      ServiceBusClientService serviceBusClientService,
                                      ServiceBusProperties serviceBusProperties) {
        this.meterRegistry = meterRegistry;
        this.serviceBusClientService = serviceBusClientService;
        this.serviceBusProperties = serviceBusProperties;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing ServiceBusMetricsCollector");

        // Initial fetch of metrics
        serviceBusClientService.collectMetrics();

        // Register all metrics with proper functions
        registerAllMetrics();

        log.info("All metrics registered");
    }

    private void registerAllMetrics() {
        // Register queue metrics
        for (QueueMetric queue : serviceBusClientService.getQueueMetrics()) {
            String queueName = queue.getName();
            String namespace = queue.getNamespace();

            Tags tags = Tags.of("entity_type",
                                "queue",
                                "entity_name",
                                queueName,
                                "namespace",
                                namespace,
                                "environment",
                                serviceBusProperties.getEnvironment());

            // Register active messages metric
            registerQueueMetric("queue_active_",
                                "azure_servicebus_active_messages",
                                queueName,
                                namespace,
                                tags,
                                "Number of active messages in the queue",
                                QueueMetric::getActiveMessages);

            // Register dead letter messages metric
            registerQueueMetric("queue_deadletter_",
                                "azure_servicebus_dead_letter_messages",
                                queueName,
                                namespace,
                                tags,
                                "Number of dead letter messages in the queue",
                                QueueMetric::getDeadLetterMessages);

            // Register scheduled messages metric
            registerQueueMetric("queue_scheduled_",
                                "azure_servicebus_scheduled_messages",
                                queueName,
                                namespace,
                                tags,
                                "Number of scheduled messages in the queue",
                                QueueMetric::getScheduledMessages);

            // Register size bytes metric
            registerQueueMetric("queue_size_",
                                "azure_servicebus_size_bytes",
                                queueName,
                                namespace,
                                tags,
                                "Size of the queue in bytes",
                                QueueMetric::getSizeBytes);

            // Register total messages metric
            registerQueueMetric("queue_total_",
                                "azure_servicebus_total_messages",
                                queueName,
                                namespace,
                                tags,
                                "Total number of messages in the queue",
                                QueueMetric::getTotalMessages);

            log.info("Registered metrics for queue: {}", queueName);
        }

        // Register topic metrics
        for (TopicMetric topic : serviceBusClientService.getTopicMetrics()) {
            String topicName = topic.getName();
            String namespace = topic.getNamespace();

            Tags tags = Tags.of("entity_type",
                                "topic",
                                "entity_name",
                                topicName,
                                "namespace",
                                namespace,
                                "environment",
                                serviceBusProperties.getEnvironment());

            // Register size bytes metric
            registerTopicMetric("topic_size_",
                                "azure_servicebus_size_bytes",
                                topicName,
                                namespace,
                                tags,
                                "Size of the topic in bytes",
                                TopicMetric::getSizeBytes);

            // Register subscription count metric
            registerTopicMetric("topic_subscription_count_",
                                "azure_servicebus_subscription_count",
                                topicName,
                                namespace,
                                tags,
                                "Number of subscriptions for the topic",
                                TopicMetric::getSubscriptionCount);

            log.info("Registered metrics for topic: {}", topicName);
        }

        // Register subscription metrics
        for (SubscriptionMetric sub : serviceBusClientService.getSubscriptionMetrics()) {
            String topicName = sub.getTopicName();
            String subscriptionName = sub.getName();
            String namespace = sub.getNamespace();
            String entityName = topicName + "/" + subscriptionName;

            Tags tags = Tags.of("entity_type",
                                "subscription",
                                "entity_name",
                                entityName,
                                "namespace",
                                namespace,
                                "topic_name",
                                topicName,
                                "subscription_name",
                                subscriptionName,
                                "environment",
                                serviceBusProperties.getEnvironment());

            // Register active messages metric
            registerSubscriptionMetric("sub_active_",
                                       "azure_servicebus_active_messages",
                                       topicName,
                                       subscriptionName,
                                       namespace,
                                       tags,
                                       "Number of active messages in the subscription",
                                       SubscriptionMetric::getActiveMessages);

            // Register dead letter messages metric
            registerSubscriptionMetric("sub_deadletter_",
                                       "azure_servicebus_dead_letter_messages",
                                       topicName,
                                       subscriptionName,
                                       namespace,
                                       tags,
                                       "Number of dead letter messages in the subscription",
                                       SubscriptionMetric::getDeadLetterMessages);

            log.info("Registered metrics for subscription: {}/{}", topicName, subscriptionName);
        }

        // Register namespace metrics
        for (NamespaceMetric ns : serviceBusClientService.getNamespaceMetrics()) {
            String namespace = ns.getNamespace();
            String environment = serviceBusProperties.getEnvironment();

            Tags tags = Tags.of(
                "namespace", namespace,
                "environment", environment
            );

            String metricId = "namespace_connections_" + namespace;
            if (!registeredMetrics.containsKey(metricId)) {
                Gauge.builder("azure_servicebus_active_connections",
                              () -> serviceBusClientService.getNamespaceMetrics()
                                                           .stream()
                                                           .filter(n -> n.getNamespace().equals(namespace))
                                                           .mapToLong(NamespaceMetric::getActiveConnections)
                                                           .findFirst()
                                                           .orElse(0))
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
                    final String finalQuotaName = quotaName;
                    Gauge.builder("azure_servicebus_quota_usage_percentage",
                                  () -> serviceBusClientService.getNamespaceMetrics()
                                                               .stream()
                                                               .filter(n -> n.getNamespace().equals(namespace))
                                                               .filter(n -> n.getQuotaUsage().containsKey(finalQuotaName))
                                                               .map(n -> n.getQuotaUsage().get(finalQuotaName))
                                                               .findFirst()
                                                               .orElse(0.0))
                         .tags(quotaTags)
                         .description("Percentage of quota used")
                         .register(meterRegistry);
                    registeredMetrics.put(metricId, true);
                }
            }

            log.info("Registered metrics for namespace: {}", namespace);
        }
    }

    private void registerQueueMetric(String metricPrefix,
                                     String metricName,
                                     String queueName,
                                     String namespace,
                                     Tags tags,
                                     String desc,
                                     ToLongFunction<QueueMetric> valueFunction) {
        String metricId = metricPrefix + namespace + "_" + queueName;

        Gauge.builder(metricName,
                      () -> serviceBusClientService.getQueueMetrics()
                                                   .stream()
                                                   .filter(q -> q.getName().equals(queueName) && q.getNamespace().equals(namespace))
                                                   .mapToLong(valueFunction)
                                                   .findFirst()
                                                   .orElse(0)).tags(tags).description(desc).register(meterRegistry);

        registeredMetrics.put(metricId, true);
    }

    private void registerTopicMetric(String metricPrefix,
                                     String metricName,
                                     String topicName,
                                     String namespace,
                                     Tags tags,
                                     String desc,
                                     ToLongFunction<TopicMetric> valueFunction) {
        String metricId = metricPrefix + namespace + "_" + topicName;

        Gauge.builder(metricName,
                      () -> serviceBusClientService.getTopicMetrics()
                                                   .stream()
                                                   .filter(t -> t.getName().equals(topicName) && t.getNamespace().equals(namespace))
                                                   .mapToLong(valueFunction)
                                                   .findFirst()
                                                   .orElse(0)).tags(tags).description(desc).register(meterRegistry);

        registeredMetrics.put(metricId, true);

    }

    /**
     * Generic method to register a subscription metric.
     */
    private void registerSubscriptionMetric(String metricPrefix,
                                            String metricName,
                                            String topicName,
                                            String subscriptionName,
                                            String namespace,
                                            Tags tags,
                                            String desc,
                                            ToLongFunction<SubscriptionMetric> valueFunction) {
        String metricId = metricPrefix + namespace + "_" + topicName + "_" + subscriptionName;

        Gauge.builder(metricName,
                      () -> serviceBusClientService.getSubscriptionMetrics()
                                                   .stream()
                                                   .filter(s -> s.getTopicName().equals(topicName) && s.getName().equals(subscriptionName)
                                                       && s.getNamespace().equals(namespace))
                                                   .mapToLong(valueFunction)
                                                   .findFirst()
                                                   .orElse(0)).tags(tags).description(desc).register(meterRegistry);

        registeredMetrics.put(metricId, true);
    }

    @Scheduled(fixedDelayString = "${azure.servicebus.metrics.scrape-interval:60000}")
    public void collectMetrics() {
        log.info("Scheduled metric collection started");
        serviceBusClientService.collectMetrics();
        log.info("Metrics collected successfully.");
    }
}