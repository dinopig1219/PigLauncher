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

    private static final String BUILD_CONFIG_UTILS =
            "com.miui.home.common.utils.BuildConfigUtils";

    private static final ThreadLocal<Integer> LARGE_FOLDER_CONSTRUCTOR_DEPTH =
            new ThreadLocal<>();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam)
            throws Throwable {

        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        log("loaded " + lpparam.packageName);

        Class<?> backgroundClass = XposedHelpers.findClassIfExists(
                LARGE_FOLDER_BACKGROUND,
                lpparam.classLoader
        );

        if (backgroundClass == null) {
            log("target class not found: " + LARGE_FOLDER_BACKGROUND);
            return;
        }

        Class<?> buildConfigUtilsClass = XposedHelpers.findClassIfExists(
                BUILD_CONFIG_UTILS,
                lpparam.classLoader
        );

        if (buildConfigUtilsClass == null) {
            log("target class not found: " + BUILD_CONFIG_UTILS);
            return;
        }

        XposedBridge.hookAllConstructors(
                backgroundClass,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        int depth = getDepth();
                        LARGE_FOLDER_CONSTRUCTOR_DEPTH.set(depth + 1);
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        int depth = getDepth() - 1;

                        if (depth <= 0) {
                            LARGE_FOLDER_CONSTRUCTOR_DEPTH.remove();
                        } else {
                            LARGE_FOLDER_CONSTRUCTOR_DEPTH.set(depth);
                        }
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                buildConfigUtilsClass,
                "isMiuiLauncher",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (getDepth() > 0) {
                            param.setResult(Boolean.TRUE);
                            log("forcing isMiuiLauncher=true inside large-folder background constructor");
                        }
                    }
                }
        );

        log("hooks installed");
    }

    private static int getDepth() {
        Integer depth = LARGE_FOLDER_CONSTRUCTOR_DEPTH.get();
        return depth == null ? 0 : depth;
    }

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }
}
