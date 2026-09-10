package io.github.piglauncher;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "PigLauncher";

    /**
     * POCO Launcher package
     */
    private static final String TARGET_PACKAGE =
            "com.mi.android.globallauncher";

    /**
     * Large folder background Drawable
     */
    private static final String LARGE_FOLDER_BACKGROUND =
            "com.miui.home.folder.FolderIcon4x4NormalBackgroundDrawable";

    /**
     * Launcher build type utility
     */
    private static final String BUILD_CONFIG_UTILS =
            "com.miui.home.common.utils.BuildConfigUtils";

    /**
     * Only marks the period when a large-folder background
     * Drawable is being constructed.
     *
     * isMiuiLauncher() will only be overridden during this period.
     */
    private static final ThreadLocal<Integer>
            LARGE_FOLDER_CONSTRUCTOR_DEPTH = new ThreadLocal<>();

    @Override
    public void handleLoadPackage(
            XC_LoadPackage.LoadPackageParam lpparam
    ) throws Throwable {

        /*
         * Only hook POCO Launcher.
         */
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        log("Loaded package: " + lpparam.packageName);

        final ClassLoader classLoader = lpparam.classLoader;

        /*
         * Find large-folder background Drawable.
         */
        final Class<?> folderBackgroundClass =
                XposedHelpers.findClassIfExists(
                        LARGE_FOLDER_BACKGROUND,
                        classLoader
                );

        if (folderBackgroundClass == null) {
            log(
                    "Class not found: "
                            + LARGE_FOLDER_BACKGROUND
            );
            return;
        }

        /*
         * Find BuildConfigUtils.
         */
        final Class<?> buildConfigUtilsClass =
                XposedHelpers.findClassIfExists(
                        BUILD_CONFIG_UTILS,
                        classLoader
                );

        if (buildConfigUtilsClass == null) {
            log(
                    "Class not found: "
                            + BUILD_CONFIG_UTILS
            );
            return;
        }

        /*
         * ---------------------------------------------------------
         * Fix 1:
         * POCO Large Folder Dark Mode
         * ---------------------------------------------------------
         *
         * POCO Launcher already contains the dark folder resources,
         * but the original launcher code additionally checks:
         *
         *     BuildConfigUtils.isMiuiLauncher()
         *
         * POCO normally returns false.
         *
         * We DO NOT globally change isMiuiLauncher().
         *
         * Instead, only while
         *
         * FolderIcon4x4NormalBackgroundDrawable
         *
         * is being constructed, we temporarily allow that check
         * to return true.
         */

        XposedBridge.hookAllConstructors(
                folderBackgroundClass,
                new XC_MethodHook() {

                    @Override
                    protected void beforeHookedMethod(
                            MethodHookParam param
                    ) {

                        int depth = getDepth();

                        LARGE_FOLDER_CONSTRUCTOR_DEPTH.set(
                                depth + 1
                        );
                    }

                    @Override
                    protected void afterHookedMethod(
                            MethodHookParam param
                    ) {

                        int depth = getDepth() - 1;

                        if (depth <= 0) {

                            LARGE_FOLDER_CONSTRUCTOR_DEPTH.remove();

                        } else {

                            LARGE_FOLDER_CONSTRUCTOR_DEPTH.set(
                                    depth
                            );
                        }
                    }
                }
        );

        /*
         * ---------------------------------------------------------
         * Local isMiuiLauncher() override
         * ---------------------------------------------------------
         *
         * Outside the large-folder constructor:
         *
         *     isMiuiLauncher() = original POCO result
         *
         * Inside the large-folder constructor:
         *
         *     isMiuiLauncher() = true
         */
        XposedHelpers.findAndHookMethod(
                buildConfigUtilsClass,
                "isMiuiLauncher",
                new XC_MethodHook() {

                    @Override
                    protected void beforeHookedMethod(
                            MethodHookParam param
                    ) {

                        if (getDepth() > 0) {

                            param.setResult(
                                    Boolean.TRUE
                            );

                            log(
                                    "Large folder: "
                                            + "temporary "
                                            + "isMiuiLauncher=true"
                            );
                        }
                    }
                }
        );

        log(
                "Large Folder Dark Mode fix installed successfully"
        );
    }

    /**
     * Current nesting depth of the large-folder constructor.
     */
    private static int getDepth() {

        Integer depth =
                LARGE_FOLDER_CONSTRUCTOR_DEPTH.get();

        return depth == null
                ? 0
                : depth;
    }

    /**
     * LSPosed log helper.
     */
    private static void log(String message) {

        XposedBridge.log(
                TAG + ": " + message
        );
    }
}
