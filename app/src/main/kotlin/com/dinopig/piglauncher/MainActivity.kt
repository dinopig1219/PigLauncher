package com.dinopig.piglauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.libxposed.service.HookedTarget
import io.github.libxposed.service.XposedService

class MainActivity : ComponentActivity(), PigLauncherApplication.ServiceStateListener {

    private var moduleStatus by mutableStateOf(ModuleStatus.DISABLED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PigLauncherApp(status = moduleStatus)
        }
    }

    override fun onStart() {
        super.onStart()
        PigLauncherApplication.addServiceStateListener(this, true)
    }

    override fun onResume() {
        super.onResume()
        refreshStatus(PigLauncherApplication.service)
    }

    override fun onStop() {
        PigLauncherApplication.removeServiceStateListener(this)
        super.onStop()
    }

    override fun onServiceStateChanged(service: XposedService?) {
        runOnUiThread {
            refreshStatus(service)
        }
    }

    private fun refreshStatus(service: XposedService?) {
        moduleStatus = resolveModuleStatus(service)
    }

    private fun resolveModuleStatus(service: XposedService?): ModuleStatus {
        if (service == null) {
            return ModuleStatus.DISABLED
        }

        val scope = runCatching { service.scope }.getOrElse {
            return ModuleStatus.DISABLED
        }

        if (TARGET_PACKAGE !in scope) {
            return ModuleStatus.DISABLED
        }

        if (service.apiVersion < XposedService.API_102) {
            return ModuleStatus.RESTART_REQUIRED
        }

        val runningTargets = runCatching { service.runningTargets }.getOrElse {
            return ModuleStatus.RESTART_REQUIRED
        }

        val active = runningTargets.any {
            it.processName == TARGET_PACKAGE && it.state == HookedTarget.State.UP_TO_DATE
        }

        return if (active) {
            ModuleStatus.ACTIVE
        } else {
            ModuleStatus.RESTART_REQUIRED
        }
    }

    private companion object {
        const val TARGET_PACKAGE = "com.mi.android.globallauncher"
    }
}
