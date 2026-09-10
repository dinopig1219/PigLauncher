package io.github.piglauncher;

import android.content.Context;
import android.view.View;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "PigLauncher";
    private static final String AT_TAG = "PigLauncher-AT";

    private static final String TARGET_PACKAGE =
            "com.mi.android.globallauncher";

    /*
     * Existing working Large Folder Dark Mode fix.
     * Keep this logic unchanged.
     */
    private static final String LARGE_FOLDER_BACKGROUND =
            "com.miui.home.folder.FolderIcon4x4NormalBackgroundDrawable";

    private static final String BUILD_CONFIG_UTILS =
            "com.miui.home.common.utils.BuildConfigUtils";

    private static final ThreadLocal<Integer> MIUI_LAUNCHER_OVERRIDE_DEPTH =
            new ThreadLocal<>();

    /*
     * Advanced Textures diagnostics only.
     *
     * IMPORTANT:
     * These hooks only observe calls/return values.
     * They DO NOT call setResult() and DO NOT change launcher behavior.
     *
     * Some Xiaomi internal classes/methods still contain the word
     * "Material" in their real class names. We keep those exact internal
     * names because that is what exists inside the APK; the feature is
     * referred to as Advanced Textures in PigLauncher.
     */
    private static final String BLUR_UTILITIES =
            "com.miui.home.common.utils.BlurUtilities";

    private static final String MIUIX_MATERIAL_BLUR_UTILITIES =
            "com.miui.home.common.utils.MiuixMaterialBlurUtilities";

    private static final String HYPER_MATERIAL_UTILS =
            "miuix.core.util.HyperMaterialUtils";

    private static final String MIUI_BLUR_UTILS =
            "miuix.core.util.MiuiBlurUtils";

    private static final String MIUI_BLUR_UI_HELPER =
            "miuix.view.MiuiBlurUiHelper";

    private static final String BOTTOM_SHEET_VIEW =
            "miuix.bottomsheet.BottomSheetView";

    private static final AtomicLong AT_LOG_SEQUENCE =
            new AtomicLong(0);

    @Override
    public void handleLoadPackage(
            XC_LoadPackage.LoadPackageParam lpparam
    ) throws Throwable {

        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        log("loaded " + lpparam.packageName);

        Class<?> buildConfigUtilsClass =
                XposedHelpers.findClassIfExists(
                        BUILD_CONFIG_UTILS,
                        lpparam.classLoader
                );

        if (buildConfigUtilsClass == null) {
            log("BuildConfigUtils not found; no fixes can be installed");
            return;
        }

        /*
         * Existing local gate used by the working large-folder fix.
         *
         * Outside an explicitly marked execution window,
         * POCO receives its original isMiuiLauncher() result.
         */
        XposedHelpers.findAndHookMethod(
                buildConfigUtilsClass,
                "isMiuiLauncher",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(
                            MethodHookParam param
                    ) {
                        if (getOverrideDepth() > 0) {
                            param.setResult(Boolean.TRUE);
                        }
                    }
                }
        );

        /*
         * Keep the working folder fix enabled.
         */
        installLargeFolderDarkModeFix(
                lpparam.classLoader
        );

        /*
         * Advanced Textures:
         *
         * NO FIX is enabled here.
         * Only diagnostic hooks are installed.
         */
        installAdvancedTexturesDiagnostics(
                lpparam.classLoader
        );

        log("hook installation finished");
    }

    /*
     * ============================================================
     * Large Folder Dark Mode Fix
     * ============================================================
     */

    private static void installLargeFolderDarkModeFix(
            ClassLoader classLoader
    ) {

        Class<?> backgroundClass =
                XposedHelpers.findClassIfExists(
                        LARGE_FOLDER_BACKGROUND,
                        classLoader
                );

        if (backgroundClass == null) {
            log(
                    "Large Folder fix skipped: class not found: "
                            + LARGE_FOLDER_BACKGROUND
            );
            return;
        }

        XposedBridge.hookAllConstructors(
                backgroundClass,
                new XC_MethodHook() {

                    @Override
                    protected void beforeHookedMethod(
                            MethodHookParam param
                    ) {
                        enterMiuiLauncherOverride();
                    }

                    @Override
                    protected void afterHookedMethod(
                            MethodHookParam param
                    ) {
                        exitMiuiLauncherOverride();
                    }
                }
        );

        log(
                "Large Folder Dark Mode fix installed"
        );
    }

    /*
     * ============================================================
     * Advanced Textures Diagnostics
     * ============================================================
     *
     * OBSERVATION ONLY:
     * - no setResult()
     * - no parameter replacement
     * - no forced blur
     * - no forced feature state
     *
     * We want to see where the OFF and ON paths differ.
     */

    private static void installAdvancedTexturesDiagnostics(
            ClassLoader classLoader
    ) {

        atLog("Installing Advanced Textures diagnostics");

        /*
         * Launcher-side blur support / entry gates.
         */
        Class<?> blurUtilities =
                findDiagnosticClass(
                        BLUR_UTILITIES,
                        classLoader
                );

        if (blurUtilities != null) {
            traceMethod(
                    blurUtilities,
                    "isBlurSupported"
            );

            traceMethod(
                    blurUtilities,
                    "isBackgroundBlurSupported"
            );

            traceMethod(
                    blurUtilities,
                    "isThreeLayerBlurSupported"
            );

            traceMethod(
                    blurUtilities,
                    "setBackgroundBlurEnabled"
            );
        }

        /*
         * Launcher wrapper around the Hyper/Advanced Textures path.
         */
        Class<?> miuixMaterialBlurUtilities =
                findDiagnosticClass(
                        MIUIX_MATERIAL_BLUR_UTILITIES,
                        classLoader
                );

        if (miuixMaterialBlurUtilities != null) {
            traceMethod(
                    miuixMaterialBlurUtilities,
                    "isSupportHyperMaterialBlur"
            );

            traceMethod(
                    miuixMaterialBlurUtilities,
                    "shouldApplyBlur",
                    View.class
            );

            traceMethod(
                    miuixMaterialBlurUtilities,
                    "applyMaterialBlur",
                    View.class,
                    Runnable.class,
                    Runnable.class
            );
        }

        /*
         * Xiaomi/MIUIX Advanced Textures feature gate.
         */
        Class<?> hyperMaterialUtils =
                findDiagnosticClass(
                        HYPER_MATERIAL_UTILS,
                        classLoader
                );

        if (hyperMaterialUtils != null) {
            traceMethod(
                    hyperMaterialUtils,
                    "isEnable"
            );

            traceMethod(
                    hyperMaterialUtils,
                    "isDefaultFeatureEnable"
            );

            traceMethod(
                    hyperMaterialUtils,
                    "isFeatureEnable",
                    Context.class
            );
        }

        /*
         * Actual low-level blur calls.
         */
        Class<?> miuiBlurUtils =
                findDiagnosticClass(
                        MIUI_BLUR_UTILS,
                        classLoader
                );

        if (miuiBlurUtils != null) {
            traceMethod(
                    miuiBlurUtils,
                    "isEnable"
            );

            traceMethod(
                    miuiBlurUtils,
                    "setBackgroundBlur",
                    View.class,
                    int.class,
                    int.class
            );

            traceMethod(
                    miuiBlurUtils,
                    "setBackgroundBlurMode",
                    View.class,
                    int.class
            );

            traceMethod(
                    miuiBlurUtils,
                    "setBackgroundBlurRadius",
                    View.class,
                    int.class
            );

            traceMethod(
                    miuiBlurUtils,
                    "setBackgroundBlurType",
                    View.class,
                    int.class
            );

            traceMethod(
                    miuiBlurUtils,
                    "setViewBlurMode",
                    View.class,
                    int.class
            );

            traceMethod(
                    miuiBlurUtils,
                    "setPassWindowBlurEnabled",
                    View.class,
                    boolean.class
            );
        }

        /*
         * MIUIX helper that owns the blur state of a UI surface.
         */
        Class<?> miuiBlurUiHelper =
                findDiagnosticClass(
                        MIUI_BLUR_UI_HELPER,
                        classLoader
                );

        if (miuiBlurUiHelper != null) {
            traceMethod(
                    miuiBlurUiHelper,
                    "isSupportBlur"
            );

            traceMethod(
                    miuiBlurUiHelper,
                    "isEnableBlur"
            );

            traceMethod(
                    miuiBlurUiHelper,
                    "isApplyBlur"
            );

            traceMethod(
                    miuiBlurUiHelper,
                    "setSupportBlur",
                    boolean.class
            );

            traceMethod(
                    miuiBlurUiHelper,
                    "setEnableBlur",
                    boolean.class
            );

            traceMethod(
                    miuiBlurUiHelper,
                    "setEnableBlurInternal",
                    boolean.class
            );

            traceMethod(
                    miuiBlurUiHelper,
                    "applyBlur",
                    boolean.class
            );

            traceMethod(
                    miuiBlurUiHelper,
                    "applyBlurInternal",
                    boolean.class
            );

            traceMethod(
                    miuiBlurUiHelper,
                    "refreshBlur"
            );
        }

        /*
         * The BottomSheet shown by pages such as "Effects".
         */
        Class<?> bottomSheetView =
                findDiagnosticClass(
                        BOTTOM_SHEET_VIEW,
                        classLoader
                );

        if (bottomSheetView != null) {
            traceMethod(
                    bottomSheetView,
                    "isSupportBlur"
            );

            traceMethod(
                    bottomSheetView,
                    "isEnableBlur"
            );

            traceMethod(
                    bottomSheetView,
                    "isApplyBlur"
            );

            traceMethod(
                    bottomSheetView,
                    "setSupportBlur",
                    boolean.class
            );

            traceMethod(
                    bottomSheetView,
                    "setEnableBlur",
                    boolean.class
            );

            traceMethod(
                    bottomSheetView,
                    "applyBlur",
                    boolean.class
            );

            traceMethod(
                    bottomSheetView,
                    "updateMaterialEffect"
            );
        }

        atLog(
                "Advanced Textures diagnostics installed; "
                        + "no Advanced Textures behavior has been modified"
        );
    }

    /*
     * Find one diagnostic class without crashing the launcher if Xiaomi
     * renames/removes it in another build.
     */
    private static Class<?> findDiagnosticClass(
            String className,
            ClassLoader classLoader
    ) {

        Class<?> clazz =
                XposedHelpers.findClassIfExists(
                        className,
                        classLoader
                );

        if (clazz == null) {
            atLog(
                    "CLASS MISSING: " + className
            );
        } else {
            atLog(
                    "CLASS OK: " + className
            );
        }

        return clazz;
    }

    /*
     * Generic exact-method tracer.
     *
     * parameterTypes are Android/Java parameter classes only.
     * The callback does not change result/arguments.
     */
    private static void traceMethod(
            final Class<?> clazz,
            final String methodName,
            Object... parameterTypes
    ) {

        try {
            Object[] hookArguments =
                    Arrays.copyOf(
                            parameterTypes,
                            parameterTypes.length + 1
                    );

            hookArguments[parameterTypes.length] =
                    new XC_MethodHook() {

                        @Override
                        protected void afterHookedMethod(
                                MethodHookParam param
                        ) {

                            String owner =
                                    clazz.getName();

                            String arguments =
                                    formatArguments(
                                            param.args
                                    );

                            String result;

                            try {
                                result =
                                        formatValue(
                                                param.getResult()
                                        );
                            } catch (Throwable throwable) {
                                result =
                                        "<result unavailable: "
                                                + throwable.getClass()
                                                .getSimpleName()
                                                + ">";
                            }

                            atLog(
                                    owner
                                            + "#"
                                            + methodName
                                            + "("
                                            + arguments
                                            + ")"
                                            + " => "
                                            + result
                            );
                        }
                    };

            XposedHelpers.findAndHookMethod(
                    clazz,
                    methodName,
                    hookArguments
            );

            atLog(
                    "HOOK OK: "
                            + clazz.getName()
                            + "#"
                            + methodName
            );

        } catch (Throwable throwable) {

            atLog(
                    "HOOK FAILED: "
                            + clazz.getName()
                            + "#"
                            + methodName
                            + " : "
                            + throwable
            );
        }
    }

    private static String formatArguments(
            Object[] args
    ) {

        if (args == null || args.length == 0) {
            return "";
        }

        StringBuilder builder =
                new StringBuilder();

        for (int i = 0; i < args.length; i++) {

            if (i > 0) {
                builder.append(", ");
            }

            builder.append(
                    formatValue(args[i])
            );
        }

        return builder.toString();
    }

    private static String formatValue(
            Object value
    ) {

        if (value == null) {
            return "null";
        }

        if (value instanceof Boolean
                || value instanceof Number
                || value instanceof String
                || value instanceof Character) {

            return String.valueOf(value);
        }

        if (value instanceof View) {

            View view =
                    (View) value;

            StringBuilder builder =
                    new StringBuilder();

            builder.append(
                    view.getClass().getName()
            );

            builder.append("@");

            builder.append(
                    Integer.toHexString(
                            System.identityHashCode(view)
                    )
            );

            try {
                builder.append(
                        "{attached="
                );

                builder.append(
                        view.isAttachedToWindow()
                );

                builder.append(
                        ",visibility="
                );

                builder.append(
                        view.getVisibility()
                );

                builder.append(
                        ",size="
                );

                builder.append(
                        view.getWidth()
                );

                builder.append("x");

                builder.append(
                        view.getHeight()
                );

                builder.append("}");
            } catch (Throwable ignored) {
                // Diagnostic formatting must never break the launcher.
            }

            return builder.toString();
        }

        if (value instanceof Context) {
            return value.getClass().getName();
        }

        Class<?> valueClass =
                value.getClass();

        if (valueClass.isArray()) {

            if (value instanceof int[]) {
                return Arrays.toString(
                        (int[]) value
                );
            }

            if (value instanceof boolean[]) {
                return Arrays.toString(
                        (boolean[]) value
                );
            }

            if (value instanceof Object[]) {
                return Arrays.toString(
                        (Object[]) value
                );
            }

            return valueClass.getName();
        }

        return valueClass.getName()
                + "@"
                + Integer.toHexString(
                        System.identityHashCode(value)
                );
    }

    /*
     * ============================================================
     * Existing folder-fix scope helpers
     * ============================================================
     */

    private static void enterMiuiLauncherOverride() {
        MIUI_LAUNCHER_OVERRIDE_DEPTH.set(
                getOverrideDepth() + 1
        );
    }

    private static void exitMiuiLauncherOverride() {

        int depth =
                getOverrideDepth() - 1;

        if (depth <= 0) {
            MIUI_LAUNCHER_OVERRIDE_DEPTH.remove();
        } else {
            MIUI_LAUNCHER_OVERRIDE_DEPTH.set(
                    depth
            );
        }
    }

    private static int getOverrideDepth() {

        Integer depth =
                MIUI_LAUNCHER_OVERRIDE_DEPTH.get();

        return depth == null
                ? 0
                : depth;
    }

    private static void log(
            String message
    ) {
        XposedBridge.log(
                TAG + ": " + message
        );
    }

    private static void atLog(
            String message
    ) {

        long sequence =
                AT_LOG_SEQUENCE.incrementAndGet();

        XposedBridge.log(
                AT_TAG
                        + " #"
                        + sequence
                        + " ["
                        + Thread.currentThread().getName()
                        + "]: "
                        + message
        );
    }
}
