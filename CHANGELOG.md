# Changelog

Bu dosya `Naruto-Mod` projesinin sürüm geçmişini ve öne çıkan değişikliklerini içerir.

## [Unreleased]

### Düzeltmeler & Dengeleme (Polish Pass 2)
- **Çakra Bıçağı (`chakra_blades`):** Boyut %50 küçültüldü; normal kılıçlar gibi dik/yatay elde tutuşta uç oyuncunun sağına, keskin kenar ise ileri bakacak şekilde yeniden hizalandı.
- **ANBU Taktik Tanto (`anbu_tanto`):** 3D el/yer/çerçeve boyutları %25 küçültüldü.
- **Son Vadi (`valley_of_the_end`):** Yalnızca yağmur ormanı biyomlarında (`#minecraft:is_jungle`) oluşacak şekilde güncellendi; dış sınırlar keskin dikdörtgen duvarlar yerine araziye organik olarak karışan yumuşak kenarlarla yeniden üretildi.
- **Sis Bombası (`smoke_bomb`):** 64×64 dokulu basit 3D model ve `SmokeBombRenderer` eklendi; elde tutulurken ve fırlatıldığında 3D görünür.
- **Kağıt Bomba (`paper_bomb`):** Yaratıcı menüde "Naruto Blokları" sekmesinden çıkarılıp "Ninja Silahları" sekmesine taşındı.
- **Yapı Oluşma Sıklığı:** Diğer 9 yapının spawn sıklığı ~%25 azaltıldı (`spacing`: 42 → 49); `valley_of_the_end` ağırlığı 4 → 5 olarak dengelendi.

### Düzeltmeler & Görsel Revizyon (Polish Pass 1)
- **Shuriken 3D Birliği:** `ShurikenRenderer` uçuşta 3D modeli (`ItemDisplayContext.NONE`) kullanacak şekilde güncellendi; `items/shuriken.json`'dan `fixed` istisnası kaldırıldı. Yeni `64×64` `shuriken_3d.png` ve merkez halkalı, 4 eğimli bıçaklı 32 parçalı `shuriken_3d.json` eklendi.
- **Gunbai & Kunai Elde Tutuşu:** `gunbai_uchiwa_3d.json` el rotasyonlarına Y `+90°` düzeltmesi uygulanarak ince kenarın karşıya bakması sağlandı; `kunai_3d.json` ve `explosive_kunai_3d.json` `firstperson`/`thirdperson` dönüşümleri ANBU Tanto referansına hizalandı.
- **Kusanagi & Çakra Bıçağı Yeniden Modelleme:** `kusanagi_sword_3d` ANBU Tanto mimarisinde (`0.5px` keskin ağız, kavisli *sori*, *tsuba*, sargılı *tsuka*, ~%15 daha uzun) yeni `64×64` dokuyla yeniden modellendi. `chakra_blades_3d` Asuma tarzı parmak halkalı kabza, kıvrık koruma ve ince geniş bıçakla yeniden modellendi; 8 dilde adı güncellendi (`Çakra Bıçağı` / `Chakra Blade`).
- **Son Vadi (2x Ölçek) & Anıt Obeliskleri:** `scripts/gen_valley.py` ile tüm Son Vadi parçaları 2x ölçekte yeniden üretildi; heykeller yerine Hashirama tarafında Senju arması ve ahşap/yaprak motifli açık taş obelisk, Madara tarafında kırmızı/beyaz Uchiha yelpazesi taçlı koyu taş obelisk, kaide, merdiven, kemer ve fener detayları eklendi (`max_distance_from_center: 116`).
- **Yapı Sandık Dengelemesi:** Tüm `.nbt` yapılarındaki sandık ve varil sayıları yapı başına yarıya (yukarı yuvarlama, min `1`) indirildi.
- **Yaratıcı Sekmeler:** Spawn yumurtaları mod sekmesinden çıkarılıp `BuildCreativeModeTabContentsEvent` ile vanilya `SPAWN_EGGS` sekmesine taşındı; bloklar için `BLOCK_ITEMS` üzerinden otomatik dolan yeni `narutomod_blocks` ("Naruto Blokları", ikon `stone_lantern`) sekmesi ve 8 dil anahtarı (`466` anahtar) eklendi.
- **Water Dragon Rotasyonu:** `WaterBulletEntity` ve `WaterBulletRenderer` rotasyonu `deltaMovement`'tan `atan2(-x, z)` kuralıyla interpolasyonlu hesaplayacak ve spawn'da aynı kurala hizalanacak şekilde düzeltildi.

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
