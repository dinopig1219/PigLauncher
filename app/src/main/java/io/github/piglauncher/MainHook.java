package io.github.piglauncher;

import android.view.View;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "PigLauncher";
    private static final String AT_TAG = "PigLauncher-AT";

    private static final String POCO_PACKAGE = "com.mi.android.globallauncher";
    private static final String MIUI_PACKAGE = "com.miui.home";

    private static final String LARGE_FOLDER_BACKGROUND =
            "com.miui.home.folder.FolderIcon4x4NormalBackgroundDrawable";

    private static final String BUILD_CONFIG_UTILS =
            "com.miui.home.common.utils.BuildConfigUtils";

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

    private static final ThreadLocal<Integer> MIUI_LAUNCHER_OVERRIDE_DEPTH =
            new ThreadLocal<>();

    private static final AtomicLong LOG_SEQUENCE =
            new AtomicLong(0);

    @Override
    public void handleLoadPackage(
            XC_LoadPackage.LoadPackageParam lpparam
    ) throws Throwable {

        boolean isPoco = POCO_PACKAGE.equals(lpparam.packageName);
        boolean isMiui = MIUI_PACKAGE.equals(lpparam.packageName);

        if (!isPoco && !isMiui) {
            return;
        }

        String source = isPoco ? "POCO" : "MIUI";

        log(source, "loaded " + lpparam.packageName);

        if (isPoco) {
            Class<?> buildConfigUtilsClass =
                    XposedHelpers.findClassIfExists(
                            BUILD_CONFIG_UTILS,
                            lpparam.classLoader
                    );

            if (buildConfigUtilsClass == null) {
                log(source, "BuildConfigUtils not found");
                return;
            }

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

            installLargeFolderDarkModeFix(
                    lpparam.classLoader,
                    source
            );

            installAdvancedTexturesGateProbe(
                    lpparam.classLoader,
                    source
            );
        }

        installAdvancedTexturesDiagnostics(
                lpparam.classLoader,
                source
        );

        log(source, "hook installation finished");
    }

    private static void installLargeFolderDarkModeFix(
            ClassLoader classLoader,
            String source
    ) {

        Class<?> backgroundClass =
                XposedHelpers.findClassIfExists(
                        LARGE_FOLDER_BACKGROUND,
                        classLoader
                );

        if (backgroundClass == null) {
            log(source, "Large Folder class not found");
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

        log(source, "Large Folder Dark Mode fix installed");
    }

    private static void installAdvancedTexturesGateProbe(
            ClassLoader classLoader,
            String source
    ) {

        Class<?> blurUtilitiesClass =
                XposedHelpers.findClassIfExists(
                        BLUR_UTILITIES,
                        classLoader
                );

        if (blurUtilitiesClass == null) {
            atLog(source, "Advanced Textures gate class missing");
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(
                    blurUtilitiesClass,
                    "isBlurSupported",
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

            atLog(source, "Advanced Textures gate probe installed");
        } catch (Throwable throwable) {
            atLog(
                    source,
                    "Advanced Textures gate probe failed: "
                            + throwable
            );
        }
    }

    private static void installAdvancedTexturesDiagnostics(
            ClassLoader classLoader,
            String source
    ) {

        atLog(source, "Installing Phase 3 diagnostics");

        hookClass(
                BLUR_UTILITIES,
                classLoader,
                source,
                false,
                "isBlurSupported",
                "isBackgroundBlurSupported",
                "isThreeLayerBlurSupported",
                "setBackgroundBlurEnabled"
        );

        hookClass(
                MIUIX_MATERIAL_BLUR_UTILITIES,
                classLoader,
                source,
                true,
                "isSupportHyperMaterialBlur",
                "shouldApplyBlur",
                "applyMaterialBlur",
                "addBackgroundBlenderColor",
                "addMiBackgroundBlendColor",
                "clearBackgroundBlendColors",
                "clearBackgroundBlendConfig",
                "clearMiBackgroundBlendColor",
                "setBackgroundBlendConfig",
                "setMiBackgroundBlendColors",
                "setMiBackgroundBlurEnhanceFlag",
                "setMiBackgroundBlurMode",
                "setMiBackgroundBlurRadius",
                "setMiBackgroundBlurScaleRatio",
                "setMiBackgroundBlurType",
                "setMiBackgroundLightBlendMode",
                "setMiViewBlurMode",
                "setWidgetBackgroundBlendColors"
        );

        hookClass(
                HYPER_MATERIAL_UTILS,
                classLoader,
                source,
                true,
                "isEnable",
                "isDefaultFeatureEnable",
                "isFeatureEnable",
                "enableHyperMaterial"
        );

        hookClass(
                MIUI_BLUR_UTILS,
                classLoader,
                source,
                true,
                "isEnable",
                "setBackgroundBlur",
                "setBackgroundBlurMode",
                "setBackgroundBlurRadius",
                "setBackgroundBlurType",
                "setViewBlurMode",
                "setPassWindowBlurEnabled",
                "addBackgroundBlenderColor",
                "addMiBackgroundBlendColor",
                "clearBackgroundBlendConfig",
                "clearMiBackgroundBlendColor",
                "setBackgroundBlendConfig",
                "setMiBackgroundBlendColors",
                "setMiBackgroundBlurEnhanceFlag",
                "setMiBackgroundBlurMode",
                "setMiBackgroundBlurRadius",
                "setMiBackgroundBlurScaleRatio",
                "setMiBackgroundBlurType",
                "setMiBackgroundLightBlendMode",
                "setMiViewBlurMode"
        );

        hookClass(
                MIUI_BLUR_UI_HELPER,
                classLoader,
                source,
                true,
                "isSupportBlur",
                "isEnableBlur",
                "isApplyBlur",
                "setSupportBlur",
                "setEnableBlur",
                "setEnableBlurInternal",
                "applyBlur",
                "applyBlurInternal",
                "refreshBlur",
                "addBackgroundBlenderColor",
                "addMiBackgroundBlendColor",
                "clearBackgroundBlendConfig",
                "clearMiBackgroundBlendColor",
                "setBackgroundBlendConfig",
                "setMiBackgroundBlendColors"
        );

        hookClass(
                BOTTOM_SHEET_VIEW,
                classLoader,
                source,
                true,
                "isSupportBlur",
                "isEnableBlur",
                "isApplyBlur",
                "setSupportBlur",
                "setEnableBlur",
                "applyBlur",
                "updateMaterialEffect",
                "enableHyperMaterial"
        );

        hookClass(
                View.class,
                source,
                true,
                "setMiBackgroundBlurMode",
                "setMiBackgroundBlurRadius",
                "setMiBackgroundBlurType",
                "setMiBackgroundBlurEnhanceFlag",
                "setMiBackgroundBlurScaleRatio",
                "setMiBackgroundLightBlendMode",
                "setMiBackgroundBlendColors",
                "addMiBackgroundBlendColor",
                "clearMiBackgroundBlendColor",
                "setMiViewBlurMode",
                "setBackgroundBlurMode",
                "setBackgroundBlurRadius",
                "setBackgroundBlurType",
                "setBackgroundBlendConfig",
                "addBackgroundBlenderColor",
                "clearBackgroundBlendConfig"
        );

        atLog(source, "Phase 3 diagnostics installed");
    }

    private static void hookClass(
            String className,
            ClassLoader classLoader,
            String source,
            boolean stack,
            String... methodNames
    ) {

        Class<?> clazz =
                XposedHelpers.findClassIfExists(
                        className,
                        classLoader
                );

        if (clazz == null) {
            atLog(source, "CLASS MISSING: " + className);
            return;
        }

        atLog(source, "CLASS OK: " + className);

        hookClass(
                clazz,
                source,
                stack,
                methodNames
        );
    }

    private static void hookClass(
            Class<?> clazz,
            String source,
            boolean stack,
            String... methodNames
    ) {

        Set<String> wanted =
                new HashSet<>(
                        Arrays.asList(methodNames)
                );

        Set<String> hooked =
                new HashSet<>();

        Method[] methods;

        try {
            methods = clazz.getDeclaredMethods();
        } catch (Throwable throwable) {
            atLog(
                    source,
                    "METHOD ENUM FAILED: "
                            + clazz.getName()
                            + " : "
                            + throwable
            );
            return;
        }

        for (Method method : methods) {
            if (!wanted.contains(method.getName())) {
                continue;
            }

            String signature =
                    methodSignature(method);

            if (!hooked.add(signature)) {
                continue;
            }

            try {
                Class<?>[] parameterTypes =
                        method.getParameterTypes();

                Object[] hookArguments =
                        new Object[
                                parameterTypes.length + 1
                        ];

                System.arraycopy(
                        parameterTypes,
                        0,
                        hookArguments,
                        0,
                        parameterTypes.length
                );

                hookArguments[
                        parameterTypes.length
                ] =
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(
                                    MethodHookParam param
                            ) {

                                StringBuilder message =
                                        new StringBuilder();

                                message.append(
                                        clazz.getName()
                                );

                                message.append("#");

                                message.append(
                                        method.getName()
                                );

                                message.append("(");

                                message.append(
                                        formatArguments(
                                                param.args
                                        )
                                );

                                message.append(")");

                                message.append(" => ");

                                try {
                                    message.append(
                                            formatValue(
                                                    param.getResult()
                                            )
                                    );
                                } catch (Throwable throwable) {
                                    message.append(
                                            "<result unavailable:"
                                    );

                                    message.append(
                                            throwable
                                                    .getClass()
                                                    .getSimpleName()
                                    );

                                    message.append(">");
                                }

                                if (stack) {
                                    String trace =
                                            formatRelevantStack();

                                    if (!trace.isEmpty()) {
                                        message.append(
                                                " | stack="
                                        );

                                        message.append(trace);
                                    }
                                }

                                atLog(
                                        source,
                                        message.toString()
                                );
                            }
                        };

                XposedHelpers.findAndHookMethod(
                        clazz,
                        method.getName(),
                        hookArguments
                );

                atLog(
                        source,
                        "HOOK OK: "
                                + signature
                );

            } catch (Throwable throwable) {
                atLog(
                        source,
                        "HOOK FAILED: "
                                + signature
                                + " : "
                                + throwable
                );
            }
        }

        for (String name : wanted) {
            boolean found = false;

            for (String signature : hooked) {
                if (signature.contains(
                        "#" + name + "("
                )) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                atLog(
                        source,
                        "METHOD MISSING: "
                                + clazz.getName()
                                + "#"
                                + name
                );
            }
        }
    }

    private static String methodSignature(
            Method method
    ) {

        StringBuilder builder =
                new StringBuilder();

        builder.append(
                method.getDeclaringClass().getName()
        );

        builder.append("#");

        builder.append(
                method.getName()
        );

        builder.append("(");

        Class<?>[] parameterTypes =
                method.getParameterTypes();

        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                builder.append(",");
            }

            builder.append(
                    parameterTypes[i].getName()
            );
        }

        builder.append(")");

        builder.append(":");

        builder.append(
                method.getReturnType().getName()
        );

        return builder.toString();
    }

    private static String formatRelevantStack() {

        StackTraceElement[] stack =
                Thread.currentThread()
                        .getStackTrace();

        StringBuilder builder =
                new StringBuilder();

        int count = 0;

        for (StackTraceElement element : stack) {

            String className =
                    element.getClassName();

            if (className.equals(
                    Thread.class.getName()
            )) {
                continue;
            }

            if (className.equals(
                    MainHook.class.getName()
            )) {
                continue;
            }

            if (className.startsWith(
                    "de.robv.android.xposed."
            )) {
                continue;
            }

            if (!className.startsWith(
                    "com.miui.home."
            )
                    && !className.startsWith(
                    "miuix."
            )
                    && !className.startsWith(
                    "android.view."
            )) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(" <- ");
            }

            builder.append(
                    className
            );

            builder.append(".");

            builder.append(
                    element.getMethodName()
            );

            builder.append(":");

            builder.append(
                    element.getLineNumber()
            );

            count++;

            if (count >= 8) {
                break;
            }
        }

        return builder.toString();
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
                builder.append("{attached=");
                builder.append(
                        view.isAttachedToWindow()
                );
                builder.append(",visibility=");
                builder.append(
                        view.getVisibility()
                );
                builder.append(",alpha=");
                builder.append(
                        view.getAlpha()
                );
                builder.append(",size=");
                builder.append(
                        view.getWidth()
                );
                builder.append("x");
                builder.append(
                        view.getHeight()
                );
                builder.append("}");
            } catch (Throwable ignored) {
            }

            return builder.toString();
        }

        Class<?> clazz =
                value.getClass();

        if (clazz.isArray()) {
            if (value instanceof int[]) {
                return Arrays.toString(
                        (int[]) value
                );
            }

            if (value instanceof long[]) {
                return Arrays.toString(
                        (long[]) value
                );
            }

            if (value instanceof float[]) {
                return Arrays.toString(
                        (float[]) value
                );
            }

            if (value instanceof double[]) {
                return Arrays.toString(
                        (double[]) value
                );
            }

            if (value instanceof boolean[]) {
                return Arrays.toString(
                        (boolean[]) value
                );
            }

            if (value instanceof Object[]) {
                return Arrays.deepToString(
                        (Object[]) value
                );
            }

            return clazz.getName();
        }

        return clazz.getName()
                + "@"
                + Integer.toHexString(
                        System.identityHashCode(value)
                );
    }

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
            String source,
            String message
    ) {
        XposedBridge.log(
                TAG
                        + " ["
                        + source
                        + "]: "
                        + message
        );
    }

    private static void atLog(
            String source,
            String message
    ) {

        long sequence =
                LOG_SEQUENCE.incrementAndGet();

        XposedBridge.log(
                AT_TAG
                        + " #"
                        + sequence
                        + " ["
                        + source
                        + "] ["
                        + Thread.currentThread().getName()
                        + "]: "
                        + message
        );
    }
}
