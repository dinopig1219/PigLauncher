package de.robv.android.xposed;

public abstract class XC_MethodHook extends XCallback {

    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
    }

    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
    }

    public static final class MethodHookParam extends XCallback.ParamObject {
        public Object thisObject;
        public Object[] args;

        public void setResult(Object result) {
            throw new UnsupportedOperationException("Compile-only stub");
        }

        public Object getResult() {
            throw new UnsupportedOperationException("Compile-only stub");
        }
    }

    public final class Unhook {
    }
}
