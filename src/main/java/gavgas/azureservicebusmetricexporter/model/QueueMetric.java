package gavgas.azureservicebusmetricexporter.model;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class QueueMetric {
    private String namespace;
    private String name;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime accessedAt;
    private long totalMessages;
    private long activeMessages;
    private long deadLetterMessages;
    private long scheduledMessages;
    private long transferMessages;
    private long transferDeadLetterMessages;
    private long sizeBytes;
    private long maxSizeBytes;
}