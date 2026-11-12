package com.jht.chimera.lib.common.dapi;

import static com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys.Constants.DAPI_DEPLOYMENT_DEV;
import static com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys.Constants.DAPI_DEPLOYMENT_PROD;
import static com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys.Constants.DAPI_DEPLOYMENT_QA;
import static com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys.Constants.DAPI_DEPLOYMENT_STAGING;

import android.annotation.SuppressLint;
import android.util.Log;

import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.NonNls;

/**
 * Utility class that assists with getting values useful for DAPI.
 */
@SuppressLint("MissingPermission")
public class DapiUtil {
    @NonNls private static final String TAG = "DapiUtil";
    // Base prefix used for all device UUIDs, last digits are the MAC address
    //private static final String UUID_PREFIX = "6A6F686E-736F-6E6C-7464";

    @NonNls private static final String DEV_URL = "https://dms-cbor-dev.jfit.co";
    @NonNls private static final String QA_URL = "https://dms-cbor-qa.jfit.co";
    @NonNls private static final String STAGING_URL = "https://dms-cbor-staging.jfit.co";
    @NonNls private static final String PROD_URL = "https://dms-cbor.jfit.co";

    // url's for consoleConfigService to send itself to core via a POST request
    private static final String CONFIG_POST_DEV_URL = "https://dev-console-management-api.jfit.co/";
    private static final String CONFIG_POST_QA_URL = "https://qa-console-management-api.jfit.co/";
    private static final String CONFIG_POST_STAGING_URL = "https://staging-console-management-api.jfit.co/";
    private static final String CONFIG_POST_PROD_URL = "https://console-management-api.jfit.co/";

    private static final String MAC_ADDRESS_FILE = "/sys/class/net/wlan0/address";
    @NonNls public static final String LOGIN_TYPE_XID = "xid";
    @NonNls public static final String LOGIN_TYPE_RFID = "rfid";
    @NonNls public static final String LOGIN_TYPE_BARCODE = "barcode";
    @NonNls public static final String LOGIN_TYPE_EMAIL = "email";
    @NonNls public static final String LOGIN_TYPE_CUSTOM = "custom";
    @NonNls public static final String LOGIN_TYPE_QR_CODE = "qr_code";
    @NonNls public static final String LOGIN_TYPE_WALLET = "wallet";
    //public static final String LOGIN_TYPE_GUEST = "guest";
    @NonNls public static final String LOGIN_TYPE_PHONE = "phone_num";
    @NonNls public static final String LOGIN_TYPE_PROFILE = "profile";  // quick select profile instead of full login
    @NonNls public static final String LOGIN_TYPE_OAUTH = "oauth";

    private DapiUtil() {
        // prevent instantiation of utility class.
    }


    /**
     * Gets the machine ID associated with this device. The machine ID is the mac address appended
     * to a set for Johnson products.
     *
     * @return The machine ID associated with this device
     */
    public static String getMachineId() {
        String dapiMachineUuid = DataMatrix.getString(DAPIKeys.DATA_DAPI_MACHINE_UUID_STRING);
        if(dapiMachineUuid.isEmpty()) {
            return computeMachineId();
        }
        return dapiMachineUuid;
    }

    public static String computeMachineId() {

        return computeMachineId(DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING));
    }


    public static String computeMachineId(String frameSerial) {

        if (frameSerial != null && !frameSerial.isEmpty()) {
            try {
                frameSerial = frameSerial.toUpperCase();
                String hash = FileUtils.computeMd5(frameSerial);
                return hash.substring(0, 8) +
                        "-" +
                        hash.substring(8, 12) +
                        "-" +
                        hash.substring(12, 16) +
                        "-" +
                        hash.substring(16, 20) +
                        "-" +
                        hash.substring(20);
            } catch (Exception e) {
                Log.w(TAG, e);
            }
        }

        return DapiUser.GUEST_USER_UUID;
    }


    /*
    public static String readMacAddressLine() {
        File file = new File(MAC_ADDRESS_FILE);
        String line = "00:00:00:00:00";
        InputStream in = null;
        BufferedReader reader;
        try {
            in = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(in));
            line = reader.readLine();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found!");
        } catch (IOException e) {
            Log.e(TAG, "IOException!");
        }

        try {
            if (in != null) in.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        return line;
    }
*/
    /**
     * Gets the URL of a valid deployment number, or blank if it is not valid.
     *
     * @param deployment The deployment number for prod, staging, qa, or dev
     * @return The associated URL
     */
    public static String getDapiUrl(int deployment) {
        switch (deployment) {
            case DAPI_DEPLOYMENT_PROD:
                return PROD_URL;
            case DAPI_DEPLOYMENT_STAGING:
                return STAGING_URL;
            case DAPI_DEPLOYMENT_QA:
                return QA_URL;
            case DAPI_DEPLOYMENT_DEV:
                return DEV_URL;
            default:
                return "";
        }
    }

    /**
     * Gets the URL of a valid deployment number, or blank if it is not valid.
     * Used for the console configuration service to send itself in a POST request to core
     * @param deployment The deployment number for prod, staging, qa, or dev
     * @return The associated URL
     */
    public static String getDapiConfigUrlPOST(int deployment) {
        switch (deployment) {
            case DAPI_DEPLOYMENT_PROD:
                return CONFIG_POST_PROD_URL;
            case DAPI_DEPLOYMENT_STAGING:
                return CONFIG_POST_STAGING_URL;
            case DAPI_DEPLOYMENT_QA:
                return CONFIG_POST_QA_URL;
            case DAPI_DEPLOYMENT_DEV:
                return CONFIG_POST_DEV_URL;
            default:
                return "";
        }
    }


    private static final int BRAND_ID_MATRIX = 0;


    /**
     * The DAPI brand id for the console
     *
     * @return The DAPI brand ID
     */
    public static int getBrandId() {
        return BuildConfig.BRAND_ID;
    }

    /**
     * The console ID for the console
     *
     * @return The DAPI console ID
     */


}
