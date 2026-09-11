package com.dinopig.piglauncher;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

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

    static void install(ClassLoader classLoader) {
        Class<?> blurUtilitiesClass = XposedHelpers.findClassIfExists(
                BLUR_UTILITIES,
                classLoader
        );

        if (blurUtilitiesClass != null) {
            try {
                XposedHelpers.findAndHookMethod(
                        blurUtilitiesClass,
                        "isBlurSupported",
                        createMiuiLauncherGateHook()
                );
            } catch (Throwable ignored) {
            }
        }

        hookPassWindowBlurFilter(
                VIEW_ROOT_IMPL_STUB_IMPL,
                classLoader,
                Context.class
        );

        hookPassWindowBlurFilter(
                GET_CAMERA_OCCUPIER_STUB_IMPL,
                classLoader
        );

        hookPassWindowBlurFilter(
                MIUI_CAMERA_COVERED_MANAGER,
                classLoader
        );
    }

    private static XC_MethodHook createMiuiLauncherGateHook() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                LauncherGate.enter();
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                LauncherGate.exit();
            }
        };
    }

    private static void hookPassWindowBlurFilter(
            String className,
            ClassLoader classLoader,
            Object... parameterTypes
    ) {
        Class<?> clazz = XposedHelpers.findClassIfExists(
                className,
                classLoader
        );

        if (clazz == null) {
            return;
        }

        Object[] args = new Object[parameterTypes.length + 1];

        System.arraycopy(
                parameterTypes,
                0,
                args,
                0,
                parameterTypes.length
        );

        args[parameterTypes.length] =
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object result = param.getResult();

                        if (!(result instanceof String)) {
                            return;
                        }

                        String original = (String) result;
                        String patched = patchPassWindowBlurFilter(original);

                        if (!original.equals(patched)) {
                            param.setResult(patched);
                        }
                    }
                };

        try {
            XposedHelpers.findAndHookMethod(
                    clazz,
                    "getPassWindowBlurFilterData",
                    args
            );
        } catch (Throwable ignored) {
        }
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
