package com.jht.chimera.lib.integrations.xarefit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.dapi.DapiLogin;
import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.stats.StatsAccumulated;
import com.jht.chimera.lib.common.stats.StatsAverages;
import com.jht.chimera.lib.common.stats.SummaryData;
import com.jht.chimera.service.dapi.DAPIUserManagement;
import com.jht.chimera.service.workout.enums.WorkoutRunState;
import com.jht.datamatrixlib.DataMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;



public class XareFitLib {
    private final String TAG = "XareFitLib";

    private static final String TREADMILL_ID = "1536";
    private static final String BIKE_ID = "1537";
    private static final String CLIMBMILL_ID = "1538";
    private static final String ASCENT_ID = "1539";
    private static final String ELLIPTICAL_ID = "1540";

    private final DataMatrix mDataMatrix;

    private boolean mIsUserLoggedIn = false;
    private boolean mStopHeartbeatLoop;
    private boolean mIsOkForUserCheckin;
    private Bitmap mQRCodeBitmap;
    private String mXareFitSiteId;
    private String mXareFitServerURL;
    private String mXareFitServerPort;
    private String mMacAddress;

    private static XareFitLib instance = null;
    public static XareFitLib getInstance() {
        if (instance == null) {
            instance = new XareFitLib();
        }

        return instance;
    }

    /**
     * Private constructor to initialize the singleton class
     */
    private XareFitLib() {
        // Singleton class private constructor to prevent instantiation
        mDataMatrix = new DataMatrix();

        mIsOkForUserCheckin = true;

        mDataMatrix.createBoolAdapter(SettingKeys.AssetManagement.DATA_ENABLE_XAREFIT_LOGIN_BOOL, enabled -> {
            if(enabled){
                // Initialize the XareFit data values from M/E/S screen
                init();
                setDataMatrixCallbacks();
            }
        }, true);

    }

    public void destroy() {
        mDataMatrix.finish();
    }

    private void setDataMatrixCallbacks(){
        // In the event the user updates the settings make sure the values are updated here
        mDataMatrix.createStringAdapter(SettingKeys.AssetManagement.DATA_XAREFIT_SITE_ID_STRING, xareFitSiteId -> {
            mXareFitSiteId = xareFitSiteId;
            mQRCodeBitmap = null;
        });
        mDataMatrix.createStringAdapter(SettingKeys.AssetManagement.DATA_XAREFIT_SERVER_URL_STRING, xareFitServerURL -> mXareFitServerURL = xareFitServerURL);
        mDataMatrix.createStringAdapter(SettingKeys.AssetManagement.DATA_XAREFIT_SERVER_PORT_STRING, xareFitServerPort -> mXareFitServerPort = xareFitServerPort);

        /*
         *  Listen for the workout run-state stop event message to process post-workout
         *  summary data if XareFit integration is enabled.
         *
         *  This case covers ending a workout and EStop
         */
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, workoutRunState -> {
            if (mIsUserLoggedIn && WorkoutRunState.STOP.ordinal() == workoutRunState) {
                new Thread(() -> handleWorkoutSummaryUpload(DataMatrix.getParcelable(WorkoutKeys.DATA_WORKOUT_SUMMARY_INFO_PARCELABLE, SummaryData.class))).start();
            }
        }, false);

        /*
         * On logout stop the heartbeat, check the user out of XareFit and cleanup the lib if
         * XareFit integration is enabled
         */
        mDataMatrix.createEventAdapter(DapiLogin.Keys.ACTION_LOGOUT, callBack -> {
            new Thread(() -> {
                    mStopHeartbeatLoop = true;
                    handleUserCheckOut();
                    cleanUp();
            }).start();
        }, false);
    }

    /*
     * Initialize the XareFit data values from M/E/S screen
     * Initialize MAC address
     */
    private void init() {
        BufferedReader in;
        try {
            in = new BufferedReader(new FileReader("/sys/class/net/wlan0/address"));
            mMacAddress = in.readLine();
            mMacAddress = mMacAddress.replace(":", "").toUpperCase();
            in.close();
        } catch (Exception ex) {
            Log.d(TAG, "failed to get mac address of wlan0(WiFi) from /sys/class/net/wlan0/address");
        }
    }

    /**
     * Cleans up the singleton instance.
     */
    private void cleanUp() {
        if(instance != null) {
            mIsOkForUserCheckin = false;
            mIsUserLoggedIn = false;
            mQRCodeBitmap = null;
        }
    }

    /**
     * Creates a QR code with the following embedded information
     *  1. XareFit site id - specified in the M/E/S page
     *  2. The mac address of the console - Currently using the value returned by eth0
     *  3. The machine type - Based off of the information obtained by the FrameConfigManager
     *
     * @param context - context used to get the configured dimensions for the QR code
     * @return a bitmap of the QR code with the embedded information
     */
    public Bitmap getLoginQRCode(Context context) {
        init(); // Initialize the XareFit data values from M/E/S screen
        if (!mXareFitSiteId.isEmpty()) {
            if(mQRCodeBitmap == null) {
                int qrCodeHeightWidth = context.getResources().getDimensionPixelSize(R.dimen.xarefit_qrcode_height_width);
                String qrCodeString = mXareFitSiteId + "," + mMacAddress + "," + getMachineType();
                BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                try {
                    mQRCodeBitmap = barcodeEncoder.encodeBitmap(qrCodeString, BarcodeFormat.QR_CODE, qrCodeHeightWidth, qrCodeHeightWidth);
                } catch (WriterException e) {
                    e.printStackTrace();
                    Log.d(TAG, "Error generating QR code");
                }
            }
        } else {
            Log.d(TAG, "The site id is empty a QR will not be generated");
        }

        return mQRCodeBitmap;
    }

    /**
     * Checks the specified XareFit server for a user login.
     *
     * Once called this method will run in a background thread until a successful login message is
     * received or the 60 second login timer has expired.  The caller is notified by a DataMatrix
     * event once one of the specified criteria are met.
     *
     * Per the generations implementation The following data is passed back to the caller.  Additional
     * information is returned by the check in call but was not used.
     *  1. A boolean flag that indicates if login was successful
     *  2. The user id or xid
     *  3. The user name
     * The user id and name will be empty string if login fails.
     */
    public void handleUserCheckIn() {
        new Thread(() -> {
            int serverQueryCount = 0;
            String response = "";
            String userName = "";
            int year;
            int gender;
            float weight;
            while (serverQueryCount <= 20) {
                if (mIsOkForUserCheckin) {
                    response = getJsonString(makeURLHeaderAddress(XareFitAPI.ASK_USER_CHECK_IN.getApiName()), 4000);
                    Log.d(TAG, "user checkIn response = " + response);
                    if (response.isEmpty()) {
                        Log.d(TAG, "got empty response from getJsonString(ASK_USER_CHECKIN, 4000ms)");
                        serverQueryCount++;
                    } else {
                        try {
                            JSONObject checkInResult = new JSONObject(response);
                            if (checkInResult.isNull("Obj")) {
                                Log.d(TAG, "user failed to log in to XareFit server => Try to connect again");
                                serverQueryCount++;
                            } else {
                                Log.d(TAG, "user logged into XareFit server successfully");
                                JSONObject checkedInUser = checkInResult.getJSONObject("Obj");
                                userName = checkedInUser.getString("Name");
                                year = checkedInUser.getInt("Year");
                                gender = checkedInUser.getInt("Gender");
                                weight = (float) checkedInUser.getDouble("Weight");
                                DAPIUserManagement.getInstance().populateXareFitUserProfile(userName,
                                        year, gender, weight);
                                mIsUserLoggedIn = true;
                                sendHeartBeats();
                                break;
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    try {
                        Log.d(TAG, "no response from getJsonString(ASK_USER_CHECKIN) wait 3 sec and try again ");
                        Thread.sleep(3000);
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                        Log.d(TAG, "error happened in Thread.sleep");
                    }
                }
            }

            DataMatrix.putBoolean(MainAppKeys.EVENT_XARE_FIT_LOGIN_SUCCESSFUL, mIsUserLoggedIn);

        }).start();
    }
	
	 /**
     * Pings the specified XareFit server for a user checkout response.
     *
     * If response is "successful", then mIsUserLoggedIn is set to false.
     */
    private void handleUserCheckOut() {
        String urlHeaderAddress = makeURLHeaderAddress(XareFitAPI.USER_CHECKOUT.getApiName());
        String response = "";
        int count = 0;

        while (count < 3) {
            Log.d(TAG, "in userCheckOut(), to call getJsonString(USER_CHECKOUT, 5000 ms)");
            response = getJsonString(urlHeaderAddress, 5000);
            Log.d(TAG, "userCheckout response = " + response);
            if(response.isEmpty()) {
                count ++;
            } else {
                try {
                    JSONObject jCheckOutResults = new JSONObject(response);
                    if (jCheckOutResults.isNull("Obj")) {
                        count++;
                        Log.d(TAG, "in userCheckOut(), got 'Obj':null from getJsonString(USER_CHECKOUT, 5000 ms)");
                    } else {
                        String successCheckOut = jCheckOutResults.getString("Obj");
                        if (successCheckOut.equalsIgnoreCase("successful")) {
                            Log.d(TAG, "in userCheckOut(), user checked out XareFit server successfully.");
                            mIsUserLoggedIn = false;
                            break;   // exit this while(true) loop
                        } else {
                            Log.d(TAG, "in userCheckOut(), didn't get 'Obj':'successful' from getJsonString(USER_CHECKOUT, 5000 ms)");
                            count++;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "in userCheckOut(), error happened in fetching info from JSON string.");
                }
            }
            try {
                Log.d(TAG, "in userCheckOut(), failed to checked out XareFit server => wait for 5 sec");
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                Log.e(TAG, "in userCheckOut(), error happened in Thread.sleep(5000)");
            }
        }   // end of while loop
        if (count == 3) {
            Log.d(TAG, "in userCheckOut(), user failed to checked out XareFit server for 3 times => stop calling userCheckout API.");
        }
    }

    /**
     * Creates a JSON string from the passed in workout summary data and passes it to the XareFit server
     *
     * @param workoutSummaryData the workout summary data
     */
    private void handleWorkoutSummaryUpload(SummaryData workoutSummaryData) {
        sendJsonString(makeURLHeaderAddress(XareFitAPI.UPLOAD_SUMMARY.getApiName()), getWorkoutSummaryJSON(workoutSummaryData));
    }

    /**
     * Sends a heartbeat message to the XareFit server every 60 seconds. Loop will continue to run
     * until stopHeartbeatLoop is set to true or 3 attempts in row fail.
     *
     * If an error occurs during the send the code will attempt another heartbeat every 6 seconds
     * until three failures in a row occur
     */
    private void sendHeartBeats() {
        new Thread(() -> {
            String urlHeaderAddress = makeURLHeaderAddress(XareFitAPI.UPLOAD_ALIVE.getApiName());
            String response;
            int count_failure = 0;
            mStopHeartbeatLoop = false;

            while (!mStopHeartbeatLoop || count_failure == 3) {
                response = getJsonString(urlHeaderAddress, 4000);
                if (response.isEmpty()) {
                    count_failure++;
                    Log.d(TAG, "no reply from XareFit server for heart-beat message");
                } else {
                    try {
                        JSONObject heartBeatResult = new JSONObject(response);
                        if (heartBeatResult.isNull("Obj")) {
                            count_failure++;
                        } else {
                            if (heartBeatResult.getString("Obj").equalsIgnoreCase("successful")) {
                                Log.d(TAG, "succeeded sending heart-beat message to XareFit server");
                                count_failure = 0;
                            } else {
                                Log.d(TAG, "failed to send heart-beat message to XareFit server ");
                                count_failure++;
                            }

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    if (count_failure != 0) {
                        Thread.sleep(5900);
                    } else {
                        Thread.sleep(60000);
                    }
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }

            }
        }).start();
    }

    /**
     * Creates the URL header to communicate with the XareFit server
     *
     * @param apiName the api call that is being made to the XareFit server
     * @return the URL to make a connection to the XareFit server
     */
    private String makeURLHeaderAddress(String apiName) {
        String urlHeaderAddress = "";
        String consoleInfo = getConsoleInfo();
        String xareURLHeader = getXareURLHeader();
        if (consoleInfo.isEmpty() || xareURLHeader.isEmpty()) {
            Log.d(TAG, "in makeURLHeaderAddress(), consoleInfo: " + consoleInfo + " or xareURLHeader: " + xareURLHeader + " is empty");
        }
        else {
            urlHeaderAddress = xareURLHeader + apiName + "?" + consoleInfo;
            Log.d(TAG, "in makeURLHeaderAddress(), xareURLHeader + apiName + consoleInfo: " + urlHeaderAddress);
        }
        return urlHeaderAddress;
    }

    /**
     * Generates the console info string to pass to the XareFit server
     *
     * The string is being generated this way to handle the case where the XareFit configurations are
     * updated and new console info string is needed
     *
     * @return the console info string used to identify the machine communicating with the XareFit server
     */
    private String getConsoleInfo() {
        String consoleInfo = "";
        if (!mXareFitSiteId.isEmpty() && !mMacAddress.isEmpty()) {
            consoleInfo = "SiteId=" + mXareFitSiteId + "&MachineId=" + mMacAddress + "&MachineType=" + getMachineType(); // NON-NLS
        }

        return consoleInfo;
    }

    /**
     * Generates the URL header used to make calls
     *
     * The header is being generated this way to handle the case where the XareFit configurations are
     * update and a new header is needed.
     *
     * @return The URL header used to call the XareFit server
     */
    private String getXareURLHeader() {
        String urlHeader = "";
        if (!mXareFitServerURL.isEmpty() && !mXareFitServerPort.isEmpty()) {
            urlHeader = "http://" + mXareFitServerURL + ":" + mXareFitServerPort + "/"; // NON-NLS
        }

        return urlHeader;
    }

    /**
     * Handles making the get calls to the XareFit server
     *
     * @param urlAddress the complete address that contains the server address the api call and the associated data
     * @param timeout the time to wait for read and write responses
     * @return the JSON data returned by the XareFit server or an empty string if no data was returned or the connection failed
     */
    @SuppressLint("SimpleDateFormat")
    private @NotNull String getJsonString(final String urlAddress, final int timeout) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String resultString = "";

        HttpURLConnection httpURLConnection = null;
        try {
            URL url = new URL(urlAddress);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setRequestProperty("Content-Type", "application/json");
            httpURLConnection.setRequestProperty("GM2-Client-Time", sdf.format(new Date()));
            httpURLConnection.setConnectTimeout(timeout);
            httpURLConnection.setReadTimeout(timeout);
            httpURLConnection.connect();

            int responseCode = httpURLConnection.getResponseCode();
            Log.d(TAG, "get responseCode = " + responseCode);

            switch(responseCode) {
                case HttpURLConnection.HTTP_OK:
                case HttpURLConnection.HTTP_CREATED:
                    resultString = getResponseData(httpURLConnection);
                    break;
                default:
                    // All other response codes are ignored
            }

            httpURLConnection.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }

        return resultString;
    }

    private @NotNull String sendJsonString(final String urlAddress, final @NotNull String json) {
        String responseMsg = "";

        HttpURLConnection httpURLConnection = null;
        try {
            // Create the connection to the XareFit server
            URL url = new URL(urlAddress);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Accept", "application/json;version=2.0");
            httpURLConnection.setRequestProperty("Content-Type", "application/json");
            httpURLConnection.setRequestProperty("Content-Length", Integer.toString(json.getBytes().length));
            httpURLConnection.setReadTimeout(10000);
            httpURLConnection.setConnectTimeout(15000);
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.connect();

            // Send the json data
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(httpURLConnection.getOutputStream());
            bufferedOutputStream.write(json.getBytes());
            bufferedOutputStream.close();

            int responseCode = httpURLConnection.getResponseCode();
            Log.d(TAG, "send responseCode = " + responseCode);

            switch (httpURLConnection.getResponseCode()) {
                case HttpURLConnection.HTTP_OK:
                case HttpURLConnection.HTTP_CREATED:
                    responseMsg = getResponseData(httpURLConnection);
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

    /**
     * Extracts and returns the result data from the passed in http connection as a string
     *
     * @param httpURLConnection the connection with the result data to be extracted
     * @return a string containing the result information extracted from the http connection
     */
    private String getResponseData(final @NotNull HttpURLConnection httpURLConnection) {
        StringBuilder resultStringBuilder = new StringBuilder();
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), "utf-8"));
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

    /**
     * Generates the JSON string with workout summary data
     *
     * @param workoutSummaryData the workout summary data to be parsed into JSON
     * @return the JSON string of workout summary data to send to the XareFit server
     */
    private String getWorkoutSummaryJSON(@NotNull SummaryData workoutSummaryData) {
        StringBuilder jsonWorkoutSummary = new StringBuilder();

        StatsAccumulated accumulatedStats = workoutSummaryData.totalSummary.statsAccumulated;
        StatsAverages averageStats = workoutSummaryData.totalSummary.statsAverages;

        int avgMetsx10 = Float.valueOf(averageStats.averageMets* 10).intValue();
        int avgInclinex10 = Float.valueOf(averageStats.averageIncline  * 10).intValue();
        int avgSpeedx1000 = Float.valueOf(averageStats.averageSpeedKph * 1000).intValue();
        int avgWatts_int = Float.valueOf(averageStats.averageWatts).intValue();
        int avgSPM_int = Float.valueOf(averageStats.averageStepsPerMinute).intValue();
        int levelx10 = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_LEVEL_INT) * 10;

        String machineType = getMachineType();
        jsonWorkoutSummary.append("{\"totalTime\":") // NON-NLS
                .append(Float.valueOf(accumulatedStats.secondsStateRun).intValue())
                .append(",\"totalTimeUnit\":\"sec\",") // NON-NLS
                .append("\"totalDistance\":"); // NON-NLS

        if (CLIMBMILL_ID.equals(machineType)) {
            jsonWorkoutSummary.append(Float.valueOf(accumulatedStats.floors * 10).intValue())
                    .append(",\"totalDistanceUnit\":\"0.1 floor\","); // NON-NLS
        } else {
            jsonWorkoutSummary.append(Float.valueOf(accumulatedStats.distanceKm * 1000).intValue())
                    .append(",\"totalDistanceUnit\":\"0.001 km\","); // NON-NLS
        }

        jsonWorkoutSummary.append("\"totalCalories\":") // NON-NLS
                .append(Float.valueOf(accumulatedStats.calories  * 1000).intValue())
                .append(",\"totalCaloriesUnit\":\"0.001 kcal\",") // NON-NLS
                .append("\"avgHR\":") // NON-NLS
                .append(Float.valueOf(averageStats.averageHeartRate).intValue())
                .append(",\"avgHRUnit\":\"bpm\","); // NON-NLS

        switch (machineType) {
            case TREADMILL_ID:
                jsonWorkoutSummary.append("\"avgMET\":") // NON-NLS
                        .append(avgMetsx10)
                        .append(",\"avgMETUnit\":\"0.1 MET\",") // NON-NLS
                        .append("\"avgPACE\":") // NON-NLS
                        .append(Integer.valueOf(Math.round(averageStats.averageSpeedKph)))
                        .append(",\"avgPACEUnit\":\"sec\",") // NON-NLS
                        .append("\"avgIncline\":" ) // NON-NLS
                        .append(avgInclinex10)
                        .append( ",\"avgInclineUnit\":\"0.1 %\",") // NON-NLS
                        .append("\"avgSpeed\":") // NON-NLS
                        .append(avgSpeedx1000)
                        .append(",\"avgSpeedUnit\":\"0.001 km/h\"}"); // NON-NLS
                break;
            case BIKE_ID:
                jsonWorkoutSummary.append("\"avgWatt\":") // NON-NLS
                        .append(avgWatts_int)
                        .append(",\"avgWattUnit\":\"Watt\",") // NON-NLS
                        .append("\"avgRPM\":") // NON-NLS
                        .append(Float.valueOf(averageStats.averageRpm).intValue())
                        .append(",\"avgRPMUnit\":\"RPM\",") // NON-NLS
                        .append("\"avgLevel\":") // NON-NLS
                        .append(levelx10)
                        .append(",\"avgLevelUnit\":\"0.1 level\",") // NON-NLS
                        .append("\"avgSpeed\":") // NON-NLS
                        .append(avgSpeedx1000)
                        .append(",\"avgSpeedUnit\":\"0.001 km/h\"}"); // NON-NLS
                break;
            case CLIMBMILL_ID:
                jsonWorkoutSummary.append("\"avgMET\":") // NON-NLS
                        .append(avgMetsx10)
                        .append(",\"avgMETUnit\":\"0.1 MET\",") // NON-NLS
                        .append("\"avgSPM\":") // NON-NLS
                        .append(Float.valueOf(averageStats.averageStepsPerMinute).intValue())
                        .append(",\"avgSPMUnit\":\"SPM\",") // NON-NLS
                        .append("\"avgLevel\":") // NON-NLS
                        .append(levelx10)
                        .append(",\"avgLevelUnit\":\"0.1 level\",") // NON-NLS
                        .append("\"avgGradient\":") // NON-NLS
                        .append(avgInclinex10)
                        .append(",\"avgGradientUnit\":\"0.1 gradient\"}"); // NON-NLS
                break;
            case ASCENT_ID:
                jsonWorkoutSummary.append("\"avgWatt\":") // NON-NLS
                        .append(avgWatts_int)
                        .append(",\"avgWattUnit\":\"Watt\",") // NON-NLS
                        .append("\"avgSPM\":") // NON-NLS
                        .append(avgSPM_int)
                        .append(",\"avgSPMUnit\":\"SPM\",") // NON-NLS
                        .append("\"avgLevel\":") // NON-NLS
                        .append(levelx10)
                        .append(",\"avgLevelUnit\":\"0.1 level\",") // NON-NLS
                        .append("\"avgGradient\":") // NON-NLS
                        .append(avgInclinex10)
                        .append(",\"avgGradientUnit\":\"0.1 gradient\"}"); // NON-NLS
                break;
            case ELLIPTICAL_ID:
                jsonWorkoutSummary.append("\"avgWatt\":") // NON-NLS
                        .append(avgWatts_int)
                        .append(",\"avgWattUnit\":\"Watt\",") // NON-NLS
                        .append("\"avgSPM\":") // NON-NLS
                        .append(avgSPM_int)
                        .append(",\"avgSPMUnit\":\"SPM\",") // NON-NLS
                        .append("\"avgLevel\":") // NON-NLS
                        .append(levelx10)
                        .append(",\"avgLevelUnit\":\"0.1 level\",") // NON-NLS
                        .append("\"avgSpeed\":") // NON-NLS
                        .append(avgSpeedx1000)
                        .append(",\"avgSpeedUnit\":\"0.001 km/h\"}"); // NON-NLS
                break;
        }

        Log.d(TAG, jsonWorkoutSummary.toString());

        return jsonWorkoutSummary.toString();
    }

    /**
     * Gets the machine type string based on the data stored in the FrameConfigManager
     *
     * @return the machine type string based on the data stored in the FrameConfigManager
     */
    private String getMachineType() {
        String machineType = "";

        switch(FrameConfigManager.getInstance().getFrameConfig().machineClassType) {
            case Treadmill:
                machineType = TREADMILL_ID;
                break;
            case Elliptical:
                machineType = ELLIPTICAL_ID;
                break;
            case Bike:
            case SpinBike:
                machineType = BIKE_ID;
                break;
            case Ascent:
                machineType = ASCENT_ID;
                break;
            case Climbmill:
                machineType = CLIMBMILL_ID;
                break;
            case Stepper:
            case Rower:
            case PassportPlayer:
            case Strength:
            case SDrive:
            case NUM_MACHINE_CLASSIFICATION_TYPES:
                Log.d(TAG, "The console is configured for an invalid machine type");
                break;
        }

        return machineType;
    }

    /**
     * Enum to specify the different calls that can be made to communicate with the XareFit server
     */
    public enum XareFitAPI {
        ASK_USER_CHECK_IN("UserCheckIn"),
        UPLOAD_SUMMARY("SummaryUpload"),
        USER_CHECKOUT("UserCheckOut"),
        UPLOAD_ALIVE("AliveUpload");

        private final String apiName;

        XareFitAPI(@NonNls String api_name) {
            this.apiName = api_name;
        }

        public String getApiName() { return apiName;}

    }
}
