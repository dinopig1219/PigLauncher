package io.github.piglauncher;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "PigLauncher";
    private static final String TARGET_PACKAGE = "com.mi.android.globallauncher";

    private static final String LARGE_FOLDER_BACKGROUND =
            "com.miui.home.folder.FolderIcon4x4NormalBackgroundDrawable";

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

        log("loaded " + lpparam.packageName);

        Class<?> buildConfigUtilsClass = XposedHelpers.findClassIfExists(
                BUILD_CONFIG_UTILS,
                lpparam.classLoader
        );

        if (buildConfigUtilsClass == null) {
            log("BuildConfigUtils not found");
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

        installLargeFolderDarkModeFix(lpparam.classLoader);
        installAdvancedTexturesFix(lpparam.classLoader);

        log("hooks installed");
    }

    private static void installLargeFolderDarkModeFix(ClassLoader classLoader) {
        Class<?> backgroundClass = XposedHelpers.findClassIfExists(
                LARGE_FOLDER_BACKGROUND,
                classLoader
        );

        if (backgroundClass == null) {
            log("Large Folder class not found");
            return;
        }

        XposedBridge.hookAllConstructors(
                backgroundClass,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        enterMiuiLauncherOverride();
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        exitMiuiLauncherOverride();
                    }
                }
        );

        log("Large Folder Dark Mode fix installed");
    }

    private static void installAdvancedTexturesFix(ClassLoader classLoader) {
        Class<?> blurUtilitiesClass = XposedHelpers.findClassIfExists(
                BLUR_UTILITIES,
                classLoader
        );

        if (blurUtilitiesClass != null) {
            XposedHelpers.findAndHookMethod(
                    blurUtilitiesClass,
                    "isBlurSupported",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            enterMiuiLauncherOverride();
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            exitMiuiLauncherOverride();
                        }
                    }
            );

            log("Advanced Textures launcher gate fix installed");
        } else {
            log("BlurUtilities not found");
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
            log("PassWindowBlur class not found: " + className);
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
                            log("PassWindowBlur allowlist patched via " + className);
                        }
                    }
                };

        try {
            XposedHelpers.findAndHookMethod(
                    clazz,
                    "getPassWindowBlurFilterData",
                    args
            );

            log("PassWindowBlur hook installed: " + className);
        } catch (Throwable throwable) {
            log("PassWindowBlur hook failed: " + className + " : " + throwable);
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
        } catch (Throwable throwable) {
            log("PassWindowBlur filter parse failed: " + throwable);
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

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }
}
