package gavgas.azureservicebusmetricexporter.service;

import com.azure.core.http.rest.PagedIterable;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.models.*;
import gavgas.azureservicebusmetricexporter.config.ServiceBusClientConfig;
import gavgas.azureservicebusmetricexporter.config.ServiceBusProperties;
import gavgas.azureservicebusmetricexporter.model.QueueMetric;
import gavgas.azureservicebusmetricexporter.model.TopicMetric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // Gereksiz stub uyarılarını önlemek için
class ServiceBusClientServiceTest {

    @Mock
    private ServiceBusAdministrationClient adminClient;

    @Mock
    private ServiceBusProperties serviceBusProperties;

    @Mock
    private ServiceBusClientConfig clientConfig;

    @Mock
    private ServiceBusProperties.Entities entities;

    @Mock
    private ServiceBusProperties.Metrics metrics;

    @Mock
    private PagedIterable<QueueProperties> queuePropertiesPagedIterable;

    @Mock
    private PagedIterable<TopicProperties> topicPropertiesPagedIterable;

    @Mock
    private PagedIterable<SubscriptionProperties> subscriptionPropertiesPagedIterable;

    @InjectMocks
    private ServiceBusClientService serviceBusClientService;

    @BeforeEach
    void setUp() {
        // Temel yapılandırma - tüm testlerde kullanılacak
        when(serviceBusProperties.getEntities()).thenReturn(entities);
        when(serviceBusProperties.getMetrics()).thenReturn(metrics);
        when(metrics.getCacheDuration()).thenReturn(Duration.ofMinutes(1));

        // Varsayılan entity filtresi
        when(entities.getCompiledFilter()).thenReturn(Pattern.compile(".*"));

        // Desteklenen entity tipleri
        Set<String> entityTypes = new HashSet<>();
        entityTypes.add("queue");
        entityTypes.add("topic");
        entityTypes.add("subscription");
        when(entities.getTypes()).thenReturn(entityTypes);

        // Temel özellikler
        when(clientConfig.getNamespace()).thenReturn("testnamespace");
        when(serviceBusProperties.getEnvironment()).thenReturn("test");
        when(serviceBusProperties.isIncludeNamespaceMetrics()).thenReturn(true);
    }

    @Test
    void collectMetrics_WithQueuesAndTopics_ShouldPopulateMetrics() {
        // Çok detaylı debug için loglama
        System.out.println("Test başlangıcı: collectMetrics_WithQueuesAndTopics_ShouldPopulateMetrics");

        // GIVEN ===================================

        // 1. Önce temel mock davranışları
        // Entity filtresi ve tipleri
        when(entities.getCompiledFilter()).thenReturn(Pattern.compile(".*"));

        Set<String> entityTypes = new HashSet<>();
        entityTypes.add("queue");
        entityTypes.add("topic");
        entityTypes.add("subscription");
        when(entities.getTypes()).thenReturn(entityTypes);

        // Temel servis özellikleri
        when(clientConfig.getNamespace()).thenReturn("testnamespace");
        when(serviceBusProperties.getEnvironment()).thenReturn("test");
        when(serviceBusProperties.isIncludeNamespaceMetrics()).thenReturn(true);

        // 2. Şimdi queue için mock davranışı

        // QueueRuntimeProperties oluşturma
        QueueRuntimeProperties queueRuntimeProps = mock(QueueRuntimeProperties.class);
        when(queueRuntimeProps.getName()).thenReturn("test-queue");
        when(queueRuntimeProps.getActiveMessageCount()).thenReturn(10); // int
        when(queueRuntimeProps.getDeadLetterMessageCount()).thenReturn(2); // int
        when(queueRuntimeProps.getScheduledMessageCount()).thenReturn(1); // int
        when(queueRuntimeProps.getTransferMessageCount()).thenReturn(0); // int
        when(queueRuntimeProps.getTransferDeadLetterMessageCount()).thenReturn(0); // int
        when(queueRuntimeProps.getTotalMessageCount()).thenReturn(13L); // long
        when(queueRuntimeProps.getSizeInBytes()).thenReturn(1024L); // long
        when(queueRuntimeProps.getCreatedAt()).thenReturn(OffsetDateTime.now());
        when(queueRuntimeProps.getUpdatedAt()).thenReturn(OffsetDateTime.now());
        when(queueRuntimeProps.getAccessedAt()).thenReturn(OffsetDateTime.now());

        // API yaklaşımı: forEach mock'lama
        doAnswer(invocation -> {
            System.out.println("QueuePropertiesPagedIterable.forEach çağrıldı");

            Object arg = invocation.getArgument(0);
            if (arg instanceof java.util.function.Consumer) {
                @SuppressWarnings("unchecked")
                java.util.function.Consumer<QueueProperties> consumer =
                    (java.util.function.Consumer<QueueProperties>) arg;

                System.out.println("Queue consumer bulundu, test-queue oluşturuluyor");

                QueueProperties queueProps = mock(QueueProperties.class);
                when(queueProps.getName()).thenReturn("test-queue");
                // Tam veri tipleriyle daha dikkatli mock'lama
                when(queueProps.getMaxSizeInMegabytes()).thenReturn(1024L); // long olarak

                System.out.println("Consumer'a test-queue iletiliyor");
                consumer.accept(queueProps);
            }
            return null;
        }).when(queuePropertiesPagedIterable).forEach(any());

        // Test amacıyla doğrudan getQueueRuntimeProperties mock'la
        when(adminClient.getQueueRuntimeProperties("test-queue")).thenReturn(queueRuntimeProps);

        // Benzer şekilde topic mock'lama
        TopicRuntimeProperties topicRuntimeProps = mock(TopicRuntimeProperties.class);
        when(topicRuntimeProps.getSizeInBytes()).thenReturn(2048L);
        when(topicRuntimeProps.getSubscriptionCount()).thenReturn(2);
        when(topicRuntimeProps.getCreatedAt()).thenReturn(OffsetDateTime.now());
        when(topicRuntimeProps.getUpdatedAt()).thenReturn(OffsetDateTime.now());
        when(topicRuntimeProps.getAccessedAt()).thenReturn(OffsetDateTime.now());

        doAnswer(invocation -> {
            System.out.println("TopicPropertiesPagedIterable.forEach çağrıldı");

            Object arg = invocation.getArgument(0);
            if (arg instanceof java.util.function.Consumer) {
                @SuppressWarnings("unchecked")
                java.util.function.Consumer<TopicProperties> consumer =
                    (java.util.function.Consumer<TopicProperties>) arg;

                TopicProperties topicProps = mock(TopicProperties.class);
                when(topicProps.getName()).thenReturn("test-topic");
                when(topicProps.getMaxSizeInMegabytes()).thenReturn(2048L); // long olarak

                consumer.accept(topicProps);
            }
            return null;
        }).when(topicPropertiesPagedIterable).forEach(any());

        when(adminClient.getTopicRuntimeProperties("test-topic")).thenReturn(topicRuntimeProps);

        // Subscription mock'lama sadece boş bir listeyle
        doAnswer(invocation -> {
            // Boş bir şey yapma
            return null;
        }).when(subscriptionPropertiesPagedIterable).forEach(any());

        // Admin client'dan listQueues/listTopics için mock'lar
        when(adminClient.listQueues()).thenReturn(queuePropertiesPagedIterable);
        when(adminClient.listTopics()).thenReturn(topicPropertiesPagedIterable);
        when(adminClient.listSubscriptions(anyString())).thenReturn(subscriptionPropertiesPagedIterable);

        // WHEN ===================================
        System.out.println("collectMetrics çağrılıyor...");
        serviceBusClientService.collectMetrics();
        System.out.println("collectMetrics tamamlandı");

        // THEN ===================================
        List<QueueMetric> queueMetrics = serviceBusClientService.getQueueMetrics();
        System.out.println("Queue metrikleri sayısı: " + queueMetrics.size());

        // Queue listesinin içeriğini detaylı görmek için
        if (queueMetrics.isEmpty()) {
            System.out.println("!!! UYARI: Queue metrik listesi boş!");
        } else {
            for (int i = 0; i < queueMetrics.size(); i++) {
                QueueMetric metric = queueMetrics.get(i);
                System.out.println("Queue metric #" + i + ": name=" + metric.getName() +
                                       ", namespace=" + metric.getNamespace() +
                                       ", activeMessages=" + metric.getActiveMessages());
            }
        }

        assertEquals(1, queueMetrics.size(), "Queue metrics listesi tam olarak bir eleman içermelidir");

        // Diğer iddialar
        if (!queueMetrics.isEmpty()) {
            QueueMetric queueMetric = queueMetrics.get(0);
            assertEquals("test-queue", queueMetric.getName(), "Queue adı doğru olmalı");
            assertEquals(13, queueMetric.getTotalMessages(), "Toplam mesaj sayısı doğru olmalı");
        }

        // Topic metrikleri kontrolü
        List<TopicMetric> topicMetrics = serviceBusClientService.getTopicMetrics();
        System.out.println("Topic metrikleri sayısı: " + topicMetrics.size());

        assertEquals(1, topicMetrics.size(), "Topic metrics listesi tam olarak bir eleman içermelidir");

        System.out.println("Test tamamlandı");
    }

    @Test
    void collectMetrics_WhenEntityFiltered_ShouldNotAddToMetrics() {
        // Given
        when(entities.getCompiledFilter()).thenReturn(Pattern.compile("allowed-.*"));

        // Filtered queue mock
        QueueProperties queueProperties = mock(QueueProperties.class);
        when(queueProperties.getName()).thenReturn("filtered-queue");

        List<QueueProperties> queuesList = new ArrayList<>();
        queuesList.add(queueProperties);

        // Mock listeler hazırlama
        when(queuePropertiesPagedIterable.iterator()).thenReturn(queuesList.iterator());
        when(adminClient.listQueues()).thenReturn(queuePropertiesPagedIterable);

        // Boş topic listesi mock'lama
        when(adminClient.listTopics()).thenReturn(topicPropertiesPagedIterable);
        when(topicPropertiesPagedIterable.iterator()).thenReturn(new ArrayList<TopicProperties>().iterator());

        // When
        serviceBusClientService.collectMetrics();

        // Then
        List<QueueMetric> queueMetrics = serviceBusClientService.getQueueMetrics();
        assertEquals(0, queueMetrics.size());

        // QueueRuntimeProperties'e çağrı olmamalı
        verify(adminClient, never()).getQueueRuntimeProperties(any());
    }

    @Test
    void collectMetrics_WhenQueueTypeDisabled_ShouldNotCollectQueueMetrics() {
        // Given
        Set<String> entityTypes = new HashSet<>();
        entityTypes.add("topic"); // Sadece topic aktif, queue devre dışı
        when(entities.getTypes()).thenReturn(entityTypes);

        // Boş topic listesi mock'lama
        when(adminClient.listTopics()).thenReturn(topicPropertiesPagedIterable);
        when(topicPropertiesPagedIterable.iterator()).thenReturn(new ArrayList<TopicProperties>().iterator());

        // When
        serviceBusClientService.collectMetrics();

        // Then
        // listQueues() metodu hiç çağrılmamalı
        verify(adminClient, never()).listQueues();
    }
}