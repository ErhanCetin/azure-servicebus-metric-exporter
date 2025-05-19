package gavgas.azureservicebusmetricexporter.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopicMetric {

    private String namespace;
    private String name;
    private long sizeBytes;
    private long maxSizeBytes;
    private long subscriptionCount;
}