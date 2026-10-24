package com.dinopig.piglauncher;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;

final class HideStatusBarClock {

    private static final String BASE_LAUNCHER =
            "com.miui.home.launcher.BaseLauncher";

    private static final String LAUNCHER =
            "com.miui.home.launcher.Launcher";

    private static final String STATUS_BAR_CONTROLLER =
            "com.miui.launcher.utils.StatusBarController";

    private static Field controllerField;
    private static Method showOrHideClock;
    private static volatile boolean clockHidden;

    private HideStatusBarClock() {
    }

    static void install(XposedModule module, ClassLoader classLoader, FeatureSwitches features) {
        Class<?> baseLauncher = MainHook.findClass(BASE_LAUNCHER, classLoader);
        Class<?> launcher = MainHook.findClass(LAUNCHER, classLoader);
        Class<?> controller = MainHook.findClass(STATUS_BAR_CONTROLLER, classLoader);

        if (baseLauncher == null || launcher == null || controller == null) {
            return;
        }

        try {
            controllerField = baseLauncher.getDeclaredField("mStatusBarController");
            controllerField.setAccessible(true);
        } catch (Throwable ignored) {
            return;
        }

        showOrHideClock = MainHook.findMethod(controller, "showOrHideClock", boolean.class, Runnable.class);

        if (showOrHideClock == null) {
            return;
        }

        showOrHideClock.setAccessible(true);

        Method onResume = MainHook.findMethod(baseLauncher, "onResume");
        Method onPause = MainHook.findMethod(baseLauncher, "onPause");

        if (onResume != null) {
            module.hook(onResume).intercept(chain -> {
                Object result = chain.proceed();
                Object thisObject = chain.getThisObject();

                if (launcher.isInstance(thisObject)) {
                    if (features.isHideStatusBarClockEnabled()) {
                        setClockVisible(thisObject, false);
                        clockHidden = true;
                    } else if (clockHidden) {
                        setClockVisible(thisObject, true);
                        clockHidden = false;
                    }
                }

                return result;
            });
        }

        if (onPause != null) {
            module.hook(onPause).intercept(chain -> {
                Object result = chain.proceed();
                Object thisObject = chain.getThisObject();

                if (launcher.isInstance(thisObject) && clockHidden) {
                    setClockVisible(thisObject, true);
                    clockHidden = false;
                }

                return result;
            });
        }
    }

    private static void setClockVisible(Object launcher, boolean visible) {
        try {
            Object controller = controllerField.get(launcher);

            if (controller != null) {
                showOrHideClock.invoke(controller, visible, null);
            }
        } catch (Throwable ignored) {
        }
    }
}
