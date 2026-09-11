package com.dinopig.piglauncher;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;

final class AdvancedTexturesFix {

    private static final String TARGET_PACKAGE = "com.mi.android.globallauncher";

    private static final String BLUR_UTILITIES =
            "com.miui.home.common.utils.BlurUtilities";

    private static final String VIEW_ROOT_IMPL_STUB_IMPL =
            "android.view.ViewRootImplStubImpl";

    private static final String GET_CAMERA_OCCUPIER_STUB_IMPL =
            "miui.util.GetCameraOccupierStubImpl";

    private static final String MIUI_CAMERA_COVERED_MANAGER =
            "android.cameracovered.MiuiCameraCoveredManager";

    private AdvancedTexturesFix() {
    }

    static void install(
            XposedModule module,
            ClassLoader classLoader,
            FeatureSwitches features
    ) {
        Class<?> blurUtilitiesClass = MainHook.findClass(
                BLUR_UTILITIES,
                classLoader
        );

        if (blurUtilitiesClass != null) {
            Method isBlurSupported = MainHook.findMethod(
                    blurUtilitiesClass,
                    "isBlurSupported"
            );

            if (isBlurSupported != null) {
                module.hook(isBlurSupported).intercept(chain -> {
                    if (!features.isAdvancedTexturesEnabled()) {
                        return chain.proceed();
                    }

                    LauncherGate.enter();

                    try {
                        return chain.proceed();
                    } finally {
                        LauncherGate.exit();
                    }
                });
            }
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
        Class<?> clazz = MainHook.findClass(
                className,
                classLoader
        );

        if (clazz == null) {
            return;
        }

        Method method = MainHook.findMethod(
                clazz,
                "getPassWindowBlurFilterData",
                parameterTypes
        );

        if (method == null) {
            return;
        }

        module.hook(method).intercept(chain -> {
            Object result = chain.proceed();

            if (!features.isAdvancedTexturesEnabled()) {
                return result;
            }

            if (!(result instanceof String)) {
                return result;
            }

            return patchPassWindowBlurFilter((String) result);
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
}
