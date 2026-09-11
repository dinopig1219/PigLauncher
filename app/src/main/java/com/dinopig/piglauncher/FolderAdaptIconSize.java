package com.dinopig.piglauncher;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;

final class FolderAdaptIconSize {

    private static final String FOLDER_ICON_2X2 =
            "com.miui.home.folder.FolderIcon2x2";

    private static final String FOLDER_INFO =
            "com.miui.home.folder.FolderInfo";

    private static final String BASE_PREVIEW_CONTAINER =
            "com.miui.home.folder.BaseFolderIconPreviewContainer2X2";

    private static final String PREVIEW_CONTAINER_4 =
            "com.miui.home.folder.FolderIconPreviewContainer2X2_4";

    private static final String PREVIEW_CONTAINER_9 =
            "com.miui.home.folder.FolderIconPreviewContainer2X2_9";

    private FolderAdaptIconSize() {
    }

    static void install(XposedModule module, ClassLoader classLoader) {
        Class<?> folderIconClass = MainHook.findClass(
                FOLDER_ICON_2X2,
                classLoader
        );

        Class<?> folderInfoClass = MainHook.findClass(
                FOLDER_INFO,
                classLoader
        );

        Class<?> basePreviewContainerClass = MainHook.findClass(
                BASE_PREVIEW_CONTAINER,
                classLoader
        );

        if (folderIconClass != null
                && folderInfoClass != null
                && basePreviewContainerClass != null) {

            Field infoField = findFieldRecursive(
                    folderIconClass,
                    "mInfo"
            );

            Method getPreviewContainer = findMethodRecursive(
                    folderIconClass,
                    "getMPreviewContainer"
            );

            Method count = findMethodRecursive(
                    folderInfoClass,
                    "count"
            );

            Method getRealPvChildCount = findMethodRecursive(
                    basePreviewContainerClass,
                    "getMRealPvChildCount"
            );

            Method setItemsMaxCount = findMethodRecursive(
                    basePreviewContainerClass,
                    "setMItemsMaxCount",
                    int.class
            );

            Method setLargeIconNum = findMethodRecursive(
                    folderIconClass,
                    "setMLargeIconNum",
                    int.class
            );

            Method createOrRemoveView = findMethodRecursive(
                    folderIconClass,
                    "createOrRemoveView"
            );

            if (infoField != null
                    && getPreviewContainer != null
                    && count != null
                    && getRealPvChildCount != null
                    && setItemsMaxCount != null
                    && createOrRemoveView != null) {

                module.hook(createOrRemoveView).intercept(chain -> {
                    Object folderIcon = chain.getThisObject();

                    Object info = infoField.get(folderIcon);
                    Object container = getPreviewContainer.invoke(folderIcon);

                    if (info != null && container != null) {
                        Object infoCountValue = count.invoke(info);
                        Object realPvChildCountValue =
                                getRealPvChildCount.invoke(container);

                        if (infoCountValue instanceof Number
                                && realPvChildCountValue instanceof Number) {

                            int infoCount =
                                    ((Number) infoCountValue).intValue();

                            int realPvChildCount =
                                    ((Number) realPvChildCountValue).intValue();

                            if (infoCount != realPvChildCount) {
                                int num = getContainerNum(container);

                                if (num > 0
                                        && realPvChildCount - num < 3) {
                                    setItemsMaxCount.invoke(
                                            container,
                                            infoCount <= num
                                                    ? num
                                                    : num + 3
                                    );
                                }
                            }
                        }
                    }

                    return chain.proceed();
                });
            }

            Method addItemOnclickListener = findMethodRecursive(
                    folderIconClass,
                    "addItemOnclickListener"
            );

            if (getPreviewContainer != null
                    && getRealPvChildCount != null
                    && setLargeIconNum != null
                    && addItemOnclickListener != null) {

                module.hook(addItemOnclickListener).intercept(chain -> {
                    Object folderIcon = chain.getThisObject();
                    Object container = getPreviewContainer.invoke(folderIcon);

                    if (container != null) {
                        Object realPvChildCountValue =
                                getRealPvChildCount.invoke(container);

                        if (realPvChildCountValue instanceof Number) {
                            int realPvChildCount =
                                    ((Number) realPvChildCountValue).intValue();

                            int num = getContainerNum(container);

                            if (num > 0) {
                                setLargeIconNum.invoke(
                                        folderIcon,
                                        realPvChildCount <= num
                                                ? num
                                                : num - 1
                                );
                            }
                        }
                    }

                    return chain.proceed();
                });
            }
        }

        hookPreviewContainer(
                module,
                classLoader,
                PREVIEW_CONTAINER_4,
                4
        );

        hookPreviewContainer(
                module,
                classLoader,
                PREVIEW_CONTAINER_9,
                9
        );
    }

    private static void hookPreviewContainer(
            XposedModule module,
            ClassLoader classLoader,
            String className,
            int num
    ) {
        Class<?> containerClass = MainHook.findClass(
                className,
                classLoader
        );

        if (containerClass == null) {
            return;
        }

        Method getRealPvChildCount = findMethodRecursive(
                containerClass,
                "getMRealPvChildCount"
        );

        Method setLargeIconNum = findMethodRecursive(
                containerClass,
                "setMLargeIconNum",
                int.class
        );

        Method preSetup2x2 = findMethodRecursive(
                containerClass,
                "preSetup2x2"
        );

        if (getRealPvChildCount == null
                || setLargeIconNum == null
                || preSetup2x2 == null) {
            return;
        }

        module.hook(preSetup2x2).intercept(chain -> {
            Object container = chain.getThisObject();

            Object realPvChildCountValue =
                    getRealPvChildCount.invoke(container);

            if (realPvChildCountValue instanceof Number) {
                int realPvChildCount =
                        ((Number) realPvChildCountValue).intValue();

                setLargeIconNum.invoke(
                        container,
                        realPvChildCount <= num
                                ? num
                                : num - 1
                );
            }

            return chain.proceed();
        });
    }

    private static int getContainerNum(Object container) {
        String simpleName = container.getClass().getSimpleName();

        if (simpleName.isEmpty()) {
            return -1;
        }

        char last = simpleName.charAt(simpleName.length() - 1);

        if (!Character.isDigit(last)) {
            return -1;
        }

        return Character.getNumericValue(last);
    }

    private static Field findFieldRecursive(
            Class<?> clazz,
            String name
    ) {
        Class<?> current = clazz;

        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (Throwable ignored) {
                current = current.getSuperclass();
            }
        }

        return null;
    }

    private static Method findMethodRecursive(
            Class<?> clazz,
            String name,
            Class<?>... parameterTypes
    ) {
        Class<?> current = clazz;

        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(
                        name,
                        parameterTypes
                );
                method.setAccessible(true);
                return method;
            } catch (Throwable ignored) {
                current = current.getSuperclass();
            }
        }

        return null;
    }
}
