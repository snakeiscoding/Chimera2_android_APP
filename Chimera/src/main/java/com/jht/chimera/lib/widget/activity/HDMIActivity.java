package com.jht.chimera.lib.widget.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.jht.androidcommonalgorithms.util.SystemDetection;
import com.jht.chimera.R;
import com.jht.chimera.drivers.audio.AudioSourceInput;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.permission.GrantPermission;
import com.jht.chimera.lib.tv.hdmi.HDMIResolution;
import com.jht.chimera.lib.tv.hdmi.HdmiCamera2;
import com.jht.chimera.lib.tv.hdmi.HdmiCameraBase;
import com.jht.chimera.lib.tv.hdmi.ResolutionMonitor;
import com.jht.chimera.lib.tv.hdmi.TvViewController;
import com.jht.chimera.service.hal.lib.hardware.HdmiIn;
import com.jht.datamatrixlib.DataMatrix;

/**
 * This is the base activity for activities that display HDMI content.  This takes care of
 * managing all of the permissions and opening up the camera.  It is expected that child classes
 * call setProperties and initHDMI in that order to start the process.  Failure to do so will result
 * in no HDMI content displayed.  This allows the sub activities to pass in the correct suraface views.
 * Also, some may show HDMI only in certain configurations so we don't always want to open the camera.
 */
public class HDMIActivity extends AudioControllerActivity implements ActivityCompat.OnRequestPermissionsResultCallback{
    private static final String TAG = "HDMIActivity";
    // Used to load the HDMI input onto the surface.
    private HdmiCamera2 hdmi;
    // Used for Parker.  We need to set the HDMI port and monitor the resolution on the port.
    private ResolutionMonitor hdmiMonitor = null;

    // The views to display the TV.
    private SurfaceView surfaceView = null;
    private TextureView textureView = null;
    private int overlayDelay = 0;
    private HdmiCameraBase.IOnOpen onOpen;
    private boolean hdmiInitialized = false;

    protected Size computeViewSize() {
        if (surfaceView != null) return new Size(surfaceView.getWidth(), surfaceView.getHeight());
        if (textureView != null) return new Size(textureView.getWidth(), textureView.getHeight());
        return new Size(0, 0);
    }

    /**
     * Callback from Android once we get permission to use the camera.
     *
     * @param requestCode   The request code we sent.
     * @param permissions   The permissions we received.
     * @param grantResults  The result of the permission request.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Make sure we got the Camera permission.  If so and we have a surface view then open the camera.
        // The surface view check ensures that setProperties was called first.
        for(int i = 0; i < permissions.length; i++) {
            if((surfaceView != null || textureView != null) && permissions[i].equals(Manifest.permission.CAMERA) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                initHDMI();
                return;
            }
        }
    }

    /**
     * Sets the properties so we can open up the camera.
     *
     * @param surfaceView       Surface view for use on Chimera based systems.
     * @param textureView       Texture view for use on Parker systems.
     * @param overlayDelay      Delay to show a black overlay to hide any weird opening artifacts.
     * @param onOpen            Callback to the child to let them know when the camera is opened.  It may need to reset communication.
     */
    protected void setProperties(SurfaceView surfaceView, TextureView textureView, int overlayDelay, HdmiCameraBase.IOnOpen onOpen) {
        this.surfaceView = surfaceView;
        this.textureView = textureView;
        this.overlayDelay = overlayDelay;
        this.onOpen = onOpen;


    }

    @Override
    public void onPause() {
        super.onPause();

        hdmiInitialized = false;
        // Stop monitoring on Parker.
        if(hdmiMonitor != null) {
            hdmiMonitor.stopMonitoring();
            hdmiMonitor = null;
        }

        // Close the HDMI and stop monitoring.
        if (hdmi != null) {
            hdmi.release();
        }
        hdmi = null;

        // Reset the audio. This should be fine, the audio flow for parker 3 is kinda weird.
        // See https://jhtnaee.atlassian.net/wiki/spaces/PAN/pages/4121362453/Parker+3.0+Audio
        // for more details.
        if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker)
                || ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker_3)) {
            DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, AudioSourceInput.Foreground.ordinal());
        }
    }

    protected void closeCamera() {
        if(hdmi != null) {
            hdmi.closeCamera();
        }
    }

    protected void openCamera() {
        Log.d(TAG, "Activity open camera");
        if(hdmi != null) {
            hdmi.openCamera();
        }
    }

    /**
     * Initialize the HDMI port and start displaying.  This function makes sure we have the correct permissions
     * before opening.
     */
    protected void initHDMI() {
        if(hdmiInitialized) return;

        if (ProductConfig.supportsHdmiSwitching()) {
            // Select the HDMI port we will use.
            Log.d(TAG, "initHDMI: call switchPort");
            HdmiIn.getInstance().switchPort(hdmiSource());
        }

        // This class needs refactoring.
        // Vortex doesn't use the camera but uses the TvView so use
        if (ProductConfig.hardware.isVortexBase()) {
            ViewGroup rootView = null;
            if (surfaceView != null) {
                rootView = (ViewGroup) surfaceView.getParent();
            } else if (textureView != null) {
                rootView = (ViewGroup) textureView.getParent();
            }

            // create the TvViewController that will attach/detach the TvView to the root view
            if (rootView != null) new TvViewController(this, rootView);

            // just call onOpen since there's no camera open
            if(onOpen != null) onOpen.onOpen();

            hdmiInitialized = true;
            return;
        }

        GrantPermission.grant(this, getPackageName(), Manifest.permission.CAMERA, getApplicationInfo().uid);
        GrantPermission.grant(this, getPackageName(), Manifest.permission.RECORD_AUDIO, getApplicationInfo().uid);
        GrantPermission.grant(this, getPackageName(), Manifest.permission.WRITE_EXTERNAL_STORAGE, getApplicationInfo().uid);
        GrantPermission.grant(this, getPackageName(), Manifest.permission.READ_EXTERNAL_STORAGE, getApplicationInfo().uid);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
            return;
        }

        // Open the camera.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && hdmi == null && (surfaceView != null || textureView != null) && !SystemDetection.isEmulator()) {

            // Open the HDMI port depending on hardware.
            boolean useResolutionMonitor = false;
            boolean useTextureView = true; // CGC-24245: just force everything to use texture until surface path is fixed
            Log.d(TAG, "config: " + ProductConfig.hardware);
            switch (ProductConfig.hardware) {
                case Parker:
                case Parker_3:
                    // TODO Verify Parker 3.0 hardware follows same logic as Parker 1
                    // Select the HDMI port we will use.
                    Log.d(TAG, "initHDMI: call switchPort "+ hdmiSource());
                    int result = HdmiIn.getInstance().switchPort(hdmiSource());
                    Log.i(TAG, "result: " + result);

                    useResolutionMonitor = true;
                    useTextureView = true;
                    break;
                case UltraThin:
                    useResolutionMonitor = true;
                    useTextureView = true;
                    break;
                case Chimera_2:
                    useResolutionMonitor = true;
                    break;
            }
            firstOpen = true;
            if (useTextureView) {
                Log.d(TAG, "initHDMI: call HdmiCamera2");
                if (surfaceView != null) surfaceView.setVisibility(View.GONE);
                textureView.setVisibility(View.VISIBLE);
                hdmi = new HdmiCamera2(getApplicationContext(), textureView, findViewById(R.id.tv_surface_overlay), overlayDelay, null, null, this::onOpen);
            }
            else {
                if(surfaceView != null) {
                    surfaceView.setVisibility(View.VISIBLE);
                }
                if(textureView != null) {
                    textureView.setVisibility(View.GONE);
                }
                hdmi = new HdmiCamera2(getApplicationContext(), surfaceView, findViewById(R.id.tv_surface_overlay), overlayDelay, null, null, this::onOpen);
            }
            hdmiInitialized = true;

            if (useResolutionMonitor) {
                // Start monitoring the HDMI port for resolution changes.
                if(hdmiMonitor == null) {
                    hdmiMonitor = new ResolutionMonitor();
                    hdmiMonitor.startMonitoring();
                    Log.d(TAG, "start monitoring...");
                    hdmiMonitor.setOnHdmiInfoChangedListener((newResolution, oldResolution) -> {
                        if (!hdmiInitialized || hdmi == null) {
                            Log.w(TAG, "Ignore resolution change, hdmi not initialized");
                            return;
                        }
                        if (oldResolution == null) {
                            Log.d(TAG, "Initial resolution detected, skip reopen");
                            if (hdmi != null) hdmi.setResolution(newResolution);
                            if (newResolution != null && !newResolution.toString().contains("0x0")) {
                                Log.d(TAG, "Valid initial resolution, forcing a fresh reopen to ensure preview starts.");
                                hdmi.reopen();
                            }
                            return;
                        }
                        Log.d(TAG, "onResolutionChange " + newResolution + " " + oldResolution);
                        onConnectionChange();
                        Log.d(TAG, "hdmi: " + hdmi);
                        hdmi.setResolution(newResolution);
                        Log.d(TAG, "set success");
                        hdmi.reopen();
                    });
                }
            }
        }
    }

    /**
     * When the camera is reset we may need to reset communication.  However we don't want to
     * do it the first time.
     */
    private boolean firstOpen = true;
    private void onOpen() {
        if(firstOpen) {
            firstOpen = false;
        }
        else if(onOpen != null) {
            onOpen.onOpen();
        }
    }

    /**
     * In general HDMI activities will want to set to HDMI, however IPTV function shares the activity
     * and in that case we want something different so child classes may override this.
     *
     * @return  The audio source input for the activity.
     */
    @Override
    protected AudioSourceInput audioSourceInput() {
        return AudioSourceInput.HDMI;
    }

    /**
     *
     * @return  The HDMI source input.  Only useful on Parker platforms.
     */
    protected int hdmiSource() {
        return ProductConfig.getHdmiInput(SettingKeys.TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING)));
    }

    /**
     * Can be overridden by child classes to be notified when we detect an HDMI connection change.
     */
    protected void onConnectionChange() {
    }

    /**
     *
     * @return True if we have detected an HDMI connection.
     */
    protected boolean connected() {
        if(hdmiMonitor == null) {
            return true;
        }
        HDMIResolution resolution = new HDMIResolution(hdmiMonitor.getCurrentHdmiInfo());
        return hdmiMonitor.getCurrentHdmiInfo() == null || resolution.getHdmiInputInfo().isConnected();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hdmiMonitor != null) {
            hdmiMonitor.stopMonitoring();
        }
        if (hdmi != null) {
            closeCamera();
        }
    }
}