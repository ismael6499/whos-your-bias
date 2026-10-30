# WhosYourBias - Handoff & Session Task Tracking

**Active Project**: `whos-your-bias`  
**User**: Agustín Acosta (`acostaagustin6499@gmail.com`)  
**Last Updated**: 2026-10-30 19:35:00 -03:00  
**Current Release Version**: `v1.2.6` (versionCode 9)

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
