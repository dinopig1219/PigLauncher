package com.dinopig.piglauncher;

final class LauncherGate {

    private static final ThreadLocal<Integer> DEPTH = new ThreadLocal<>();

    private LauncherGate() {
    }

    static void enter() {
        DEPTH.set(getDepth() + 1);
    }

    static void exit() {
        int depth = getDepth() - 1;

        if (depth <= 0) {
            DEPTH.remove();
        } else {
            DEPTH.set(depth);
        }
    }

    static boolean isActive() {
        return getDepth() > 0;
    }

    private static int getDepth() {
        Integer depth = DEPTH.get();
        return depth == null ? 0 : depth;
    }
}
