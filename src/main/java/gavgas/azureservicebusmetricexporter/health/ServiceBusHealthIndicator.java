package gavgas.azureservicebusmetricexporter.health;

import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import gavgas.azureservicebusmetricexporter.service.ServiceBusClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for Azure Service Bus
 */
@Component
@Slf4j
public class ServiceBusHealthIndicator implements HealthIndicator {

    private final ServiceBusAdministrationClient adminClient;
    private final ServiceBusClientService serviceBusClientService;

    public ServiceBusHealthIndicator(ServiceBusAdministrationClient adminClient,
                                     ServiceBusClientService serviceBusClientService) {
        this.adminClient = adminClient;
        this.serviceBusClientService = serviceBusClientService;
    }

    @Override
    public Health health() {
        try {
            // Check connectivity by listing queues
            adminClient.listQueues().stream().findFirst();

            return Health.up()
                         .withDetail("status", "connected")
                         .withDetail("queueCount", serviceBusClientService.getQueueMetrics().size())
                         .withDetail("topicCount", serviceBusClientService.getTopicMetrics().size())
                         .withDetail("subscriptionCount", serviceBusClientService.getSubscriptionMetrics().size())
                         .build();
        } catch (Exception e) {
            log.warn("Service Bus health check failed", e);
            return Health.down()
                         .withDetail("status", "disconnected")
                         .withDetail("error", e.getMessage())
                         .build();
        }
    }
}