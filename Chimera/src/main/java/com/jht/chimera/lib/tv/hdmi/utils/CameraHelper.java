package com.jht.chimera.lib.tv.hdmi.utils;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class CameraHelper {
    private static final String TAG = "CameraHelper";
    private Context context;

    public static final int ERROR_NO_CAMERA = -1;
    public static final int ERROR_NO_PERMISSION = -2;
    public static final int ERROR_CANNOT_ACCESS = -3;

    private int MAX_WIDTH = 1920;
    private int MAX_HEIGHT = 1080;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private CameraManager mCameraManager;
    private CameraDevice mCamera;
    private CaptureRequest.Builder mPreviewBuilder;
    private Size mPreviewSize;
    private String[] mCameraIdList;
    private String mCameraID;

    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private ImageReader mImageReader;

    private OnCameraStatusChangedListener listener;

    private long frameCounter = 0;
    private int mSkipFrame = 1;

    public CameraHelper(Context context, int maxWidth, int maxHeight) {
        this.context = context;
        MAX_WIDTH = maxWidth;
        MAX_HEIGHT = maxHeight;
    }

    public void setSkipFrame(int skipFrame) {
        mSkipFrame = skipFrame;
    }

    public void setOnCameraStatusChangedListener(OnCameraStatusChangedListener listener) {
        this.listener = listener;
    }

    public void initial() {
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraIdList = mCameraManager.getCameraIdList();  // {"0","1"}
            if (mCameraIdList == null || mCameraIdList.length <= 0) {
                if (listener != null) {
                    listener.onError(ERROR_NO_CAMERA);
                }
            }
        } catch (CameraAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            if (listener != null) {
                listener.onError(ERROR_NO_PERMISSION);
            }
        }
    }

    public void startBackgroundThread() {
        mHandlerThread = new HandlerThread("CameraDelay");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    public void stopBackgroundThread() {
        mHandlerThread.quitSafely();
        try {
            mHandlerThread.join();
            mHandlerThread = null;
            mHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void resetCamera() {
        // TODO Auto-generated method stub
        if (mCamera != null) {
            mCamera.close();
            mCamera = null;
        }
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void setCameraParameters() {
        // TODO Auto-generated method stub
        if (mCamera == null) {
            return;
        }

        try {
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCameraID);
            StreamConfigurationMap configurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            Size[] previewSizes = configurationMap.getOutputSizes(ImageReader.class);
            if (LogUtils.DEBUG) {
                Log.i(TAG, "Supported resolution:" + Arrays.toString(previewSizes));
            }
            mPreviewSize = null;
            for (Size previewSize : previewSizes) {
                if (mPreviewSize == null) {
                    if (previewSize.getWidth() <= MAX_WIDTH && previewSize.getHeight() <= MAX_HEIGHT) {
                        mPreviewSize = previewSize;
                        break;
                    }
                }
            }
            if (LogUtils.DEBUG) {
                Log.i(TAG, "Preview width*height = " + mPreviewSize.getWidth() + " * " + mPreviewSize.getHeight());
            }

            mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.YUV_420_888, 2);
            mImageReader.setOnImageAvailableListener(
                    new OnImageAvailableListenerImpl(), mHandler);

            // ***************  Preview Builder ******************
            mPreviewBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewBuilder.addTarget(mImageReader.getSurface());

            int mDisplayOrientation = context.getDisplay().getRotation() * 90;
            int mJpegOrientation = getJpegOrientation(characteristics, mDisplayOrientation);
            mPreviewBuilder.set(CaptureRequest.JPEG_ORIENTATION, mJpegOrientation);

            List<OutputConfiguration> outputSurfaces = new ArrayList<>();
            outputSurfaces.add(new OutputConfiguration(mImageReader.getSurface()));
            mCamera.createCaptureSession(new SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR,
                    outputSurfaces,
                    Executors.newCachedThreadPool(), mSessionStateCallback));

        } catch (CameraAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private int getJpegOrientation(CameraCharacteristics c, int deviceOrientation) {
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN)
            return 0;
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;

        // Reverse device orientation for front-facing cameras
        boolean facingFront = c.get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_FRONT;
        if (facingFront) deviceOrientation = -deviceOrientation;

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation

        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    public void openCamera() {
        try {
            if (mCameraIdList.length == 1) {
                mCameraManager.openCamera(mCameraIdList[0], mDeviceStateCallback, mHandler);
            } else {
                for (String cameraId : mCameraIdList) {
                    CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                    if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                        mCameraManager.openCamera(cameraId, mDeviceStateCallback, mHandler);
                        break;
                    } else if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                        mCameraManager.openCamera(cameraId, mDeviceStateCallback, mHandler);
                        break;
                    }
                }
            }
        } catch (SecurityException e) {
            // TODO: handle exception
            e.printStackTrace();
            if (listener != null) {
                listener.onError(ERROR_NO_PERMISSION);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            if (listener != null) {
                listener.onError(ERROR_CANNOT_ACCESS);
            }
        }
    }

    private final CameraDevice.StateCallback mDeviceStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {
            // TODO Auto-generated method stub
            if (LogUtils.DEBUG) {
                Log.i(TAG, "Camera opened...");
            }
            if (listener != null) {
                listener.onOpened();
            }

            mCameraID = camera.getId();
            mCamera = camera;
            setCameraParameters();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onClosed(CameraDevice camera) {
            // TODO Auto-generated method stub
            super.onClosed(camera);
            if (listener != null) {
                listener.onClosed();
            }
        }
    };

    private final CameraCaptureSession.StateCallback mSessionStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(CameraCaptureSession session) {
            // TODO Auto-generated method stub
            try {
                if (LogUtils.DEBUG) {
                    Log.i(TAG, "CameraCaptureSession  onConfigured");
                }
                session.setRepeatingRequest(mPreviewBuilder.build(), null, mHandler);
            } catch (CameraAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            // TODO Auto-generated method stub
            if (LogUtils.DEBUG) {
                Log.i(TAG, "CameraCaptureSession  onConfigureFailed");
            }
        }
    };

    private class OnImageAvailableListenerImpl implements ImageReader.OnImageAvailableListener {
        private ReentrantLock lock = new ReentrantLock();
        private byte[] nv21;

        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();
            ++frameCounter;
            if (listener != null && image != null && (mSkipFrame <= 0 || frameCounter % (mSkipFrame + 1) == 0)) {
                if (LogUtils.DEBUG) {
                    Log.i(TAG, "onImageAvailable: " + frameCounter + "(" + mPreviewSize.toString() + "), skipFrame= " + mSkipFrame);
                }
                lock.lock();

                nv21 = convertYUV420888ToNV21(image);
                listener.onPreview(nv21, mPreviewSize, frameCounter);

                lock.unlock();
            }
            image.close();
        }
    }

    public static byte[] convertYUV420888ToNV21(Image image) {
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uvBuffer = image.getPlanes()[2].getBuffer();
        int yLength = yBuffer.remaining();
        int uvLength = uvBuffer.remaining();
        byte[] data = new byte[image.getWidth() * image.getHeight() * 3 / 2];
        yBuffer.get(data, 0, yLength);
        uvBuffer.get(data, yLength, uvLength);
        return data;
    }


    public interface OnCameraStatusChangedListener {
        void onError(int errorCode);

        void onOpened();

        void onClosed();

        void onPreview(byte[] nv21, Size previewSize, long frameIndex); // for yuv(native-c++)
    }
}
