package com.dinopig.piglauncher;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;

final class ShowAllHideApp {

    private static final String HIDE_APP_CONTROLLER =
            "com.miui.home.settings.HideAppController";

    private static final String APP_INFO =
            "com.miui.home.launcher.AppInfo";

    private ShowAllHideApp() {
    }

    static void install(XposedModule module, ClassLoader classLoader) {
        Class<?> controller = MainHook.findClass(HIDE_APP_CONTROLLER, classLoader);
        Class<?> appInfo = MainHook.findClass(APP_INFO, classLoader);

        if (controller == null || appInfo == null) {
            return;
        }

        Method method = MainHook.findMethod(controller, "isHideAppValid", appInfo);

        if (method == null) {
            return;
        }

        module.hook(method).intercept(chain -> Boolean.TRUE);
    }
}
