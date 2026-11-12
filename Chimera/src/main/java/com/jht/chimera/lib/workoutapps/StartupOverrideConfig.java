package com.jht.chimera.lib.workoutapps;

import java.util.Arrays;
import java.util.List;

/**
 * This configuration class serves as the single source of truth for
 * default system startup override apps. It decouples UI layouts from hardcoded string arrays and maps.
 */
public final class StartupOverrideConfig {

    public static class Entry {
        public final String uuid;
        public final String displayName;

        public Entry(String uuid, String displayName) {
            this.uuid = uuid;
            this.displayName = displayName;
        }
    }

    public static final List<Entry> DEFAULT_OVERRIDE_APPS = Arrays.asList(
            new Entry("", "Matrix"),
            new Entry(AppIds.MATRIX_TEST_UUID, "Matrix Test App"),
            new Entry(AppIds.HEROS_JOURNEY_UUID, "Hero's Journey"),
            new Entry(AppIds.CUSTOM_OVERRIDE_APP_UUID, "Custom Override")
    );

    /**
     * Resolves the user-friendly name of the starting app from the UUID.
     * Returns null if the UUID is not in the default starting apps list.
     *
     * @param uuid The starting app override UUID
     * @return The user-friendly display name, or null if not found
     */
    public static String getOverrideAppName(String uuid) {
        for (Entry entry : DEFAULT_OVERRIDE_APPS) {
            if (entry.uuid.equals(uuid)) {
                return entry.displayName;
            }
        }
        return null;
    }
}
