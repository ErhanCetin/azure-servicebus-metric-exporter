package gavgas.azureservicebusmetricexporter.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for application status information
 */
@RestController
@Slf4j
public class StatusController {

    private final Instant startTime = Instant.now();
    private final BuildProperties buildProperties;

    public StatusController(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    /**
     * Returns application status information
     */
    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        String status = "ok";

        Map<String, String> buildInfo = new HashMap<>();
        buildInfo.put("version", buildProperties.getVersion());
        buildInfo.put("java_version", System.getProperty("java.version"));
        buildInfo.put("os", System.getProperty("os.name"));
        buildInfo.put("arch", System.getProperty("os.arch"));

        // Add uptime
        Duration uptime = Duration.between(startTime, Instant.now());

        response.put("status", status);
        response.put("timestamp", Instant.now().toString());
        response.put("uptime", formatDuration(uptime));
        response.put("build_info", buildInfo);

        return ResponseEntity.ok(response);
    }

    // Custom query UI for development
    @GetMapping(value = "/query", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getQueryUI() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Azure Service Bus Exporter Query UI</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        max-width: 800px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    h1 {
                        color: #0078d4;
                    }
                    .form-group {
                        margin-bottom: 15px;
                    }
                    label {
                        display: block;
                        margin-bottom: 5px;
                    }
                    input, select {
                        width: 100%;
                        padding: 8px;
                        box-sizing: border-box;
                    }
                    button {
                        background-color: #0078d4;
                        color: white;
                        padding: 10px 15px;
                        border: none;
                        cursor: pointer;
                    }
                    #result {
                        margin-top: 20px;
                        background-color: #f5f5f5;
                        padding: 15px;
                        border-radius: 5px;
                        white-space: pre-wrap;
                    }
                </style>
            </head>
            <body>
                <h1>Azure Service Bus Exporter Query UI</h1>
                
                <div class="form-group">
                    <label for="endpoint">Endpoint:</label>
                    <select id="endpoint">
                        <option value="/actuator/prometheus">Default Prometheus metrics</option>
                        <option value="/probe/metrics">Probe metrics</option>
                        <option value="/probe/metrics/list">Metrics list</option>
                        <option value="/probe/metrics/resource">Resource metrics</option>
                        <option value="/actuator/health">Health</option>
                        <option value="/status">Status</option>
                    </select>
                </div>
                
                <div class="form-group">
                    <label for="params">Query Parameters (optional):</label>
                    <input type="text" id="params" placeholder="name=value&name2=value2">
                </div>
                
                <button onclick="fetchMetrics()">Fetch Metrics</button>
                
                <div id="result"></div>
                
                <script>
                    function fetchMetrics() {
                        const endpoint = document.getElementById('endpoint').value;
                        const params = document.getElementById('params').value;
                        const url = endpoint + (params ? '?' + params : '');
                        
                        document.getElementById('result').textContent = 'Loading...';
                        
                        fetch(url)
                            .then(response => response.text())
                            .then(data => {
                                document.getElementById('result').textContent = data;
                            })
                            .catch(error => {
                                document.getElementById('result').textContent = 'Error: ' + error;
                            });
                    }
                </script>
            </body>
            </html>
            """;
    }

    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        return String.format("%d days, %d hours, %d minutes, %d seconds", days, hours, minutes, seconds);
    }

    @Data
    @AllArgsConstructor
    static class StatusResponse {
        private String status;
        private String timestamp;
        private String uptime;
        private Map<String, String> build_info;
    }
}
