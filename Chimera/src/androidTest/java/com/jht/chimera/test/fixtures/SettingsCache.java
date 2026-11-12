package com.jht.chimera.test.fixtures;

import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.settings.SettingsExportHelper;

import org.json.JSONArray;

/**
 * This class caches the system settings which includes the DM settings keys and the settings files.
 */
class SettingsCache {

    private JSONArray cache;
    private boolean cachedSettingsDir = false;

    public SettingsCache() {
        cacheSettingsDir();
        cacheDataMatrixSettings();
    }

    public void cacheSettingsDir() {
        ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
        executeAsRoot.addCommand("mkdir -p /sdcard/test/backup");
        executeAsRoot.addCommand("rm -rf /sdcard/test/backup/*");
        executeAsRoot.addCommand("cp -rf /data/data/com.jht.chimera/files/settings/* /sdcard/test/backup");
        executeAsRoot.execute();
        cachedSettingsDir = true;
    }

    private void restoreSettingsDir() {
        if(cachedSettingsDir) {
            ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
            executeAsRoot.addCommand("rm -rf /data/data/com.jht.chimera/files/settings/*");
            executeAsRoot.addCommand("cp -rf /sdcard/test/backup/* /data/data/com.jht.chimera/files/settings ");
            executeAsRoot.execute();
            ExecuteAsRoot.fixFilesInPackage("com.jht.chimera");
            cachedSettingsDir = false;
        }
    }

    private void cacheDataMatrixSettings() {
        cache = SettingsExportHelper.getDataMatrixSettings(true, null);
    }

    public void restore() {
        restoreDataMatrixSettings();
        restoreSettingsDir();
    }

    private void restoreDataMatrixSettings() {
        if(cache == null) { return; }
        SettingsExportHelper.updateDataMatrixKeys(cache);
    }
}
