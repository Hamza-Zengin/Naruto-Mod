# Changelog

Bu dosya `Naruto-Mod` projesinin sürüm geçmişini ve öne çıkan değişikliklerini içerir.

## [Unreleased]

### İçerik
- **Köy Yapı Blokları (Parti C-1):** Oyuncunun kendi shinobi köyünü inşa edebilmesi için **Tatami Hasırı** (`tatami`), **Tatami Hasırı Plakası** (`tatami_slab`), komşulara bağlanan yarı saydam **Şoji Paneli** (`shoji_panel`) ve ışık seviyesi `13` olan **Taş Fener** (`stone_lantern`) blokları, kraft tarifleri, loot tabloları ve `8` dil desteğiyle eklendi.

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

### İçerik Genişlemesi & Revizyonlar (2026-11-18)
- **32 Veri Odaklı Jutsu:** `Phoenix Flower` (90° yaylı 4 ateş topu), `Chidori Stream` (ustalıkla uzayan elektrik alanı), `Mystical Palm`, `Earth Dragon Bullet`, `Vacuum Spheres`, `Stone Spikes` ve `Water Dragon` eklendi.
- **Parşömen & Ekonomi Sistemleri:** Tier 4+ jutsuları parşömene aktaran `BlankJutsuScrollItem` ve 64'lü cephane taşıyan `SealedAmmoScrollItem` eklendi.
- **10 Dünya Yapısı & 47 `.nbt` Şablonu:** 5 Büyük Shinobi Köyü (`Konoha` — tek İchiraku Ramen ile, `Suna`, `Kiri`, `Kumo`, `Iwa`), dev 5 parçalı `Son Vadi`, `Chūnin Sınav Arenası`, `Myōbokuzan`, `Uchiha Tapınağı` ve tek seferlik savunuculu `Akatsuki Sığınağı`; tüm yapılar birleşik `narutomod_structures.json` ızgarasında en az `24` chunk (`384` blok) aralıkla konumlandırıldı.
- **10 Yeni Köy Mobu & Doku Güncellemesi:** Her köye özel `5` Köy Ninjası (`VillageNinjaEntity`) ve `5` Tüccar Köylü (`ShinobiVillagerEntity`); 3D kafa bandıyla çakışmayan `y=12` göz hizası.
- **Gerçekçi 3D Silahlar (`64×64`) & Köy Zırhları:** `Kunai`, `Patlayıcı Kunai`, `Samehada`, `Chakra Kılıcı (Hien)` ve `ANBU Kılıcı (Tanto)` için kademeli incelen 3D modeller, `64×64` kaplamalar, yerde/çerçevede/fırlatıldığında 3D görünüm; 5 köy Kage Şapkası, Jōnin Yeleği, Savaşan Devletler Zırhı ve Bilge Pelerini pasifleri.
