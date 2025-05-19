package gavgas.azureservicebusmetricexporter.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriptionMetric {
    private String namespace;
    private String topicName;
    private String name;
    private long activeMessages;
    private long deadLetterMessages;
    private long scheduledMessages;
    private long transferMessages;
    private long transferDeadLetterMessages;
}
