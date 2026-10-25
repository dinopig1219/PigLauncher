package com.dinopig.piglauncher;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.libxposed.api.XposedModule;

final class AdvancedTexturesFix {

    private static final String TAG = "PigLauncher";

    private static final String TARGET_PACKAGE = "com.mi.android.globallauncher";

    private static final String BLUR_UTILITIES =
            "com.miui.home.common.utils.BlurUtilities";

    private static final String VIEW_ROOT_IMPL_STUB_IMPL =
            "android.view.ViewRootImplStubImpl";

    private static final String GET_CAMERA_OCCUPIER_STUB_IMPL =
            "miui.util.GetCameraOccupierStubImpl";

    private static final String MIUI_CAMERA_COVERED_MANAGER =
            "android.cameracovered.MiuiCameraCoveredManager";

    private static final String BLUR_SETTING_KEY = "background_blur_enable";

    private static final int MAX_FILTER_LOGS = 16;
    private static final int HEAD_LENGTH = 200;

    private static final AtomicInteger FILTER_LOG_COUNT = new AtomicInteger();

    private static volatile int lastBlurSupported = -1;

    private static volatile XposedModule logger;
    private static volatile ContentObserver blurObserver;

    private AdvancedTexturesFix() {
    }

    static void install(
            XposedModule module,
            ClassLoader classLoader,
            FeatureSwitches features
    ) {
        logger = module;

        Class<?> blurUtilitiesClass = MainHook.findClass(
                BLUR_UTILITIES,
                classLoader
        );

        if (blurUtilitiesClass != null) {
            Method isBlurSupported = MainHook.findMethod(
                    blurUtilitiesClass,
                    "isBlurSupported"
            );

            Method setBackgroundBlurEnabled = MainHook.findMethod(
                    blurUtilitiesClass,
                    "setBackgroundBlurEnabled"
            );

            log("install isBlurSupported=" + (isBlurSupported != null)
                    + " setBackgroundBlurEnabled=" + (setBackgroundBlurEnabled != null));

            if (isBlurSupported != null) {
                module.hook(isBlurSupported).intercept(chain -> {
                    if (!features.isAdvancedTexturesEnabled()) {
                        return chain.proceed();
                    }

                    LauncherGate.enter();

                    try {
                        Object result = chain.proceed();
                        logBlurSupported(blurUtilitiesClass, result);
                        return result;
                    } finally {
                        LauncherGate.exit();
                    }
                });
            }

            if (setBackgroundBlurEnabled != null) {
                module.hook(setBackgroundBlurEnabled).intercept(chain -> {
                    Object result = chain.proceed();

                    if (features.isAdvancedTexturesEnabled()) {
                        logBlurSetting("start");
                        registerBlurObserver(blurUtilitiesClass, setBackgroundBlurEnabled);
                    }

                    log("setBackgroundBlurEnabled ENABLED="
                            + readStatic(blurUtilitiesClass, "IS_BACKGROUND_BLUR_ENABLED"));

                    lastBlurSupported = -1;

                    return result;
                });
            }
        } else {
            log("install BlurUtilities not found");
        }

        hookPassWindowBlurFilter(
                module,
                VIEW_ROOT_IMPL_STUB_IMPL,
                classLoader,
                features,
                Context.class
        );

        hookPassWindowBlurFilter(
                module,
                GET_CAMERA_OCCUPIER_STUB_IMPL,
                classLoader,
                features
        );

        hookPassWindowBlurFilter(
                module,
                MIUI_CAMERA_COVERED_MANAGER,
                classLoader,
                features
        );
    }

    private static void hookPassWindowBlurFilter(
            XposedModule module,
            String className,
            ClassLoader classLoader,
            FeatureSwitches features,
            Class<?>... parameterTypes
    ) {
        String shortName = className.substring(className.lastIndexOf('.') + 1);

        Class<?> clazz = MainHook.findClass(
                className,
                classLoader
        );

        if (clazz == null) {
            log("install " + shortName + " class not found");
            return;
        }

        Method method = MainHook.findMethod(
                clazz,
                "getPassWindowBlurFilterData",
                parameterTypes
        );

        if (method == null) {
            log("install " + shortName + " method not found");
            return;
        }

        log("install " + shortName + " hooked");

        module.hook(method).intercept(chain -> {
            Object result = chain.proceed();

            if (!features.isAdvancedTexturesEnabled()) {
                return result;
            }

            if (!(result instanceof String)) {
                logFilter(shortName, "result=" + result);
                return result;
            }

            String original = (String) result;
            String patched = patchPassWindowBlurFilter(original);

            logFilter(
                    shortName,
                    "len=" + original.length()
                            + " hadTarget=" + original.contains(TARGET_PACKAGE)
                            + " hasTarget=" + patched.contains(TARGET_PACKAGE)
                            + " changed=" + !original.equals(patched)
                            + " head=" + original.substring(0, Math.min(HEAD_LENGTH, original.length()))
            );

            return patched;
        });
    }

    private static String patchPassWindowBlurFilter(String original) {
        if (original == null || original.isEmpty()) {
            return original;
        }

        try {
            JSONObject root = new JSONObject(original);
            JSONObject filter = root.optJSONObject("passwindow_blur_filter");

            if (filter == null && root.optJSONArray("package_name") != null) {
                filter = root;
            }

            if (filter == null) {
                return original;
            }

            JSONArray packages = filter.optJSONArray("package_name");

            if (packages == null) {
                return original;
            }

            for (int i = 0; i < packages.length(); i++) {
                if (TARGET_PACKAGE.equals(packages.optString(i))) {
                    return original;
                }
            }

            packages.put(TARGET_PACKAGE);

            return root.toString();
        } catch (Throwable ignored) {
            return original;
        }
    }

    private static void log(String message) {
        XposedModule module = logger;

        if (module == null) {
            return;
        }

        try {
            module.log(Log.INFO, TAG, message);
        } catch (Throwable ignored) {
        }
    }

    private static void logFilter(String name, String message) {
        if (FILTER_LOG_COUNT.incrementAndGet() > MAX_FILTER_LOGS) {
            return;
        }

        log("filter[" + name + "] " + message);
    }

    private static void logBlurSupported(Class<?> blurUtilitiesClass, Object result) {
        int value = Boolean.TRUE.equals(result) ? 1 : 0;

        if (value == lastBlurSupported) {
            return;
        }

        lastBlurSupported = value;

        log("isBlurSupported=" + result
                + " SUPPORTED=" + readStatic(blurUtilitiesClass, "IS_BACKGROUND_BLUR_SUPPORTED")
                + " ENABLED=" + readStatic(blurUtilitiesClass, "IS_BACKGROUND_BLUR_ENABLED"));
    }

    private static void logBlurSetting(String reason) {
        ContentResolver resolver = currentResolver();

        if (resolver == null) {
            log("blurSetting " + reason + " resolver=null");
            return;
        }

        log("blurSetting " + reason
                + " secure=" + Settings.Secure.getInt(resolver, BLUR_SETTING_KEY, -1)
                + " system=" + Settings.System.getInt(resolver, BLUR_SETTING_KEY, -1)
                + " global=" + Settings.Global.getInt(resolver, BLUR_SETTING_KEY, -1)
                + " statusDefault=" + systemProperty("persist.sys.background_blur_status_default")
                + " supported=" + systemProperty("persist.sys.background_blur_supported")
                + " release=" + systemProperty("persist.sys.advanced_visual_release"));
    }

    private static void registerBlurObserver(
            Class<?> blurUtilitiesClass,
            Method setBackgroundBlurEnabled
    ) {
        if (blurObserver != null) {
            return;
        }

        try {
            ContentResolver resolver = currentResolver();

            if (resolver == null) {
                return;
            }

            blurObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    Object before = readStatic(blurUtilitiesClass, "IS_BACKGROUND_BLUR_ENABLED");

                    logBlurSetting("changed enabledBefore=" + before);

                    int value = Settings.Secure.getInt(resolver, BLUR_SETTING_KEY, -1);

                    if (value == 1 && Boolean.FALSE.equals(before)) {
                        try {
                            setBackgroundBlurEnabled.invoke(null);
                        } catch (Throwable throwable) {
                            log("blurSetting refresh failed " + throwable);
                        }
                    }
                }
            };

            resolver.registerContentObserver(
                    Settings.Secure.getUriFor(BLUR_SETTING_KEY),
                    false,
                    blurObserver
            );

            log("blurSetting observer registered");
        } catch (Throwable throwable) {
            blurObserver = null;
            log("blurSetting observer failed " + throwable);
        }
    }

    private static ContentResolver currentResolver() {
        try {
            Object application = Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication")
                    .invoke(null);

            return application instanceof Context
                    ? ((Context) application).getContentResolver()
                    : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String systemProperty(String key) {
        try {
            Object value = Class.forName("android.os.SystemProperties")
                    .getMethod("get", String.class, String.class)
                    .invoke(null, key, "");

            return String.valueOf(value);
        } catch (Throwable ignored) {
            return "?";
        }
    }

    private static Object readStatic(Class<?> clazz, String name) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(null);
        } catch (Throwable ignored) {
            return "?";
        }
    }
}
