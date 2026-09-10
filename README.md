# PigLauncher

LSPosed module for fixing POCO Launcher large-folder background in dark mode.

## Target

- POCO Launcher package: `com.mi.android.globallauncher`
- Main target classes:
  - `com.miui.home.folder.FolderIcon4x4NormalBackgroundDrawable`
  - `com.miui.home.common.utils.BuildConfigUtils#isMiuiLauncher()`

The module does not globally force `isMiuiLauncher()` to true.
It only changes the result while the large-folder background drawable is being constructed.

## GitHub Actions

Push this project to GitHub.

Then:

`Actions -> Build APK -> Run workflow`

Artifact:

`PigLauncher-release`

## LSPosed scope

Enable the module and select only:

`com.mi.android.globallauncher`

Restart POCO Launcher or reboot the device.

## Logs

Search LSPosed logs for:

`PigLauncher`
