
package gavgas.azureservicebusmetricexporter.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class NamespaceMetric {
    private String namespace;
    private long activeConnections;
    private Map<String, Double> quotaUsage;
}