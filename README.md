# PigLauncher

An LSPosed module for restoring and unlocking features missing from POCO Launcher compared with Xiaomi HyperOS System Launcher.

## About

PigLauncher is designed to improve the POCO Launcher experience by restoring features that are available in HyperOS System Launcher but are missing or limited in POCO Launcher (and also add some additional hooks since no one do it for POCO Launcher).

The module only targets POCO Launcher (`com.mi.android.globallauncher`).

## Features

- Unlock Dark Folder
  - Makes folders use a dark appearance when the system is in Dark Mode.

- Unlock Advanced Textures
  - Restores the missing Advanced Textures feature in POCO Launcher.

- Grid Folder Icon Fill
  - When a 2×2 or 3×3 folder contains exactly 4 or 9 apps, the last icon is displayed as a normal clickable app icon.

- Fix Predictive Back Gesture Speed
  - Fixes the abnormal predictive back animation speed in POCO Launcher to better match the native behaviour.

- Unlock All Widgets Animation
  - Enables the back bouncing animation for all widgets.

## Notes

Initial development and testing are based on:

`RELEASE-6.01.05.2407-06081949`

Tested on OS3. Compatibility with other POCO Launcher versions and system versions may vary.

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