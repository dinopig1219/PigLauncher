package com.dinopig.piglauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.util.UUID

internal class HookStatusClient(
    private val context: Context,
    private val onStatusChanged: (Boolean) -> Unit,
) {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var receiver: BroadcastReceiver? = null
    private var timeout: Runnable? = null

    fun query() {
        cancel()

        val nonce = UUID.randomUUID().toString()
        val statusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != HookStatusBridge.RESPONSE_ACTION) {
                    return
                }

                if (intent.getStringExtra(HookStatusBridge.EXTRA_NONCE) != nonce) {
                    return
                }

                finish(true)
            }
        }

        receiver = statusReceiver
        val filter = IntentFilter(HookStatusBridge.RESPONSE_ACTION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(statusReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            appContext.registerReceiver(statusReceiver, filter)
        }

        val timeoutRunnable = Runnable { finish(false) }
        timeout = timeoutRunnable
        handler.postDelayed(timeoutRunnable, 1500L)

        val query = Intent(HookStatusBridge.QUERY_ACTION)
            .setPackage(HookStatusBridge.TARGET_PACKAGE)
            .putExtra(HookStatusBridge.EXTRA_NONCE, nonce)

        appContext.sendBroadcast(query)
    }

    fun cancel() {
        timeout?.let(handler::removeCallbacks)
        timeout = null
        receiver?.let {
            runCatching { appContext.unregisterReceiver(it) }
        }
        receiver = null
    }

    private fun finish(active: Boolean) {
        if (receiver == null) {
            return
        }

        cancel()
        onStatusChanged(active)
    }
}
