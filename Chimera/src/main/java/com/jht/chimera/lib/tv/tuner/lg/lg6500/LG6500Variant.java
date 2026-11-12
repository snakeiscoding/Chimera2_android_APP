package com.jht.chimera.lib.tv.tuner.lg.lg6500;

import androidx.annotation.NonNull;

import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;

public enum LG6500Variant {
    // Enum to distinguish between the different LG6500 variants (US8 / KM6).
    US8("settings/us8", SettingKeys.TV.DATA_TV_US8_DEFAULT_CHANNEL_INT, SettingKeys.TV.DATA_TV_US8_DEFAULT_CHANNEL_UUID_STRING, TVKeys.DATA_US8_CURRENT_CHANNEL_INT),
    KM6("settings/km6", SettingKeys.TV.DATA_TV_KM6_DEFAULT_CHANNEL_INT, SettingKeys.TV.DATA_TV_KM6_DEFAULT_CHANNEL_UUID_STRING, TVKeys.DATA_KM6_CURRENT_CHANNEL_INT);

    @NonNull private final String settingsDirectory;
    @NonNull private final Object defaultChannelKey;
    @NonNull private final Object defaultChannelUuidKey;
    @NonNull private final Object currentChannelKey;

    LG6500Variant(@NonNull String settingsDirectory, @NonNull Object defaultChannelKey, @NonNull Object defaultChannelUuidKey, @NonNull Object currentChannelKey) {
        this.settingsDirectory = settingsDirectory;
        this.defaultChannelKey = defaultChannelKey;
        this.defaultChannelUuidKey = defaultChannelUuidKey;
        this.currentChannelKey = currentChannelKey;
    }

    @NonNull public String getSettingsDirectory() { return settingsDirectory; }
    @NonNull public Object getDefaultChannelKey() { return defaultChannelKey; }
    @NonNull public Object getDefaultChannelUuidKey() { return defaultChannelUuidKey; }
    @NonNull public Object getCurrentChannelKey() { return currentChannelKey; }
}
