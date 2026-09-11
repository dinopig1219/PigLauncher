package com.dinopig.piglauncher;

import android.content.SharedPreferences;

final class FeatureSwitches {

    private final SharedPreferences preferences;
    private final SharedPreferences.OnSharedPreferenceChangeListener listener;

    private volatile boolean folderDarkMode = false;
    private volatile boolean advancedTextures = false;
    private volatile boolean folderAdaptIconSize = false;
    private volatile boolean predictiveBackProgress = false;
    private volatile boolean allWidgetAnimation = false;

    FeatureSwitches(SharedPreferences preferences) {
        this.preferences = preferences;
        listener = (sharedPreferences, key) -> reload(sharedPreferences);

        reload(preferences);

        if (preferences != null) {
            preferences.registerOnSharedPreferenceChangeListener(listener);
        }
    }

    boolean isFolderDarkModeEnabled() {
        return folderDarkMode;
    }

    boolean isAdvancedTexturesEnabled() {
        return advancedTextures;
    }

    boolean isFolderAdaptIconSizeEnabled() {
        return folderAdaptIconSize;
    }

    boolean isPredictiveBackProgressEnabled() {
        return predictiveBackProgress;
    }

    boolean isAllWidgetAnimationEnabled() {
        return allWidgetAnimation;
    }

    private void reload(SharedPreferences preferences) {
        if (preferences == null) {
            return;
        }

        folderDarkMode = preferences.getBoolean(
                FeatureKeys.FOLDER_DARK_MODE,
                false
        );

        advancedTextures = preferences.getBoolean(
                FeatureKeys.ADVANCED_TEXTURES,
                false
        );

        folderAdaptIconSize = preferences.getBoolean(
                FeatureKeys.FOLDER_ADAPT_ICON_SIZE,
                false
        );

        predictiveBackProgress = preferences.getBoolean(
                FeatureKeys.PREDICTIVE_BACK_PROGRESS,
                false
        );

        allWidgetAnimation = preferences.getBoolean(
                FeatureKeys.ALL_WIDGET_ANIMATION,
                false
        );
    }
}
