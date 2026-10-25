package com.dinopig.piglauncher;

import java.lang.ref.WeakReference;
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

    private static final String MINUS_ONE_SCREEN_VIEW =
            "com.miui.home.launcher.MinusOneScreenView";

    private static final String LAUNCHER_STATE =
            "com.miui.home.launcher.LauncherState";

    private static final String STATE_MANAGER =
            "com.miui.home.launcher.LauncherStateManager";

    private static Field controllerField;
    private static Method showOrHideClock;
    private static Method isFolderOpened;
    private static Method isMinusScreenShowing;
    private static Method isGoogleMinusScreenShowing;
    private static Method isInState;
    private static Class<?> launcherStateClass;
    private static volatile Object assistantOverlayState;

    private static volatile WeakReference<Object> launcherRef = new WeakReference<>(null);
    private static volatile boolean foreground;
    private static volatile boolean folderOpen;
    private static volatile boolean minusOpen;
    private static volatile boolean clockHidden;

    private HideStatusBarClock() {
    }

    static void install(XposedModule module, ClassLoader classLoader, FeatureSwitches features) {
        Class<?> baseLauncher = MainHook.findClass(BASE_LAUNCHER, classLoader);
        Class<?> launcher = MainHook.findClass(LAUNCHER, classLoader);
        Class<?> controller = MainHook.findClass(STATUS_BAR_CONTROLLER, classLoader);
        Class<?> minusOneScreenView = MainHook.findClass(MINUS_ONE_SCREEN_VIEW, classLoader);
        Class<?> launcherState = MainHook.findClass(LAUNCHER_STATE, classLoader);
        Class<?> stateManager = MainHook.findClass(STATE_MANAGER, classLoader);

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

        isFolderOpened = accessible(MainHook.findMethod(baseLauncher, "isFolderOpened"));
        isMinusScreenShowing = accessible(MainHook.findMethod(baseLauncher, "isMinusScreenShowing"));
        isGoogleMinusScreenShowing = accessible(MainHook.findMethod(baseLauncher, "isGoogleMinusScreenShowing"));

        if (launcherState != null) {
            launcherStateClass = launcherState;
            isInState = accessible(MainHook.findMethod(baseLauncher, "isInState", launcherState));
        }

        Method onResume = MainHook.findMethod(baseLauncher, "onResume");
        Method onPause = MainHook.findMethod(baseLauncher, "onPause");
        Method onFolderOpenOrClose = MainHook.findMethod(baseLauncher, "onFolderOpenOrClose", boolean.class);
        Method onOverlayScrollEnd = MainHook.findMethod(baseLauncher, "onOverlayScrollEnd");
        Method setCurrentScreenInner = minusOneScreenView == null
                ? null
                : MainHook.findMethod(minusOneScreenView, "setCurrentScreenInner", int.class);

        Method onStateTransitionEnd = stateManager == null || launcherState == null
                ? null
                : MainHook.findMethod(stateManager, "onStateTransitionEnd", launcherState);

        if (onResume != null) {
            module.hook(onResume).intercept(chain -> {
                Object result = chain.proceed();
                Object thisObject = chain.getThisObject();

                if (launcher.isInstance(thisObject)) {
                    launcherRef = new WeakReference<>(thisObject);
                    foreground = true;
                    folderOpen = invokeBoolean(isFolderOpened, thisObject);
                    minusOpen = readMinusOpen(thisObject);
                    apply(thisObject, features.isHideStatusBarClockEnabled());
                }

                return result;
            });
        }

        if (onPause != null) {
            module.hook(onPause).intercept(chain -> {
                Object result = chain.proceed();
                Object thisObject = chain.getThisObject();

                if (launcher.isInstance(thisObject)) {
                    launcherRef = new WeakReference<>(thisObject);
                    foreground = false;
                    apply(thisObject, features.isHideStatusBarClockEnabled());
                }

                return result;
            });
        }

        if (onFolderOpenOrClose != null) {
            module.hook(onFolderOpenOrClose).intercept(chain -> {
                Object result = chain.proceed();
                Object thisObject = chain.getThisObject();

                if (launcher.isInstance(thisObject)) {
                    folderOpen = Boolean.TRUE.equals(chain.getArg(0));
                    apply(thisObject, features.isHideStatusBarClockEnabled());
                }

                return result;
            });
        }

        if (onOverlayScrollEnd != null) {
            module.hook(onOverlayScrollEnd).intercept(chain -> {
                Object result = chain.proceed();
                Object thisObject = chain.getThisObject();

                if (launcher.isInstance(thisObject)) {
                    minusOpen = readMinusOpen(thisObject);
                    apply(thisObject, features.isHideStatusBarClockEnabled());
                }

                return result;
            });
        }

        if (onStateTransitionEnd != null) {
            module.hook(onStateTransitionEnd).intercept(chain -> {
                Object result = chain.proceed();
                Object launcherObject = launcherRef.get();

                if (launcherObject != null) {
                    minusOpen = readMinusOpen(launcherObject);
                    apply(launcherObject, features.isHideStatusBarClockEnabled());
                }

                return result;
            });
        }

        if (setCurrentScreenInner != null) {
            module.hook(setCurrentScreenInner).intercept(chain -> {
                Object result = chain.proceed();
                Object launcherObject = launcherRef.get();

                if (launcherObject != null) {
                    minusOpen = readMinusOpen(launcherObject);
                    apply(launcherObject, features.isHideStatusBarClockEnabled());
                }

                return result;
            });
        }
    }

    private static void apply(Object launcher, boolean enabled) {
        if (enabled && foreground && !folderOpen && !minusOpen) {
            setClockVisible(launcher, false);
            clockHidden = true;
        } else if (clockHidden) {
            setClockVisible(launcher, true);
            clockHidden = false;
        }
    }

    private static boolean readMinusOpen(Object launcher) {
        return invokeBoolean(isMinusScreenShowing, launcher)
                || invokeBoolean(isGoogleMinusScreenShowing, launcher)
                || inAssistantOverlay(launcher);
    }

    private static boolean inAssistantOverlay(Object launcher) {
        try {
            if (isInState == null || launcherStateClass == null) {
                return false;
            }

            if (assistantOverlayState == null) {
                assistantOverlayState = staticValue(launcherStateClass, "ASSISTANT_OVERLAY_STATE");
            }

            return assistantOverlayState != null
                    && Boolean.TRUE.equals(isInState.invoke(launcher, assistantOverlayState));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object staticValue(Class<?> clazz, String name) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean invokeBoolean(Method method, Object target) {
        try {
            return method != null && Boolean.TRUE.equals(method.invoke(target));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Method accessible(Method method) {
        if (method != null) {
            method.setAccessible(true);
        }

        return method;
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
