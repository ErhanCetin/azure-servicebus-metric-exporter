# Grafana Dashboard Import Talimatları

Hazırladığım Azure Service Bus Metrics Dashboard'unu Grafana'nıza import etmek için aşağıdaki adımları izleyin.

## 1. Dashboard JSON Dosyasını Kaydedin

Öncelikle, size sağladığım JSON dosyasını bilgisayarınıza kaydedin:

1. `azure-servicebus-dashboard.json` dosyasındaki içeriği kopyalayın
2. Bilgisayarınızda bu içeriği bir metin editöründe (NotePad, VSCode vb.) açın
3. Dosyayı `azure-servicebus-dashboard.json` adıyla kaydedin

## 2. Grafana'da Dashboard'u Import Edin

1. Grafana'yı açın (http://localhost:3000)
2. Sol taraftaki menüden "+" işaretine tıklayın
3. Açılan menüden "Import" seçeneğini tıklayın
4. Import sayfasında aşağıdaki seçeneklerden birini kullanabilirsiniz:
  - "Upload JSON file" butonu ile kaydettiğiniz JSON dosyasını yükleyin
  - Veya "Import via panel json" alanına JSON içeriğini doğrudan yapıştırın

5. "Load" butonuna tıklayın
6. Dashboard ayarlarını son bir kez kontrol edin:
  - Name: "Azure Service Bus Metrics Dashboard" (değiştirebilirsiniz)
  - Folder: Bir klasör seçin veya "General" olarak bırakın
  - Prometheus veri kaynağı seçimi: "Prometheus" seçeneğini seçin

7. "Import" butonuna tıklayarak dashboard'u içe aktarın

## 3. Dashboard'u Özelleştirme

Dashboard'u başarıyla içe aktardıktan sonra:

1. **Değişkenleri Kontrol Edin**:
  - Dashboard üst kısmında "Environment" ve "Queue" değişkenlerini göreceksiniz
  - Bu değişkenler otomatik olarak metriklerinizdeki etiketlerden değerleri alacaktır

2. **Zaman Aralığını Ayarlayın**:
  - Sağ üst köşedeki zaman seçiciden istediğiniz aralığı seçin
  - Dashboard varsayılan olarak son 1 saatlik veriyi gösterir

3. **Otomatik Yenileme**:
  - Sağ üst köşedeki yenileme simgesinden otomatik yenileme sıklığını ayarlayabilirsiniz
  - Varsayılan yenileme aralığı 10 saniyedir

## 4. Dashboard İçeriği

Bu dashboard aşağıdaki panellerden oluşmaktadır:

### Genel Bakış Satırı
- **Total Active Messages**: Tüm aktif mesajların toplam sayısı (gauge)
- **Total Dead Letter Messages**: Tüm dead letter mesajlarının toplam sayısı (gauge)
- **Messages by Environment**: Ortam bazında mesaj dağılımı (pasta grafiği)
- **Active Messages by Queue**: Kuyruk bazında aktif mesaj sayıları (zaman serisi grafiği)

### Kuyruk Metrikleri Satırı
- **Dead Letter Messages by Queue**: Kuyruk bazında dead letter mesajları (zaman serisi grafiği)
- **Queue Size (Bytes)**: Kuyruk boyutları (zaman serisi grafiği)
- **Current Queue Status**: Aktif mesajların mevcut durumu (tablo)
- **Dead Letter Status**: Dead letter mesajlarının mevcut durumu (tablo)

## 5. Sorun Giderme

Dashboard yüklendiğinde grafikler boş görünüyorsa:

1. **Prometheus Bağlantısını Kontrol Edin**:
  - Dashboardun sağ üst köşesindeki dişli ikonuna tıklayın
  - "Variables" seçeneğini tıklayın
  - "environment" ve "queue" değişkenlerinde değerler görüp görmediğinizi kontrol edin
  - Değerler görünmüyorsa, Prometheus veri kaynağı ayarlarınızı kontrol edin

2. **Zaman Aralığını Genişletin**:
  - Sağ üst köşedeki zaman seçiciyi kullanarak zaman aralığını genişletin (örneğin "Last 3 hours")

3. **Prometheus'ta Metrikleri Kontrol Edin**:
  - http://localhost:9090/graph adresine gidin
  - "azure_servicebus_active_messages" gibi temel bir metriği sorgulayın
  - Veri görüyorsanız ancak Grafana'da görmüyorsanız, Grafana'daki veri kaynağı yapılandırmanızı kontrol edin

## 6. Dashboard'u Kaydetme ve Paylaşma

Özelleştirmelerinizi tamamladıktan sonra:

1. Sağ üst köşedeki disk simgesine tıklayarak değişiklikleri kaydedebilirsiniz
2. Grafana'nın paylaşım özelliklerini kullanarak dashboard'u ekibinizle paylaşabilirsiniz
3. Dashboard'u dışa aktarmak için:
  - Dişli ikonuna tıklayın
  - "Export" seçeneğini tıklayın
  - JSON formatında dışa aktarın

Herhangi bir sorunuz veya özelleştirme ihtiyacınız varsa, lütfen belirtin.