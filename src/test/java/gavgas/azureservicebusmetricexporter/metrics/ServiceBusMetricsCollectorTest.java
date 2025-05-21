package gavgas.azureservicebusmetricexporter.metrics;

import gavgas.azureservicebusmetricexporter.config.ServiceBusProperties;
import gavgas.azureservicebusmetricexporter.model.QueueMetric;
import gavgas.azureservicebusmetricexporter.service.ServiceBusClientService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServiceBusMetricsCollectorTest {

    @Mock
    private ServiceBusClientService serviceBusClientService;

    @Mock
    private ServiceBusProperties serviceBusProperties;

    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {

        meterRegistry = new SimpleMeterRegistry();

        when(serviceBusProperties.getEnvironment()).thenReturn("test");

        when(serviceBusClientService.getQueueMetrics()).thenReturn(Collections.emptyList());
        when(serviceBusClientService.getTopicMetrics()).thenReturn(Collections.emptyList());
        when(serviceBusClientService.getSubscriptionMetrics()).thenReturn(Collections.emptyList());
        when(serviceBusClientService.getNamespaceMetrics()).thenReturn(Collections.emptyList());
    }

    @Test
    void init_ShouldRegisterTestGauge() {
        // When
        ServiceBusMetricsCollector collector = new ServiceBusMetricsCollector(meterRegistry, serviceBusClientService, serviceBusProperties);
        collector.init();

        // Then
        verify(serviceBusClientService).collectMetrics();
    }

    @Test
    void collectMetrics_ShouldCallServiceBusClientService() {
        // Given
        ServiceBusMetricsCollector collector = new ServiceBusMetricsCollector(meterRegistry, serviceBusClientService, serviceBusProperties);

        // When
        collector.collectMetrics();

        // Then
        verify(serviceBusClientService).collectMetrics();
    }

    @Test
    void init_WithSingleQueue_ShouldRegisterQueueMetrics() {
        List<QueueMetric> queueMetrics = new ArrayList<>();
        QueueMetric testQueue = QueueMetric.builder()
                                           .namespace("testnamespace")
                                           .name("test-queue")
                                           .activeMessages(10)
                                           .deadLetterMessages(2)
                                           .scheduledMessages(1)
                                           .sizeBytes(1024).totalMessages(13).build();
        queueMetrics.add(testQueue);

        when(serviceBusClientService.getQueueMetrics()).thenReturn(queueMetrics);

        // When
        ServiceBusMetricsCollector collector = new ServiceBusMetricsCollector(meterRegistry, serviceBusClientService, serviceBusProperties);
        collector.init();

        meterRegistry.getMeters()
                     .forEach(meter -> System.out.println("- " + meter.getId().getName() + ", tags: " + meter.getId().getTags()));

        assertNotNull(meterRegistry.find("azure_servicebus_active_messages")
                                   .tag("entity_type", "queue")
                                   .tag("entity_name", "test-queue")
                                   .gauge());

    }
}