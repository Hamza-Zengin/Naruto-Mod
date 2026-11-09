# Naruto-Mod (Minecraft 1.20.1 Forge)

[![Build and Release](https://github.com/Hamza-Zengin/Naruto-Mod/actions/workflows/gradle.yml/badge.svg)](https://github.com/Hamza-Zengin/Naruto-Mod/actions/workflows/gradle.yml)
[![Discord](https://img.shields.io/discord/168282484037910528.svg?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/fAJ3xJg)
[![License: All rights reserved](https://img.shields.io/badge/License-All_Rights_Reserved-blue.svg?style=for-the-badge)](LICENSE)

An extensive Minecraft 1.20.1 Forge mod introducing the rich shinobi universe of Naruto into Minecraft. Features 18+ signature jutsus, clan bloodlines, 3D animated ninja weapons, tactical armors, rogue ninja mobs, and custom sound/particle effects.

---

## 🌟 Key Features

### 🥋 Ninja Clans & Bloodlines
Choose and seal your shinobi heritage via the Ninja Menu (`J` key):
- **Wandering Shinobi**: Balanced chakra and stamina for flexible play.
- **Uchiha Clan**: Fire style mastery, Sharingan awakening, and Susanoo armor.
- **Hyūga Clan**: Byakugan vision and Gentle Fist combat.
- **Uzumaki Clan**: +25% massive chakra capacity and 2x faster chakra recovery.
- **Nara Clan**: 30% faster stamina recovery and shadow manipulation techniques.
- **Senju Clan**: Wood Style mastery, vitality regeneration, and enhanced reserves.
- **Kaguya Clan**: Shikotsumyaku bone arts, bonus piercing damage, and damage resistance.

### 🌀 18+ Ninjutsu, Genjutsu & Taijutsu Techniques
- **Chakra Basics**: Ninja Leap (`X`), Chakra Dash, Water Walking, and Chakra Charging.
- **Ninjutsu**: Fireball Jutsu (*Katon: Gōkakyū*), Water Bullet (*Suiton: Suidanha*), Wind Breaker (*Fuuton: Daitoppa*), Mud Wall (*Doton: Doryūheki*).
- **Master Techniques**: Rasengan, Chidori (with hand-ready strike mode), Rasenshuriken, Shadow Clone (*Kage Bunshin*), Kawarimi (*Body Replacement*), Shadow Possession (*Kage Mane*), Wood Prison (*Mokuton: Jukai Kōtan*), Bone Dance (*Sawarabi no Mai*).
- **Kekkei Genkai & Forbidden Arts**: Sharingan, Byakugan, Amaterasu, Susanoo Ribcage, Kirin, and the Eight Inner Gates (*Hachimon*).

### ⚔️ 3D Weapons & Tactical Gear
- **Ninja Weapons**: Kunai, Explosive Kunai, Shuriken, Senbon, 3D Fūma Shuriken, Smoke Bombs, and Paper Bombs.
- **Legendary Swords**: Sword of Kusanagi (AoE Chidori current), Samehada (chakra and heart drain), Kaguya Bone Sword.
- **Armors & Attire**: Village Headbands (Leaf, Sand, Mist, Cloud, Rock, Sound, Rain, etc.), ANBU Battle Armor with 3D Back Tantō, Chūnin Tactical Vest, Akatsuki Cloak, and ANBU Masks.
- **Consumables**: Ichiraku Ramen and Military Rations Pills.
- **Entities & Bosses**: Rogue Ninjas (Nukenin) and Akatsuki S-Rank Rogue Shinobi.

---

## 📦 SekCLib Dependency

This mod relies on **SekCLib** (`com.sekwah.sekclib`) for capability tracking and network data synchronization. A pre-compiled, deobfuscated release for Forge 1.20.1 is bundled inside [`libs/SekCLib-1.20.1-1.0.2-deobf.jar`](libs/SekCLib-1.20.1-1.0.2-deobf.jar).

---

## 🌐 Localization Parity

All translation keys are 100% synchronized across all supported languages:
- 🇺🇸 **English** (`en_us`) — Reference
- 🇹🇷 **Türkçe** (`tr_tr`) — Complete
- 🇩🇪 **Deutsch** (`de_de`) — Complete
- 🇫🇷 **Français** (`fr_fr`) — Complete
- 🇷🇺 **Русский** (`ru_ru`) — Complete

To verify language file parity:
```bash
python3 scripts/check_lang.py
```

---

## 🛠️ Building & Developing

### Requirements
- **JDK 17** (e.g. Eclipse Temurin or Zulu)
- Python 3.8+ (for model and lang verification scripts)

### Commands
```bash
# Verify language files and model part names
python3 scripts/check_lang.py
python3 scripts/check_model_parts.py

# Run automated tests
./gradlew test

# Build production JAR
./gradlew build

# Run client in development
./gradlew runClient
```

---

## 🤝 Contributing

Contributions are welcome! Please adhere to [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/):
- `feat`: New features (abilities, weapons, armors)
- `fix`: Bug fixes
- `test`: Adding or updating tests
- `docs`: Documentation updates
- `build` / `ci`: Build system or GitHub Actions adjustments
- `refactor`: Code improvements without behavior changes
