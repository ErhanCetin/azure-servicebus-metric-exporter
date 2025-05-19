package gavgas.azureservicebusmetricexporter.config;

import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Configuration
public class ServiceBusClientConfig {

    private final ServiceBusProperties properties;
    private String namespace;

    public ServiceBusClientConfig(ServiceBusProperties properties) {
        this.properties = properties;
    }

    @Bean
    public ServiceBusAdministrationClient serviceBusAdministrationClient() {
        // Validate connection string
        String connectionString = properties.getAuth().getConnectionString();
        if (!StringUtils.hasText(connectionString)) {
            throw new IllegalArgumentException("Azure Service Bus connection string is required");
        }

        // Extract namespace from connection string
        this.namespace = extractNamespaceFromConnectionString(connectionString);
        if (this.namespace == null) {
            throw new IllegalArgumentException("Could not extract namespace from connection string. " +
                                                   "Make sure the connection string is in the format: " +
                                                   "Endpoint=sb://namespace.servicebus.windows.net/;...");
        }

        log.info("Using Service Bus namespace: {}", namespace);

        // Create and return the client
        return new ServiceBusAdministrationClientBuilder()
            .connectionString(connectionString)
            .buildClient();
    }

    /**
     * Extract namespace from connection string.
     * Typical format: Endpoint=sb://namespace.servicebus.windows.net/;...
     */
    private String extractNamespaceFromConnectionString(String connectionString) {
        String pattern = "Endpoint=sb://([^.]+)\\.servicebus\\.windows\\.net/";
        Matcher matcher = Pattern.compile(pattern).matcher(connectionString);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Get the extracted namespace.
     * This is useful for other components that need to know the namespace.
     */
    public String getNamespace() {
        return namespace;
    }
}