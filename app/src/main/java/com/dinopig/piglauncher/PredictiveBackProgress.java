package com.dinopig.piglauncher;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import io.github.libxposed.api.XposedModule;

final class PredictiveBackProgress {

    private static final String GESTURE_STUB_VIEW =
            "com.miui.home.recents.GestureStubView";

    private static final String GESTURE_STUB_VIEW_CALLBACK =
            "com.miui.home.recents.GestureStubView$3";

    private static final String GESTURE_BACK_ARROW_VIEW =
            "com.miui.home.recents.GestureBackArrowView";

    private static final String BACK_CALLBACK_CONTROLLER =
            "com.miui.home.recents.OnBackInvokedCallbackController";

    private static final String BACK_MOTION_EVENT_PROVIDER =
            "android.window.BackMotionEventProvider";

    private PredictiveBackProgress() {
    }

    static void install(XposedModule module, ClassLoader classLoader) {
        Class<?> gestureStubViewClass = MainHook.findClass(
                GESTURE_STUB_VIEW,
                classLoader
        );

        Class<?> callbackClass = MainHook.findClass(
                GESTURE_STUB_VIEW_CALLBACK,
                classLoader
        );

        Class<?> gestureBackArrowViewClass = MainHook.findClass(
                GESTURE_BACK_ARROW_VIEW,
                classLoader
        );

        Class<?> backCallbackControllerClass = MainHook.findClass(
                BACK_CALLBACK_CONTROLLER,
                classLoader
        );

        Class<?> backMotionEventProviderClass = MainHook.findClass(
                BACK_MOTION_EVENT_PROVIDER,
                classLoader
        );

        if (gestureStubViewClass == null
                || callbackClass == null
                || gestureBackArrowViewClass == null
                || backCallbackControllerClass == null
                || backMotionEventProviderClass == null) {
            return;
        }

        Field gestureStubViewField = findFirstFieldByType(
                callbackClass,
                gestureStubViewClass
        );

        Field currXField = findFieldRecursive(
                gestureStubViewClass,
                "mCurrX"
        );

        Field currYField = findFieldRecursive(
                gestureStubViewClass,
                "mCurrY"
        );

        Field downXField = findFieldRecursive(
                gestureStubViewClass,
                "mDownX"
        );

        Field gestureBackArrowViewField = findFieldRecursive(
                gestureStubViewClass,
                "mGestureBackArrowView"
        );

        Field gestureStubPosField = findFieldRecursive(
                gestureStubViewClass,
                "mGestureStubPos"
        );

        Field backCallbackControllerField = findFieldRecursive(
                gestureStubViewClass,
                "mOnBackInvokedCallbackController"
        );

        Field windowManagerField = findFieldRecursive(
                gestureStubViewClass,
                "mWindowManager"
        );

        Method onSwipeProgress = findMethodRecursive(
                gestureBackArrowViewClass,
                "onSwipeProgress",
                float.class
        );

        Method onBackProgressed = findMethodByNameAndParameterCount(
                backCallbackControllerClass,
                "onBackProgressed",
                1
        );

        Method getBackMotionEvent = findStaticMethodByNameAndParameterCount(
                backMotionEventProviderClass,
                "getInstance",
                7
        );

        Method onSwipeProcess = findMethodByNameAndParameterCount(
                callbackClass,
                "onSwipeProcess",
                1
        );

        if (gestureStubViewField == null
                || currXField == null
                || currYField == null
                || downXField == null
                || gestureBackArrowViewField == null
                || gestureStubPosField == null
                || backCallbackControllerField == null
                || windowManagerField == null
                || onSwipeProgress == null
                || onBackProgressed == null
                || getBackMotionEvent == null
                || onSwipeProcess == null) {
            return;
        }

        module.hook(onSwipeProcess).intercept(chain -> {
            Object progressValue = chain.getArg(0);

            if (!(progressValue instanceof Number)) {
                return chain.proceed();
            }

            try {
                Object callback = chain.getThisObject();
                Object gestureStubView = gestureStubViewField.get(callback);

                if (gestureStubView == null) {
                    return chain.proceed();
                }

                Object backCallbackController =
                        backCallbackControllerField.get(gestureStubView);

                Object windowManager =
                        windowManagerField.get(gestureStubView);

                Object gestureBackArrowView =
                        gestureBackArrowViewField.get(gestureStubView);

                Float currX = getFloat(
                        currXField,
                        gestureStubView
                );

                Float currY = getFloat(
                        currYField,
                        gestureStubView
                );

                Float downX = getFloat(
                        downXField,
                        gestureStubView
                );

                Integer gestureStubPos = getInt(
                        gestureStubPosField,
                        gestureStubView
                );

                if (backCallbackController == null
                        || windowManager == null
                        || currX == null
                        || currY == null
                        || downX == null
                        || gestureStubPos == null) {
                    return chain.proceed();
                }

                float progress =
                        ((Number) progressValue).floatValue();

                if (gestureBackArrowView != null) {
                    onSwipeProgress.invoke(
                            gestureBackArrowView,
                            progress
                    );
                }

                float threshold = getFullyStretchedThreshold(
                        windowManager,
                        gestureStubView
                );

                if (threshold <= 0.0f) {
                    return chain.proceed();
                }

                float backProgress =
                        Math.abs(currX - downX) / threshold;

                backProgress = Math.max(
                        0.0f,
                        Math.min(1.0f, backProgress)
                );

                Object backMotionEvent =
                        getBackMotionEvent.invoke(
                                null,
                                currX,
                                currY,
                                backProgress,
                                0.0f,
                                0.0f,
                                gestureStubPos == 0 ? 0 : 1,
                                null
                        );

                if (backMotionEvent == null) {
                    return chain.proceed();
                }

                onBackProgressed.invoke(
                        backCallbackController,
                        backMotionEvent
                );

                return null;
            } catch (Throwable ignored) {
                return chain.proceed();
            }
        });
    }

    private static float getFullyStretchedThreshold(
            Object windowManager,
            Object gestureStubView
    ) {
        try {
            Method getMaximumWindowMetrics =
                    findMethodByNameAndParameterCount(
                            windowManager.getClass(),
                            "getMaximumWindowMetrics",
                            0
                    );

            if (getMaximumWindowMetrics == null) {
                return -1.0f;
            }

            Object windowMetrics =
                    getMaximumWindowMetrics.invoke(windowManager);

            if (windowMetrics == null) {
                return -1.0f;
            }

            Method getBounds = findMethodByNameAndParameterCount(
                    windowMetrics.getClass(),
                    "getBounds",
                    0
            );

            if (getBounds == null) {
                return -1.0f;
            }

            Object bounds = getBounds.invoke(windowMetrics);

            if (bounds == null) {
                return -1.0f;
            }

            Method width = findMethodByNameAndParameterCount(
                    bounds.getClass(),
                    "width",
                    0
            );

            if (width == null) {
                return -1.0f;
            }

            Object widthValue = width.invoke(bounds);

            if (!(widthValue instanceof Number)) {
                return -1.0f;
            }

            float screenWidth =
                    ((Number) widthValue).floatValue();

            float density = getWindowDensity(
                    windowMetrics,
                    gestureStubView
            );

            if (density <= 0.0f) {
                return -1.0f;
            }

            return Math.min(
                    screenWidth,
                    density * 412.0f
            );
        } catch (Throwable ignored) {
            return -1.0f;
        }
    }

    private static float getWindowDensity(
            Object windowMetrics,
            Object gestureStubView
    ) {
        try {
            Method getDensity = findMethodByNameAndParameterCount(
                    windowMetrics.getClass(),
                    "getDensity",
                    0
            );

            if (getDensity != null) {
                Object densityValue =
                        getDensity.invoke(windowMetrics);

                if (densityValue instanceof Number) {
                    float density =
                            ((Number) densityValue).floatValue();

                    if (density > 0.0f) {
                        return density;
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            Method getResources = findMethodByNameAndParameterCount(
                    gestureStubView.getClass(),
                    "getResources",
                    0
            );

            if (getResources == null) {
                return -1.0f;
            }

            Object resources = getResources.invoke(gestureStubView);

            if (resources == null) {
                return -1.0f;
            }

            Method getDisplayMetrics =
                    findMethodByNameAndParameterCount(
                            resources.getClass(),
                            "getDisplayMetrics",
                            0
                    );

            if (getDisplayMetrics == null) {
                return -1.0f;
            }

            Object displayMetrics =
                    getDisplayMetrics.invoke(resources);

            if (displayMetrics == null) {
                return -1.0f;
            }

            Field densityField = findFieldRecursive(
                    displayMetrics.getClass(),
                    "density"
            );

            if (densityField == null) {
                return -1.0f;
            }

            Object densityValue =
                    densityField.get(displayMetrics);

            if (densityValue instanceof Number) {
                return ((Number) densityValue).floatValue();
            }
        } catch (Throwable ignored) {
        }

        return -1.0f;
    }

    private static Float getFloat(
            Field field,
            Object target
    ) {
        try {
            Object value = field.get(target);

            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static Integer getInt(
            Field field,
            Object target
    ) {
        try {
            Object value = field.get(target);

            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static Field findFirstFieldByType(
            Class<?> clazz,
            Class<?> type
    ) {
        Class<?> current = clazz;

        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                if (type.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                    } catch (Throwable ignored) {
                    }

                    return field;
                }
            }

            current = current.getSuperclass();
        }

        return null;
    }

    private static Field findFieldRecursive(
            Class<?> clazz,
            String name
    ) {
        Class<?> current = clazz;

        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (Throwable ignored) {
                current = current.getSuperclass();
            }
        }

        return null;
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

    private static Method findMethodByNameAndParameterCount(
            Class<?> clazz,
            String name,
            int parameterCount
    ) {
        Class<?> current = clazz;

        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (name.equals(method.getName())
                        && method.getParameterCount() == parameterCount) {
                    try {
                        method.setAccessible(true);
                    } catch (Throwable ignored) {
                    }

                    return method;
                }
            }

            current = current.getSuperclass();
        }

        return null;
    }

    private static Method findStaticMethodByNameAndParameterCount(
            Class<?> clazz,
            String name,
            int parameterCount
    ) {
        Class<?> current = clazz;

        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (name.equals(method.getName())
                        && method.getParameterCount() == parameterCount
                        && Modifier.isStatic(method.getModifiers())) {
                    try {
                        method.setAccessible(true);
                    } catch (Throwable ignored) {
                    }

                    return method;
                }
            }

            current = current.getSuperclass();
        }

        return null;
    }
}
