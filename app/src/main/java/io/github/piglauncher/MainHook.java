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

    private static final String FOLDER_SHEET =
            "com.miui.home.folder.FolderSheet";

    private static final String FOLDER_INFO =
            "com.miui.home.model.api.IFolderInfo";

    private static final String FOLDER =
            "com.miui.home.folder.api.IFolder";

    private static final String BLUR_UTILITIES =
            "com.miui.home.common.utils.BlurUtilities";

    private static final String BUILD_CONFIG_UTILS =
            "com.miui.home.common.utils.BuildConfigUtils";

    private static final String DEVICE_CONFIGS =
            "com.miui.home.common.device.DeviceConfigs";

    private static final String VIEW_ROOT_IMPL_STUB_IMPL =
            "android.view.ViewRootImplStubImpl";

    private static final String GET_CAMERA_OCCUPIER_STUB_IMPL =
            "miui.util.GetCameraOccupierStubImpl";

    private static final String MIUI_CAMERA_COVERED_MANAGER =
            "android.cameracovered.MiuiCameraCoveredManager";

    private static final ThreadLocal<Integer> MIUI_LAUNCHER_OVERRIDE_DEPTH =
            new ThreadLocal<>();

    private static final ThreadLocal<Integer> SMALL_FOLDER_APPEARANCE_DEPTH =
            new ThreadLocal<>();

    private static final ThreadLocal<Integer> SMALL_FOLDER_BLUR_GATE_DEPTH =
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

        Class<?> deviceConfigsClass = XposedHelpers.findClassIfExists(
                DEVICE_CONFIGS,
                lpparam.classLoader
        );

        if (buildConfigUtilsClass == null || deviceConfigsClass == null) {
            return;
        }

        XposedHelpers.findAndHookMethod(
                buildConfigUtilsClass,
                "isMiuiLauncher",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (getMiuiLauncherOverrideDepth() > 0) {
                            param.setResult(Boolean.TRUE);
                        }
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                deviceConfigsClass,
                "isDefaultMiuiIcon",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (getSmallFolderAppearanceDepth() > 0) {
                            param.setResult(Boolean.TRUE);
                        }
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                deviceConfigsClass,
                "isUseDefaultFolderIcon",
                boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (getSmallFolderBlurGateDepth() > 0
                                && param.args.length > 0
                                && Boolean.TRUE.equals(param.args[0])) {
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
                    createMiuiLauncherGateHook()
            );
        }

        Class<?> smallFolderClass = XposedHelpers.findClassIfExists(
                SMALL_FOLDER,
                classLoader
        );

        if (smallFolderClass != null) {
            try {
                XposedHelpers.findAndHookMethod(
                        smallFolderClass,
                        "refreshBackground",
                        createSmallFolderAppearanceHook()
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
                        createSmallFolderAppearanceHook()
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
                            createSmallFolderAppearanceHook()
                    );
                } catch (Throwable ignored) {
                }
            }

            try {
                XposedHelpers.findAndHookMethod(
                        smallFolderClass,
                        "updateFolderIconBg$lambda$2",
                        smallFolderClass,
                        createSmallFolderAppearanceHook()
                );
            } catch (Throwable ignored) {
            }
        }

        Class<?> blurUtilitiesClass = XposedHelpers.findClassIfExists(
                BLUR_UTILITIES,
                classLoader
        );

        if (blurUtilitiesClass != null) {
            try {
                XposedHelpers.findAndHookMethod(
                        blurUtilitiesClass,
                        "isFolderBlurSupported",
                        boolean.class,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                if (param.args.length > 0
                                        && Boolean.TRUE.equals(param.args[0])) {
                                    enterSmallFolderBlurGate();
                                }
                            }

                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                if (param.args.length > 0
                                        && Boolean.TRUE.equals(param.args[0])) {
                                    exitSmallFolderBlurGate();
                                }
                            }
                        }
                );
            } catch (Throwable ignored) {
            }
        }

        Class<?> folderSheetClass = XposedHelpers.findClassIfExists(
                FOLDER_SHEET,
                classLoader
        );

        if (folderSheetClass != null) {
            try {
                XposedHelpers.findAndHookMethod(
                        folderSheetClass,
                        "getFolderPickerSelectDefaultFolderBg",
                        createMiuiLauncherGateHook()
                );
            } catch (Throwable ignored) {
            }
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
                enterMiuiLauncherOverride();
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                exitMiuiLauncherOverride();
            }
        };
    }

    private static XC_MethodHook createSmallFolderAppearanceHook() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                enterSmallFolderAppearance();
                enterMiuiLauncherOverride();
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                exitMiuiLauncherOverride();
                exitSmallFolderAppearance();
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
                getMiuiLauncherOverrideDepth() + 1
        );
    }

    private static void exitMiuiLauncherOverride() {
        int depth = getMiuiLauncherOverrideDepth() - 1;

        if (depth <= 0) {
            MIUI_LAUNCHER_OVERRIDE_DEPTH.remove();
        } else {
            MIUI_LAUNCHER_OVERRIDE_DEPTH.set(depth);
        }
    }

    private static int getMiuiLauncherOverrideDepth() {
        Integer depth = MIUI_LAUNCHER_OVERRIDE_DEPTH.get();
        return depth == null ? 0 : depth;
    }

    private static void enterSmallFolderAppearance() {
        SMALL_FOLDER_APPEARANCE_DEPTH.set(
                getSmallFolderAppearanceDepth() + 1
        );
    }

    private static void exitSmallFolderAppearance() {
        int depth = getSmallFolderAppearanceDepth() - 1;

        if (depth <= 0) {
            SMALL_FOLDER_APPEARANCE_DEPTH.remove();
        } else {
            SMALL_FOLDER_APPEARANCE_DEPTH.set(depth);
        }
    }

    private static int getSmallFolderAppearanceDepth() {
        Integer depth = SMALL_FOLDER_APPEARANCE_DEPTH.get();
        return depth == null ? 0 : depth;
    }

    private static void enterSmallFolderBlurGate() {
        SMALL_FOLDER_BLUR_GATE_DEPTH.set(
                getSmallFolderBlurGateDepth() + 1
        );
    }

    private static void exitSmallFolderBlurGate() {
        int depth = getSmallFolderBlurGateDepth() - 1;

        if (depth <= 0) {
            SMALL_FOLDER_BLUR_GATE_DEPTH.remove();
        } else {
            SMALL_FOLDER_BLUR_GATE_DEPTH.set(depth);
        }
    }

    private static int getSmallFolderBlurGateDepth() {
        Integer depth = SMALL_FOLDER_BLUR_GATE_DEPTH.get();
        return depth == null ? 0 : depth;
    }
}
