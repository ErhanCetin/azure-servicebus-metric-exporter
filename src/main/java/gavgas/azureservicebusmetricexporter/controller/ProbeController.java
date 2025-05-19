package gavgas.azureservicebusmetricexporter.controller;

import gavgas.azureservicebusmetricexporter.model.NamespaceMetric;
import gavgas.azureservicebusmetricexporter.model.QueueMetric;
import gavgas.azureservicebusmetricexporter.model.SubscriptionMetric;
import gavgas.azureservicebusmetricexporter.model.TopicMetric;
import gavgas.azureservicebusmetricexporter.service.ServiceBusClientService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for custom Service Bus metric endpoints
 * Provides additional endpoints beyond standard Prometheus metrics
 */
@RestController
@RequestMapping(value ="/probe", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
public class ProbeController {

    private final ServiceBusClientService serviceBusClientService;

    public ProbeController(ServiceBusClientService serviceBusClientService) {
        this.serviceBusClientService = serviceBusClientService;
    }

    /**
     * Returns all Service Bus metrics in JSON format
     */
    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<MetricsResponse>> getProbeMetrics() {
        log.info("Request received for /probe/metrics");

        // Ensure metrics are up-to-date
        serviceBusClientService.collectMetrics();

        MetricsResponse metricsResponse = new MetricsResponse(
            serviceBusClientService.getQueueMetrics(),
            serviceBusClientService.getTopicMetrics(),
            serviceBusClientService.getSubscriptionMetrics(),
            serviceBusClientService.getNamespaceMetrics()
        );

        return ResponseEntity.ok(new ApiResponse<>("success", metricsResponse));
    }

    /**
     * Returns a list of all Service Bus entities
     */
    @GetMapping("/metrics/list")
    public ResponseEntity<ApiResponse<EntitiesResponse>> getProbeMetricsList() {
        log.info("Request received for /probe/metrics/list");

        // Ensure metrics are up-to-date
        serviceBusClientService.collectMetrics();

        List<String> queues = serviceBusClientService.getQueueMetrics().stream()
                                                     .map(QueueMetric::getName)
                                                     .collect(Collectors.toList());

        List<String> topics = serviceBusClientService.getTopicMetrics().stream()
                                                     .map(TopicMetric::getName)
                                                     .collect(Collectors.toList());

        List<Map<String, String>> subscriptions = serviceBusClientService.getSubscriptionMetrics().stream()
                                                                         .map(s -> {
                                                                             Map<String, String> map = new HashMap<>();
                                                                             map.put("topic", s.getTopicName());
                                                                             map.put("subscription", s.getName());
                                                                             return map;
                                                                         })
                                                                         .collect(Collectors.toList());

        EntitiesResponse entitiesResponse = new EntitiesResponse(queues, topics, subscriptions);

        return ResponseEntity.ok(new ApiResponse<>("success", entitiesResponse));
    }

    /**
     * Returns metrics for a specific entity (queue, topic, or subscription)
     */
    @GetMapping("/metrics/resource")
    public ResponseEntity<?> getProbeMetricsResource(
        @RequestParam("type") String entityType,
        @RequestParam("name") String entityName) {

        log.info("Request received for /probe/metrics/resource with type={}, name={}", entityType, entityName);

        if (entityType == null || entityName == null) {
            return ResponseEntity.badRequest().body(
                new ApiResponse<>("error", "Both 'type' and 'name' query parameters are required"));
        }

        // Ensure metrics are up-to-date
        serviceBusClientService.collectMetrics();

        Object result = switch(entityType.toLowerCase()) {
            case "queue" -> serviceBusClientService.getQueueMetrics().stream()
                                                   .filter(q -> q.getName().equals(entityName))
                                                   .findFirst();

            case "topic" -> serviceBusClientService.getTopicMetrics().stream()
                                                   .filter(t -> t.getName().equals(entityName))
                                                   .findFirst();

            case "subscription" -> {
                // For subscription, name should be in format "topicName/subscriptionName"
                String[] parts = entityName.split("/", 2);
                if (parts.length != 2) {
                    yield Optional.empty();
                }

                String topicName = parts[0];
                String subscriptionName = parts[1];

                yield serviceBusClientService.getSubscriptionMetrics().stream()
                                             .filter(s -> s.getTopicName().equals(topicName) && s.getName().equals(subscriptionName))
                                             .findFirst();
            }

            default -> Optional.empty();
        };

        if (result instanceof Optional<?> optional && optional.isEmpty()) {
            return ResponseEntity.status(404).body(
                new ApiResponse<>("error", entityType + " with name '" + entityName + "' not found"));
        }

        if (result instanceof Optional<?> optional) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("timestamp", Instant.now().toString());
            response.put("entity_type", entityType);
            response.put("entity_name", entityName);
            response.put("metrics", optional.get());

            return ResponseEntity.ok(response);
        }

        return ResponseEntity.badRequest().body(
            new ApiResponse<>("error", "Entity type must be one of: queue, topic, subscription"));
    }

    @Data
    @AllArgsConstructor
    static class ApiResponse<T> {
        private String status;
        private T data;
    }

    @Data
    @AllArgsConstructor
    static class MetricsResponse {
        private List<QueueMetric> queues;
        private List<TopicMetric> topics;
        private List<SubscriptionMetric> subscriptions;
        private List<NamespaceMetric> namespaces;
    }

    @Data
    @AllArgsConstructor
    static class EntitiesResponse {
        private List<String> queues;
        private List<String> topics;
        private List<Map<String, String>> subscriptions;
    }
}