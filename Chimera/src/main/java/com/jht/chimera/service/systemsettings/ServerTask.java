package com.jht.chimera.service.systemsettings;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.jht.chimera.lib.common.datamatrixkeys.ImportExportKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.settings.SettingsExportHelper;
import com.jht.chimera.lib.settings.compatibilitymanager.SiteSurveyCompatibilityManager;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.NonNls;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

class ServerTask {
    public static final String TAG = "ServerTask";

    private enum STATE {
        NEEDS_CLIENT_CONSOLE_SERIAL_NUMBER,
        CONNECTED,
        READY_TO_BEGIN_TRANSFER,
        READY_TO_SEND_ASSETS
    }

    private STATE state = STATE.NEEDS_CLIENT_CONSOLE_SERIAL_NUMBER;

    private final Context context;
    private final Socket socket;
    private String clientHost;
    private final File zipFile;
    private PrintWriter out;
    private int clientApiVersion = SystemSettings.DEFAULT_CLIENT_API_VERSION;
    private int clientSettingsVersion = SystemSettings.DEFAULT_CLIENT_SETTINGS_VERSION;

    ServerTask(@NonNull Context context, @NonNull Socket socket, @NonNull File zipFile) {
        this.context = context;
        this.zipFile = zipFile;
        this.socket = socket;
        new Thread(this::manageClientConnection).start();
    }

    private void manageClientConnection() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out = new PrintWriter(socket.getOutputStream(), true);

            try {
                out.println(SystemSettings.REQUEST_CLIENT_VERSION);
                socket.setSoTimeout(3000);
                JSONObject clientVersions = new JSONObject(in.readLine());
                if (clientVersions.has("APIVersion") && clientVersions.has("SettingsVersion")) {
                    this.clientApiVersion = clientVersions.getInt("APIVersion");
                    this.clientSettingsVersion = clientVersions.getInt("SettingsVersion");
                }
            } catch (Exception ignored) {
                Log.w(TAG, "Invalid version response from client, assuming APIVersion " + clientApiVersion + " and SettingsVersion " + clientSettingsVersion);
            } finally {
                socket.setSoTimeout(0);
            }

            String inputLine;
            out.println(SystemSettings.REQUEST_SERIAL_NUMBER + ":" + DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING, "SOME SERIAL NUMBER")); //NON-NLS
            while ((inputLine = in.readLine()) != null) {
                processInput(inputLine);
            }
            notifyClientIsReady(clientHost, SystemSettings.FAILED_EXPORT);
        } catch (Exception ignored) {}
        finally {
            destroy();
        }
    }

    void destroy() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    void exportSettings(String ipAddress) {
        if(socket.isClosed() || out == null) {
            notifyClientIsReady(ipAddress, SystemSettings.RECONNECT);
        }
        else if(clientHost != null && clientHost.equals(ipAddress)) {
            out.println(SystemSettings.IS_CLIENT_READY);
        }
    }

    private void notifyClientIsReady(String hostName, @NonNls String status) {
        DataMatrix.putStringList(ImportExportKeys.DATA_CONSOLE_IDENTIFICATION_AND_STATUS_STRING_LIST, new ArrayList<>(Arrays.asList(hostName, status)));
    }

    private void processInput(@NonNull String input) throws IOException {
        switch (state) {
            case NEEDS_CLIENT_CONSOLE_SERIAL_NUMBER:
                clientHost = input;
                notifyClientIsReady(input, SystemSettings.CONNECTED);
                state = STATE.CONNECTED;
            case CONNECTED:
                if(input.equalsIgnoreCase(SystemSettings.READY_TO_RECEIVE_SETTINGS)) {
                    notifyClientIsReady(clientHost, SystemSettings.TRANSFERRING);
                    JSONArray jsonArray = SettingsExportHelper.getDataMatrixSettings(
                        false,
                        (key, value) -> {
                            // Validate each setting's compatibility before including it in export
                            boolean isCompatible = SiteSurveyCompatibilityManager.INSTANCE.isSettingCompatible(key, value, SystemSettings.SETTINGS_VERSION, clientSettingsVersion);
                            if (!isCompatible) { Log.d(TAG, "Excluding " + key + " from Site Survey export due to incompatibility."); }
                            return isCompatible;
                        }
                    );
                    out.println(jsonArray.toString());
                    state = STATE.READY_TO_BEGIN_TRANSFER;
                }
                break;
            case READY_TO_BEGIN_TRANSFER:
                if(input.equalsIgnoreCase(SystemSettings.MODEL_SETTINGS_RECEIVED)) {
                    SettingsExportHelper.zipSettings(context, zipFile);
                    OutputStream os = socket.getOutputStream();
                    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(zipFile));
                    byte[] buff = new byte[2048];
                    int c;

                    while ((c = bis.read(buff)) > 0) {
                        os.write(buff, 0, c);
                        os.flush();
                    }

                    buff[0] = 23;
                    os.write(buff, 0, 1);
                    os.write(buff, 0, 1);
                    os.write(buff, 0, 1);
                    state = STATE.READY_TO_SEND_ASSETS;
                }
                break;
            case READY_TO_SEND_ASSETS:
                if(input.equalsIgnoreCase(SystemSettings.FINISHED_EXPORT)) {
                    state = STATE.CONNECTED;
                    notifyClientIsReady(clientHost, SystemSettings.DONE);
                }
                break;
        }
    }
}
