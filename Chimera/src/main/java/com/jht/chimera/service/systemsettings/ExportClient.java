package com.jht.chimera.service.systemsettings;

import static com.jht.androidcommonalgorithms.network.AddressHelper.getIPAddress;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.datamatrixkeys.ConsoleConfigKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.enums.JHTTypes;
import com.jht.chimera.lib.settings.SettingsExportHelper;
import com.jht.chimera.lib.widget.dialog.MessageCenterMessage;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.NonNls;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

class ExportClient {

    private final TVSettingsManager tvSettingsManager;
    private final Context context;
    private final String hostName;
    private final int port;
    private MessageCenterMessage importingMsg;
    private MessageCenterMessage importMsgSuccess;
    private MessageCenterMessage importMsgFail;
    private final static String TAG = "ExportClient";

    ExportClient(Context context, String hostName, int port) {
        this.context = context;
        this.hostName = hostName;
        this.port = port;
        tvSettingsManager = new TVSettingsManager(context);
        new Thread(this::export).start();
    }

    private void export() {
        @NonNls String serverConsoleSerialNumber = "Matrix Console";
        try (
                Socket socket = new Socket(hostName, port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {

            tvSettingsManager.storeSettings();
            String fromServer;
            while ((fromServer = in.readLine()) != null) {
                Log.d(TAG,"Reading in line from server: " + fromServer);
                if (fromServer.startsWith(SystemSettings.REQUEST_CLIENT_VERSION)) {
                    JSONObject versionInfo = new JSONObject();
                    versionInfo.put("APIVersion", SystemSettings.API_VERSION);
                    versionInfo.put("SettingsVersion", SystemSettings.SETTINGS_VERSION);
                    out.println(versionInfo);
                }
                else if (fromServer.startsWith(SystemSettings.REQUEST_SERIAL_NUMBER)) {
                    serverConsoleSerialNumber = sendSerialNumberResponse(serverConsoleSerialNumber, out, fromServer);
                } else if (fromServer.equalsIgnoreCase(SystemSettings.IS_CLIENT_READY)) {
                    sendImportNotification(serverConsoleSerialNumber);
                    DataMatrix.sendEvent(ConsoleConfigKeys.EVENT_CONSOLE_THEME_READY);
                    out.println(SystemSettings.READY_TO_RECEIVE_SETTINGS);
                } else if (fromServer.startsWith("[{")) {
                    SettingsExportHelper.updateDataMatrixKeys(new JSONArray(fromServer));
                    getSettingFiles(socket, out);
                    updateTVTuner(out);
                    DataMatrix.sendEvent(ConsoleConfigKeys.EVENT_CONSOLE_THEME_READY);
                }
            }

        } catch (Exception e) {
            notifyExportFailure();
        }
    }

    private void notifyExportFailure() {
        hideMessage(importingMsg);
        hideMessage(importMsgSuccess);
        importMsgFail = new MessageCenterMessage()
                .setMessageText(context.getString(R.string.import_settings_fail_pg))
                .setTextColor(Color.WHITE)
                .setBackgroundColor(Color.RED)
                .setId(JHTTypes.MessageIds.IMPORT_SETTINGS_FAIL.getId())
                .setHideTime(36000);
        Log.d(TAG,"Displaying unable to import settings banner");
        DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, importMsgFail);
    }

    private void updateTVTuner(PrintWriter out) {
        tvSettingsManager.updateTVSettings(() -> {
            hideMessage(importingMsg);
            importMsgSuccess = new MessageCenterMessage()
                    .setMessageText(context.getString(R.string.import_settings_success_pg))
                    .setTextColor(Color.BLACK)
                    .setBackgroundColor(0xFFF5B324)
                    .setId(JHTTypes.MessageIds.IMPORT_SETTINGS_SUCCESS.getId())
                    .setHideTime(36000);
            DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, importMsgSuccess);
            out.println(SystemSettings.FINISHED_EXPORT);
        });
    }

    private void getSettingFiles(Socket socket, PrintWriter out) throws IOException {
        out.println(SystemSettings.MODEL_SETTINGS_RECEIVED);
        File zipFile = new File(SystemSettings.zipLocation);
        if (zipFile.exists()) {
            FileUtils.delete(zipFile);
        }

        InputStream iStream = socket.getInputStream();
        try(FileOutputStream fout = new FileOutputStream(SystemSettings.zipLocation)) {
            int c;
            byte[] buff = new byte[2048];
            while ((c = iStream.read(buff)) > 0) {
                if (c > 2 && buff[c - 1] == 23 && buff[c - 2] == 23 && buff[c - 3] == 23) {
                    fout.write(buff, 0, c - 3);
                    fout.flush();
                    break;
                }
                fout.write(buff, 0, c);
                fout.flush();
            }
        }
        ExecuteAsRoot.runSyncCommand();
        SettingsExportHelper.extractSettings(context, new File(SystemSettings.zipLocation));
    }

    private void sendImportNotification(@NonNls String serverConsoleSerialNumber) {
        hideMessage(importMsgFail);
        hideMessage(importMsgSuccess);
        importingMsg = new MessageCenterMessage()
                .setMessageText(context.getString(R.string.importing_configuration_pg).replace("$1", serverConsoleSerialNumber))
                .setTextColor(Color.WHITE)
                .setBackgroundColor(Color.BLACK)
                .setId(JHTTypes.MessageIds.IMPORTING_SETTINGS.getId())
                .setHideTime(36000);
        DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, importingMsg);
    }

    @NonNls
    private String sendSerialNumberResponse(@NonNls String serverConsoleSerialNumber, PrintWriter out, String fromServer) {
        String consoleSerial = fromServer.split(":")[1];
        if (!consoleSerial.isEmpty()) {
            serverConsoleSerialNumber = consoleSerial;
        }
        if (!DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING, "").isEmpty()) {
            out.println(DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING));
        } else {
            out.println("Console Serial Number Not Available, IP: " + getIPAddress()); //NON-NLS
        }
        return serverConsoleSerialNumber;
    }

    private void hideMessage(MessageCenterMessage message) {
        if(message != null) {
            message.setHidden(true);
            DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, message);
        }
    }

}
