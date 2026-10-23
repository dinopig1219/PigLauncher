# PigLauncher

An LSPosed module for restoring and unlocking features missing from POCO Launcher compared with Xiaomi HyperOS System Launcher.

## About

PigLauncher is born to improve the POCO Launcher experience by providing modifications to the launcher (since no one do it so this module born).

The module only targets POCO Launcher (`com.mi.android.globallauncher`).

## Features

- Unlock Dark Folder
  - Unlock dark appearance for folders when the system is in Dark Mode. (available in HyperOS System Launcher but nuked in POCO Launcher)

- Unlock Advanced Textures
  - Unlock Advanced Textures. (available in HyperOS System Launcher but nuked in POCO Launcher)

- Grid Folder Icon Fill
  - When a 2×2 or 3×3 folder contains exactly 4 or 9 apps, the last icon is displayed as a normal app icon size, and it is clickable.

- Fix Predictive Back Gesture Speed
  - Fixes the abnormal predictive back animation speed to restore the native speed.

- Unlock All Widgets Animation
  - Enables the back bouncing animation for all widgets.

- Hidden Apps Manager
  - Able to hide any app icon from launcher.

## Notes

Initial development and testing are based on:

`RELEASE-6.01.05.2407-06081949, RELEASE-7.50.06.2592-09011644`

Tested with these versions on OS3 only. Compatibility with other POCO Launcher versions and OS versions may vary.

## How to Use

1. Install PigLauncher.
2. Enable PigLauncher in LSPosed.
3. Make sure POCO Launcher is included in the module scope.
4. Open PigLauncher and enable the features you want.
5. Restart POCO Launcher after changing module settings.
6. Enjoy!

## References

##### GNU General Public License v3.0

- [「XiaomiHelper」by HowieHChen](https://github.com/HowieHChen/XiaomiHelper)

##### GNU Affero General Public License v3

- [「HyperCeiler」by ReChronoRain](https://github.com/ReChronoRain/HyperCeiler)

##### Apache-2.0

- [「miuix」by compose-miuix-ui](https://github.com/compose-miuix-ui/miuix)