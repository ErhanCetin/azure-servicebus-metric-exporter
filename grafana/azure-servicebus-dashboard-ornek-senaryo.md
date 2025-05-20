# Azure Service Bus Dashboard Genişletme Talimatları

Mevcut dashboard'ınızı topic ve subscription metriklerini de içerecek şekilde genişletmek için aşağıdaki adımları izleyin. Bu, Azure Service Bus'ın tüm bileşenlerini tek bir dashboard'da görmenizi sağlayacaktır.

## 1. Değişkenler Ekleyin

Öncelikle yeni değişkenler ekleyelim:

1. Dashboard ayarlarına gidin (sağ üst köşedeki dişli simgesi)
2. "Variables" sekmesine tıklayın
3. Yeni bir değişken ekleyin:
    - Name: `entity_type`
    - Label: "Entity Type"
    - Type: "Custom"
    - Options: `queue,topic,subscription`
    - Include All option: Açık
    - Default: "All"
4. "Entity Type" değişkenini kaydedin

5. Topic değişkeni ekleyin:
    - Name: `topic`
    - Label: "Topic"
    - Type: "Query"
    - Datasource: Prometheus
    - Query: `label_values(azure_servicebus_size_bytes{entity_type="topic", environment=~"$environment"}, entity_name)`
    - Include All option: Açık
    - "Add" butonuna tıklayın

6. Subscription değişkeni ekleyin:
    - Name: `subscription`
    - Label: "Subscription"
    - Type: "Query"
    - Datasource: Prometheus
    - Query: `label_values(azure_servicebus_active_messages{entity_type="subscription", environment=~"$environment"}, subscription_name)`
    - Include All option: Açık
    - "Add" butonuna tıklayın

## 2. Topic ve Subscription Satırı Ekleyin

Dashboard'a yeni bir satır ekleyelim:

1. "Queue Metrics" satırının altına gelin
2. "+ Add" butonuna tıklayın ve "Row" seçin
3. Başlık: "Topics & Subscriptions"
4. "Add" butonuna tıklayın

## 3. Topic Metrikleri için Paneller Ekleyin

Yeni satırın altına Topic ile ilgili paneller ekleyelim:

### Panel 1: Topic Size (Time Series)

1. "Add panel" butonuna tıklayın
2. Panel düzenleme ekranında:
    - Veri kaynağı olarak Prometheus'u seçin
    - Metrics Browser: `azure_servicebus_size_bytes{entity_type="topic", environment=~"$environment", entity_name=~"$topic"}`
    - Panel tipi: "Time series"
    - Panel başlığı: "Topic Size (Bytes)"
    - Standard Options > Unit: "bytes (IEC)"
    - "Apply" butonuna tıklayın

### Panel 2: Topic Subscription Count (Time Series)

1. "Add panel" butonuna tıklayın
2. Panel düzenleme ekranında:
    - Veri kaynağı olarak Prometheus'u seçin
    - Metrics Browser: `azure_servicebus_subscription_count{entity_type="topic", environment=~"$environment", entity_name=~"$topic"}`
    - Panel tipi: "Time series"
    - Panel başlığı: "Subscription Count by Topic"
    - "Apply" butonuna tıklayın

### Panel 3: Topic Metrics Table

1. "Add panel" butonuna tıklayın
2. Panel düzenleme ekranında:
    - Veri kaynağı olarak Prometheus'u seçin
    - Metrics Browser: `azure_servicebus_size_bytes{entity_type="topic", environment=~"$environment", entity_name=~"$topic"}`
    - Format: "Table"
    - Panel tipi: "Table"
    - Panel başlığı: "Current Topic Status"
    - Transformations > "Add transformation" > "Organize fields" ekleyin:
        - "__name__", "entity_type", "instance", "job", "namespace" sütunlarını gizleyin
    - "Apply" butonuna tıklayın

## 4. Subscription Metrikleri için Paneller Ekleyin

### Panel 4: Subscription Active Messages (Time Series)

1. "Add panel" butonuna tıklayın
2. Panel düzenleme ekranında:
    - Veri kaynağı olarak Prometheus'u seçin
    - Metrics Browser: `azure_servicebus_active_messages{entity_type="subscription", environment=~"$environment", topic_name=~"$topic", subscription_name=~"$subscription"}`
    - Panel tipi: "Time series"
    - Panel başlığı: "Active Messages by Subscription"
    - Legend Format: `{{topic_name}}/{{subscription_name}}`
    - "Apply" butonuna tıklayın

### Panel 5: Subscription Dead Letter Messages (Time Series)

1. "Add panel" butonuna tıklayın
2. Panel düzenleme ekranında:
    - Veri kaynağı olarak Prometheus'u seçin
    - Metrics Browser: `azure_servicebus_dead_letter_messages{entity_type="subscription", environment=~"$environment", topic_name=~"$topic", subscription_name=~"$subscription"}`
    - Panel tipi: "Time series"
    - Panel başlığı: "Dead Letter Messages by Subscription"
    - Legend Format: `{{topic_name}}/{{subscription_name}}`
    - "Apply" butonuna tıklayın

### Panel 6: Subscription Metrics Table

1. "Add panel" butonuna tıklayın
2. Panel düzenleme ekranında:
    - Veri kaynağı olarak Prometheus'u seçin
    - Metrics Browser: `azure_servicebus_active_messages{entity_type="subscription", environment=~"$environment", topic_name=~"$topic", subscription_name=~"$subscription"}`
    - Format: "Table"
    - Panel tipi: "Table"
    - Panel başlığı: "Current Subscription Status"
    - Transformations > "Add transformation" > "Organize fields" ekleyin:
        - "__name__", "entity_type", "instance", "job", "namespace" sütunlarını gizleyin
    - "Apply" butonuna tıklayın

## 5. Genel Metrikleri Güncelleme

Genel bakış panellerinin entity type'ı filtrelemesini kaldırarak tüm entity'leri göstermelerini sağlayalım:

1. "Total Active Messages" panelini düzenleyin:
    - Metrics Browser: `sum(azure_servicebus_active_messages{environment=~"$environment", entity_type=~"$entity_type"})`

2. "Total Dead Letter Messages" panelini düzenleyin:
    - Metrics Browser: `sum(azure_servicebus_dead_letter_messages{environment=~"$environment", entity_type=~"$entity_type"})`

3. "Messages by Environment" panelini düzenleyin:
    - Metrics Browser: `sum by(environment) (azure_servicebus_active_messages{environment=~"$environment", entity_type=~"$entity_type"})`

4. "Current Queue Status" benzeri tablolarda entity_type filtresini tutun, böylece ayrı tablolarda sadece ilgili entityler gösterilsin.

## 6. Dashboard'u Kaydedin

1. Tüm değişiklikleri tamamladıktan sonra sağ üst köşedeki "Save dashboard" butonuna tıklayın
2. "Save" butonuna tıklayın

## 7. Özelleştirme İpuçları

- Panel boyutlarını ve yerleşimini düzenleyebilirsiniz
- Panel seçeneklerini (renk şemaları, grafikler, eşik değerleri) özelleştirebilirsiniz
- Legend formatını değiştirebilirsiniz (örneğin, Subscription metriklerinde `{{topic_name}}/{{subscription_name}}` kullanabilirsiniz)
- Correlation için time range seçicisini kullanabilirsiniz
- Panel Grupları oluşturabilirsiniz

Bu şekilde tek bir dashboard'da Queue, Topic ve Subscription metriklerini görüntüleyebilirsiniz.