# WhosYourBias - Handoff & Session Task Tracking

**Active Project**: `whos-your-bias`  
**User**: Agustín Acosta (`acostaagustin6499@gmail.com`)  
**Last Updated**: 2026-10-30 20:35:00 -03:00  
**Current Release Version**: `v1.2.9` (versionCode 12)

---

## Task Backlog & Status

### 1. Gacha Timer Cooldown (3 Minutes)
- **Requirement**: Allow opening Gacha envelopes every **3 minutes** (reduced from 24h -> 6h -> 15m -> 3m).
- **Status**: `[x] Done`
- **Implementation**:
  - `MainActivity.java`: Updated `gachaCooldown = 3 * 60 * 1000L` on home screen strip and `cooldown = 3 * 60 * 1000L` in dialog.
  - Centralized i18n strings in `res/values/strings.xml` and `res/values-es/strings.xml` updated to mention 3 minutes.

### 2. Photocard Unlock Consistency & Count
- **Requirement**:
  - The counter `📸 <Idol>'s Photocards (X/30)` must strictly match the count of unlocked photocards visible in the binder.
  - Slots #1, #2, #3 must always be unlocked for all idols as free starter cards.
  - Fixed issue where Sakura (and others) showed `(3/30)` in the title but cards #2 and #3 appeared locked with padlock due to stale virtual URI identities in `unlockedPhotocards`.
  - For Moka, 30/30 photocards remain 100% unlocked.
- **Status**: `[x] Done`
- **Implementation**:
  - `IdolCard.java`: Ensured starter slots 0, 1, 2 are recognized as unlocked in `isPhotocardUnlocked` regardless of URI migration. Stale `fandom_pc://` URIs are filtered out from `getUnlockedPhotocards()`.
  - `MainActivity.java`: Binder title dynamically counts unlocked cards from the rendered list (`card.isPhotocardUnlocked(p)`).

### 3. Duplicate Photocards Elimination
- **Requirement**:
  - Remove all duplicate photos for Moka and all other idols (e.g. Moka had 3 identical glitter selfies).
  - Ensure all 30 photocards for Moka and ILLIT members are distinct, high-definition concept and album photos.
  - Force-sync curated bundled photos over legacy cached data in user's SQLite/JSON repo.
- **Status**: `[x] Done`
- **Implementation**:
  - `BiasRepository.java`: For all ILLIT idols (`c.getId().startsWith("idol_illit_")`), bundled curated 30-photocard lists are strictly synced on repository startup to overwrite any duplicate entries from previous app versions.

### 4. Smart Face Centering & Headroom Protection (No Chopped Foreheads)
- **Requirement**:
  - Prevent cutting off Moka's and Minju's foreheads/hair in match cards and binder thumbnails.
  - Protect crown headroom: ensure the top of the head is always inside the frame (`top = 0f` or crown margin >= 5%).
  - In wide shots (e.g. Moka sitting far away on the bed), auto-zoom into her face and center on her.
  - Fix Minju's 2nd photocard thumbnail which was cropped by secondary `CENTER_CROP`.
- **Status**: `[x] Done`
- **Implementation**:
  - `CardImageLoader.java`: Headroom check enhanced with `estimatedCrownY = face.centerY - (face.eyeDistance * 1.95f)`. When hair/head is near the top 15% of the photo, `top = 0f` is enforced so forehead and bangs are never sliced off. Fallback when face detection is uncertain also uses `top = 0f`.
  - Cropped `photo_65.webp` (Moka bed wide shot) to 225x300 portrait tightly framed on Moka.
  - Cropped `photo_31.webp` (Minju concept photo 2) to 249x333 portrait framing her face and eyes.
  - `MainActivity.java`: `buildPhotocardBinderCell` now loads thumbnails with `loadCardImageWithSize(this, temp, ivThumb, dp(192), dp(252))` and `ScaleType.FIT_XY`, eliminating the double-crop artifact.

### 5. Instant Preloading Before Battles (Eliminate Cat Loading Flicker)
- **Requirement**:
  - Preload all contestant photocards into memory cache before the match starts, so cards transition instantly with 0ms delay.
  - Avoid showing the loading cat placeholder during battles.
- **Status**: `[x] Done`
- **Implementation**:
  - `CardImageLoader.java`: `preloadMatchCardsAsync` now decodes and caches both the raw bitmap in `rawMemoryCache` AND the pre-fitted card bitmap in `fittedMemoryCache` with key `rawKey + "_480x380"` across all unlocked photocards of every match contestant.

### 6. YouTube Song Search & Direct Link Resolution in BGM Selector
- **Requirement**:
  - Fix direct YouTube link resolution (e.g. `https://youtu.be/b8xFEnrDuIU` returned "no songs found").
  - Fix search by name on Android returning 0 YouTube results (caused by 302 redirect on mobile user-agent).
  - Show 16:9 thumbnail, video title, channel name, and allow direct playback.
- **Status**: `[x] Done`
- **Implementation**:
  - `YoutubeBgmController.java`:
    1. Direct YouTube link/ID detection: resolves video ID via official YouTube oEmbed API (`https://www.youtube.com/oembed?url=...`), retrieving full title (`paw, paw!`), author (`ILLIT - Topic`), and thumbnail (`hqdefault.jpg`) instantly.
    2. YouTube InnerTube API endpoint (`https://www.youtube.com/youtubei/v1/search`) with direct POST JSON payload: eliminates all 302 redirects on mobile and returns instant video cards with thumbnails, titles, and channels.
    3. Desktop User-Agent scrape fallback with redirect following.

### 7. Clear Button ('✕') on All Search Bars
- **Requirement**:
  - Add an `✕` icon button on the right side of all search inputs (idols search bar, groups search bar, BGM song selector search bar, custom game creator).
  - The `✕` icon appears when text is entered and clears the text when clicked.
- **Status**: `[x] Done`
- **Implementation**:
  - `MainActivity.java`: Added `wrapSearchBarWithClearButton(EditText et, ThemeManager.AppTheme theme)` with dynamic `TextWatcher` visibility and padding preservation.
  - Applied to `etSearch` (Idols tab) and `etGroupSearch` (Groups tab).
  - `YoutubeBgmController.java`: Integrated `btnClear` inside `searchWrap` FrameLayout in song selector dialog.

### 8. Git Commit History Policy
- **Requirement**:
  - Commit history starts Thursday Oct 29 at **18:14** (after work hours).
  - No commits between 09:00 and 18:00 on weekdays unless backdated.
  - Release version bumped to `v1.2.6` and published.
- **Status**: `[x] Done`

### 9. Fix Crash in Song Search (Canvas Recycled Bitmap Guard)
- **Requirement**:
  - Fix crash `java.lang.RuntimeException: Canvas: trying to use a recycled bitmap` when searching songs in YouTube BGM controller.
  - Root cause: `Bitmap.createScaledBitmap` returns the same source reference when dimensions match (320x180 YouTube thumbnails), causing `sample.recycle()` in face detection to recycle `src` directly before drawing to canvas.
- **Status**: `[x] Done`
- **Implementation**:
  - `CardImageLoader.java`: Guarded face detection downsampling to ensure `src` is NEVER recycled if returned by `createScaledBitmap` or format conversions (`if (sample != src && sample != rgb565) sample.recycle();`).
  - Added defensive `isRecycled()` checks and wrapped `canvas.drawBitmap` in try-catch.

### 10. BGM Startup Silence & Dynamic Battle Volume
- **Requirement**:
  - Do NOT play music immediately on app startup / fresh install (`MainActivity`). Avoid loud jarring music on opening the app.
  - Automatically start BGM when entering a challenge/battle (`MatchActivity`).
  - Dynamic volume: Soft gentle volume (20%) when played in the main menu; energetic volume (85%) in tournament battles.
  - Pause BGM when tournament finishes / exits so the home menu returns to peace.
  - Default preset remains "✨ ILLIT - It's Me" via fast iTunes studio preview (.m4a); YouTube search is used when the user wants to search and pick specific YouTube songs/videos.
- **Status**: `[x] Done`
- **Implementation**:
  - `YoutubeBgmController.java`:
    - `shouldAutoPlay` defaults to `isMatch` (`activity instanceof MatchActivity`). App launch in `MainActivity` stays silent with `▶` button.
    - Set dynamic volume: `0.20f` (20%) in `MainActivity` menu, `0.85f` (85%) in `MatchActivity`. Same volume scaling applied to YouTube iframe (`player.setVolume(targetVol)`).
    - `destroy()` in `MatchActivity` calls `pause()` and dismisses the notification.
  - `MainActivity.java`: Pauses menu BGM before launching `MatchActivity` to guarantee seamless audio transition.

### 11. Full-Image Blurred Background, Volume Slider & Challenge Cover Mode
- **Requirement**:
  - Eliminate all face-zoom cropping on cards (which caused blurriness and awkward cuts on wide shots like Moka and close-ups like Iroha).
  - Render cards with a beautiful **blurred background (fondo difuminado)** and the **entire photo sharp and uncropped (fitCenter)**.
  - Set default match photo mode to **Solo Portada (Cover only)**.
  - Remove the `📸 1/30` / `1/4` pill when in default Cover mode; in random/selected mode, display `📸 #{N}` (photocard number in collection).
  - Add in-app music volume slider and configuration.
  - Fix YouTube audio playback in background WebView.
- **Status**: `[x] Done`
- **Implementation**:
  - `CardImageLoader.java`: Replaced face-zoom cropping in `createPhysicalPhotocardBitmap` with downsampled `fastStackBlur` background + 35% dark scrim + uncropped `fitCenter` foreground photo.
  - `IdolCard.java`: Default `useRandomPhotocard` set to `false` (Solo Portada).
  - `MatchActivity.java`: Hid cycling pill when in Cover mode; in random mode, shows `📸 #N` corresponding to the photo's number in the idol's collection.
  - `YoutubeBgmController.java`: Added real-time volume slider in song selector dialog and via long-click on mute button (`setCustomVolumePercent`). Fixed WebView container sizing (keeping active 1x1 rendering container) so YouTube iframe audio plays continuously.
  - Layout XMLs (`activity_main.xml`, `activity_match.xml`): Updated `bgmVideoContainer` to maintain 1x1 active rendering size instead of 0x0 `GONE`.

### 12. Direct YouTube Embed Audio & Saved Song Deletion
- **Requirement**:
  - Fix YouTube audio playback so custom YouTube songs/videos play with sound reliably in background.
  - Allow removing/deleting songs and videos from `⭐ My Saved Songs & Videos` (custom manual additions).
- **Status**: `[x] Done`
- **Implementation**:
  - `YoutubeBgmController.java`:
    - Loaded direct YouTube embed URL (`https://www.youtube.com/embed/VID?autoplay=1&enablejsapi=1&playsinline=1&loop=1&playlist=VID`) to bypass cross-origin iframe autoplay blocks.
    - Attached `onPageFinished` handler in `WebViewClient` controlling native HTML5 `<video>` element directly (`v.volume = fVol; v.muted = false; v.play();`).
    - Added `🗑️` button to `addYoutubeSongRow` under saved songs and updated `removeCustomSong` to match on video ID as well as full URL.
  - Centralized i18n: Added `bgm_video_removed_toast` in `strings.xml` and `values-es/strings.xml`.

### 13. Language Manager (i18n), Check for Updates UI Polish & Official YouTube Iframe Audio API (v1.2.10)
- **Requirement**:
  - Fix "Check for Updates" button on "My Page" which was compressed into a narrow column causing text to break awkwardly into 4 lines (`Check \n for Upd \n ates`).
  - Follow phone system language by default (English if system is English, Spanish if Spanish) instead of hardcoding Spanish strings.
  - Allow user to manually select and switch app language in settings/profile (System Default, English, Español).
  - Fix YouTube audio playback using official YouTube Iframe API and full background viewport (220dp hidden behind main container) so YouTube never throttles or pauses audio.
- **Status**: `[x] Done`
- **Implementation**:
  - `LocaleManager.java`: Created centralized locale controller managing `app_language` (`system`, `en`, `es`), updating `Configuration` with `LocaleList`, and displaying an interactive language selection dialog.
  - `MainActivity.java` & `MatchActivity.java`: Added `attachBaseContext(LocaleManager.applyLocale(newBase))` for instant localized string loading.
  - `activity_main.xml`: Replaced hardcoded navigation tab strings (`Artistas`, `Mi página`, `Idols`, `Arena`) with `@string/nav_tab_*` resources.
  - `MainActivity.java` (Profile Header): Restructured action buttons into two spacious, symmetrical rows:
    - Row 1: `[📋 Change Photo]` + `[🎨 Theme Studio]`
    - Row 2: `[🔄 Check for Updates]` + `[🌐 Language]`
    - Row 3: `[📤 Share My Idols via WhatsApp]`
  - `YoutubeBgmController.java`:
    - Switched from direct mobile embed page to official YouTube IFrame JavaScript API (`https://www.youtube.com/iframe_api`) loaded via `loadDataWithBaseURL("https://www.youtube.com", ...)`.
    - Implemented `bgmPlay()`, `bgmPause()`, `bgmSetVol()`, `bgmMute()`, and `bgmUnmute()` bridge methods.
    - Set video container to standard 220dp height situated behind the main content views, eliminating WebView layout collapsing and ensuring continuous background audio.
  - Bumped version to `v1.2.10` (versionCode 13). Installed via ADB and deployed to `Mobile Drop` and `Downloads`.

### 14. Floating Draggable Mini YouTube Video Player & Cross-Activity Seamless BGM (v1.2.11)
- **Requirement**:
  - Fix YouTube audio: Previously, hiding the WebView behind opaque views caused Chromium's occlusion culling and background throttling to pause audio playback.
  - Implement a visible foreground **floating mini video frame (PiP)** so the video plays actively in real time, preventing Android/Chromium throttling and letting the user enjoy the music video while playing or browsing.
  - Fix BGM sync when leaving challenges: When exiting `MatchActivity`, music no longer pauses abruptly or desyncs; BGM transitions seamlessly back to `MainActivity` with synchronized `▶`/`⏸` buttons.
  - Set default volume strictly to **70%** (eliminating 85%/25%/20% overrides) and strictly respect the user's slider setting across challenges and menus.
- **Status**: `[x] Done`
- **Implementation**:
  - `activity_main.xml` & `activity_match.xml`: Added elevated foreground `CardView` (`bgmVideoContainer`, `matchBgmVideoContainer`) with rounded corners, 16:9 ratio, and close button `✕`.
  - `YoutubeBgmController.java`:
    - Added touch drag support (`OnTouchListener`) allowing user to freely drag the floating mini video card anywhere on screen.
    - Set default volume to 70% in `getActiveVolumeLevel()` and replaced all hardcoded 85%/25%/20% volume levels with `getActiveVolumeLevel() / 100f`.
    - Added `syncState()` method called in `MainActivity.onResume()` to seamlessly continue YouTube or iTunes BGM when exiting challenges.
    - Embedded hybrid iframe with `autoplay=1&playsinline=1` and `window.onYouTubeIframeAPIReady` for instant reliable playback and controls.
  - Bumped version to `v1.2.11` (versionCode 14). Installed via ADB and deployed to `Mobile Drop` and `Downloads`.

### 15. Fix YouTube Error 152-4 (Referer Injection) & Dedicated Top Drag Handle (v1.2.12)
- **Requirement**:
  - Fix YouTube Error 152-4 ("This video is unavailable. Error code: 152 - 4"): Caused by loading embed iframe through in-memory `loadDataWithBaseURL` which lacked a valid HTTP `Referer` header required by YouTube for official music videos.
  - Fix Draggable Floating Mini Player: The WebView was intercepting all touch events, preventing `CardView.onTouchEvent` from firing.
- **Status**: `[x] Done`
- **Implementation**:
  - `YoutubeBgmController.java`:
    - Replaced `loadDataWithBaseURL` with `webView.loadUrl(embedUrl, headers)` passing explicit `Referer: https://www.youtube.com/` and `Origin: https://www.youtube.com`, satisfying YouTube's embed origin verification and eliminating Error 152-4.
    - Attached `OnTouchListener` directly to the non-WebView drag handle bar (`miniPlayerDragBar`, `matchMiniPlayerDragBar`), giving users an instant, responsive drag experience to move the floating card anywhere on screen.
    - Injected direct `<video>` play, volume, and `.ytp-play-button` click scripts in `onPageFinished` and `togglePlayPause`.
  - Layout XMLs (`activity_main.xml`, `activity_match.xml`): Added dedicated top drag bar `[⠿ Drag           ✕]` above the WebView in the floating card.
  - Bumped version to `v1.2.12` (versionCode 15). Installed via ADB and deployed to `Mobile Drop` and `Downloads`.
