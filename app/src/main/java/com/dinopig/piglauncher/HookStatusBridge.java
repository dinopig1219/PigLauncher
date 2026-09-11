package com.dinopig.piglauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

final class HookStatusBridge {

    static final String MODULE_PACKAGE = "com.dinopig.piglauncher";
    static final String TARGET_PACKAGE = "com.mi.android.globallauncher";
    static final String QUERY_ACTION = MODULE_PACKAGE + ".action.QUERY_HOOK_STATUS";
    static final String RESPONSE_ACTION = MODULE_PACKAGE + ".action.HOOK_STATUS_RESPONSE";
    static final String EXTRA_NONCE = MODULE_PACKAGE + ".extra.NONCE";

    private static boolean registered;

    private HookStatusBridge() {
    }

    static void install(ClassLoader classLoader) {
        Class<?> applicationClass = XposedHelpers.findClassIfExists(
                "android.app.Application",
                classLoader
        );

        if (applicationClass == null) {
            return;
        }

        XposedHelpers.findAndHookMethod(
                applicationClass,
                "attach",
                Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object contextObject = param.args[0];

                        if (contextObject instanceof Context) {
                            register((Context) contextObject);
                        }
                    }
                }
        );
    }

    private static synchronized void register(Context context) {
        if (registered) {
            return;
        }

        Context appContext = context.getApplicationContext();
        if (appContext == null) {
            appContext = context;
        }

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context receiverContext, Intent intent) {
                if (!QUERY_ACTION.equals(intent.getAction())) {
                    return;
                }

                String nonce = intent.getStringExtra(EXTRA_NONCE);

                if (nonce == null || nonce.isEmpty()) {
                    return;
                }

                Intent response = new Intent(RESPONSE_ACTION)
                        .setPackage(MODULE_PACKAGE)
                        .putExtra(EXTRA_NONCE, nonce);

                receiverContext.sendBroadcast(response);
            }
        };

        IntentFilter filter = new IntentFilter(QUERY_ACTION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            appContext.registerReceiver(receiver, filter);
        }

        registered = true;
    }
}
