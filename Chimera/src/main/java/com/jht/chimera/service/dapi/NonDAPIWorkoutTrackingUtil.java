package com.jht.chimera.service.dapi;

import android.util.Log;

import com.jht.chimera.lib.common.dapi.DapiUtil;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataMatrix;
import com.jhtp.dapiservice.comm.CborCommands;
import com.jhtp.dapiservice.wt.cardio.DapiCardioComplete;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class NonDAPIWorkoutTrackingUtil {

    private NonDAPIWorkoutTrackingUtil() {
        // prevent instantiation of utility class
    }

    private static String TAG = "NonDAPIWorkoutTrackingUtil";

    public static void sendNonDAPIWorkoutSummary(DapiCardioComplete cardioComplete) {
        // If a non-Dapi intranet endpoint is defined in M/E/S,
        // then use the stored UID from the RFID chip as the user token
        final String rfidToken = DataMatrix.getString(DAPIKeys.DATA_NON_DAPI_USER_CHIP_UUID_STRING);
        final String alternateEndpoint = DataMatrix.getString(SettingKeys.Experimental.DATA_WORKOUT_DATA_ALTERNATE_ENDPOINT_STRING);

        new Thread(() -> sendJsonString(alternateEndpoint, getCardioSummaryJson(cardioComplete, rfidToken))).start();

        // Reset DATA_NON_DAPI_USER_CHIP_UUID_STRING to empty string after workout is complete
        DataMatrix.putString(DAPIKeys.DATA_NON_DAPI_USER_CHIP_UUID_STRING, "");
    }

    private static String getCardioSummaryJson(DapiCardioComplete cardioComplete, String rfidToken) {
        StringBuilder jsonWorkoutSummary = new StringBuilder();
        jsonWorkoutSummary.append("{\n\"body\": \"[") // NON-NLS
                .append(CborCommands.CardioWorkoutComplete.Value)
                .append("," + 4)
                .append(",\\\"" + rfidToken + "\\\"")
                .append(",\\\"" + DapiUtil.getMachineId() + "\\\"")
                .append("," + cardioComplete.workoutType)
                .append(",\\\"" + cardioComplete.programUuid + "\\\"")
                .append(",\\\"" + cardioComplete.programDescription + "\\\"")
                .append(",\\\"" + cardioComplete.workoutOriginator + "\\\"")
                .append("," + cardioComplete.programLevel)
                .append("," + cardioComplete.workoutStartTime)
                .append("," + cardioComplete.workoutTime)
                .append("," + cardioComplete.warmUpTime)
                .append("," + cardioComplete.coolDownTime)
                .append("," + cardioComplete.distanceKm)
                .append("," + cardioComplete.averagePace)
                .append("," + cardioComplete.peakResistance)
                .append("," + cardioComplete.averageResistance)
                .append("," + cardioComplete.peakHeartRate)
                .append("," + cardioComplete.averageHeartRate)
                .append("," + cardioComplete.calories)
                .append("," + cardioComplete.peakWatts)
                .append("," + cardioComplete.averageWatts)
                .append("," + cardioComplete.totalFloors)
                .append("," + cardioComplete.totalSteps)
                .append("," + cardioComplete.totalElevation)
                .append("," + cardioComplete.peakKph)
                .append("," + cardioComplete.averageKph)
                .append("," + cardioComplete.weightKg)
                .append("]\"\n}");

        Log.d(TAG, "jsonString = " + jsonWorkoutSummary);
        return jsonWorkoutSummary.toString();
    }

    private static @NotNull String sendJsonString(final String urlAddress, String jsonString) {
        String responseMsg = "";

        HttpURLConnection httpURLConnection = null;
        try {

            URL url = new URL(urlAddress);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Accept", "application/json");
            httpURLConnection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            httpURLConnection.setRequestProperty("Content-Length", Integer.toString(jsonString.getBytes().length));
            httpURLConnection.setReadTimeout(10000);
            httpURLConnection.setConnectTimeout(15000);
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.connect();

            // Send the json data
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(httpURLConnection.getOutputStream());
            bufferedOutputStream.write(jsonString.getBytes());
            bufferedOutputStream.flush();
            bufferedOutputStream.close();

            int responseCode = httpURLConnection.getResponseCode();
            Log.d(TAG, "responseCode = " + responseCode);

            switch (httpURLConnection.getResponseCode()) {
                case HttpURLConnection.HTTP_OK:
                case HttpURLConnection.HTTP_CREATED:
                    responseMsg = getResponseData(httpURLConnection);
                    Log.d(TAG, "responseMsg = " + responseMsg);
                    break;
                default:
                    // All other response codes are ignored
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }

        return responseMsg;
    }

    private static String getResponseData(final @NotNull HttpURLConnection httpURLConnection) {
        StringBuilder resultStringBuilder = new StringBuilder();
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), "UTF-8"));
            String currentLine;
            while ((currentLine = bufferedReader.readLine()) != null) {
                resultStringBuilder.append(currentLine)
                        .append("\n");
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
            resultStringBuilder.append("");
        }

        return resultStringBuilder.toString();
    }
}
