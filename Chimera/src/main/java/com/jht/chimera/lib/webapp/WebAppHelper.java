package com.jht.chimera.lib.webapp;

import android.util.Base64;
import android.util.Log;
import android.webkit.WebView;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.dapi.DapiUtil;
import com.jht.chimera.lib.common.dapi.JumpSession;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.widgets.EmbeddedAuthScriptKt;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class WebAppHelper {
    @NonNls private static final String TAG = "WebAppHelper";
    private static AtomicInteger sUid = new AtomicInteger();

    public static String getShim(String url) {

        if (url == null || url.isEmpty()) return "";

        String shimText = "";
        try {
            // Create a URL for the desired page
            URL shimUrl = new URL(url);

            // Read all the text returned by the server
            BufferedReader in = new BufferedReader(new InputStreamReader(shimUrl.openStream()));
            String str;
            String newLine = System.getProperty("line.separator");
            StringBuilder sb = new StringBuilder(1000);
            while ((str = in.readLine()) != null) {
                sb.append(str);
                sb.append(newLine);
            }
            in.close();
            shimText = "jht_shim = function () {" + sb.toString() + "}"; //NON-NLS
            //shimText = "jht_shim = function () {alert('foo')}";
			/*
			PrintWriter out = new PrintWriter("/data/jht/shim.txt");
			out.println(shimText);
			out.close();
			*/
        } catch (Exception e) {
            Log.w(TAG, e);
        }

        // Fix for MFP.  Bad hack, but works while waiting for web.
        if(!shimText.contains(".fb-login-button")) {
            return shimText.replace("\".member-login > ul > li:nth-child(2)\",", "\".member-login > ul > li:nth-child(2)\",\n\".fb-login-button\",");
        }

        return shimText;
    }

    public static void incrementUid() {
        sUid.incrementAndGet();
    }

    public static void injectScriptFile(final WebView view, final String scriptFile) {
        final int uid = sUid.incrementAndGet();

        if (uid != sUid.get()) return;
        if (scriptFile == null || scriptFile.isEmpty()) return;

        byte[] buffer = scriptFile.getBytes();

        // String-ify the script byte-array using BASE64 encoding !!!
        String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
        view.loadUrl("javascript:(function() {" + "var parent = document.getElementsByTagName('head').item(0);"
                + "var script = document.createElement('script');" + "script.type = 'text/javascript';" +
                "script.innerHTML = window.atob('" + encoded + "');" + "parent.appendChild(script)" + "})()");
        view.loadUrl("javascript:jht_shim()");
        /*
        // NOTE: for come reason calling view.loadUrl causes pages not to finish load so adding a delay to fix this issue
        sHandler.postDelayed(() -> {
            if (uid != sUid.get()) return;

            byte[] buffer = scriptFile.getBytes();

            // String-ify the script byte-array using BASE64 encoding !!!
            String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
            view.loadUrl("javascript:(function() {" + "var parent = document.getElementsByTagName('head').item(0);"
                    + "var script = document.createElement('script');" + "script.type = 'text/javascript';" +
                    "script.innerHTML = window.atob('" + encoded + "');" + "parent.appendChild(script)" + "})()");
            view.loadUrl("javascript:jht_shim()");
        }, 500);
        */
    }

    public static void injectTokens(final WebView view) {
        JumpSession session = JumpSession.current();
        view.evaluateJavascript(EmbeddedAuthScriptKt.buildEmbeddedAuthScript(session.getAccessToken(), session.getIdToken()), null);
    }

    @SuppressWarnings("SameParameterValue")
    private static String TOTPSHA256(String key, long counter, int digits) {
        // compute SHA-256 HMAC of the key
        Mac sha256_HMAC;
        try {
            sha256_HMAC = Mac.getInstance("HmacSHA256");
            @SuppressWarnings("CharsetObjectCanBeUsed") SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("US-ASCII"), "HmacSHA256");
            sha256_HMAC.init(secret_key);

            // convert the counter to bytes, check if the system is big endian and reverse if necessary
            byte[] counter_bytes = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(counter).array();
            if (ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN)) {
                int i = 0;
                int j = counter_bytes.length - 1;
                byte tmp;
                while (j > i) {
                    tmp = counter_bytes[j];
                    counter_bytes[j] = counter_bytes[i];
                    counter_bytes[i] = tmp;
                    j--;
                    i++;
                }
            }

            // compute the hash using the counter value
            byte[] hmac_result = sha256_HMAC.doFinal(counter_bytes);

            // get the last 4 bits of the HMAC Result to determine the offset
            int offset = hmac_result[hmac_result.length - 1] & 0xf;

            // get the value of 4 bytes of the HMAC Result starting at the offset position
            int bin_code = (hmac_result[offset] & 0x7f) << 24
                    | (hmac_result[offset + 1] & 0xff) << 16
                    | (hmac_result[offset + 2] & 0xff) << 8
                    | (hmac_result[offset + 3] & 0xff);

            // HOTP = bin_code modulo 10^(digits)
            int hotp = bin_code % (int) Math.pow(10, digits);

            // truncate the string to the number of significant digits
            StringBuilder result = new StringBuilder(Integer.toString(hotp));
            while (result.length() < digits) {
                result.insert(0, "0");
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.w(TAG, e);
        } catch (InvalidKeyException e) {
            Log.w(TAG, e);
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, e);
        }

        return "";
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    public static String TOTPSHA512(String key, long counter, int digits)
    {
        // compute SHA-512 HMAC of the key
        Mac sha512_HMAC;
        try
        {
            sha512_HMAC = Mac.getInstance("HmacSHA512");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("US-ASCII"), "HmacSHA512");
            sha512_HMAC.init(secret_key);

            // convert the counter to bytes, check if the system is big endian and reverse if necessary
            byte[] counter_bytes = ByteBuffer.allocate(Long.SIZE/Byte.SIZE).putLong(counter).array();
            if (ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN))
            {
                int i = 0;
                int j = counter_bytes.length - 1;
                byte tmp;
                while (j > i)
                {
                    tmp = counter_bytes[j];
                    counter_bytes[j] = counter_bytes[i];
                    counter_bytes[i] = tmp;
                    j--;
                    i++;
                }
            }

            // compute the hash using the counter value
            byte[] hmac_result = sha512_HMAC.doFinal(counter_bytes);

            // get the last 4 bits of the HMAC Result to determine the offset
            int offset = hmac_result[hmac_result.length - 1] & 0xf;

            // get the value of 4 bytes of the HMAC Result starting at the offset position
            int bin_code = (hmac_result[offset] & 0x7f) << 24
                    | (hmac_result[offset + 1] & 0xff) << 16
                    | (hmac_result[offset + 2] & 0xff) << 8
                    | (hmac_result[offset + 3] & 0xff);

            // HOTP = bin_code modulo 10^(digits)
            int hotp = bin_code % (int)Math.pow(10, digits);

            // truncate the string to the number of significant digits
            StringBuilder result = new StringBuilder(Integer.toString(hotp));
            while (result.length() < digits) {
                result.insert(0, "0");
            }
            return result.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            Log.w(TAG, e);
        }
        catch (InvalidKeyException e)
        {
            Log.w(TAG, e);
        }
        catch (UnsupportedEncodingException e)
        {
            Log.w(TAG, e);
        }

        return "";
    }

    public static String GenerateHotpToken(@NonNls String Uuid, @NonNls String xid, @NonNls String Salt) {
        String Secret = Uuid + Salt + xid;
        int SecondsSinceEpoch = (int) Math.floor((float) System.currentTimeMillis() / 1000.0f / 7200.0f);

        return TOTPSHA256(Secret, SecondsSinceEpoch, 8);
    }

    public static void addStandardAuthorization(HttpURLConnection connection, String xid, String saltToken) {
        connection.addRequestProperty(
            "x-jfit-authorization",
            GenerateHotpToken(DapiUtil.getMachineId(), xid, saltToken)
        );
    }

    public static URL getURL(String endpoint) {

        @NonNls String url = "";
        @NonNls String prefix = "";
        switch(JHTCommonTypes.DAPIEnvironment.values()[DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT)]) {
            case dev:
                prefix = "dev-";
                break;
            case qa:
                prefix = "qa-";
                break;
            case staging:
                prefix = "staging-";
                break;
            case production:
            case NUM_DAPI_ENVIRONMENTS:
                break;
        }
        url += "https://" + prefix + endpoint;
        return addURLParameters(url);
    }

    @Nullable
    public static URL addURLParameters(@NonNls String url) {
        try {
            url += "/?";
            url += "machine_id=" + URLEncoder.encode(DapiUtil.getMachineId(), "UTF-8" );
            url += "&club_id=" + URLEncoder.encode( "" + DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT), "UTF-8");

            return new URL(url);
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, e);
            return null;
        } catch (MalformedURLException e) {
            Log.w(TAG, e);
            return null;
        }
    }

}
