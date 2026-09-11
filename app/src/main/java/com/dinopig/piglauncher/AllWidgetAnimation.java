package com.dinopig.piglauncher;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;

final class AllWidgetAnimation {

    private static final String LAUNCHER_WIDGET_VIEW =
            "com.miui.home.launcher.LauncherWidgetView";

    private static final String MAML_WIDGET_VIEW =
            "com.miui.home.launcher.maml.MaMlWidgetView";

    private AllWidgetAnimation() {
    }

    static void install(XposedModule module, ClassLoader classLoader, FeatureSwitches features) {
        hookTransitionAnimation(
                module,
                classLoader,
                LAUNCHER_WIDGET_VIEW,
                features
        );

        hookTransitionAnimation(
                module,
                classLoader,
                MAML_WIDGET_VIEW,
                features
        );
    }

    private static void hookTransitionAnimation(
            XposedModule module,
            ClassLoader classLoader,
            String className,
            FeatureSwitches features
    ) {
        Class<?> clazz = MainHook.findClass(
                className,
                classLoader
        );

        if (clazz == null) {
            return;
        }

        Method method = findMethodRecursive(
                clazz,
                "isUseTransitionAnimation"
        );

        if (method == null) {
            return;
        }

        module.hook(method).intercept(chain -> {
            if (!features.isAllWidgetAnimationEnabled()) {
                return chain.proceed();
            }

            return Boolean.TRUE;
        });
    }

    private static Method findMethodRecursive(
            Class<?> clazz,
            String name,
            Class<?>... parameterTypes
    ) {
        Class<?> current = clazz;

        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(
                        name,
                        parameterTypes
                );
                method.setAccessible(true);
                return method;
            } catch (Throwable ignored) {
                current = current.getSuperclass();
            }
        }

        return null;
    }
}
