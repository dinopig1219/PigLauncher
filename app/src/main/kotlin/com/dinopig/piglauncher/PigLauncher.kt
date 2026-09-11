package com.dinopig.piglauncher

import android.app.Application
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.util.concurrent.CopyOnWriteArraySet

class PigLauncher : Application(), XposedServiceHelper.OnServiceListener {

    companion object {
        private val listeners = CopyOnWriteArraySet<ServiceStateListener>()

        @Volatile
        var service: XposedService? = null
            private set

        fun addServiceStateListener(
            listener: ServiceStateListener,
            notifyImmediately: Boolean,
        ) {
            listeners.add(listener)
            if (notifyImmediately) {
                listener.onServiceStateChanged(service)
            }
        }

        fun removeServiceStateListener(listener: ServiceStateListener) {
            listeners.remove(listener)
        }
    }

    override fun onCreate() {
        super.onCreate()
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        Companion.service = service
        notifyServiceStateChanged(service)
    }

    override fun onServiceDied(service: XposedService) {
        if (Companion.service === service) {
            Companion.service = null
        }
        notifyServiceStateChanged(Companion.service)
    }

    private fun notifyServiceStateChanged(service: XposedService?) {
        listeners.forEach { it.onServiceStateChanged(service) }
    }

    interface ServiceStateListener {
        fun onServiceStateChanged(service: XposedService?)
    }
}
