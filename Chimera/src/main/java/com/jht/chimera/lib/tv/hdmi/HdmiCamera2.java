package com.jht.chimera.lib.tv.hdmi;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.events.JHTNotification;
import com.jht.chimera.drivers.audio.AudioSourceInput;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.service.hal.lib.hardware.Driver;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.NonNls;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class HdmiCamera2 extends HdmiCameraBase {
    @NonNls private final String TAG = "HdmiCamera2";
    private CameraDevice mCamera = null;
    private ImageReader imageReader = null;

    private final Context context;
    private String cameraId = null;
    private CameraManager cameraManager;
    private Surface surface = null;
    private boolean sessionStarted = false;
    private boolean reopen = false;
    private final View overlay;
    private final int overlayDelay;
    private CameraCaptureSession session = null;
    private SurfaceTexture surfaceTexture = null;
    private int frameCount = 0;
    private long lastFrameTime = 0L;
    private int previewSurfaceWidth = -1;
    private int previewSurfaceHeight = -1;

    private boolean isInterlaced = false;
    private boolean deinterlacerInitialized;
    private boolean firstFrameProcessed = false;
    private int lastWidth = 0;
    private int lastHeight = 0;
    private Handler signalChangeHandler;
    private Runnable signalChangeRunnable;
    private static final int DEINTERLACE_MOTION_ADAPTIVE = 2;
    private static final long SIGNAL_CHECK_INTERVAL_MS = 2000L;
    private boolean surfaceReady = false;
    private boolean hdmiInfoReady = false;
    private HdmiInputInfo resolution = null;
    private final Object mSessionLock = new Object();
    private volatile boolean mCameraClosing = true;
    private volatile boolean mInErrorState = false;
    private volatile boolean mFrameProcessingPaused = false;

    public void setResolution(HdmiInputInfo currentResolution) {
        this.resolution = currentResolution;
    }
    private void pauseFrameProcessing() {
        overlay.post(() -> overlay.setVisibility(View.VISIBLE));
        mFrameProcessingPaused = true;
    }

    private void resumeFrameProcessing() {
        overlay.postDelayed(() -> {
            overlay.setVisibility(View.GONE);
            if (session != null){
                if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker_3))
                    DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, AudioSourceInput.HDMI.ordinal());
                }
        }, 3000);
        mFrameProcessingPaused = false;
    }
    public HdmiCamera2(Context context, SurfaceView surfaceView, View overlay, int overlayDelay, @Nullable ImageView splash, @Nullable List<HDMIResolution> supportedResolutions, IOnOpen onOpen) {
        super(splash, supportedResolutions, onOpen);
        this.context = context;
        this.overlay = overlay;
        this.overlayDelay = overlayDelay;

        resolution = ResolutionManager.readResolution().getHdmiInputInfo();
        Log.d(TAG, "HdmiCamera2 using SurfaceView " + resolution);

        Driver.initDeinterlacer(DEINTERLACE_MOTION_ADAPTIVE);
        deinterlacerInitialized = true;
        startSignalChangeDetection();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (!released()) {
                    getHandler().post(() -> {
                        surface = holder.getSurface();
                        surfaceReady = true;
                        tryInitDeinterlacerIfNeeded();
                    });
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                if (!released()) {
                    getHandler().post(() -> {
                        surface = holder.getSurface();
                        previewSurfaceWidth = width;
                        previewSurfaceHeight = height;
                        surfaceReady = true;
                        tryInitDeinterlacerIfNeeded();
                        if (mCamera != null && !sessionStarted) {
                            triggerPreviewAttempt();
                        }
                    });
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                surface = null;
                surfaceReady = false;
                resetDeinterlacerState();
            }
        });
        if (overlay != null) overlay.setVisibility(View.VISIBLE);

    }

    private void initSurface(SurfaceTexture texture) {
        this.surface = new Surface(texture);
        this.surfaceTexture = texture;
        surfaceReady = true;
        resolution = ResolutionManager.readResolution().getHdmiInputInfo();
        Log.d(TAG, "initSurface " + resolution);
        if (resolution != null) {
            texture.setDefaultBufferSize(resolution.getWidth(), resolution.getHeight());
            hdmiInfoReady = true;
            tryInitDeinterlacerIfNeeded();
        }
        if (mCamera != null && !sessionStarted) {
            triggerPreviewAttempt();
        }
    }

    public HdmiCamera2(Context context, TextureView textureView, View overlay, int overlayDelay, @Nullable ImageView splash, @Nullable List<HDMIResolution> supportedResolutions, IOnOpen onOpen) {
        super(splash, supportedResolutions, onOpen);
        this.context = context;
        this.overlay = overlay;
        this.overlayDelay = overlayDelay;

        Driver.initDeinterlacer(DEINTERLACE_MOTION_ADAPTIVE);
        deinterlacerInitialized = true;
        startSignalChangeDetection();

        SurfaceTexture texture = textureView.getSurfaceTexture();
        if (texture != null) {
            previewSurfaceWidth = textureView.getWidth();
            previewSurfaceHeight = textureView.getHeight();
            initSurface(texture);
        } else {
            textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
                    Log.d(TAG, "onSurfaceTextureAvailable");
                    previewSurfaceWidth = width;
                    previewSurfaceHeight = height;
                    initSurface(surfaceTexture);

                    Log.d(TAG, "onSurfaceTextureAvailable:　preview size:　" + previewSurfaceWidth +"x"+ previewSurfaceHeight);
                }

                @Override
                public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
                    previewSurfaceWidth = width;
                    previewSurfaceHeight = height;
                    Log.d(TAG, "preview size:　" + previewSurfaceWidth +"x"+ previewSurfaceHeight);
                }

                @Override
                public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                    surface = null;
                    surfaceReady = false;
                    resetDeinterlacerState();
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
                }
            });
        }
        if (overlay != null) overlay.setVisibility(View.VISIBLE);
    }

    private void startSignalChangeDetection() {
        synchronized(mSessionLock) {
            if (signalChangeHandler == null) {
                signalChangeHandler = getHandler();
                signalChangeRunnable = this::checkSignalChange;
                signalChangeHandler.postDelayed(signalChangeRunnable, SIGNAL_CHECK_INTERVAL_MS);
            }
        }
    }

    private void checkSignalChange() {
        synchronized(mSessionLock) {
            if (signalChangeHandler == null || mCameraClosing) {
                return;
            }

            try {
                boolean currentInterlaced = Driver.isInterlaced() == 1;

                if (currentInterlaced != isInterlaced) {
                    Log.i(TAG, "Signal change detected: interlaced=" + currentInterlaced);
                    signalChangeHandler.post(() -> {
                        isInterlaced = currentInterlaced;
                        if (currentInterlaced) {
                            Driver.initDeinterlacer(DEINTERLACE_MOTION_ADAPTIVE);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking signal change: " + e.getMessage());
            }
        }
    }

    private void stopSignalChangeDetection() {
        if (signalChangeRunnable != null) {
            signalChangeHandler.removeCallbacks(signalChangeRunnable);
        }
        signalChangeHandler = null;
        signalChangeRunnable = null;
    }

    private void processFrame(Image image) {
        if (mFrameProcessingPaused) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        frameCount++;

        // Check signal status every 2 seconds
        if (currentTime - lastFrameTime >= 1000) {
            boolean newIsInterlaced = Driver.isInterlaced() == 1;
            if (newIsInterlaced != isInterlaced) {
                Log.i(TAG, "Interlaced state changed: " + isInterlaced + " -> " + newIsInterlaced);
                isInterlaced = newIsInterlaced;
                hdmiInfoReady = true;
                deinterlacerInitialized = false;
                tryInitDeinterlacerIfNeeded();
            }
            lastFrameTime = currentTime;
            frameCount = 0;
        }

        // Check if the resolution has changed.
        int width = image.getWidth();
        int height = image.getHeight();
        if (width != lastWidth || height != lastHeight) {
            Log.i(TAG, "Resolution changed: " + lastWidth + "x" + lastHeight + " -> " + width + "x" + height);
            lastWidth = width;
            lastHeight = height;
            deinterlacerInitialized = false;
            tryInitDeinterlacerIfNeeded();
            return;
        }

        // If the deinterlacer has not been initialized, or if it is the first frame, return directly.
        if (!isInterlaced) {
            Log.v(TAG, "Signal is not interlaced, skipping deinterlacing");
            return;
        }
        if (!deinterlacerInitialized) {
            Log.d(TAG, "Deinterlacer not initialized, trying to initialize...");
            tryInitDeinterlacerIfNeeded();
            return;
        }

        if (!firstFrameProcessed) {
            firstFrameProcessed = true;
            Log.i(TAG, "Skipping first frame for deinterlacing safety");
            return;
        }

        try {
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            int yStride = planes[0].getRowStride();
            int uStride = planes[1].getRowStride();
            int vStride = planes[2].getRowStride();
            // Only needed for testing sometimes but takes to much processing otherwise.
            // Log.d(TAG, "Processing frame: " + width + "x" + height + ", strides: Y=" + yStride + ", U=" + uStride + ", V=" + vStride);

            if (ySize < width * height || uSize < (width / 2) * (height / 2) || vSize < (width / 2) * (height / 2)) {
                Log.e(TAG, "Buffer size mismatch: Y=" + ySize + ", U=" + uSize + ", V=" + vSize + ", W=" + width + ", H=" + height);
                return;
            }

            byte[] yData = new byte[ySize];
            byte[] uData = new byte[uSize];
            byte[] vData = new byte[vSize];
            yBuffer.get(yData);
            uBuffer.get(uData);
            vBuffer.get(vData);

            int result = Driver.processFrame(
                    yData, uData, vData,
                    yData, uData, vData,
                    yStride, uStride, vStride,
                    width, height
            );

            if (result != 0) {
                Log.e(TAG, "Frame processing failed with error code: " + result);
                deinterlacerInitialized = false;
                tryInitDeinterlacerIfNeeded();
                return;
            }

            try {
                if (session != null && mCamera != null && surface != null) {
                    CaptureRequest.Builder previewRequest = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    previewRequest.addTarget(surface);
                    previewRequest.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    session.setRepeatingRequest(previewRequest.build(), null, null);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to update preview: " + e.getMessage());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing frame: " + e.getMessage());
            deinterlacerInitialized = false;
            tryInitDeinterlacerIfNeeded();
        }
    }

    private void tryInitDeinterlacerIfNeeded() {
        if (!deinterlacerInitialized && isInterlaced && surfaceReady && hdmiInfoReady) {
            try {
                Driver.initDeinterlacer(DEINTERLACE_MOTION_ADAPTIVE);
                deinterlacerInitialized = true;
                firstFrameProcessed = false;
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize deinterlacer: " + e.getMessage());
                deinterlacerInitialized = false;
            }
        }
    }

    private void resetDeinterlacerState() {
        deinterlacerInitialized = false;
        surfaceReady = false;
        hdmiInfoReady = false;
        firstFrameProcessed = false;
        lastWidth = 0;
        lastHeight = 0;
    }
    private static final int FALLBACK_PREVIEW_WIDTH = 640;
    private static final int FALLBACK_PREVIEW_HEIGHT = 480;

    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum(
                    (long) lhs.getWidth() * lhs.getHeight() -
                            (long) rhs.getWidth() * rhs.getHeight()
            );
        }
    }

    @Nullable
    public Size chooseOptimalPreviewSize(
            String cameraId,
            int targetWidth,
            int targetHeight,
            @Nullable Integer actualHdmiWidth,
            @Nullable Integer actualHdmiHeight
    ) {
        CameraCharacteristics characteristics;
        try {
            characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Log.d(TAG, "Supported YUV_420_888 resolutions: " +
                    Arrays.toString(map.getOutputSizes(ImageFormat.YUV_420_888)));
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to get characteristics for camera " + cameraId + ": " + e.getMessage(), e);
            return null;
        }

        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) {
            Log.e(TAG, "Cannot get SCALER_STREAM_CONFIGURATION_MAP for camera " + cameraId);
            return null;
        }

        Size[] supportedSizes = map.getOutputSizes(SurfaceTexture.class);
        if (supportedSizes == null || supportedSizes.length == 0) {
            Log.e(TAG, "No supported SurfaceTexture sizes for camera " + cameraId);
            return null;
        }

        // Log.i(TAG, "Choosing optimal preview size for camera " + cameraId + ".");
        // Log.i(TAG, "TextureView (target) size: " + targetWidth + "x" + targetHeight);
        // Log.i(TAG, "Actual HDMI signal size (if known): " +  (actualHdmiWidth != null && actualHdmiHeight != null ? actualHdmiWidth + "x" + actualHdmiHeight : "N/A"));
        StringBuilder supportedSizeStr = new StringBuilder();
        for (Size size : supportedSizes) {
            supportedSizeStr.append(size.getWidth()).append("x").append(size.getHeight()).append(" ");
        }
        // Log.i(TAG, "Supported SurfaceTexture sizes: " + supportedSizeStr.toString().trim());



        Size chosenSize = null;

        // Priority 1: Try to match the actual HDMI signal resolution if it's supported
        if (actualHdmiWidth != null && actualHdmiHeight != null) {
            // Log.d(TAG, "priority 1");
            for (Size size : supportedSizes) {
                if (size.getWidth() == actualHdmiWidth && size.getHeight() == actualHdmiHeight) {
                    chosenSize = size;
                    break;
                }
            }
        }

        // Priority 2: Try to find an exact match for the TextureView (target) dimensions
        if (chosenSize == null) {
            // Log.d(TAG, "priority 2");
            for (Size size : supportedSizes) {
                if (size.getWidth() == targetWidth && size.getHeight() == targetHeight) {
                    chosenSize = size;
                    break;
                }
            }
        }

        // Priority 3: If no exact match for HDMI or TextureView, try common HD resolutions
        // (Prefer 1080p if HDMI was 1080p or unknown, 720p if HDMI was 720p)
        if (chosenSize == null) {
            // Log.d(TAG, "priority 3");
            for (Size size : supportedSizes) {
                if (size.getWidth() == 1920 && size.getHeight() == 1080) {
                    chosenSize = size;
                    break;
                }
            }
            for (Size size : supportedSizes) {
                if (size.getWidth() == 1280 && size.getHeight() == 720) {
                    chosenSize = size;
                    break;
                }
            }

        }

        // Priority 4: Fallback to FALLBACK_PREVIEW_WIDTH/HEIGHT if supported
        if (chosenSize == null) {
            // Log.d(TAG, "priority 4");
            for (Size size : supportedSizes) {
                if (size.getWidth() == FALLBACK_PREVIEW_WIDTH && size.getHeight() == FALLBACK_PREVIEW_HEIGHT) {
                    chosenSize = size;
                    break;
                }
            }
        }
        // Priority 5: If still no suitable size, pick the largest available one.
        if (chosenSize == null) {
            // Log.d(TAG, "priority 5");
            Arrays.sort(supportedSizes, new CompareSizesByArea());
            chosenSize = supportedSizes[supportedSizes.length - 1];
        }

        // Log.i(TAG, "Final chosen preview size: " + chosenSize.getWidth() + "x" + chosenSize.getHeight());
        return chosenSize;
    }

    private Size determinePreviewSize(int surfaceWidth, int surfaceHeight, HdmiInputInfo hdmiInfo) {
        if ((mCamera != null) && cameraId != null && hdmiInfo != null && hdmiInfo.isConnected()) {
            return chooseOptimalPreviewSize(
                    cameraId,
                    surfaceWidth,
                    surfaceHeight,
                    hdmiInfo.getWidth(),
                    hdmiInfo.getHeight()
            );
        } else {
            StringBuilder reason = new StringBuilder();
            if (mCamera == null) reason.append("Camera not open. ");
            if (cameraId == null) reason.append("Camera ID unknown. ");
            if (hdmiInfo == null) reason.append("HDMI info unknown. ");
            else if (!hdmiInfo.isConnected()) reason.append("HDMI not connected. ");

            Log.w(TAG, "Cannot determine preview size. Reason: " + reason);
            return null;
        }
    }

    public void triggerPreviewAttempt() {
        synchronized(mSessionLock) {
            if (mInErrorState) {
                Log.w(TAG, "Cannot trigger preview - camera in error state");
                return;
            }
            if (surface != null && surface.isValid() && surfaceTexture != null && previewSurfaceWidth != -1 && previewSurfaceHeight != -1 && mCamera != null && resolution != null) {
                Size selectedPreviewSize = determinePreviewSize(previewSurfaceWidth, previewSurfaceHeight, resolution);
                if (selectedPreviewSize != null) {
                    // Log.i(TAG, "Optimal preview size determined: " + selectedPreviewSize.getWidth() + "x" + selectedPreviewSize.getHeight());
                    try {
                        Log.d(TAG, "Setting SurfaceTexture DefaultBufferSize to: " +
                                selectedPreviewSize.getWidth() + "x" + selectedPreviewSize.getHeight());
                        previewSurfaceWidth = selectedPreviewSize.getWidth();
                        previewSurfaceHeight = selectedPreviewSize.getHeight();
                        if (surfaceTexture != null) {
                            surfaceTexture.setDefaultBufferSize(
                                    selectedPreviewSize.getWidth(),
                                    selectedPreviewSize.getHeight()
                            );
                            this.surface = new Surface(surfaceTexture);
                        }
                        Log.d(TAG, "setDefaultBufferSize successful.");
                        startSessionAndPreview(surface);
                    } catch (Exception e) {
                        Log.e(TAG, "Exception during setDefaultBufferSize or startPreview: " + e.getMessage(), e);
                    }
                } else {
                    Log.e(TAG, "Failed to determine optimal preview size. Cannot start preview.");
                }
            } else {
                Log.w(TAG, "Conditions not met for starting preview in triggerPreviewAttempt.");
            }
        }
    }
    private void startSessionAndPreview(Surface surface) {
        synchronized (mSessionLock) {
            if (isCameraInvalid()) {
                return;
            } else if (sessionStarted){
                return;
            } else if (released()) {
                return;
            }
            sessionStarted = true;

            try {
                pauseFrameProcessing();
                if (imageReader != null) {
                    imageReader.close();
                    imageReader = null;
                }
                if (session != null) {
                    try {
                        session.abortCaptures();
                    } catch (CameraAccessException e) {
                        Log.w(TAG, "abortCaptures failed", e);
                    }
                    session.close();
                    session = null;
                }

                // create new ImageReader
                imageReader = ImageReader.newInstance(
                        previewSurfaceWidth,
                        previewSurfaceHeight,
                        ImageFormat.YUV_420_888,
                        2
                );


                List<Surface> surfaces = Arrays.asList(surface, imageReader.getSurface());
                mCamera.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession s) {
                        synchronized (mSessionLock) {
                            if (isCameraInvalid()) {
                                Log.w(TAG, "onConfigured: camera invalid, closing session");
                                s.close();
                                return;
                            }

                            if (session != null && session != s) {
                                Log.w(TAG, "onConfigured: obsolete session, closing");
                                session.close();
                                return;
                            }

                            session = s;
                            try {
                                CaptureRequest.Builder requestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                // 确保所有Surface有效
                                if (!surface.isValid()) {
                                    Log.e(TAG, "Preview surface is invalid!");
                                    return;
                                }
                                if (!imageReader.getSurface().isValid()) {
                                    Log.e(TAG, "ImageReader surface is invalid!");
                                    return;
                                }
                                requestBuilder.addTarget(surface);
                                requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                requestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                                imageReader.setOnImageAvailableListener(reader -> {
                                    try (Image image = reader.acquireLatestImage()) {
                                        if (image != null) {
                                            processFrame(image);
                                        }
                                    }
                                }, getHandler());
                                session.setRepeatingRequest(requestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                                    @Override
                                    public void onCaptureFailed(@NonNull CameraCaptureSession session,
                                                                @NonNull CaptureRequest request,
                                                                @NonNull CaptureFailure failure) {
                                        Log.e(TAG, "Capture failed: " + failure);
                                    }

                                    @Override
                                    public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                                                   @NonNull CaptureRequest request,
                                                                   @NonNull TotalCaptureResult result) {
                                    }
                                    int bufferLostCount = 0;
                                    @Override
                                    public void onCaptureBufferLost(@NonNull CameraCaptureSession session,
                                                                    @NonNull CaptureRequest request,
                                                                    @NonNull Surface target,
                                                                    long frameNumber) {
                                        Log.w(TAG, "onCaptureBufferLost: frame=" + frameNumber);
                                        bufferLostCount++;
                                        if (bufferLostCount <= 4) {
                                            Log.w(TAG, "Tolerating buffer lost #" + bufferLostCount);
                                            return;
                                        }
                                        getHandler().postDelayed(() -> {
                                            if (!released() && mCamera != null && sessionStarted) {
                                                Log.w(TAG, "Restarting session due to buffer lost");
                                                reopen();
                                            }
                                        }, 100);
                                    }
                                }, getHandler());
                                resumeFrameProcessing();
                            } catch (CameraAccessException e) {
                                Log.e(TAG, "Failed to start preview: " + e);
                                closeCamera();
                            }
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        Log.e(TAG, "Camera session configure failed");
                        if (!released()) {
                            reopen();
                        }
                    }
                }, getHandler());
            } catch (CameraAccessException | IllegalStateException e) {
                Log.e(TAG, "startSessionAndPreview failed: " + e);

                getHandler().postDelayed(this::reopen, 1000);
            }
        }
    }


    private boolean isCameraInvalid() {
        synchronized(mSessionLock) {
            return mCamera == null || mCameraClosing || mInErrorState;
        }
    }

    @Override
    public void reopen() {
        Handler h = getHandler();
        if (h != null) {
            h.removeCallbacksAndMessages(null);
            reopen = true;
            h.post(this::closeCamera);
        }
    }

    @Override
    public void openCamera() {
        super.openCamera();
        Log.d(TAG, "1 openCamera");
        if (mCamera != null || released()) {
            Log.d(TAG, "camera is null or released");
            return;
        }
        if (!mCameraClosing) {
            Log.d(TAG, "Camera is currently in opening process, skipping.");
            return;
        }
        mCameraClosing = false;
        getHandler().post(() -> {
            try {
                if (cameraManager == null) {
                    cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
                }

                if (cameraManager.getCameraIdList().length == 0) {
                    Log.e(TAG, "No cameras available");
                    mCameraClosing = true;
                    getHandler().postDelayed(this::openCamera, 500);
                    return;
                }

                Log.d(TAG, "2 openCamera");
                if (!isResolutionSupported()) {
                    Log.d(TAG, "2.1 openCamera");
                    mCameraClosing = true;
                    getHandler().postDelayed(this::openCamera, 500);
                    return;
                }

                Log.d(TAG, "3 openCamera");
                onOpen();

                String[] ids = cameraManager.getCameraIdList();
                if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker)
                        || ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker_3)
                        || ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.UltraThin)
                ) {
                    this.cameraId = ids[0];
                } else if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera_2)) {
                    this.cameraId = "0";
                } else {
                    this.cameraId = ids[0];
                }
                Log.d(TAG, "5 openCamera");
                if (this.cameraId != null) {
                    Log.d(TAG, "6 openCamera");
                    loadCamera();
                } else {
                    mCameraClosing = true;
                }
            } catch (CameraAccessException e) {
                Log.e(TAG, "Camera access exception", e);
                mCameraClosing = true;
                getHandler().postDelayed(this::openCamera, 1000);
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error in openCamera", e);
                mCameraClosing = true;
            }
        });
    }

    private void loadCamera() {
        if (context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    getHandler().post(() -> {
                        synchronized(mSessionLock) {
                            mCamera = camera;
                            if (surface != null && session == null){
                                triggerPreviewAttempt();
                            }
                        }
                    });
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    getHandler().post(() -> {
                        synchronized(mSessionLock) {
                            if (mCamera == camera) {
                                closeCamera();
                            }
                        }
                    });
                }

                @Override
                public void onClosed(@NonNull CameraDevice camera) {
                    overlay.post(() -> overlay.setVisibility(View.VISIBLE));
                    notification.setNotification();
                }


                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    // Log.d(TAG, "camera: " + cameraId + ", is camera invalid: " + isCameraInvalid());
                    Log.e(TAG, "Camera error: " + error + (error == 4 ? " (FATAL)" : ""));
                    getHandler().post(() -> {
                        synchronized (mSessionLock) {
                            if (mCamera == camera) {
                                mInErrorState = true;

                                // Free up resources immediately
                                if (session != null) {
                                    session = null;
                                }
                                mCamera.close();
                                mCamera = null;
                                sessionStarted = false;

                                int retryDelay;
                                if (error == CameraDevice.StateCallback.ERROR_CAMERA_DEVICE) {
                                    retryDelay = 300;
                                    Log.w(TAG, "Fatal error detected, waiting longer before retry");
                                } else {
                                    retryDelay = 100;
                                }
                                reopen = true;

                                overlay.post(() -> overlay.setVisibility(View.VISIBLE));

                                if (reopen && !released()) {
                                    getHandler().postDelayed(() -> {
                                        synchronized (mSessionLock) {
                                            if (!released()) {
                                                // Log.i(TAG, "Attempting recovery from error " + error);
                                                mInErrorState = false;
                                                reopen();
                                            }
                                        }
                                    }, retryDelay);
                                }
                                reopen = false;
                            }
                        }
                    });
                }
            }, getHandler());
        } catch (CameraAccessException e) {
            Log.e(TAG, "CameraAccessException in loadCamera: " + e.getMessage(), e);
            getHandler().postDelayed(() -> {
                if (!released()) {
                    reopen = true;
                    closeCamera();
                }
            }, 300);
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException - camera permission may have been revoked", e);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "IllegalArgumentException - camera ID " + cameraId + " may be invalid", e);
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException - camera service may be unavailable", e);
            getHandler().postDelayed(() -> {
                if (!released()) {
                    reopen = true;
                    closeCamera();
                }
            }, 500);
        }
    }

    private final JHTNotification notification = new JHTNotification();

    @Override
    public void closeCamera() {
        // Log.d(TAG, "close camera");
        synchronized (mSessionLock) {
            if (mCameraClosing) return;
            mCameraClosing = true;

            getHandler().removeCallbacksAndMessages(null);
            stopSignalChangeDetection();

            // close session
            if (session != null) {
                try {
                    session.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing session: " + e);
                }
                session = null;
                if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker_3)) {
                    DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, AudioSourceInput.LineIn.ordinal());
                }
            }
            // close camera
            if (mCamera != null) {
                try {
                    mCamera.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing camera: " + e);
                }
                mCamera = null;
            }

            if (imageReader != null) {
                imageReader.close();
                imageReader = null;
            }
            sessionStarted = false;
            mInErrorState = false;

            // close overlay
            if (overlay != null) overlay.post(() -> overlay.setVisibility(View.VISIBLE));

            // delayed retry
            if (reopen && !released()) {
                getHandler().postDelayed(() -> {
                    synchronized (mSessionLock) {
                        if (!released()) {
                            openCamera();
                            reopen = false;
                        }
                    }
                }, 100);
            } else {
                super.closeCamera();
            }
        }
    }
    @Override
    public void release() {
        overlay.post(() -> overlay.setVisibility(View.VISIBLE));
        super.release();
    }
}