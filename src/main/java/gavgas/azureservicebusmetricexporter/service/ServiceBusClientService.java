package gavgas.azureservicebusmetricexporter.service;

import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.models.QueueRuntimeProperties;
import com.azure.messaging.servicebus.administration.models.SubscriptionRuntimeProperties;
import com.azure.messaging.servicebus.administration.models.TopicRuntimeProperties;
import gavgas.azureservicebusmetricexporter.config.ServiceBusClientConfig;
import gavgas.azureservicebusmetricexporter.config.ServiceBusProperties;
import gavgas.azureservicebusmetricexporter.model.NamespaceMetric;
import gavgas.azureservicebusmetricexporter.model.QueueMetric;
import gavgas.azureservicebusmetricexporter.model.SubscriptionMetric;
import gavgas.azureservicebusmetricexporter.model.TopicMetric;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ServiceBusClientService {
    private final ServiceBusAdministrationClient adminClient;
    private final ServiceBusProperties properties;
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();

    private List<QueueMetric> queueMetrics = new ArrayList<>();
    private List<TopicMetric> topicMetrics = new ArrayList<>();
    private List<SubscriptionMetric> subscriptionMetrics = new ArrayList<>();
    private List<NamespaceMetric> namespaceMetrics = new ArrayList<>();

    private Instant lastUpdate = Instant.EPOCH;
    private String namespace;
    private Pattern environmentFilter;

    public ServiceBusClientService(
        ServiceBusAdministrationClient adminClient,
        ServiceBusProperties properties,
        ServiceBusClientConfig clientConfig) {
        this.adminClient = adminClient;
        this.properties = properties;
        this.namespace = clientConfig.getNamespace();

        // Initialize environment filter pattern based on configured environment
        initEnvironmentFilter();

        log.info("ServiceBusClientService initialized for namespace: {}, environment: {}",
                 namespace, properties.getEnvironment());
    }

    /**
     * Initialize the environment filter pattern based on the configured environment
     */
    private void initEnvironmentFilter() {
        String environment = properties.getEnvironment();
        if (environment != null && !environment.isEmpty()) {
            // Create pattern that matches entity names starting with the environment value
            // e.g., if environment=dev, pattern will match "dev", "dev1", "dev-cdp", etc.
            String pattern = "^" + environment + ".*";
            environmentFilter = Pattern.compile(pattern);
            log.info("Environment filter initialized for environment '{}' with pattern: {}",
                     environment, pattern);
        } else {
            // If no environment is specified, accept all entity names
            environmentFilter = Pattern.compile(".*");
            log.warn("No environment specified, all entities will be considered");
        }
    }

    /**
     * Check if an entity name matches the environment filter
     * @param entityName The name of the entity to check
     * @return true if the entity name should be included, false otherwise
     */
    private boolean matchesEnvironmentFilter(String entityName) {
        if (entityName == null || entityName.isEmpty()) {
            return false;
        }

        boolean matches = environmentFilter.matcher(entityName).matches();
        if (!matches) {
            log.debug("Entity '{}' filtered out by environment filter ({})",
                      entityName, properties.getEnvironment());
        }
        return matches;
    }

    public void collectMetrics() {
        cacheLock.readLock().lock();
        try {
            Duration timeSinceLastUpdate = Duration.between(lastUpdate, Instant.now());
            if (timeSinceLastUpdate.compareTo(properties.getMetrics().getCacheDuration()) < 0 &&
                !lastUpdate.equals(Instant.EPOCH)) {
                log.debug("Using cached metrics, cache duration not expired yet");
                return;
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        cacheLock.writeLock().lock();
        try {
            log.info("Starting metric collection");

            // Clear metrics
            queueMetrics = new ArrayList<>();
            topicMetrics = new ArrayList<>();
            subscriptionMetrics = new ArrayList<>();
            namespaceMetrics = new ArrayList<>();

            // Collect metrics
            collectQueues();
            collectTopics();

            if (properties.isIncludeNamespaceMetrics()) {
                // Create basic namespace metrics (limited information available with connection string)
                Map<String, Double> quotaUsage = new HashMap<>();
                NamespaceMetric namespaceMetric = NamespaceMetric.builder()
                                                                 .namespace(namespace)
                                                                 .activeConnections(0) // Not available through admin client
                                                                 .quotaUsage(quotaUsage)
                                                                 .build();

                namespaceMetrics.add(namespaceMetric);
            }

            lastUpdate = Instant.now();
            log.info("Metric collection completed");
        } catch (Exception e) {
            log.error("Error collecting metrics", e);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    private void collectQueues() {
        Pattern entityFilter = properties.getEntities().getCompiledFilter();

        try {
            log.info("Collecting Service Bus queue metrics");

            // Check if queue metrics are enabled in configuration
            if (!properties.getEntities().getTypes().contains("queue")) {
                log.info("Queue metrics collection is disabled in configuration");
                return;  // Skip entirely if queue type is not enabled
            }

            adminClient.listQueues().forEach(queueProperties -> {
                String queueName = queueProperties.getName();

                // Apply both entity filter and environment filter
                if (!entityFilter.matcher(queueName).matches() ||
                    !matchesEnvironmentFilter(queueName)) {
                    return;
                }

                try {
                    QueueRuntimeProperties runtimeProps = adminClient.getQueueRuntimeProperties(queueName);

                    QueueMetric queueMetric = QueueMetric.builder()
                                                         .namespace(namespace)
                                                         .name(queueName)
                                                         .createdAt(runtimeProps.getCreatedAt())
                                                         .updatedAt(runtimeProps.getUpdatedAt())
                                                         .accessedAt(runtimeProps.getAccessedAt())
                                                         .totalMessages(runtimeProps.getTotalMessageCount())
                                                         .activeMessages(runtimeProps.getActiveMessageCount())
                                                         .deadLetterMessages(runtimeProps.getDeadLetterMessageCount())
                                                         .scheduledMessages(runtimeProps.getScheduledMessageCount())
                                                         .transferMessages(runtimeProps.getTransferMessageCount())
                                                         .transferDeadLetterMessages(runtimeProps.getTransferDeadLetterMessageCount())
                                                         .sizeBytes(runtimeProps.getSizeInBytes())
                                                         .maxSizeBytes(queueProperties.getMaxSizeInMegabytes() * 1024 * 1024L)
                                                         .build();

                    queueMetrics.add(queueMetric);
                    log.debug("Collected metrics for queue: {}", queueName);
                } catch (Exception e) {
                    log.warn("Failed to get metrics for queue {}: {}", queueName, e.getMessage());
                }
            });

            log.info("Collected metrics for {} queues", queueMetrics.size());
        } catch (Exception e) {
            log.error("Error collecting queue metrics", e);
        }
    }

    private void collectTopics() {
        Pattern entityFilter = properties.getEntities().getCompiledFilter();

        // Check if queue metrics are enabled in configuration
        if (!properties.getEntities().getTypes().contains("topic")) {
            log.info("Queue metrics collection is disabled in configuration");
            return;  // Skip entirely if topic type is not enabled
        }

        try {
            log.info("Collecting Service Bus topic metrics");
            adminClient.listTopics().forEach(topicProperties -> {
                String topicName = topicProperties.getName();

                // Apply both entity filter and environment filter
                if (!entityFilter.matcher(topicName).matches() ||
                    !matchesEnvironmentFilter(topicName)) {
                    return;
                }

                try {
                    TopicRuntimeProperties runtimeProps = adminClient.getTopicRuntimeProperties(topicName);

                    TopicMetric topicMetric = TopicMetric.builder()
                                                         .namespace(namespace)
                                                         .name(topicName)
                                                         .sizeBytes(runtimeProps.getSizeInBytes())
                                                         .maxSizeBytes(topicProperties.getMaxSizeInMegabytes() * 1024 * 1024L)
                                                         .subscriptionCount(runtimeProps.getSubscriptionCount())
                                                         .build();

                    topicMetrics.add(topicMetric);
                    log.debug("Collected metrics for topic: {}", topicName);

                    // Collect subscriptions for this topic
                    if (properties.getEntities().getTypes().contains("subscription")) {
                        collectSubscriptions(topicName, entityFilter);
                    }
                } catch (Exception e) {
                    log.warn("Failed to get metrics for topic {}: {}", topicName, e.getMessage());
                }
            });

            log.info("Collected metrics for {} topics and {} subscriptions",
                     topicMetrics.size(), subscriptionMetrics.size());
        } catch (Exception e) {
            log.error("Error collecting topic metrics", e);
        }
    }

    private void collectSubscriptions(String topicName, Pattern entityFilter) {
        // Skip subscriptions if the topic doesn't match environment filter
        if (!matchesEnvironmentFilter(topicName)) {
            return;
        }

        try {
            adminClient.listSubscriptions(topicName).forEach(subscriptionProperties -> {
                String subscriptionName = subscriptionProperties.getSubscriptionName();
                String entityName = topicName + "/" + subscriptionName;

                // Apply entity filter - environment already checked for topic
                if (!entityFilter.matcher(entityName).matches()) {
                    return;
                }

                try {
                    SubscriptionRuntimeProperties runtimeProps = adminClient.getSubscriptionRuntimeProperties(
                        topicName, subscriptionName);

                    SubscriptionMetric subscriptionMetric = SubscriptionMetric.builder()
                                                                              .namespace(namespace)
                                                                              .topicName(topicName)
                                                                              .name(subscriptionName)
                                                                              .activeMessages(runtimeProps.getActiveMessageCount())
                                                                              .deadLetterMessages(runtimeProps.getDeadLetterMessageCount())
                                                                              .scheduledMessages(0) // Not available in current API version
                                                                              .transferMessages(runtimeProps.getTransferMessageCount())
                                                                              .transferDeadLetterMessages(runtimeProps.getTransferDeadLetterMessageCount())
                                                                              .build();

                    subscriptionMetrics.add(subscriptionMetric);
                    log.debug("Collected metrics for subscription: {}/{}", topicName, subscriptionName);
                } catch (Exception e) {
                    log.warn("Failed to get metrics for subscription {}/{}: {}",
                             topicName, subscriptionName, e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Error collecting subscription metrics for topic {}", topicName, e);
        }
    }

    public List<QueueMetric> getQueueMetrics() {
        cacheLock.readLock().lock();
        try {
            return new ArrayList<>(queueMetrics);
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    public List<TopicMetric> getTopicMetrics() {
        cacheLock.readLock().lock();
        try {
            return new ArrayList<>(topicMetrics);
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    public List<SubscriptionMetric> getSubscriptionMetrics() {
        cacheLock.readLock().lock();
        try {
            return new ArrayList<>(subscriptionMetrics);
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    public List<NamespaceMetric> getNamespaceMetrics() {
        cacheLock.readLock().lock();
        try {
            return new ArrayList<>(namespaceMetrics);
        } finally {
            cacheLock.readLock().unlock();
        }
    }
}