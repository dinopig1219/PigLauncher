package com.dinopig.piglauncher;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;

public final class MainHook extends XposedModule {

    static final String TARGET_PACKAGE = "com.mi.android.globallauncher";

    private static final String BUILD_CONFIG_UTILS =
            "com.miui.home.common.utils.BuildConfigUtils";

    private static final String DEVICE_CONFIGS =
            "com.miui.home.common.device.DeviceConfigs";

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!TARGET_PACKAGE.equals(param.getPackageName())) {
            return;
        }

        ClassLoader classLoader = param.getClassLoader();

        Class<?> buildConfigUtilsClass = findClass(
                BUILD_CONFIG_UTILS,
                classLoader
        );

        Class<?> deviceConfigsClass = findClass(
                DEVICE_CONFIGS,
                classLoader
        );

        if (buildConfigUtilsClass == null || deviceConfigsClass == null) {
            return;
        }

        Method isMiuiLauncher = findMethod(
                buildConfigUtilsClass,
                "isMiuiLauncher"
        );

        if (isMiuiLauncher != null) {
            hook(isMiuiLauncher).intercept(chain -> {
                if (LauncherGate.isActive()) {
                    return Boolean.TRUE;
                }
                return chain.proceed();
            });
        }

        Method isDefaultMiuiIcon = findMethod(
                deviceConfigsClass,
                "isDefaultMiuiIcon"
        );

        if (isDefaultMiuiIcon != null) {
            hook(isDefaultMiuiIcon).intercept(chain -> {
                if (FolderDarkModeFix.isSmallFolderAppearanceActive()) {
                    return Boolean.TRUE;
                }
                return chain.proceed();
            });
        }

        Method isUseDefaultFolderIcon = findMethod(
                deviceConfigsClass,
                "isUseDefaultFolderIcon",
                boolean.class
        );

        if (isUseDefaultFolderIcon != null) {
            hook(isUseDefaultFolderIcon).intercept(chain -> {
                if (FolderDarkModeFix.isSmallFolderBlurGateActive()
                        && Boolean.TRUE.equals(chain.getArg(0))) {
                    return Boolean.TRUE;
                }
                return chain.proceed();
            });
        }

        FolderDarkModeFix.install(this, classLoader);
        AdvancedTexturesFix.install(this, classLoader);
    }

    static Class<?> findClass(String className, ClassLoader classLoader) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static Method findMethod(
            Class<?> clazz,
            String methodName,
            Class<?>... parameterTypes
    ) {
        try {
            return clazz.getDeclaredMethod(methodName, parameterTypes);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static Constructor<?>[] findConstructors(Class<?> clazz) {
        try {
            return clazz.getDeclaredConstructors();
        } catch (Throwable ignored) {
            return new Constructor<?>[0];
        }
    }
}
