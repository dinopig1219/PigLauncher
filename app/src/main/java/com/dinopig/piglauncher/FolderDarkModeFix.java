package com.dinopig.piglauncher;

import android.graphics.Canvas;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

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

    static void install(ClassLoader classLoader) {
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

    static boolean isSmallFolderAppearanceActive() {
        return getSmallFolderAppearanceDepth() > 0;
    }

    static boolean isSmallFolderBlurGateActive() {
        return getSmallFolderBlurGateDepth() > 0;
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

    private static XC_MethodHook createSmallFolderAppearanceHook() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                enterSmallFolderAppearance();
                LauncherGate.enter();
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                LauncherGate.exit();
                exitSmallFolderAppearance();
            }
        };
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
