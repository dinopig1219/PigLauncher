package com.dinopig.piglauncher;

import android.os.Handler;
import android.os.Looper;
import android.view.View;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import io.github.libxposed.api.XposedModule;

final class HideStatusBarClock {

    private static final String BASE_LAUNCHER =
            "com.miui.home.launcher.BaseLauncher";

    private static final String LAUNCHER =
            "com.miui.home.launcher.Launcher";

    private static final String STATUS_BAR_CONTROLLER =
            "com.miui.launcher.utils.StatusBarController";

    private static final String WORKSPACE =
            "com.miui.home.launcher.Workspace";

    private static final String STATE_MANAGER =
            "com.miui.home.launcher.LauncherStateManager";

    private static final String LAUNCHER_STATE =
            "com.miui.home.launcher.LauncherState";

    private static final String MINUS_ONE_SCREEN_VIEW =
            "com.miui.home.launcher.MinusOneScreenView";

    private static final String GADGET_INFO =
            "com.miui.home.launcher.gadget.GadgetInfo";

    private static final String MAML_WIDGET_INFO =
            "com.miui.home.launcher.maml.MaMlWidgetInfo";

    private static final String MAML_WIDGET_UTILITIES =
            "com.miui.home.launcher.maml.MaMlWidgetUtilities";

    private static final int CLOCK_CATEGORY_ID = 2;
    private static final long UPDATE_DELAY_MS = 100L;

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    private static FeatureSwitches features;
    private static WeakReference<Object> launcherRef = new WeakReference<>(null);
    private static volatile boolean paused;
    private static volatile boolean clockHidden;

    private static final Runnable UPDATE = () -> {
        Object launcher = launcherRef.get();

        if (launcher != null) {
            update(launcher, null);
        }
    };

    private static Field controllerField;
    private static Field workspaceField;
    private static Field gadgetsField;
    private static Field mamlViewsField;
    private static Field pauseField;
    private static Field duringMinusOneField;
    private static Field workspaceLoadingField;
    private static Field googleMinusField;
    private static Field screenIdField;

    private static Method showOrHideClock;
    private static Method changeStatusBarMode;
    private static Method isMinusScreenShowing;
    private static Method isFolderOpened;
    private static Method isInState;
    private static Method getCurrentScreenId;
    private static Method getScreenIdByIndex;
    private static Method getCategoryId;
    private static Method isMamlClock;

    private static Class<?> gadgetInfoClass;
    private static Class<?> mamlWidgetInfoClass;

    private static Object feedState;
    private static Object allAppsState;
    private static Object feedOverlayState;
    private static Object assistantOverlayState;

    private HideStatusBarClock() {
    }

    static void install(XposedModule module, ClassLoader classLoader, FeatureSwitches switches) {
        features = switches;

        Class<?> baseLauncher = MainHook.findClass(BASE_LAUNCHER, classLoader);
        Class<?> launcher = MainHook.findClass(LAUNCHER, classLoader);
        Class<?> controller = MainHook.findClass(STATUS_BAR_CONTROLLER, classLoader);
        Class<?> workspace = MainHook.findClass(WORKSPACE, classLoader);
        Class<?> stateManager = MainHook.findClass(STATE_MANAGER, classLoader);
        Class<?> launcherState = MainHook.findClass(LAUNCHER_STATE, classLoader);
        Class<?> minusOneScreenView = MainHook.findClass(MINUS_ONE_SCREEN_VIEW, classLoader);
        Class<?> mamlUtilities = MainHook.findClass(MAML_WIDGET_UTILITIES, classLoader);

        gadgetInfoClass = MainHook.findClass(GADGET_INFO, classLoader);
        mamlWidgetInfoClass = MainHook.findClass(MAML_WIDGET_INFO, classLoader);

        if (baseLauncher == null || launcher == null || controller == null
                || workspace == null || launcherState == null) {
            return;
        }

        controllerField = field(baseLauncher, "mStatusBarController");
        workspaceField = field(baseLauncher, "mWorkspace");
        gadgetsField = field(baseLauncher, "mGadgets");
        mamlViewsField = field(baseLauncher, "mMaMlViews");
        pauseField = field(baseLauncher, "mIsPause");
        duringMinusOneField = field(baseLauncher, "mDuringMinusOneStartActivityForResult");
        workspaceLoadingField = field(baseLauncher, "mWorkspaceLoading");
        googleMinusField = field(baseLauncher, "mIsGoogleMinusScreenShowing");

        showOrHideClock = method(controller, "showOrHideClock", boolean.class, Runnable.class);
        changeStatusBarMode = method(baseLauncher, "changeStatusBarMode");
        isMinusScreenShowing = method(baseLauncher, "isMinusScreenShowing");
        isFolderOpened = method(baseLauncher, "isFolderOpened");
        isInState = method(baseLauncher, "isInState", launcherState);
        getCurrentScreenId = method(workspace, "getCurrentScreenId");
        getScreenIdByIndex = method(workspace, "getScreenIdByIndex", int.class);

        if (gadgetInfoClass != null) {
            getCategoryId = method(gadgetInfoClass, "getCategoryId");
        }

        if (mamlUtilities != null && mamlWidgetInfoClass != null) {
            isMamlClock = method(mamlUtilities, "isClock", mamlWidgetInfoClass);
        }

        if (controllerField == null || workspaceField == null || gadgetsField == null
                || mamlViewsField == null || showOrHideClock == null || isInState == null
                || getCurrentScreenId == null || getScreenIdByIndex == null) {
            return;
        }

        feedState = staticValue(launcherState, "FEED_STATE");
        allAppsState = staticValue(launcherState, "ALL_APPS");
        feedOverlayState = staticValue(launcherState, "FEED_OVERLAY_STATE");
        assistantOverlayState = staticValue(launcherState, "ASSISTANT_OVERLAY_STATE");

        Method onResume = MainHook.findMethod(baseLauncher, "onResume");
        Method onPause = MainHook.findMethod(baseLauncher, "onPause");
        Method beforeSnapToScreen = MainHook.findMethod(workspace, "beforeSnapToScreen", int.class);

        if (onResume != null) {
            module.hook(onResume).intercept(chain -> {
                Object result = chain.proceed();
                Object thisObject = chain.getThisObject();

                if (launcher.isInstance(thisObject)) {
                    launcherRef = new WeakReference<>(thisObject);
                    paused = false;
                    scheduleUpdate();
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
                    paused = true;
                    update(thisObject, null);
                }

                return result;
            });
        }

        if (beforeSnapToScreen != null) {
            module.hook(beforeSnapToScreen).intercept(chain -> {
                Object result = chain.proceed();
                Object launcherObject = launcherRef.get();

                if (launcherObject != null) {
                    try {
                        Object screenId = getScreenIdByIndex.invoke(
                                chain.getThisObject(),
                                chain.getArg(0)
                        );

                        update(launcherObject, (Long) screenId);
                    } catch (Throwable ignored) {
                    }
                }

                return result;
            });
        }

        hookSchedule(module, stateManager, "onStateTransitionEnd", launcherState);
        hookSchedule(module, minusOneScreenView, "setCurrentScreenInner", int.class);
        hookSchedule(module, baseLauncher, "onFolderOpenAnimFinish");
        hookSchedule(module, baseLauncher, "onFolderCloseAnimFinish");
        hookSchedule(module, baseLauncher, "onOverlayScrollEnd");
        hookSchedule(module, baseLauncher, "finishLoading", boolean.class, boolean.class);
    }

    private static void hookSchedule(
            XposedModule module,
            Class<?> clazz,
            String name,
            Class<?>... parameterTypes
    ) {
        if (clazz == null) {
            return;
        }

        Method method = MainHook.findMethod(clazz, name, parameterTypes);

        if (method == null) {
            return;
        }

        module.hook(method).intercept(chain -> {
            Object result = chain.proceed();
            scheduleUpdate();
            return result;
        });
    }

    private static void scheduleUpdate() {
        HANDLER.removeCallbacks(UPDATE);
        HANDLER.postDelayed(UPDATE, UPDATE_DELAY_MS);
    }

    private static void update(Object launcher, Long screenId) {
        try {
            if (!features.isHideStatusBarClockEnabled()) {
                if (clockHidden) {
                    setClockVisible(launcher, true);
                    clockHidden = false;
                }

                return;
            }

            if (readBoolean(duringMinusOneField, launcher)
                    || readBoolean(workspaceLoadingField, launcher)) {
                return;
            }

            long currentScreenId = screenId != null
                    ? screenId
                    : (Long) getCurrentScreenId.invoke(workspaceField.get(launcher));

            boolean showClock = paused
                    || readBoolean(pauseField, launcher)
                    || invokeBoolean(isMinusScreenShowing, launcher)
                    || readBoolean(googleMinusField, launcher)
                    || invokeBoolean(isFolderOpened, launcher)
                    || inState(launcher, feedState)
                    || inState(launcher, allAppsState)
                    || inState(launcher, feedOverlayState)
                    || inState(launcher, assistantOverlayState)
                    || !screenHasClockWidget(launcher, currentScreenId);

            setClockVisible(launcher, showClock);
            clockHidden = !showClock;
        } catch (Throwable ignored) {
        }
    }

    private static void setClockVisible(Object launcher, boolean visible) throws Throwable {
        Object controller = controllerField.get(launcher);

        if (controller == null) {
            return;
        }

        Runnable callback = () -> HANDLER.post(() -> {
            try {
                if (changeStatusBarMode != null) {
                    changeStatusBarMode.invoke(launcher);
                }
            } catch (Throwable ignored) {
            }
        });

        showOrHideClock.invoke(controller, visible, callback);
    }

    private static boolean screenHasClockWidget(Object launcher, long screenId) throws Throwable {
        for (Field listField : new Field[]{gadgetsField, mamlViewsField}) {
            Object list = listField.get(launcher);

            if (!(list instanceof ArrayList)) {
                continue;
            }

            for (Object item : new ArrayList<Object>((ArrayList<?>) list)) {
                if (!(item instanceof View)) {
                    continue;
                }

                Object info = ((View) item).getTag();

                if (info != null && screenIdOf(info) == screenId && isClockWidget(info)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isClockWidget(Object info) throws Throwable {
        if (gadgetInfoClass != null && gadgetInfoClass.isInstance(info)) {
            return getCategoryId != null
                    && ((Integer) getCategoryId.invoke(info)).intValue() == CLOCK_CATEGORY_ID;
        }

        if (mamlWidgetInfoClass != null && mamlWidgetInfoClass.isInstance(info)) {
            return isMamlClock != null
                    && Boolean.TRUE.equals(isMamlClock.invoke(null, info));
        }

        return false;
    }

    private static long screenIdOf(Object info) throws Throwable {
        if (screenIdField == null) {
            Class<?> clazz = info.getClass();

            while (clazz != null && screenIdField == null) {
                try {
                    screenIdField = clazz.getDeclaredField("screenId");
                    screenIdField.setAccessible(true);
                } catch (NoSuchFieldException ignored) {
                    clazz = clazz.getSuperclass();
                }
            }
        }

        return screenIdField == null ? Long.MIN_VALUE : screenIdField.getLong(info);
    }

    private static boolean inState(Object launcher, Object state) throws Throwable {
        return state != null && Boolean.TRUE.equals(isInState.invoke(launcher, state));
    }

    private static boolean readBoolean(Field field, Object target) throws Throwable {
        return field != null && field.getBoolean(target);
    }

    private static boolean invokeBoolean(Method method, Object target) throws Throwable {
        return method != null && Boolean.TRUE.equals(method.invoke(target));
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

    private static Field field(Class<?> clazz, String name) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method method(Class<?> clazz, String name, Class<?>... parameterTypes) {
        Method method = MainHook.findMethod(clazz, name, parameterTypes);

        if (method != null) {
            method.setAccessible(true);
        }

        return method;
    }
}
