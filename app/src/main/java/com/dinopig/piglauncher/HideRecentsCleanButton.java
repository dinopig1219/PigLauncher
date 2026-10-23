package com.dinopig.piglauncher;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;

final class HideRecentsCleanButton {

    private static final String RECENTS_CONTAINER =
            "com.miui.home.recents.views.RecentsContainer";

    private HideRecentsCleanButton() {
    }

    static void install(XposedModule module, ClassLoader classLoader, FeatureSwitches features) {
        Class<?> clazz = MainHook.findClass(RECENTS_CONTAINER, classLoader);

        if (clazz == null) {
            return;
        }

        Method method = MainHook.findMethod(clazz, "isClearContainerVisible");

        if (method == null) {
            return;
        }

        module.hook(method).intercept(chain -> {
            if (!features.isHideRecentsCleanButtonEnabled()) {
                return chain.proceed();
            }

            return Boolean.FALSE;
        });
    }
}
