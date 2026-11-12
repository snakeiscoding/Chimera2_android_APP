package com.jht.chimera.service.consoleconfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.test.fixtures.BaseTestFixture;
import com.jht.datamatrixlib.DataMatrix;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import okhttp3.mockwebserver.MockWebServer;

class ConsoleConfigServiceTestFixture extends BaseTestFixture {
    private final MockWebServer webServer = new MockWebServer();
    private final ConsoleConfigServiceResponseHandler responseHandler = new ConsoleConfigServiceResponseHandler();
    private final ConsoleConfigService configService;
    private final File skinZip = new File(getTargetContext().getFilesDir(), "consoleconfig/skin_test.zip");

    ConsoleConfigServiceTestFixture() throws IOException {
        webServer.setDispatcher(responseHandler);
        webServer.start();
        configService = new ConsoleConfigService(webServer.url("/").url().toString(), getTargetContext());
    }

    /**
     * Loads the asset into the mock web server and tells the config service to download the config.
     *
     * @param assetPath The asset we are testing.
     */
    void testConfig(String assetPath) {
        responseHandler.setResponseXML(FileUtils.readAssetToString(getTestContext(), assetPath));
        configService.loadConsoleConfig();
    }

    /**
     * Test a configuration that contains a custom skin.
     *
     * @param version           The version the console currently has.
     * @param assetPath         The path for the XML configuration.
     * @param shouldDownload    True if the new skin should be downloaded.
     */
    void runSkinTest(String version, String assetPath, boolean shouldDownload) throws IOException {
        File skinTestFile = new File("/data/data/com.jht.chimera/files/settings/theme/temp.txt");
        String skinTestFileContents = "This is a test\n";
        writeSkinZip(skinTestFile.getName(), skinTestFileContents);
        DataMatrix.putString(SettingKeys.AssetManagement.DATA_CUSTOM_SKIN_VERSION_STRING, version);
        testConfig(assetPath);
        if(shouldDownload) {
            assertTrue(skinTestFile.exists());
            assertEquals(skinTestFileContents, FileUtils.readFileToString(skinTestFile));
            assertTrue(skinTestFile.delete());
        }
        else {
            assertFalse(skinTestFile.exists());
        }

    }

    /**
     * Load XML and check the mfi and gymkit states.
     *
     * @param assetName         Name of the asset containing the XML.
     * @param mfiEnabled        True if we expect mfi to be enabled at the end of the test.
     * @param gymkitEnabled     True if we expect gymkit to be enabled at the end of the test.
     *
     */
    void runAppleTest(String assetName, boolean mfiEnabled, boolean gymkitEnabled) {
        testConfig(assetName);
        assertEquals(mfiEnabled, DataMatrix.getBoolean(SettingKeys.Hardware.DATA_MFI_ENABLE_BOOL));
        assertEquals(gymkitEnabled, DataMatrix.getBoolean(SettingKeys.Hardware.DATA_GYMKIT_ENABLE_BOOL));

    }

    /**
     * Load XML and check the mfi and gymkit states.
     *
     * @param assetName         Name of the asset containing the XML.
     * @param logsEnabled       True if persistent logs should be enabled.
     *
     */
    void runPersistentTest(String assetName, boolean logsEnabled) {
        testConfig(assetName);
        assertEquals(logsEnabled, DataMatrix.getBoolean(SettingKeys.Debug.DATA_ENABLE_PERSISTENT_LOG_BOOL));

    }
    /**
     * Builds the zip the skin XML assets point at. A *.zip asset is gitignored, and assets:// checks
     * the target files dir before the APK, so it is written there.
     */
    private void writeSkinZip(String entryName, String contents) throws IOException {
        if (skinZip.exists()) {
            return;
        }
        assertTrue(skinZip.getParentFile().isDirectory() || skinZip.getParentFile().mkdirs());
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(skinZip))) {
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(contents.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }

    @Override
    public void close() {
        super.close();
        //noinspection ResultOfMethodCallIgnored
        skinZip.delete();
        try {
            webServer.close();
        } catch (IOException ignored) {
        }
    }
}
