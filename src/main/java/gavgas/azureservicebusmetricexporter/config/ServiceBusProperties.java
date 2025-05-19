package gavgas.azureservicebusmetricexporter.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Configuration properties for Azure Service Bus exporter.
 * Maps to the 'azure.servicebus' prefix in application.yml.
 */
@Data
@Configuration
@Validated  // Doğrulama etkinleştir
@ConfigurationProperties(prefix = "azure.servicebus")
public class ServiceBusProperties {

    /**
     * Authentication settings for Azure Service Bus.
     */
    private Auth auth = new Auth();

    /**
     * Entity filter settings for queues, topics, and subscriptions.
     */
    private Entities entities = new Entities();

    /**
     * Metrics collection and formatting settings.
     */
    private Metrics metrics = new Metrics();


    /**
     * Whether to include namespace-level metrics.
     */
    private boolean includeNamespaceMetrics = true;

    /**
     * Authentication settings for Azure Service Bus.
     */
    @Data
    public static class Auth {
        /**
         * Authentication mode. Currently only 'connection_string' is supported.
         */
        private String mode = "connection_string";

        /**
         * Service Bus connection string.
         * Format: Endpoint=sb://namespace.servicebus.windows.net/;SharedAccessKeyName=keyName;SharedAccessKey=keyValue
         */
        @NotBlank(message = "Azure Service Bus connection string is required")
        private String connectionString;
    }

    /**
     * Entity filter settings.
     */
    @Data
    public static class Entities {
        /**
         * Regular expression pattern for filtering entities by name.
         * Default: ".*" (include all entities)
         */
        private String filter = ".*";

        /**
         * Types of entities to collect metrics for.
         * Possible values: "queue", "topic", "subscription"
         */
        private Set<String> types = Set.of("queue", "topic", "subscription");

        // Compiled pattern for efficient filtering
        private Pattern compiledFilter;

        /**
         * Gets the compiled regex pattern for entity filtering.
         * @return Compiled Pattern object
         */
        public Pattern getCompiledFilter() {
            if (compiledFilter == null) {
                compiledFilter = Pattern.compile(filter);
            }
            return compiledFilter;
        }
    }

    /**
     * Metrics collection and formatting settings.
     */
    @Data
    public static class Metrics {
        /**
         * Prefix for metric names in Prometheus format.
         * Default: "azure_servicebus"
         */
        private String namespace = "azure_servicebus";

        /**
         * Duration to cache metrics before requesting fresh data.
         * Default: 1 minute
         */
        private Duration cacheDuration = Duration.ofMinutes(1);

        /**
         * Interval between metric collection operations.
         * Default: 1 minute
         */
        private Duration scrapeInterval = Duration.ofMinutes(1);
    }
}