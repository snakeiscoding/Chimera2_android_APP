package com.jht.chimera.service.systemsettings;

import static com.jht.androidcommonalgorithms.network.AddressHelper.getIPAddress;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.network.AddressHelper;
import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.androidcommonalgorithms.util.ThreadHelper;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.ImportExportKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.usb.StorageUtils;
import com.jht.chimera.lib.network.ethernet.NetworkSettingsImportExport;
import com.jht.chimera.lib.settings.SettingsExportHelper;
import com.jht.chimera.service.systemsettings.XMLHelper.XMLNode;
import com.jht.chimera.service.systemsettings.tv_channel_manager.TvChannelManager;
import com.jht.chimera.service.systemsettings.tv_channel_manager.endpoints.ChannelListDestination;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.NonNls;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Random;

import kotlin.Unit;

public class SystemSettings extends Service {
    public static final int API_VERSION = 3;
    public static final int DEFAULT_CLIENT_API_VERSION = 2;
    public static final int SETTINGS_VERSION = 3;
    public static final int DEFAULT_CLIENT_SETTINGS_VERSION = 1;

    public static final String REQUEST_CLIENT_VERSION = "RequestClientVersion"; // NON-NLS
    public static final String REQUEST_SERIAL_NUMBER = "RequestConsoleSerialNumber"; // NON-NLS
    public static final String READY_TO_RECEIVE_SETTINGS = "readyToReceiveSettings"; // NON-NLS
    public static final String IS_CLIENT_READY = "isClientReadyForSettings"; // NON-NLS
    public static final String MODEL_SETTINGS_RECEIVED = "model_settings_received"; // NON-NLS
    public static final String TRANSFERRING = "Transfering"; // NON-NLS
    public static final String CONNECTED = "connected"; // NON-NLS
    public static final String DONE = "Done"; // NON-NLS
    public static final String FAILED_EXPORT = "Failed Export"; // NON-NLS
    public static final String RECONNECT = "reconnect"; // NON-NLS
    public static final String EXIT = "Exit"; // NON-NLS
    public static final String FINISHED_EXPORT = "Export_Done"; // NON-NLS


    interface IImportSettingsComplete {
        void onImportSettingsComplete();
    }

    private final DataMatrix mDataMatrix = new DataMatrix();

    private final ArrayList<ServerTask> serverTasks = new ArrayList<>();
    @SuppressLint("SdCardPath")
    @NonNls
    static final String zipLocation = "/sdcard/Download/settings.zip";

    private TVSettingsManager tvSettingsManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        listenForClientConnections();
        listenForSystemRequests();
        tvSettingsManager = new TVSettingsManager(this);
        if (BuildConfig.HAS_TV) {
            TvChannelManager.INSTANCE.startDailyChannelListImport(this);
        }
        new ListenForBroadcast(this, mDataMatrix);
    }

    private void listenForClientConnections() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(1602)) {
                while (true) {
                    serverTasks.add(new ServerTask(this, serverSocket.accept(), new File(zipLocation)));
                }
            } catch (IOException ignored) {
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        mDataMatrix.finish();
        TvChannelManager.INSTANCE.stopDailyChannelListImport();
        super.onDestroy();
    }

    private void listenForSystemRequests() {
        mDataMatrix.createEventAdapter(ImportExportKeys.ACTION_BROADCAST_SETTINGS, s -> broadcastConsoleSettings());
        mDataMatrix.createEventAdapter(ImportExportKeys.ACTION_EXPORT_SETTINGS_USB, s -> exportSettingsToUSB());
        mDataMatrix.createEventAdapter(ImportExportKeys.ACTION_IMPORT_SETTINGS_USB, s -> importSettingsFromUSB());
        mDataMatrix.createStringAdapter(ImportExportKeys.DATA_EXPORT_SETTINGS_STRING, this::exportSettingsToNetwork);
        mDataMatrix.createEventAdapter(ImportExportKeys.ACTION_IMPORT_TV_CHANNELS_INTERNET, s -> transferTvChannelList(ChannelListDestination.INTERNET, true));
        mDataMatrix.createEventAdapter(ImportExportKeys.ACTION_EXPORT_TV_CHANNELS_INTERNET, s -> transferTvChannelList(ChannelListDestination.INTERNET, false));
    }

    private void broadcastConsoleSettings() {
        if (AddressHelper.isNetworkConnected(getApplicationContext())) {
            exitClientWorkingThreads();
            new Thread(() -> {
                String xml = createExportXML();
                try (DatagramSocket ds = new DatagramSocket()) {
                    ds.setBroadcast(true);
                    DatagramPacket dp =new DatagramPacket(xml.getBytes(), xml.length(), InetAddress.getByName("255.255.255.255"), 1600);
                    for (int i = 0; i < 5; i++) {
                        ds.send(dp);
                        ThreadHelper.sleepIgnoreInterruption(5000);
                    }

                } catch (Exception ignored) {}
                finally {
                    DataMatrix.sendEvent(ImportExportKeys.EVENT_REFRESH_UI);
                }
            }).start();
        } else {
            DataMatrix.sendEvent(ImportExportKeys.EVENT_REFRESH_UI);
        }
    }

    private void exportSettingsToUSB() {
        new Thread(() -> {
            NetworkSettingsImportExport.usbExportWifiConfig();
            File exportDir = new File(StorageUtils.getUSBPath(), "settings");
            FileUtils.delete(exportDir);
            //noinspection ResultOfMethodCallIgnored
            exportDir.mkdirs();
            if (exportDir.exists()) {
                FileUtils.writeToFile(SettingsExportHelper.getDataMatrixSettings(false, null).toString(), new File(exportDir, "/json_settings"));
                SettingsExportHelper.collectSettings(this, new File(exportDir, "settings.zip"));
            }
            DataMatrix.sendEvent(ImportExportKeys.EVENT_FINISHED_EXPORT_USB);
        }).start();
    }

    private void exportSettingsToNetwork(String s) {
        new Thread(() -> {
            if (AddressHelper.isNetworkConnected(getApplicationContext())) {
                exportSettingsToClients(s);
            }
        }).start();
    }

    private void importSettingsFromUSB() {
        new Thread(() -> {
            File importDir = new File(StorageUtils.getUSBPath(), "settings");
            if (importDir.exists()) {
                importWifiFromUSB();
                tvSettingsManager.storeSettings();
                File jsonFile = new File(importDir, "/json_settings");
                if (jsonFile.exists()) {
                    try {
                        SettingsExportHelper.updateDataMatrixKeys(new JSONArray(FileUtils.readFileToString(jsonFile)));
                    } catch (JSONException ignored) {
                    }
                }
                SettingsExportHelper.extractSettings(this, new File(importDir, "/settings.zip"));
                tvSettingsManager.updateTVSettings(() -> DataMatrix.sendEvent(ImportExportKeys.EVENT_FINISHED_IMPORT_USB));
            }
        }).start();
    }

    private void importWifiFromUSB() {
        if(!DataMatrix.getBoolean(SettingKeys.Experimental.DATA_DISABLE_WIFI_BOOL)) {
            NetworkSettingsImportExport.usbImportWifiConfig();
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                Log.d("SystemSettings", "DISABLING WIFI");
                wm.setWifiEnabled(false);
                ThreadHelper.sleepIgnoreInterruption(500);
                Log.d("SystemSettings", "ENABLING WIFI");
                wm.setWifiEnabled(true);
                // send broadcast which will trigger WifiConfigManager loadFromStore
                if (!ProductConfig.hardware.isVortexBase()) {
                    sendBroadcast(new Intent(Intent.ACTION_LOCKED_BOOT_COMPLETED));
                }
            }
        }
    }

    private void transferTvChannelList(
        ChannelListDestination destination,
        boolean isImport
    ) {
        TvChannelManager.INSTANCE.transferChannelList(
            this,
            SettingKeys.TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING)),
            destination,
            isImport,
            (transferEvent) -> {
                DataMatrix.putParcelable(ImportExportKeys.EVENT_FINISHED_TV_CHANNEL_TRANSFER_PARCELABLE, transferEvent);
                return Unit.INSTANCE;
            }
        );
    }

    private void exportSettingsToClients(String ipAddress) {
        for(ServerTask task: serverTasks) {
            task.exportSettings(ipAddress);
        }
    }

    private void exitClientWorkingThreads() {
        for(ServerTask task: serverTasks) {
            task.destroy();
        }
        serverTasks.clear();
    }

    /*
     * This creates the xml file to send to broadcast to the other consoles.
     */
    private String createExportXML() {
        XMLHelper helper = new XMLHelper();
        Random random = new Random();
        XMLNode settings = helper.AddNode(new XMLNode("SettingsAvailable", null));
        settings.AddChild(new XMLNode("Nonce", Integer.toString(random.nextInt())));
        settings.AddChild(new XMLNode("Console", getConsoleName()));
        settings.AddChild(new XMLNode("APIVersion", Integer.toString(SystemSettings.API_VERSION)));
        settings.AddChild(new XMLNode("SettingsVersion", Integer.toString(SystemSettings.SETTINGS_VERSION)));
        settings.AddChild(new XMLNode("Host", getIPAddress()));
        settings.AddChild(new XMLNode("Port", "1600"));
        settings.AddChild(new XMLNode("SettingsPort", "1602"));
        settings.AddChild(new XMLNode("ConsoleSerialNumber", "console_serial"));
        settings.AddChild(new XMLNode("FrameSerialNumber", "frame_serial"));
        return helper.getXML();

    }

    public static String getConsoleName() {
        switch (ProductConfig.flavor) {

            case Chimera:
                return "CHIMERA_MAIN_MATRIX_CHIMERA";
            case Parker:
                return "PARKER_MAIN_MATRIX_PARKER";
            case Vision600E:
                return "VISION600E_MATRIX_PARKER";
            case T600XE:
                return "T600XE_MATRIX_PARKER";
            case LCGUI:
                return "CHIMERA_MAIN_MATRIX_LCGUI";
            case Unknown:
            default:
                return "UNKNOWN";
        }
    }

}
