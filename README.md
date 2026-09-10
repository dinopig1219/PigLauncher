# PigLauncher

A small LSPosed module for restoring selected upstream MIUI Launcher behavior in POCO Launcher without globally pretending that POCO is the MIUI system launcher.

## Current fixes

### 1. Large Folder Dark Mode

Target:

- `com.miui.home.folder.FolderIcon4x4NormalBackgroundDrawable`
- `com.miui.home.common.utils.BuildConfigUtils#isMiuiLauncher()`

POCO already contains the dark large-folder resources, but the upstream path is gated by `isMiuiLauncher()`.

PigLauncher temporarily satisfies that gate only while the large-folder background drawable is being constructed.

### 2. Advanced Material / Blur

Target:

- `com.miui.home.common.utils.BlurUtilities#isBlurSupported()`
- `com.miui.home.common.utils.BuildConfigUtils#isMiuiLauncher()`

PigLauncher does **not** replace `isBlurSupported()` with `true`.

Instead, it lets Xiaomi's original `isBlurSupported()` execute and temporarily satisfies `isMiuiLauncher()` only during that method call. This keeps the original device/system blur capability checks intact while removing the POCO-vs-MIUI launcher gate.

## Target launcher

`com.mi.android.globallauncher`

The initial implementation is based on POCO Launcher:

`RELEASE-6.01.05.2407-06081949`

## GitHub Actions secrets

Add these repository secrets:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`
- `TELEGRAM_BOT_TOKEN`
- `TELEGRAM_CHAT_ID`

Pushes to `main` build debug + signed release APKs. Tags are also uploaded to GitHub Releases, and the signed release APK is sent to the configured Telegram channel.

## LSPosed

Install the signed APK, enable the module, and select only:

`com.mi.android.globallauncher`

Then restart POCO Launcher or reboot.

## Logs

Search LSPosed logs for:

`PigLauncher`

Expected startup entries include:

- `Fix 1 installed: large folder dark-mode background`
- `Fix 2 installed: Advanced Material / blur support`
