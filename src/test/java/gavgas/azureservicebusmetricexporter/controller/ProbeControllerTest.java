package gavgas.azureservicebusmetricexporter.controller;

import gavgas.azureservicebusmetricexporter.model.NamespaceMetric;
import gavgas.azureservicebusmetricexporter.model.QueueMetric;
import gavgas.azureservicebusmetricexporter.model.SubscriptionMetric;
import gavgas.azureservicebusmetricexporter.model.TopicMetric;
import gavgas.azureservicebusmetricexporter.service.ServiceBusClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProbeControllerTest {

    @Mock
    private ServiceBusClientService serviceBusClientService;

    @InjectMocks
    private ProbeController probeController;

    private QueueMetric testQueueMetric;
    private TopicMetric testTopicMetric;
    private SubscriptionMetric testSubscriptionMetric;
    private NamespaceMetric testNamespaceMetric;

    @BeforeEach
    void setUp() {
        // Test verilerini oluştur
        testQueueMetric = QueueMetric.builder()
                                     .namespace("testnamespace")
                                     .name("test-queue")
                                     .activeMessages(10)
                                     .deadLetterMessages(2)
                                     .scheduledMessages(1)
                                     .sizeBytes(1024)
                                     .totalMessages(13)
                                     .build();

        testTopicMetric = TopicMetric.builder()
                                     .namespace("testnamespace")
                                     .name("test-topic")
                                     .sizeBytes(2048)
                                     .subscriptionCount(3)
                                     .build();

        testSubscriptionMetric = SubscriptionMetric.builder()
                                                   .namespace("testnamespace")
                                                   .topicName("test-topic")
                                                   .name("test-subscription")
                                                   .activeMessages(5)
                                                   .deadLetterMessages(1)
                                                   .build();

        Map<String, Double> quotaUsage = new HashMap<>();
        quotaUsage.put("messages", 45.5);
        testNamespaceMetric = NamespaceMetric.builder()
                                             .namespace("testnamespace")
                                             .activeConnections(3)
                                             .quotaUsage(quotaUsage)
                                             .build();
    }

    @Test
    void getProbeMetrics_ShouldReturnAllMetrics() {
        // Given
        List<QueueMetric> queueMetrics = Collections.singletonList(testQueueMetric);
        List<TopicMetric> topicMetrics = Collections.singletonList(testTopicMetric);
        List<SubscriptionMetric> subscriptionMetrics = Collections.singletonList(testSubscriptionMetric);
        List<NamespaceMetric> namespaceMetrics = Collections.singletonList(testNamespaceMetric);

        when(serviceBusClientService.getQueueMetrics()).thenReturn(queueMetrics);
        when(serviceBusClientService.getTopicMetrics()).thenReturn(topicMetrics);
        when(serviceBusClientService.getSubscriptionMetrics()).thenReturn(subscriptionMetrics);
        when(serviceBusClientService.getNamespaceMetrics()).thenReturn(namespaceMetrics);

        // When
        ResponseEntity<?> response = probeController.getProbeMetrics();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(serviceBusClientService).collectMetrics();
        verify(serviceBusClientService).getQueueMetrics();
        verify(serviceBusClientService).getTopicMetrics();
        verify(serviceBusClientService).getSubscriptionMetrics();
        verify(serviceBusClientService).getNamespaceMetrics();
    }

    @Test
    void getProbeMetricsList_ShouldReturnEntityList() {
        // Given
        List<QueueMetric> queueMetrics = Collections.singletonList(testQueueMetric);
        List<TopicMetric> topicMetrics = Collections.singletonList(testTopicMetric);
        List<SubscriptionMetric> subscriptionMetrics = Collections.singletonList(testSubscriptionMetric);

        when(serviceBusClientService.getQueueMetrics()).thenReturn(queueMetrics);
        when(serviceBusClientService.getTopicMetrics()).thenReturn(topicMetrics);
        when(serviceBusClientService.getSubscriptionMetrics()).thenReturn(subscriptionMetrics);

        // When
        ResponseEntity<?> response = probeController.getProbeMetricsList();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(serviceBusClientService).collectMetrics();
        verify(serviceBusClientService).getQueueMetrics();
        verify(serviceBusClientService).getTopicMetrics();
        verify(serviceBusClientService).getSubscriptionMetrics();
    }

    @Test
    void getProbeMetricsResource_WithQueueType_ShouldReturnQueueMetrics() {
        // Given
        List<QueueMetric> queueMetrics = Collections.singletonList(testQueueMetric);
        when(serviceBusClientService.getQueueMetrics()).thenReturn(queueMetrics);

        // When
        ResponseEntity<?> response = probeController.getProbeMetricsResource("queue", "test-queue");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(serviceBusClientService).collectMetrics();
        verify(serviceBusClientService).getQueueMetrics();
    }

    @Test
    void getProbeMetricsResource_WithTopicType_ShouldReturnTopicMetrics() {
        // Given
        List<TopicMetric> topicMetrics = Collections.singletonList(testTopicMetric);
        when(serviceBusClientService.getTopicMetrics()).thenReturn(topicMetrics);

        // When
        ResponseEntity<?> response = probeController.getProbeMetricsResource("topic", "test-topic");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(serviceBusClientService).collectMetrics();
        verify(serviceBusClientService).getTopicMetrics();
    }

    @Test
    void getProbeMetricsResource_WithSubscriptionType_ShouldReturnSubscriptionMetrics() {
        // Given
        List<SubscriptionMetric> subscriptionMetrics = Collections.singletonList(testSubscriptionMetric);
        when(serviceBusClientService.getSubscriptionMetrics()).thenReturn(subscriptionMetrics);

        // When
        ResponseEntity<?> response = probeController.getProbeMetricsResource("subscription", "test-topic/test-subscription");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(serviceBusClientService).collectMetrics();
        verify(serviceBusClientService).getSubscriptionMetrics();
    }

    @Test
    void getProbeMetricsResource_WithNonexistentEntity_ShouldReturn404() {
        // Given
        List<QueueMetric> emptyList = new ArrayList<>();
        when(serviceBusClientService.getQueueMetrics()).thenReturn(emptyList);

        // When
        ResponseEntity<?> response = probeController.getProbeMetricsResource("queue", "non-existent-queue");

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(serviceBusClientService).collectMetrics();
        verify(serviceBusClientService).getQueueMetrics();
    }

    @Test
    void getProbeMetricsResource_WithInvalidSubscriptionFormat_ShouldReturn404() {
        // When
        ResponseEntity<?> response = probeController.getProbeMetricsResource("subscription", "invalid-format");

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(serviceBusClientService).collectMetrics();
    }

    @Test
    void getProbeMetricsResource_WithNullParameters_ShouldReturnBadRequest() {
        // When
        ResponseEntity<?> response = probeController.getProbeMetricsResource(null, null);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void getProbeMetricsResource_WithInvalidType_ShouldReturnNotFound() {
        // When
        ResponseEntity<?> response = probeController.getProbeMetricsResource("invalid", "test-name");

        // Then
        // ProbeController sınıfı şu anda NOT_FOUND dönüyor, test adı bir önceki davranışa göre oluşturulmuş olabilir
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}