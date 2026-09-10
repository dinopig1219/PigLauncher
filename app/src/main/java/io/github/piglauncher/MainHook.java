package io.github.piglauncher;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class MainHook implements IXposedHookLoadPackage {

    private static final String TARGET_PACKAGE = "com.mi.android.globallauncher";

    private static final String LARGE_FOLDER_BACKGROUND =
            "com.miui.home.folder.FolderIcon4x4NormalBackgroundDrawable";

    private static final String SMALL_FOLDER =
            "com.miui.home.folder.FolderIcon1x1";

    private static final String FOLDER_INFO =
            "com.miui.home.model.api.IFolderInfo";

    private static final String FOLDER =
            "com.miui.home.folder.api.IFolder";

    private static final String BLUR_UTILITIES =
            "com.miui.home.common.utils.BlurUtilities";

    private static final String BUILD_CONFIG_UTILS =
            "com.miui.home.common.utils.BuildConfigUtils";

    private static final String VIEW_ROOT_IMPL_STUB_IMPL =
            "android.view.ViewRootImplStubImpl";

    private static final String GET_CAMERA_OCCUPIER_STUB_IMPL =
            "miui.util.GetCameraOccupierStubImpl";

    private static final String MIUI_CAMERA_COVERED_MANAGER =
            "android.cameracovered.MiuiCameraCoveredManager";

    private static final ThreadLocal<Integer> MIUI_LAUNCHER_OVERRIDE_DEPTH =
            new ThreadLocal<>();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam)
            throws Throwable {

        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        Class<?> buildConfigUtilsClass = XposedHelpers.findClassIfExists(
                BUILD_CONFIG_UTILS,
                lpparam.classLoader
        );

        if (buildConfigUtilsClass == null) {
            return;
        }

        XposedHelpers.findAndHookMethod(
                buildConfigUtilsClass,
                "isMiuiLauncher",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (getOverrideDepth() > 0) {
                            param.setResult(Boolean.TRUE);
                        }
                    }
                }
        );

        installFolderDarkModeFix(lpparam.classLoader);
        installAdvancedTexturesFix(lpparam.classLoader);
    }

    private static void installFolderDarkModeFix(ClassLoader classLoader) {
        Class<?> largeFolderBackgroundClass = XposedHelpers.findClassIfExists(
                LARGE_FOLDER_BACKGROUND,
                classLoader
        );

        if (largeFolderBackgroundClass != null) {
            XposedBridge.hookAllConstructors(
                    largeFolderBackgroundClass,
                    createGateHook()
            );
        }

        Class<?> smallFolderClass = XposedHelpers.findClassIfExists(
                SMALL_FOLDER,
                classLoader
        );

        if (smallFolderClass == null) {
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(
                    smallFolderClass,
                    "refreshBackground",
                    createGateHook()
            );
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(
                    smallFolderClass,
                    "drawChild",
                    Canvas.class,
                    View.class,
                    long.class,
                    createGateHook()
            );
        } catch (Throwable ignored) {
        }

        Class<?> folderInfoClass = XposedHelpers.findClassIfExists(
                FOLDER_INFO,
                classLoader
        );

        Class<?> folderClass = XposedHelpers.findClassIfExists(
                FOLDER,
                classLoader
        );

        if (folderInfoClass != null && folderClass != null) {
            try {
                XposedHelpers.findAndHookMethod(
                        smallFolderClass,
                        "setup",
                        folderInfoClass,
                        folderClass,
                        createGateHook()
                );
            } catch (Throwable ignored) {
            }
        }

        try {
            XposedHelpers.findAndHookMethod(
                    smallFolderClass,
                    "updateFolderIconBg$lambda$2",
                    smallFolderClass,
                    createGateHook()
            );
        } catch (Throwable ignored) {
        }
    }

    private static void installAdvancedTexturesFix(ClassLoader classLoader) {
        Class<?> blurUtilitiesClass = XposedHelpers.findClassIfExists(
                BLUR_UTILITIES,
                classLoader
        );

        if (blurUtilitiesClass != null) {
            try {
                XposedHelpers.findAndHookMethod(
                        blurUtilitiesClass,
                        "isBlurSupported",
                        createGateHook()
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

    private static XC_MethodHook createGateHook() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                enterMiuiLauncherOverride();
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                exitMiuiLauncherOverride();
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

    private static void enterMiuiLauncherOverride() {
        MIUI_LAUNCHER_OVERRIDE_DEPTH.set(
                getOverrideDepth() + 1
        );
    }

    private static void exitMiuiLauncherOverride() {
        int depth = getOverrideDepth() - 1;

        if (depth <= 0) {
            MIUI_LAUNCHER_OVERRIDE_DEPTH.remove();
        } else {
            MIUI_LAUNCHER_OVERRIDE_DEPTH.set(depth);
        }
    }

    private static int getOverrideDepth() {
        Integer depth = MIUI_LAUNCHER_OVERRIDE_DEPTH.get();
        return depth == null ? 0 : depth;
    }
}
