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

## In-App Auto-Update Architecture (Direct APK Distribution via GitHub Releases)
This architecture allows any non-Play Store Android app to self-update cleanly with in-app download progress and direct installation.

### 1. Requirements in `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />

<application ...>
    <provider
        android:name="androidx.core.content.FileProvider"
        android:authorities="${applicationId}.fileprovider"
        android:exported="false"
        android:grantUriPermissions="true">
        <meta-data
            android:name="android.support.FILE_PROVIDER_PATHS"
            android:resource="@xml/file_paths" />
    </provider>
</application>
```
File paths configuration (`res/xml/file_paths.xml`):
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="cache_apks" path="updates/" />
    <external-cache-path name="ext_cache_apks" path="updates/" />
</paths>
```

### 2. Update Check Workflow (`AppUpdateManager.java`):
- **Endpoint:** Queries `https://api.github.com/repos/<owner>/<repo>/releases/latest` in a background thread executor.
- **Comparison:** Compares current version from `packageInfo.versionName` against `tag_name` (e.g. `1.2.19` vs `v1.2.19`).
- **Assets:** Searches release assets for files ending with `.apk` (preferring release or matching app name) and reads `browser_download_url` and size.

### 3. Smooth Permission Handling (Android 8.0+ / API 26+):
- Check `activity.getPackageManager().canRequestPackageInstalls()`.
- If false, cache the downloaded file path in `pendingApkFile` and direct user to:
  ```java
  Intent permIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
          Uri.parse("package:" + activity.getPackageName()));
  activity.startActivity(permIntent);
  ```
- In `MainActivity.onResume()`, call:
  ```java
  AppUpdateManager.resumePendingInstallIfAllowed(this);
  ```
  If permission was granted while in Settings, it immediately launches package installation without re-downloading.

### 4. Direct APK Installation via FileProvider:
```java
Uri apkUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", apkFile);
Intent installIntent = new Intent(Intent.ACTION_VIEW);
installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
activity.startActivity(installIntent);
```

---

## Git Workflow & History Backdating
1. Commits must be made outside Argentine working hours (after 18:00 or before 09:00 on weekdays, or on weekends).
2. Use sequential non-round minute/second offsets for realistic history representation:
   ```powershell
   $env:GIT_AUTHOR_DATE="2026-10-30T23:20:14-03:00"; $env:GIT_COMMITTER_DATE="2026-10-30T23:20:14-03:00"; git commit -m "feat: commit message"
   Remove-Item Env:\GIT_AUTHOR_DATE
   Remove-Item Env:\GIT_COMMITTER_DATE
   ```
