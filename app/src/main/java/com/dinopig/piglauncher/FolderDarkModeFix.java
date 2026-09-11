package com.dinopig.piglauncher;

import android.graphics.Canvas;
import android.view.View;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;

final class FolderDarkModeFix {

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

    private static final ThreadLocal<Integer> SMALL_FOLDER_APPEARANCE_DEPTH =
            new ThreadLocal<>();

    private static final ThreadLocal<Integer> SMALL_FOLDER_BLUR_GATE_DEPTH =
            new ThreadLocal<>();

    private FolderDarkModeFix() {
    }

    static void install(XposedModule module, ClassLoader classLoader) {
        Class<?> largeFolderBackgroundClass = MainHook.findClass(
                LARGE_FOLDER_BACKGROUND,
                classLoader
        );

        if (largeFolderBackgroundClass != null) {
            for (Constructor<?> constructor
                    : MainHook.findConstructors(largeFolderBackgroundClass)) {
                module.hook(constructor).intercept(chain -> {
                    LauncherGate.enter();
                    try {
                        return chain.proceed();
                    } finally {
                        LauncherGate.exit();
                    }
                });
            }
        }

        Class<?> smallFolderClass = MainHook.findClass(
                SMALL_FOLDER,
                classLoader
        );

        if (smallFolderClass != null) {
            Method refreshBackground = MainHook.findMethod(
                    smallFolderClass,
                    "refreshBackground"
            );

            if (refreshBackground != null) {
                hookSmallFolderAppearance(module, refreshBackground);
            }

            Method drawChild = MainHook.findMethod(
                    smallFolderClass,
                    "drawChild",
                    Canvas.class,
                    View.class,
                    long.class
            );

            if (drawChild != null) {
                hookSmallFolderAppearance(module, drawChild);
            }

            Class<?> folderInfoClass = MainHook.findClass(
                    FOLDER_INFO,
                    classLoader
            );

            Class<?> folderClass = MainHook.findClass(
                    FOLDER,
                    classLoader
            );

            if (folderInfoClass != null && folderClass != null) {
                Method setup = MainHook.findMethod(
                        smallFolderClass,
                        "setup",
                        folderInfoClass,
                        folderClass
                );

                if (setup != null) {
                    hookSmallFolderAppearance(module, setup);
                }
            }

            Method updateFolderIconBg = MainHook.findMethod(
                    smallFolderClass,
                    "updateFolderIconBg$lambda$2",
                    smallFolderClass
            );

            if (updateFolderIconBg != null) {
                hookSmallFolderAppearance(module, updateFolderIconBg);
            }
        }

        Class<?> blurUtilitiesClass = MainHook.findClass(
                BLUR_UTILITIES,
                classLoader
        );

        if (blurUtilitiesClass != null) {
            Method isFolderBlurSupported = MainHook.findMethod(
                    blurUtilitiesClass,
                    "isFolderBlurSupported",
                    boolean.class
            );

            if (isFolderBlurSupported != null) {
                module.hook(isFolderBlurSupported).intercept(chain -> {
                    boolean enabled = Boolean.TRUE.equals(chain.getArg(0));

                    if (!enabled) {
                        return chain.proceed();
                    }

                    enterSmallFolderBlurGate();
                    try {
                        return chain.proceed();
                    } finally {
                        exitSmallFolderBlurGate();
                    }
                });
            }
        }

        Class<?> folderSheetClass = MainHook.findClass(
                FOLDER_SHEET,
                classLoader
        );

        if (folderSheetClass != null) {
            Method getFolderPickerSelectDefaultFolderBg = MainHook.findMethod(
                    folderSheetClass,
                    "getFolderPickerSelectDefaultFolderBg"
            );

            if (getFolderPickerSelectDefaultFolderBg != null) {
                module.hook(getFolderPickerSelectDefaultFolderBg).intercept(chain -> {
                    LauncherGate.enter();
                    try {
                        return chain.proceed();
                    } finally {
                        LauncherGate.exit();
                    }
                });
            }
        }
    }

    static boolean isSmallFolderAppearanceActive() {
        return getSmallFolderAppearanceDepth() > 0;
    }

    static boolean isSmallFolderBlurGateActive() {
        return getSmallFolderBlurGateDepth() > 0;
    }

    private static void hookSmallFolderAppearance(
            XposedModule module,
            Method method
    ) {
        module.hook(method).intercept(chain -> {
            enterSmallFolderAppearance();
            LauncherGate.enter();
            try {
                return chain.proceed();
            } finally {
                LauncherGate.exit();
                exitSmallFolderAppearance();
            }
        });
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
