package com.dinopig.piglauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.libxposed.service.XposedService

class MainActivity : ComponentActivity(), PigLauncherApplication.ServiceStateListener {

    private var serviceConnected by mutableStateOf(false)
    private var hookInjected by mutableStateOf(false)
    private lateinit var hookStatusClient: HookStatusClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hookStatusClient = HookStatusClient(this) {
            hookInjected = it
        }
        setContent {
            PigLauncherApp(
                status = when {
                    !serviceConnected -> ModuleStatus.DISABLED
                    hookInjected -> ModuleStatus.ACTIVE
                    else -> ModuleStatus.RESTART_REQUIRED
                },
            )
        }
    }

    override fun onStart() {
        super.onStart()
        PigLauncherApplication.addServiceStateListener(this, true)
    }

    override fun onResume() {
        super.onResume()
        hookStatusClient.query()
    }

    override fun onPause() {
        hookStatusClient.cancel()
        super.onPause()
    }

    override fun onStop() {
        PigLauncherApplication.removeServiceStateListener(this)
        super.onStop()
    }

    override fun onServiceStateChanged(service: XposedService?) {
        runOnUiThread {
            serviceConnected = service != null
            if (service == null) {
                hookInjected = false
            } else {
                hookStatusClient.query()
            }
        }
    }
}
