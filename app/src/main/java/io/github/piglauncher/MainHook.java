package io.github.piglauncher;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "PigLauncher";
    private static final String TARGET_PACKAGE = "com.mi.android.globallauncher";

    // Fix 1: large folder dark-mode background
    private static final String LARGE_FOLDER_BACKGROUND =
            "com.miui.home.folder.FolderIcon4x4NormalBackgroundDrawable";

    // Fix 2: Advanced Material / background blur support
    private static final String BLUR_UTILITIES =
            "com.miui.home.common.utils.BlurUtilities";

    private static final String BUILD_CONFIG_UTILS =
            "com.miui.home.common.utils.BuildConfigUtils";

    /*
     * We do NOT globally turn POCO Launcher into MIUI Launcher.
     *
     * Instead, this depth is > 0 only while code that legitimately needs
     * the MIUI-launcher gate bypass is executing:
     *
     *  1. FolderIcon4x4NormalBackgroundDrawable constructors
     *  2. BlurUtilities.isBlurSupported()
     *
     * If either original POCO code path calls BuildConfigUtils.isMiuiLauncher()
     * during that window, only that call is changed to true.
     *
     * A depth counter (rather than a boolean) keeps nested calls safe.
     */
    private static final ThreadLocal<Integer> MIUI_LAUNCHER_OVERRIDE_DEPTH =
            new ThreadLocal<>();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam)
            throws Throwable {

        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        log("loaded " + lpparam.packageName);

        Class<?> buildConfigUtilsClass = XposedHelpers.findClassIfExists(
                BUILD_CONFIG_UTILS,
                lpparam.classLoader
        );

        if (buildConfigUtilsClass == null) {
            log("BuildConfigUtils not found; no fixes can be installed");
            return;
        }

        /*
         * Central gate override.
         *
         * IMPORTANT:
         * isMiuiLauncher() still returns POCO's original value everywhere
         * else in the launcher.
         */
        XposedHelpers.findAndHookMethod(
                buildConfigUtilsClass,
                "isMiuiLauncher",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (getOverrideDepth() > 0) {
                            param.setResult(Boolean.TRUE);
                        }
                    }
                }
        );

        installLargeFolderDarkModeFix(lpparam.classLoader);
        installAdvancedMaterialBlurFix(lpparam.classLoader);

        log("hook installation finished");
    }

    /**
     * Fix 1
     *
     * POCO has the dark large-folder resources, but the upstream constructor
     * additionally checks BuildConfigUtils.isMiuiLauncher().
     *
     * We temporarily satisfy that gate only while this drawable is being
     * constructed, so the original launcher code can choose its own dark
     * colors/stroke/animation resources.
     */
    private static void installLargeFolderDarkModeFix(ClassLoader classLoader) {
        Class<?> backgroundClass = XposedHelpers.findClassIfExists(
                LARGE_FOLDER_BACKGROUND,
                classLoader
        );

        if (backgroundClass == null) {
            log("Fix 1 skipped: class not found: " + LARGE_FOLDER_BACKGROUND);
            return;
        }

        XposedBridge.hookAllConstructors(
                backgroundClass,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        enterMiuiLauncherOverride();
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        exitMiuiLauncherOverride();
                    }
                }
        );

        log("Fix 1 installed: large folder dark-mode background");
    }

    /**
     * Fix 2
     *
     * BlurUtilities.isBlurSupported() in the POCO build reaches an
     * isMiuiLauncher() gate. That makes the method report no blur support
     * even when the device/system blur capability itself is available.
     *
     * Rather than replacing isBlurSupported() with a hardcoded true, we let
     * the ORIGINAL method execute and only satisfy isMiuiLauncher() while it
     * runs. This preserves all of Xiaomi's other checks, such as device/system
     * blur support and whether blur is actually enabled.
     *
     * This is intentionally safer than:
     *   - globally replacing isMiuiLauncher() with true, or
     *   - replacing isBlurSupported() with true.
     */
    private static void installAdvancedMaterialBlurFix(ClassLoader classLoader) {
        Class<?> blurUtilitiesClass = XposedHelpers.findClassIfExists(
                BLUR_UTILITIES,
                classLoader
        );

        if (blurUtilitiesClass == null) {
            log("Fix 2 skipped: class not found: " + BLUR_UTILITIES);
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(
                    blurUtilitiesClass,
                    "isBlurSupported",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            enterMiuiLauncherOverride();
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            exitMiuiLauncherOverride();
                        }
                    }
            );

            log("Fix 2 installed: Advanced Material / blur support");
        } catch (Throwable t) {
            log("Fix 2 failed to hook BlurUtilities.isBlurSupported(): " + t);
        }
    }

    private static void enterMiuiLauncherOverride() {
        MIUI_LAUNCHER_OVERRIDE_DEPTH.set(getOverrideDepth() + 1);
    }

    private static void exitMiuiLauncherOverride() {
        int depth = getOverrideDepth() - 1;

        if (depth <= 0) {
            MIUI_LAUNCHER_OVERRIDE_DEPTH.remove();
        } else {
            MIUI_LAUNCHER_OVERRIDE_DEPTH.set(depth);
        }
    }

    private static int getOverrideDepth() {
        Integer depth = MIUI_LAUNCHER_OVERRIDE_DEPTH.get();
        return depth == null ? 0 : depth;
    }

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }
}
