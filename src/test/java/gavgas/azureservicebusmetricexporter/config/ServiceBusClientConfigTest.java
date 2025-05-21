package gavgas.azureservicebusmetricexporter.config;

import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceBusClientConfigTest {

    @Mock
    private ServiceBusProperties serviceBusProperties;

    @Mock
    private ServiceBusProperties.Auth auth;

    @InjectMocks
    private ServiceBusClientConfig serviceBusClientConfig;

    @Test
    void extractNamespaceFromConnectionString_WithValidString_ShouldExtractNamespace() {
        // Given
        String connectionString = "Endpoint=sb://testnamespace.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=abc123";

        // When
        String result = ReflectionTestUtils.invokeMethod(serviceBusClientConfig, "extractNamespaceFromConnectionString", connectionString);

        // Then
        assertEquals("testnamespace", result);
    }

    @Test
    void extractNamespaceFromConnectionString_WithInvalidString_ShouldReturnNull() {
        // Given
        String connectionString = "InvalidConnectionString";

        // When
        String result = ReflectionTestUtils.invokeMethod(serviceBusClientConfig, "extractNamespaceFromConnectionString", connectionString);

        // Then
        assertNull(result);
    }

    @Test
    void serviceBusAdministrationClient_WithEmptyConnectionString_ShouldThrowException() {
        // Given
        when(serviceBusProperties.getAuth()).thenReturn(auth);
        when(auth.getConnectionString()).thenReturn("");

        // When & Then
        Exception exception = assertThrows(IllegalArgumentException.class,
                                           () -> serviceBusClientConfig.serviceBusAdministrationClient());
        assertEquals("Azure Service Bus connection string is required", exception.getMessage());
    }

    @Test
    void serviceBusAdministrationClient_WithInvalidConnectionString_ShouldThrowException() {
        // Given
        when(serviceBusProperties.getAuth()).thenReturn(auth);
        when(auth.getConnectionString()).thenReturn("InvalidConnectionString");

        // When & Then
        Exception exception = assertThrows(IllegalArgumentException.class,
                                           () -> serviceBusClientConfig.serviceBusAdministrationClient());
        assertTrue(exception.getMessage().contains("Could not extract namespace from connection string"));
    }
}