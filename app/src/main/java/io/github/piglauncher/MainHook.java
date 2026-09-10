package io.github.piglauncher;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "PigLauncher";
    private static final String TARGET_PACKAGE = "com.mi.android.globallauncher";

    private static final String LARGE_FOLDER_BACKGROUND =
            "com.miui.home.folder.FolderIcon4x4NormalBackgroundDrawable";

    private static final String BLUR_UTILITIES =
            "com.miui.home.common.utils.BlurUtilities";

    private static final String BUILD_CONFIG_UTILS =
            "com.miui.home.common.utils.BuildConfigUtils";

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
         * Do not globally turn POCO Launcher into MIUI Launcher.
         * isMiuiLauncher() is overridden only inside explicitly marked
         * execution windows.
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

        // Stable fix: large-folder dark-mode background.
        installLargeFolderDarkModeFix(lpparam.classLoader);

        /*
         * Advanced Material / Blur experimental fix is intentionally disabled.
         *
         * Testing showed that merely unlocking BlurUtilities.isBlurSupported()
         * can make some Advanced Material surfaces become black instead of
         * receiving the expected material blur.
         *
         * Keep the implementation below for further investigation, but do not
         * install it until the actual Hyper Material / BottomSheet apply path
         * has been identified.
         */
        // installAdvancedMaterialBlurFix(lpparam.classLoader);

        log("hook installation finished");
    }

    /**
     * Fix 1: Large Folder Dark Mode
     *
     * POCO contains the same dark large-folder resources, but the upstream
     * drawable constructor additionally gates them behind isMiuiLauncher().
     *
     * Only while the drawable is being constructed do we let that single
     * launcher-type check pass. All other POCO launcher-type checks keep
     * their original behavior.
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
     * Experimental Fix 2: Advanced Material / Blur
     *
     * CURRENTLY NOT INSTALLED.
     *
     * This implementation only removes the POCO launcher-type gate while
     * Xiaomi's original BlurUtilities.isBlurSupported() executes.
     *
     * It is retained for research, but enabling it currently causes some
     * Advanced Material surfaces to render black because the lower-level
     * Hyper Material / BottomSheet blur application path still needs to be
     * identified and matched with the system launcher.
     */
    @SuppressWarnings("unused")
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
