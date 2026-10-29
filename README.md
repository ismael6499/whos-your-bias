# ✨ Who's your Bias — K-Pop Photocard & 1v1 Bias Arena

> *"Who is your bias? I am your bias, it's me!"* — **ILLIT** (`You're Under My Spell / It's Me`)

**Who's your Bias** (`com.nitsutech.whosyourbias`) is a native Android K-Pop tournament, photocard binder, and bias world-cup app built in **Java**. Choose your ultimate bias in 1v1 duels across **287+ idols** and **55 K-Pop groups**, customize the app's pastel theme & palette, listen to K-Pop background music with media notification controls, collect photocards, and share your podium posters or custom decks via WhatsApp!

---

## 🌸 Key Features

### 🏆 4 Game Modes & 1v1 Duel Arena
- **World Cup Bracket (`Eliminación Directa`)**: Classic tournament rounds (`Round of 64`, `32`, `16`, `Quarter-Finals`, `Semi-Finals`, `Grand Final`) with automatic **🥇 1st Place**, **🥈 2nd Place**, and **🥉 3rd Place** podium tracking.
- **King of the Hill (`Rey de la Colina`)**: The reigning bias stays on stage defending their crown against incoming challengers.
- **Round Robin League (`Liga Todos contra Todos`)**: Points-based league table where every idol faces off.
- **Double Elimination (`Doble Eliminación`)**: Winners Bracket + Losers Bracket survival format.
- **3D Card Flip Gesture**: Slide your finger across any card during a match (or tap `🔄 Info`) to flip the card in 3D and inspect full idol details (real name, age, zodiac sign, groupmates, role, and bio) without accidentally voting! Tap **`✓ Elegir`** when you're ready to pick your bias.

### 📸 287+ Idols, 55 Groups & Multi-Photocard Binder
- Pre-loaded with **287 idols** across **55 groups & soloists** with official concept photos from Kpop Wiki:
  - **HYBE Labels**: `ILLIT`, `NewJeans`, `LE SSERAFIM`, `BTS`, `SEVENTEEN`, `TOMORROW X TOGETHER`, `ENHYPEN`, `BOYNEXTDOOR`, `TWS`, `&TEAM`, `CORTIS`, `KATSEYE`, `fromis_9`
  - **JYP Entertainment**: `TWICE`, `Stray Kids`, `ITZY`, `NMIXX`, `NEXZ`, `NiziU`, `KickFlip`, `DAY6`, `Xdinary Heroes`
  - **YG & THEBLACKLABEL**: `BLACKPINK`, `BABYMONSTER`, `TREASURE`, `BIGBANG`, `2NE1`, `WINNER`, `MEOVV`, `izna`, `ALLDAY PROJECT`, `Somi`
  - **SM Entertainment**: `aespa`, `Red Velvet`, `RIIZE`, `NCT 127`, `NCT DREAM`, `WayV`, `NCT WISH`, `Hearts2Hearts`, `EXO`, `SHINee`, `Girls' Generation`
  - **More Fan Favorites**: `(G)I-DLE`, `IVE`, `ATEEZ`, `ZEROBASEONE`, `KISS OF LIFE`, `STAYC`, `tripleS`, `QWER`, `FIFTY FIFTY`, `RESCENE`, `KiiiKiii`, ` Kep1er`, `VIVIZ`, `IU`, `BIBI`, `Kwon Eunbi`, `Chuu`, `Yena`
- **Uncropped Full-Fit Portrait Rendering**: Idol faces and foreheads are never cut off—every portrait is fitted cleanly over a soft blurred pastel backdrop.
- **Clipboard Image Paste (`📋 Pegar Copiada`)**: Copy any image or URL to your phone's clipboard and paste it directly into your Profile Avatar (*Mi página*), an Idol's Photocard Binder, or when creating a new Idol card.

### 🎨 Full Pastel Theme & Color Studio
- Tap the **`🎨`** button in the top bar (or in *Mi página*) to open the **Theme & Color Studio**:
  - **7 Built-In Presets**: *🌸 Pastel Pink & Gold*, *🌼 Vibrary Cream & Gold*, *💜 Dreamy Lavender (ILLIT)*, *🍑 Peach Coquette*, *🌿 Mint & Matcha Pastel*, *☁️ Cotton Candy Sky*, and *🌙 Midnight Pastel (Dark)*.
  - **Custom Color Picker**: Customize individual interface parts (*Primary Color*, *Secondary Color*, *App Background*, *Card Surface*, *Main Text*) using the interactive rainbow touch bar, RGB sliders, Hex code input, or 24 K-Pop pastel swatches, and save your own custom presets!

### 🎵 Background Music Player, Search & Notification Controls
- Built-in audio player defaulted to **ILLIT — *"You're Under My Spell (Who is your bias? It's Me!)"*** with 20+ curated K-Pop hits (`ILLIT`, `NEXZ`, `NewJeans`, `aespa`, `LE SSERAFIM`, `BABYMONSTER`, `TWICE`, `Stray Kids`, `BTS`, `BLACKPINK`).
- **In-App Song Search & Save (`⭐ Guardar`)**: Search any song or artist and save your favorite tracks directly to your personal **`⭐ Mis Canciones Guardadas`** playlist.
- **Media Notification**: Pause, resume, or stop the music anytime directly from the Android notification shade.

### 📤 WhatsApp Status Poster & `.wyb` Deck Sharing
- **WhatsApp Status / Story Poster**: Generates a high-resolution vertical poster styled with your active App Theme colors, rounded-clipped champion portrait, **`🥇 1° Puesto`**, **`🥈 2° Puesto`**, and **`🥉 3° Puesto`** podium, and clean branding.
- **Share & Import Decks**: Share custom matches or idol packs with friends as a `.wyb` file or a compact `WYB1-...` WhatsApp text code with selective import preview.

---

## 📲 Download & Install

Grab the latest **`WhosYourBias.apk`** directly from the [**Releases**](../../releases) section and install it on any Android 8.0+ (API 26+) device.

---

## 🛠️ Tech Stack & Build

- **Language**: Java 17
- **Platform**: Android SDK 34 (`minSdk 26`, `targetSdk 34`)
- **Architecture**: Single-Activity Vibrary/Moshicam-inspired UI + `MatchActivity` 1v1 Arena + `BiasRepository` JSON persistence + `CardImageLoader` dual Memory/Disk cache + `ThemeManager` custom palette engine.

```bash
./gradlew assembleDebug
```
