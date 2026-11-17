# Changelog

Bu dosya `Naruto-Mod` projesinin sürüm geçmişini ve öne çıkan değişikliklerini içerir.

## [0.1.0] - 2026-11-16

### Mimari & Altyapı
- **NeoForge 26.3 Tam Geçişi:** `DataComponents` tabanlı eşya/parşömen verileri, `StreamCodec` ağ paket altyapısı ve modern item/equipment model sistemiyle tam uyum.
- **SekCLib 0.1.0 Entegrasyonu (Breaking Change Uyumu):**
  - SekCLib `capabilitysync` paket yapısı düzleştirildi (`com.sekwah.sekclib.network.capabilitysync.*` → `com.sekwah.sekclib.capabilitysync.*`).
  - Spectator (izleyici) modundaki oyuncular için otomatik tam capability senkronizasyonu desteği eklendi.
  - `ColorUtil` ve `RenderMathUtil` ortak render yardımcıları SekCLib üzerinden kullanılmaya başlandı.
- **`NinjaData` Modülerleştirme:** `INinjaData` arayüzü ve `@Sync` alanları korunarak çakra/stamina hesaplamaları (`ChakraStaminaLogic`), rütbe ilerlemesi (`RankProgression`) ve görev mantığı (`MissionLogic`) bağımsız yardımcı sınıflara ayrıldı.
- **Otomatik Test & GameTest Altyapısı:** JUnit birim testlerine ek olarak `runGameTestServer` (`NarutoModGameTests`) ile `ServerSecurityValidator`, jutsu çakra/cooldown akışı, görev döngüsü ve C2S paket codec'leri gerçek NeoForge sunucu ortamında doğrulanır hale getirildi.

### Oynanış & Sistemler
- **Veri Odaklı (Data-Driven) Jutsu Dengeleme:** `data/narutomod/jutsus/` altındaki JSON tanımlarıyla çakra maliyeti, bekleme süresi, temel hasar ve rütbe gereksinimlerini özelleştirme; sunucudan istemciye otomatik senkronizasyon (`JutsuDataManager`).
- **Elementel Jutsu Etkileşimleri:** Savaş alanında aktif element komboları (Su + Yıldırım, Rüzgar + Ateş, Toprak + Yıldırım).
- **Oynanış Hissi (Game Feel):** Duruma duyarlı saydamlaşan dinamik HUD, kademeli el mührü ses/kıvılcım efektleri ve jutsu kullanımında mikro ekran sarsıntısı.
- **Taktiksel NPC & Çevresel İzler:** `AkatsukiNinjaEntity` ve `RogueNinjaEntity` için gelişmiş taktiksel yapay zeka; `AmaterasuFireBlock`, krater ve toz efektleri gibi kalıcı/yarı-kalıcı çevresel izler.

### Sunucu Güvenliği & Uyumluluk
- **C2S Paket Güvenliği:** Sunucu tarafında istek doğrulama ve hız sınırlama (rate-limiting).
- **Alan Koruma & Griefing Kontrolü:** `narutoJutsuGriefing` oyun kuralı ve sunucu alan koruma eklentileriyle uyumlu blok etkileşimi.

### Ekosistem & Arayüz
- **Global Loot Modifiers (GLM):** `AddNarutoLootModifier` ile vanilya sandıklarına veri odaklı ganimet enjeksiyonu (`data/narutomod/loot_modifiers/`).
- **Shinobi Başarım Ağacı:** `NinjaProgressTrigger` tabanlı özel ilerleme ve başarım ağacı (`data/narutomod/advancement/shinobi/`).
- **Dinamik Jutsu Ansiklopedisi:** `NinjaMenuScreen` üzerinde `JutsuDataManager` verilerini canlı gösteren Jutsu Detay Kartı (element rengi, çakra maliyeti, saniye cinsinden bekleme süresi, temel hasar ve kilit durumu).
