# Azure Service Bus Metric Exporter Kurulum ve Kullanım Kılavuzu

Bu belge, Azure Service Bus Metric Exporter uygulamasını başarıyla çalıştırdıktan sonraki adımları ve erişilebilen servislerin URL'lerini içermektedir.

## İçindekiler

1. [Başlatma](#başlatma)
2. [Erişilebilen Servisler](#erişilebilen-servisler)
3. [Prometheus Kullanımı](#prometheus-kullanımı)
4. [Grafana Kurulumu](#grafana-kurulumu)
5. [Grafana Dashboard Oluşturma](#grafana-dashboard-oluşturma)
6. [Sorun Giderme](#sorun-giderme)

## Başlatma

Uygulamayı aşağıdaki komutla başarıyla başlattınız:

```bash
sh deploy-docker.sh -c "$AZURE_SERVICEBUS_CONNECTION_STRING"
```

Bu komut şunları yapar:
- Projeyi derler
- Docker imajını oluşturur
- Docker Compose ile 3 servisi başlatır:
   - azure-servicebus-exporter
   - prometheus
   - grafana

Servislerin durumunu aşağıdaki komutla kontrol edebilirsiniz:

```bash
docker ps
```

## Erişilebilen Servisler

### Azure Service Bus Metric Exporter

- **Ana Sayfa:** http://localhost:8080
- **API Endpointleri:**
   - Metrikler: http://localhost:8080/probe/metrics
   - Metrik Listesi: http://localhost:8080/probe/metrics/list
   - Kaynak Metriği: http://localhost:8080/probe/metrics/resource?type=queue&name=your-queue-name
   - Sağlık Durumu: http://localhost:8080/actuator/health
   - Prometheus Metrikleri: http://localhost:8080/actuator/prometheus
   - Uygulama Bilgisi: http://localhost:8080/actuator/info
   - Durum Bilgisi: http://localhost:8080/status
   - Query UI: http://localhost:8080/query

### Prometheus

- **Ana Sayfa:** http://localhost:9090
- **Hedefler:** http://localhost:9090/targets
- **Sorgular:** http://localhost:9090/graph

### Grafana

- **Ana Sayfa:** http://localhost:3000
- **Giriş Bilgileri:**
   - Kullanıcı adı: `admin`
   - Şifre: `admin`
   - İlk girişte şifre değiştirmeniz istenecektir

## Prometheus Kullanımı

Prometheus, metriklerinizi toplar ve depolayan zaman serisi veritabanıdır.

1. **Hedefleri Kontrol Etme**
   - http://localhost:9090/targets adresine gidin
   - `azure-servicebus-exporter` hedefinin `UP` durumunda olduğunu doğrulayın

2. **Metrik Sorgulama**
   - http://localhost:9090/graph adresine gidin
   - Expression alanına bir sorgu yazın, örneğin:
      - `azure_servicebus_active_messages` - Tüm aktif mesajları gösterir
      - `azure_servicebus_active_messages{entity_type="queue"}` - Sadece kuyruklardaki aktif mesajları gösterir
      - `azure_servicebus_active_messages{environment="dev"}` - Sadece dev ortamındaki aktif mesajları gösterir
   - "Execute" butonuna tıklayın
   - "Graph" veya "Table" sekmesine geçerek sonuçları görüntüleyin

3. **Yararlı Prometheus Sorguları**
   - `up{job="azure-servicebus-exporter"}` - Exporter'ın çalışıp çalışmadığını gösterir
   - `azure_servicebus_dead_letter_messages` - Dead letter mesajlarını gösterir
   - `azure_servicebus_size_bytes` - Queue boyutlarını gösterir
   - `sum by(environment) (azure_servicebus_active_messages)` - Ortam bazında toplam aktif mesajları gösterir
   - `sum by(entity_name) (azure_servicebus_active_messages)` - Kuyruk bazında toplam aktif mesajları gösterir

## Grafana Kurulumu

Grafana'yı aşağıdaki adımlarla yapılandırabilirsiniz:

1. **Grafana'ya Giriş**
   - http://localhost:3000 adresine gidin
   - Kullanıcı adı: `admin`
   - Şifre: `admin`
   - İlk girişte şifre değiştirmeniz istenecektir

2. **Prometheus Veri Kaynağını Ekleme**
   - Configuration (dişli simgesi) > Data Sources > Add data source
   - Prometheus'u seçin
   - Aşağıdaki ayarları yapın:
      - Name: `Prometheus`
      - URL: `http://prometheus:9090` (önemli: `localhost` yerine `prometheus` kullanın)
      - Access: Server (default)
      - Diğer ayarları varsayılan haliyle bırakın
   - "Save & Test" butonuna tıklayın
   - "Successfully queried the Prometheus API." mesajını görmelisiniz

## Grafana Dashboard Oluşturma

0. You can easily import json to [Grafana Dashboard JSON to import](grafana/azure-servicebus-dashboard.json) 

1. **Yeni Dashboard Oluşturma**
   - + (sol menü) > Dashboard > Add a new panel
   - Veri kaynağı olarak Prometheus'u seçin
   - Metrics Browser alanına bir sorgu yazın, örneğin:
      - `azure_servicebus_active_messages`
   - Panel ayarlarını yapılandırın:
      - Title: "Aktif Mesajlar"
      - Description: "Queue başına aktif mesaj sayısı"
   - "Apply" butonuna tıklayarak paneli ekleyin

2. **Önerilen Paneller**
   - **Aktif Mesaj Sayıları** (Time Series)
      - Metrics: `azure_servicebus_active_messages{entity_type="queue"}`
      - Legend: {{entity_name}} - {{environment}}

   - **Dead Letter Mesajları** (Time Series)
      - Metrics: `azure_servicebus_dead_letter_messages{entity_type="queue"}`
      - Legend: {{entity_name}} - {{environment}}

   - **Kuyruk Boyutu** (Bar Gauge)
      - Metrics: `azure_servicebus_size_bytes{entity_type="queue"}`
      - Unit: bytes (IEC)

   - **Ortam Bazlı Özet** (Pie Chart)
      - Metrics: `sum by(environment) (azure_servicebus_active_messages)`

   - **Genel Bakış Tablosu** (Table)
      - A: `azure_servicebus_active_messages{entity_type="queue"}`
      - B: `azure_servicebus_dead_letter_messages{entity_type="queue"}`
      - Transform: "Organize fields" ve sonra "Table"

3. **Template Variables Ekleme**
   - Dashboard settings (dişli simgesi) > Variables > New
   - Name: `environment`
   - Type: Query
   - Label: "Ortam"
   - Query: `label_values(azure_servicebus_active_messages, environment)`
   - Include All option: ON
   - Preview of values: Mevcut ortam değerlerini gösterir
   - Add butonuna tıklayın

4. **Dashboard Kaydetme**
   - Sağ üst köşedeki disk simgesine tıklayın
   - Name: "Azure Service Bus Metrics"
   - Folder: General (veya başka bir klasör)
   - "Save" butonuna tıklayın

## Sorun Giderme

### Service Bus Exporter Sorunları

**Sağlık Durumunu Kontrol Edin:**
```bash
curl http://localhost:8080/actuator/health
```

**Logları Kontrol Edin:**
```bash
docker logs azure-servicebus-exporter
```

### Prometheus Sorunları

**Hedefleri Kontrol Edin:**
- http://localhost:9090/targets adresine gidin
- `azure-servicebus-exporter` hedefinin durumunu kontrol edin

**Logları Kontrol Edin:**
```bash
docker logs prometheus
```

### Grafana Sorunları

**Veri Kaynağı Bağlantısını Kontrol Edin:**
- Configuration > Data Sources > Prometheus > Save & Test

**Logları Kontrol Edin:**
```bash
docker logs grafana
```

### Genel Sorun Giderme

**Tüm Konteynerları Yeniden Başlatın:**
```bash
docker-compose down
./deploy-docker.sh -c "$AZURE_SERVICEBUS_CONNECTION_STRING"
```

**Prometheus.yml Dosyasını Kontrol Edin:**
```bash
cat prometheus.yml
```

**Docker Compose Ağ Ayarlarını Kontrol Edin:**
```bash
docker network ls
docker network inspect azure-servicebus-metric-exporter_default
```

---

Uygulama çalıştırıldıktan sonra, bu kılavuzdaki adımları izleyerek Azure Service Bus metriklerinizi görselleştirebilir ve izleyebilirsiniz. Herhangi bir sorununuz olursa, Sorun Giderme bölümündeki adımları izleyin veya daha fazla yardım için dokümantasyona başvurun.