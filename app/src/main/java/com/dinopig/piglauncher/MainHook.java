package com.dinopig.piglauncher;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class MainHook implements IXposedHookLoadPackage {

    private static final String TARGET_PACKAGE = "com.mi.android.globallauncher";

    private static final String BUILD_CONFIG_UTILS =
            "com.miui.home.common.utils.BuildConfigUtils";

    private static final String DEVICE_CONFIGS =
            "com.miui.home.common.device.DeviceConfigs";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam)
            throws Throwable {

        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        Class<?> buildConfigUtilsClass = XposedHelpers.findClassIfExists(
                BUILD_CONFIG_UTILS,
                lpparam.classLoader
        );

        Class<?> deviceConfigsClass = XposedHelpers.findClassIfExists(
                DEVICE_CONFIGS,
                lpparam.classLoader
        );

        if (buildConfigUtilsClass == null || deviceConfigsClass == null) {
            return;
        }

        XposedHelpers.findAndHookMethod(
                buildConfigUtilsClass,
                "isMiuiLauncher",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (LauncherGate.isActive()) {
                            param.setResult(Boolean.TRUE);
                        }
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                deviceConfigsClass,
                "isDefaultMiuiIcon",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (FolderDarkModeFix.isSmallFolderAppearanceActive()) {
                            param.setResult(Boolean.TRUE);
                        }
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                deviceConfigsClass,
                "isUseDefaultFolderIcon",
                boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (FolderDarkModeFix.isSmallFolderBlurGateActive()
                                && param.args.length > 0
                                && Boolean.TRUE.equals(param.args[0])) {
                            param.setResult(Boolean.TRUE);
                        }
                    }
                }
        );

        FolderDarkModeFix.install(lpparam.classLoader);
        AdvancedTexturesFix.install(lpparam.classLoader);
    }
}
