package de.robv.android.xposed;

import java.util.Set;

public final class XposedBridge {

    private XposedBridge() {
    }

    public static void log(String text) {
        throw new UnsupportedOperationException("Compile-only stub");
    }

    public static Set<XC_MethodHook.Unhook> hookAllConstructors(
            Class<?> hookClass,
            XC_MethodHook callback
    ) {
        throw new UnsupportedOperationException("Compile-only stub");
    }
}
