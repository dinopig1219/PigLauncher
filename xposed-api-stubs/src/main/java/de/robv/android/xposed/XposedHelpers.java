package de.robv.android.xposed;

public final class XposedHelpers {

    private XposedHelpers() {
    }

    public static Class<?> findClassIfExists(
            String className,
            ClassLoader classLoader
    ) {
        throw new UnsupportedOperationException("Compile-only stub");
    }

    public static XC_MethodHook.Unhook findAndHookMethod(
            Class<?> clazz,
            String methodName,
            Object... parameterTypesAndCallback
    ) {
        throw new UnsupportedOperationException("Compile-only stub");
    }
}
