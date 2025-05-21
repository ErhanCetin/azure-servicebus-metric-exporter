package gavgas.azureservicebusmetricexporter;


import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "azure.servicebus.auth.connection-string=Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=test"
})
class AzureServicebusMetricExporterApplicationTests {

    @Test
    void contextLoads() {
    }
}