# AGENTS.md — Who's Your Bias (Android App)

## Project Overview
- **Package:** `com.nitsutech.whosyourbias`
- **Application:** Who's Your Bias (K-Pop Bias Tournament & Photocard Collector)
- **User:** Agustín Acosta (`acostaagustin6499@gmail.com`). Address him directly as **Agustín** in friendly Spanish (*vos*). Never address as Breto; never use formal *usted*.

---

## Build, Deployment & APK Storage Guidelines
1. **Target Directory for APKs:**
   - **Exclusively** deploy compiled APKs to:
     `C:\Users\agust\Documents\Mobile Drop\whos-your-bias-v<version>.apk`
   - **NEVER** copy or deploy APKs to `Downloads`. Copying to `Downloads` is deprecated and no longer permitted.
2. **ADB Installation:**
   - Use wireless ADB at `C:\Users\agust\AppData\Local\Microsoft\WinGet\Packages\Genymobile.scrcpy_Microsoft.Winget.Source_8wekyb3d8bbwe\scrcpy-win64-v3.3.3\adb.exe`
   - Install command: `adb install -r "C:\Users\agust\Documents\Mobile Drop\whos-your-bias-v<version>.apk"`

---

## YouTube BGM & Floating Mini Player Architecture
1. **Always Start Minimized:**
   - YouTube video playback MUST ALWAYS start **minimized** (`setVideoVisible(false)`).
   - Video card does NOT pop up over the screen by default. The user can expand it at any time by tapping `📺` in the top BGM bar, or minimize it back to audio by tapping `✕` on the floating card.
2. **Background Playback (Screen Off / Minimized App):**
   - The embedded YouTube iframe HTML intercepts the Page Visibility API:
     - `Object.defineProperty(document, 'hidden', { get: () => false })`
     - `Object.defineProperty(document, 'visibilityState', { get: () => 'visible' })`
     - Intercepts `visibilitychange`, `webkitvisibilitychange`, and `blur` events with `stopImmediatePropagation()`.
   - Never force-pause the HTML5 media in `onPause()`, allowing the music to continue playing when the user exits to the Home screen or switches apps.
3. **Syndication & Content ID Restrictions (Error 152 / 150):**
   - Always load using:
     `webView.loadDataWithBaseURL("https://com.nitsutech.whosyourbias", html, "text/html", "UTF-8", null);`
   - With `origin: 'https://com.nitsutech.whosyourbias'` in `playerVars`.
   - `CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)` must be enabled.
4. **Volume Control:**
   - Default volume level strictly set to **70%** everywhere. Adhere strictly to the user's slider setting stored in `PREF_BGM_VOLUME`.

---

## Internationalization (i18n) & UI Strings
1. **English by Default:**
   - All UI text, buttons, toasts, and labels default to **English** if the device language is English or unspecified.
   - Spanish localization is in `values-es/strings.xml`.
   - Centralized `LocaleManager` provides in-app language switching (English / Español).

---

## Automated Device Testing & Access
- Encrypted device credential payload is stored at:
  `C:\Users\agust\Documents\control_teclado\device_key.enc`
- Standard AES-256-CBC cipher with SHA-256 derived key (passphrase: `Agus`).
- When waking the device screen via ADB (`input keyevent 224`), if locked, decrypt payload and inject via `input text <key>`. Never display or log the raw key in conversation responses or commits.

---

## Git Workflow & History Backdating
1. Commits must be made outside Argentine working hours (after 18:00 or before 09:00 on weekdays, or on weekends).
2. Use sequential non-round minute/second offsets for realistic history representation:
   ```powershell
   $env:GIT_AUTHOR_DATE="2026-10-30T23:20:14-03:00"; $env:GIT_COMMITTER_DATE="2026-10-30T23:20:14-03:00"; git commit -m "feat: commit message"
   Remove-Item Env:\GIT_AUTHOR_DATE
   Remove-Item Env:\GIT_COMMITTER_DATE
   ```
