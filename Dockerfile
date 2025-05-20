FROM eclipse-temurin:21-jre-alpine as runner

WORKDIR /app

# Create a group and user to run our app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the application JAR file
COPY build/libs/azure-servicebus-metric-exporter-*.jar app.jar

# Set necessary JVM options
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=85.0"

# Switch to non-root user
USER appuser

# Expose Prometheus metrics port
EXPOSE 8080

# Set a healthcheck
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]