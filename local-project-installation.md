# Docker Kullanım Talimatları

Azure Service Bus Metric Exporter uygulamasını Docker ile çalıştırmak için aşağıdaki adımları izleyin.

## 1. Ön Koşullar

Aşağıdaki yazılımların sisteminizde yüklü olduğundan emin olun:

- Docker Engine
- Docker Compose
- JDK 21 (sadece uygulama yapılandırması için)

## 1.1 To start projects quickly in docker env or you can continue with step 2.
```bash
export ENVIRONMENT="dev"
export AZURE_SERVICEBUS_CONNECTION_STRING="Endpoint=sb://namespace.servicebus.windows.net/;SharedAccessKeyName=<your SharedAccessKeyName>;SharedAccessKey=<your SharedAccessKey>"
sh deploy-docker.sh -c "$AZURE_SERVICEBUS_CONNECTION_STRING"
```

## 2. Uygulama Derleme

İlk olarak, uygulamayı Gradle ile derleyin:

```bash
./gradlew clean build
```

Bu komut, `build/libs/` dizini altında bir JAR dosyası oluşturacaktır.

## 3. Docker İmajı Oluşturma (Manuel)

Docker imajını manuel olarak oluşturmak isterseniz:

```bash
docker build -t azure-servicebus-metric-exporter:latest .
```

## 4. Docker Compose ile Çalıştırma

Docker Compose, otomatik olarak imajı oluşturacak ve servisleri başlatacaktır:

```bash
# Azure Service Bus bağlantı dizesini ayarlayın
export AZURE_SERVICEBUS_CONNECTION_STRING="your-connection-string"
export ENVIRONMENT="dev"  # Opsiyonel, varsayılan: local

# Tüm servisleri (uygulama, Prometheus ve Grafana) başlatın
docker-compose up -d
```

Eğer sadece imajı oluşturmak isterseniz:

```bash
docker-compose build
```

## 5. Hizmetlere Erişim

Servisler çalıştıktan sonra, aşağıdaki URL'lerden erişebilirsiniz:

- Azure Service Bus Exporter: http://localhost:8080
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (kullanıcı: admin, şifre: admin)

## 6. Servisleri Durdurma

```bash
docker-compose down
```

Eğer volume verileri dahil tümünü silmek isterseniz:

```bash
docker-compose down -v
```

## 7. Hata Ayıklama ve Logları İzleme

```bash
# Tüm servislerin loglarını görüntüleme
docker-compose logs -f

# Sadece uygulama loglarını görüntüleme
docker-compose logs -f azure-servicebus-exporter
```

## 8. Sağlık Kontrolü

Uygulamanın çalışıp çalışmadığını kontrol etmek için:

```bash
curl http://localhost:8080/actuator/health
```