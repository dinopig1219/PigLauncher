package de.robv.android.xposed.callbacks;
import de.robv.android.xposed.XCallback;

public abstract class XC_LoadPackage extends XCallback {
    public static final class LoadPackageParam extends XCallback.ParamObject {
        public String packageName;
        public String processName;
        public ClassLoader classLoader;
    }
}
