package com.dinopig.piglauncher

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

internal object LauncherIconSettings {

    private fun componentName(context: Context): ComponentName {
        return ComponentName(
            context.packageName,
            "${context.packageName}.launcher",
        )
    }

    fun isHidden(context: Context): Boolean {
        return when (
            context.packageManager.getComponentEnabledSetting(
                componentName(context),
            )
        ) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED -> true
            else -> false
        }
    }

    fun setHidden(
        context: Context,
        hidden: Boolean,
    ) {
        context.packageManager.setComponentEnabledSetting(
            componentName(context),
            if (hidden) {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
            },
            PackageManager.DONT_KILL_APP,
        )
    }
}
