package com.dinopig.piglauncher

import android.content.Context
import io.github.libxposed.service.XposedService

internal data class FeatureSettingsState(
    val folderDarkMode: Boolean = false,
    val advancedTextures: Boolean = false,
    val folderAdaptIconSize: Boolean = false,
    val predictiveBackProgress: Boolean = false,
    val allWidgetAnimation: Boolean = false,
)

internal object FeatureSettings {

    private const val LOCAL_GROUP = "feature_settings"

    fun load(
        context: Context,
        service: XposedService?,
    ): FeatureSettingsState {
        val local = context.getSharedPreferences(
            LOCAL_GROUP,
            Context.MODE_PRIVATE,
        )

        val remote = runCatching {
            service?.getRemotePreferences(FeatureKeys.GROUP)
        }.getOrNull()

        val localEditor = local.edit()
        val remoteEditor = remote?.edit()

        fun read(key: String): Boolean {
            if (remote != null && remote.contains(key)) {
                val value = remote.getBoolean(key, false)
                localEditor.putBoolean(key, value)
                return value
            }

            val value = local.getBoolean(key, false)
            remoteEditor?.putBoolean(key, value)
            return value
        }

        val state = FeatureSettingsState(
            folderDarkMode = read(FeatureKeys.FOLDER_DARK_MODE),
            advancedTextures = read(FeatureKeys.ADVANCED_TEXTURES),
            folderAdaptIconSize = read(FeatureKeys.FOLDER_ADAPT_ICON_SIZE),
            predictiveBackProgress = read(FeatureKeys.PREDICTIVE_BACK_PROGRESS),
            allWidgetAnimation = read(FeatureKeys.ALL_WIDGET_ANIMATION),
        )

        localEditor.apply()
        remoteEditor?.apply()

        return state
    }

    fun set(
        context: Context,
        service: XposedService?,
        key: String,
        value: Boolean,
    ) {
        context.getSharedPreferences(
            LOCAL_GROUP,
            Context.MODE_PRIVATE,
        ).edit()
            .putBoolean(key, value)
            .apply()

        runCatching {
            service
                ?.getRemotePreferences(FeatureKeys.GROUP)
                ?.edit()
                ?.putBoolean(key, value)
                ?.apply()
        }
    }
}
