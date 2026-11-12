package com.jht.mastboard;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.util.ExecuteAsRoot;
import com.jht.interfaces.models.WorkoutModel;
import com.jht.mastboard.data.LEDItem;
import com.jht.mastboard.data.LedPriorityManager;
import com.jht.mastboard.data.LedZone;
import com.jht.mastboard.device.Mastboard;
import com.jht.mastboard.device.MastboardManager;
import com.jht.mastboard.data.UiEventKeys;
import com.jht.mastboard.interfaces.MastboardModel;
import com.jht.mastboard.lib.HapticMotorEvent;
import com.jht.mastboard.lib.PanelSide;
import com.jht.mastboard.lib.TouchData;
import com.jht.liberror.ErrorCodeManager;
import com.jht.liberror.ErrorCodes;
import com.jht.services.ChildConsoleService;
import com.jht.services.ConsoleServiceState;
import com.jht.util.UsbPermission;
import com.jht.util.ServiceHelper;

import org.jetbrains.annotations.NonNls;

import java.io.File;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;

/****
 * This service handles all communication to the Mast Board on the Project One frame. This will
 * include the LCD, LEDs, and fan. The other communications to the board will be handled by the
 * same manager as previous.
 */
@SuppressLint("NewApi")
@AndroidEntryPoint
public class MastBoardCommService extends ChildConsoleService {
    @Inject MastboardModel mastboardModel;
    @Inject WorkoutModel workoutModel;

    // need to create in onCreate, injector runs after construction
    private LedPriorityManager ledPriorityManager; // this now attaches its own listeners

    @NonNull
    private static final String TAG = "MastBoardCommService";

    /**
     * Maximum number of times to try connecting to either Left or Right LCD via SPI protocol before sending an error.
     */
    private static final int MAX_SPI_HEARTBEAT_COUNTER = 5;

    /**
     * Maximum number of times to try connecting to either Left or Right LCD via I2C protocol before sending an error.
     */
    private static final int I2C_NOT_FOUND_COUNT_MAX = 5;

    private NewLCDPanelController mNewLCDPanelController;

    private int failed_R_SPI_heartbeats_counter = 0;
    private int failed_L_SPI_heartbeats_counter = 0;
    private int failed_R_I2C_heartbeats_counter = 0;
    private int failed_L_I2C_heartbeats_counter = 0;
    private boolean isTouchSamplingRunning = false;
    private boolean isFaultDetectionRunning = false;

    private boolean leftLCDCommunicationFailed = false;
    private boolean rightLCDCommunicationFailed = false;

    private Thread touchSampleThread;
    private Thread FaultDetectThread;

    private Mastboard mastboard;

    @NonNls private static final String UPDATE_LCD_TOUCH_FIRMWARE_BROADCAST = "p1_lcd_touch_firmware_update";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ServiceHelper.startForegroundService(this);

        // clear old version info immediately before we attempt to get fresh info (if console not attached to frame, needs to show toggles not available)
        Settings.Secure.putString(getContentResolver(), "p1_lcd_toggle_version", "N/A"); //NON-NLS

        // use a handler post to attempt to prevent ANR
        new Handler().post(this::init);
    }

    /**
     * Initialization
     */
    private void init() {
        // Make sure we have permissions to USB devices on boot
        UsbPermission.requestUsbPermission();

        // Note: it was observed that the first write can sometime hang and occasionally can hang the process stopping all
        // threads with no crash, no exception, and no ANR.
        //
        // start a monitor process that will check every second to see if the mastboard process goes into a reap_as state;
        // if it does, kill the mastboard process then stop
        String monitorName = "monitor_mastboard";
        /* had to move from chimera's ExecuteAsRoot to common algo version, but they work a little different, think chimera builds in sh call
        ExecuteAsRoot.run("exec -a " + monitorName + " sh -c 'while true; do ps -A | grep " + Application.getProcessName() +
                " | grep -q reap_as && kill -9 $(pidof " + Application.getProcessName() + ") && break; sleep 1; done' &");*/
        ExecuteAsRoot.executeCommand("kill -9 $(pidof " + monitorName + ")"); // kill anything already running to avoid duplicates?
        String proc = Application.getProcessName();
        String cmd = "nohup sh -c 'exec -a " + monitorName + " sh -c \"while true; do " +
                        "ps -A | grep " + proc + " | grep -q reap_as && " +
                        "kill -9 $(pidof " + proc + ") && break; " +
                        "sleep 1; done\"' >/dev/null 2>&1 &";
        ExecuteAsRoot.executeCommand(cmd);

        // start the write failure detection first to make sure it runs since there's a chance the first write can hang in mastboard init
        startWriteFailureDetectionThread();
        mastboard = MastboardManager.getInstance().getMastboard();

        ledPriorityManager = new LedPriorityManager(mastboardModel, workoutModel);
        mNewLCDPanelController = new NewLCDPanelController(getApplicationContext());

        mastboard.setInitListener(() -> {
            Log.d("Mastboard", "Performing initialization");
            // start after initialization is complete
            startTouchSamplingThread();
            startFaultDetectionThread();
            Settings.Secure.putString(getContentResolver(), "p1_lcd_toggle_version", mastboard.getTouchFirmwareVersion());
            // if issues arise, mitigate with a check flag.
            setupDataMatrix();

            // stop the monitor process
            ExecuteAsRoot.executeCommand("kill -9 $(pidof " + monitorName + ")");
        });

        setupLCDAdapters();

        registerReceiver(mUpdateLCDTouchFirmwareReceiver, new IntentFilter(UPDATE_LCD_TOUCH_FIRMWARE_BROADCAST));
        registerReceiver(usbReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));

        updateMyServiceState(ConsoleServiceState.READY);
        Log.d("ServiceStarter", "Ending " + this);
    }


    /**
     * Starts thread to check if the first write failed. For some reason, on boot, the first write
     * can hang. If the first write fails, we kill the process which should restart the service.
     *
     * Trying to close the open devices did not stop the write from hanging so killing the process
     * until we find a better solution.
     *
     */
    private void startWriteFailureDetectionThread() {
        new Thread(() -> {
            Log.d(TAG, "--> WriteFailureDetectionThread: START");

            // wait until the mastboard is created
            while (mastboard == null) {
                SystemClock.sleep(100);
            }

            // if mastboard is not ready, check for first write failure until mastboard is ready
            while (!mastboard.isReady()) {
                // restart service if first write failed
                if (mastboard.didFirstWriteFail()) {
                    Log.e(TAG, "--> WriteFailureDetectionThread detected first write failure. Restarting service!");
                    ExecuteAsRoot.executeCommand("kill -9 $(pidof " + Application.getProcessName() + ")");
                    break;
                }

                SystemClock.sleep(100);
            }

            if (mastboard.isReady()) Log.d(TAG, "--> mastboard is ready");
            Log.d(TAG, "--> WriteFailureDetectionThread: END");
        }, "writeFailureDetection").start();
    }

    /**
     * Sets up the adapters necessary to maintain LCD displays correctly.
     */
    private void setupLCDAdapters() {

        // Setup LCD Panel specific interactions
        // fixme: Isn't this something being set in a key?
        mastboardModel.getOnLcdDebugLayoutEvent().listen(() -> {
            mNewLCDPanelController.enableLCDDebuggingPanels();
            return Unit.INSTANCE;
        });
        mastboardModel.getLcdDebugPanels().listenAndTrigger(enable -> {
            mNewLCDPanelController.enableDebugPanels(enable);
            return Unit.INSTANCE;
        });

        mastboardModel.getUiEvent().listenAndTrigger((event)->{
            try {
                setupToggles(event);
            } catch (Exception ignored) {
            }
            return Unit.INSTANCE;
        });

        workoutModel.getWorkoutRunState().listenAndTrigger(value -> {
            try { mNewLCDPanelController.updateWorkoutToggleDisplays(); } catch (Exception ignored) {}
            return Unit.INSTANCE;
        });

    }

    @Override
    public void onDestroy() {
        mastboardModel.close();
        workoutModel.close();
        ledPriorityManager.close();
        unregisterReceiver(mUpdateLCDTouchFirmwareReceiver);
        unregisterReceiver(usbReceiver);
        super.onDestroy();
    }

    /**
     * Check for response from the left/right LCD and touch sensor. If either protocol response fails, set failure flag
     * to true. Update the appropriate counter. If counter > counter maximum, send the correct error code.
     *
     * If failure flag is true, reset the LCD, reinit communication, and set failure flag to false.
     *
     * Error codes that can be thrown:
     *      LCDLeftToggleI2CCommunicationLost - 0320, ErrorCodeLevel B.
     *      LCDLeftToggleSPICommunicationLost - 0321, ErrorCodeLevel B.
     *      LCDRightToggleI2CCommunicationLost - 0322, ErrorCodeLevel B.
     *      LCDRightToggleSPICommunicationLost - 0323, ErrorCodeLevel B.
     **/
    private void detectFault() {
        // check all statuses of LCD SPI and I2C connections, for both left and right sides.
        boolean leftI2CStatus = mastboard.I2CisResponding(PanelSide.LEFT);
        boolean leftSPIStatus = mastboard.SPIisResponding(PanelSide.LEFT);
        boolean rightI2CStatus = mastboard.I2CisResponding(PanelSide.RIGHT);
        boolean rightSPIStatus = mastboard.SPIisResponding(PanelSide.RIGHT);

        // no error code by default
        int errorCode;

        /*RIGHT LCD*/
        // if right LCD I2C or SPI connection failed
        if (!rightI2CStatus || !rightSPIStatus) {
            rightLCDCommunicationFailed = true;

            // I2C failed, update counter. If count > I2C_NOT_FOUND_COUNT_MAX, send error code, set counter to 0.
            if (!rightI2CStatus) {
                failed_R_I2C_heartbeats_counter++;

                if (failed_R_I2C_heartbeats_counter >= I2C_NOT_FOUND_COUNT_MAX) {
                    errorCode = ErrorCodes.LCDRightToggleI2CCommunicationLost.getCode();
                    Log.w(TAG, "failed_R_I2C_heartbeats_counter>=5, throwing " + errorCode + " error");
                    failed_R_I2C_heartbeats_counter = 0;
                    ErrorCodeManager.handleErrorCode(errorCode);
                }
            }

            // SPI failed, update counter. If count > MAX_SPI_HEARTBEAT_COUNTER, send error code, set counter to 0.
            if (!rightSPIStatus) {
                failed_R_SPI_heartbeats_counter++;
                if (failed_R_SPI_heartbeats_counter >= MAX_SPI_HEARTBEAT_COUNTER) {
                    errorCode = ErrorCodes.LCDRightToggleSPICommunicationLost.getCode();
                    Log.w(TAG, "failed_R_SPI_heartbeats_counter>=5, throwing " + errorCode + " error");
                    failed_R_SPI_heartbeats_counter = 0;
                    ErrorCodeManager.handleErrorCode(errorCode);
                }
            }
        } else if (rightLCDCommunicationFailed) {
            //reset right LCD, set counters to 0, and reinit. Set failure flag to false.
            mastboard.resetRightLCD();
            failed_R_SPI_heartbeats_counter = 0;
            failed_R_I2C_heartbeats_counter = 0;

            new Handler(Looper.getMainLooper()).post(() -> mNewLCDPanelController.reInitRightLCD());
            rightLCDCommunicationFailed = false;
        }

        /*LEFT LCD*/
        // if left LCD I2C or SPI connection failed
        if (!leftI2CStatus || !leftSPIStatus) {
            leftLCDCommunicationFailed = true;

            // I2C failed, update counter. If count > I2C_NOT_FOUND_COUNT_MAX, send error code, set counter to 0.
            if (!leftI2CStatus) {
                failed_L_I2C_heartbeats_counter++;

                if (failed_L_I2C_heartbeats_counter >= I2C_NOT_FOUND_COUNT_MAX) {
                    errorCode = ErrorCodes.LCDLeftToggleI2CCommunicationLost.getCode();
                    Log.w(TAG, "failed_L_I2C_heartbeats_counter>=5, throwing " + errorCode + " error");
                    failed_L_I2C_heartbeats_counter = 0;
                    ErrorCodeManager.handleErrorCode(errorCode);
                }
            }

            // SPI failed, update counter. If count > MAX_SPI_HEARTBEAT_COUNTER, send error code, set counter to 0.
            if (!leftSPIStatus) {
                failed_L_SPI_heartbeats_counter++;
                if (failed_L_SPI_heartbeats_counter >= MAX_SPI_HEARTBEAT_COUNTER) {
                    errorCode = ErrorCodes.LCDLeftToggleSPICommunicationLost.getCode();
                    Log.w(TAG, "failed_L_SPI_heartbeats_counter>=5, throwing " + errorCode + " error");
                    failed_L_SPI_heartbeats_counter = 0;
                    ErrorCodeManager.handleErrorCode(errorCode);
                }
            }
        } else if (leftLCDCommunicationFailed) {
            //reset left LCD, set counters to 0, and reinit. Set failure flag to false.
            mastboard.resetLeftLCD();
            failed_L_SPI_heartbeats_counter = 0;
            failed_L_I2C_heartbeats_counter = 0;

            new Handler(Looper.getMainLooper()).post(() -> mNewLCDPanelController.reInitLeftLCD());
            leftLCDCommunicationFailed = false;
        }
    }

    /**
     * Monitor SPI connection. If no connection after 5 tries, with a 2 second wait in-between, will throw appropriate
     * error based on toggle side.
     * Note: the current logic resets the error code counter once a fault is detected but it will keep throwing the
     * error code once the counter reaches the fault tolerance.
     */
    private void startFaultDetectionThread() {
        if (!mastboard.isReady()) return;
        if (FaultDetectThread == null) {
            FaultDetectThread = new Thread(() -> {
                isFaultDetectionRunning = true;
                while (isFaultDetectionRunning) {
                    //Any faster will result in error code thrown during initialization
                    SystemClock.sleep(2000);
                    detectFault();
                }
            });
            FaultDetectThread.start();
        }
    }

    /**
     * Starts thread to watch LCD touch data thru I2C chip. Monitor I2C connection. If no connection after 5 tries,
     * with a 0.5 second wait in-between, will throw the appropriate error.
     */
    private void startTouchSamplingThread() {
        if (!mastboard.isReady()) return;
        //period needs to be less than 16.6ms total if we want to be able to catch all data points from
        // the touch controller (defaults to 60Hz, its max sample rate)
        if (touchSampleThread == null) {
            touchSampleThread = new Thread(() -> {
                // we don't need gestures right now, so try slowing down to reduce system strain without loosing responsiveness
                long lastTouchActivity = 0; // initial value doesn't matter, it starts out slow scan anyway
                int delay = 100; // thread delay in milliseconds
                isTouchSamplingRunning = true;
                while (isTouchSamplingRunning) {
                    SystemClock.sleep(delay);
                    if (updateLastTouch()) {
                        delay = 25; // fast scan if touched
                        lastTouchActivity = SystemClock.uptimeMillis();
                    } else if (SystemClock.uptimeMillis() - lastTouchActivity > 1000) {
                        delay = 100; // slow scan if idle for a while
                    }
                }
            }, "touchSample");
            touchSampleThread.start();
        }
    }

    private void stopTouchSamplingThread() {
        if (touchSampleThread != null) {
            isTouchSamplingRunning = false;
            try {
                touchSampleThread.join(5000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            touchSampleThread = null;
        }
    }

    private void setupDataMatrix() {
        mastboardModel.getOnLedColor().listen(ledItem -> {
            // applies the mastboard portion of LED color (IO also needs to react to LED color as well, so both just act on this event)
            setLedColor(ledItem);
            return Unit.INSTANCE;
        });
        mastboardModel.getLedBrightness().listenAndTrigger(value -> {
            MastboardManager.updateLEDBrightness(value);
            return Unit.INSTANCE;
        });
        mastboardModel.getLcdBrightness().listenAndTrigger( value -> {
            MastboardManager.updateBacklight(value);
            return Unit.INSTANCE;
        });

        mastboardModel.getOnHapticEvent().listen(event -> {
            int hapticMotorEffect = mastboardModel.getHapticMotorEffect().getValue();
            if (event instanceof HapticMotorEvent.BuzzPanel) {
                MastboardManager.buzzHapticMotor(hapticMotorEffect, ((HapticMotorEvent.BuzzPanel) event).getPanelSide());
            } else if (event instanceof HapticMotorEvent.BuzzAllPanels) {
                for (PanelSide side: PanelSide.values()) {
                    MastboardManager.buzzHapticMotor(hapticMotorEffect, side);
                }
            }
            return Unit.INSTANCE;
        });

        mastboardModel.getUserHapticsEnabled().listen(userHapticsEnabled -> {
            checkSetHapticsOn();
            return Unit.INSTANCE;
        });
        mastboardModel.getSystemHapticsEnabled().listen(systemHapticsEnabled -> {
            checkSetHapticsOn();
            return Unit.INSTANCE;
        });
        checkSetHapticsOn();
    }

    private void checkSetHapticsOn() {
        boolean systemHapticsEnabled = mastboardModel.getSystemHapticsEnabled().getValue();
        boolean userHapticsEnabled = mastboardModel.getUserHapticsEnabled().getValue();
        mastboard.setHapticsOn(systemHapticsEnabled && userHapticsEnabled);
    }

    private boolean updateLastTouch() {
        TouchData touchDataRight = mastboard.getTouchData(PanelSide.RIGHT);
        TouchData touchDataLeft = mastboard.getTouchData(PanelSide.LEFT);
        return mNewLCDPanelController.updateLastTouch(touchDataLeft, touchDataRight);
    }

    /**
     * Setup the LCD toggles when a UI event is triggered. To prevent issues with state, the code in
     * this method is encased in a synchronized block.
     *
     * @param uiEvent The event reported by the system.
     */
    private void setupToggles(final String uiEvent) {
        synchronized (this) {
            switch (UiEventKeys.getEventFromString(uiEvent)) {
                case SPLASH_SCREEN:
                    mNewLCDPanelController.setState(PanelControllerState.DisplayingSplashScreen);
                    break;

                case SCREENSAVER_SCREEN:
                    mNewLCDPanelController.setState(PanelControllerState.InScreensaver);
                    break;

                case WELCOME_SCREEN:
                    mNewLCDPanelController.setState(PanelControllerState.DisplayingWelcomeScreen);
                    break;

                case GO_SCREEN:
                    mNewLCDPanelController.setState(PanelControllerState.DisplayingGoScreen);
                    break;

                case TRAINING_EXERCISES_SCREEN:
                    mNewLCDPanelController.setState(PanelControllerState.DisplayingTrainingExercisesScreen);
                    break;

                case CUSTOM_WORKOUTS_SCREEN:
                    mNewLCDPanelController.setState(PanelControllerState.DisplayingCustomWorkoutScreen);
                    break;

                case OPEN_FEATURE_WORKOUT_SETUP:
                    mNewLCDPanelController.setDisplayingFeaturedWorkoutSetup(true);
                    break;

                case CLOSE_FEATURE_WORKOUT_SETUP:
                    mNewLCDPanelController.setDisplayingFeaturedWorkoutSetup(false);
                    break;

                case OPEN_MATRIX_DASHBOARD:
                    mNewLCDPanelController.setDisplayingMatrixDashboard(true);
                    break;

                case CLOSE_MATRIX_DASHBOARD:
                    mNewLCDPanelController.setDisplayingMatrixDashboard(false);
                    break;

                case OPEN_CONNECT_TO_WATCH:
                    mNewLCDPanelController.setDisplayingConnectToWatch(true);
                    break;

                case CLOSE_CONNECT_TO_WATCH:
                    mNewLCDPanelController.setDisplayingConnectToWatch(false);
                    break;

                case LOGIN_REGISTER:
                    mNewLCDPanelController.setState(PanelControllerState.DisplayingLoginRegisterScreen);
                    break;

                case OPEN_COUNTDOWN_POPOVER:
                    mNewLCDPanelController.setDisplayingCountdownPopover(true);
                    break;

                case CLOSE_COUNTDOWN_POPOVER:
                    mNewLCDPanelController.setDisplayingCountdownPopover(false);
                    break;

                case WORKOUT:
                    mNewLCDPanelController.setState(PanelControllerState.InWorkout);
                    break;

                case SHOW_ESTOP:
                    mNewLCDPanelController.setDisplayingEStopActive(true);
                    break;

                case CLOSE_ESTOP:
                    mNewLCDPanelController.setDisplayingEStopActive(false);
                    break;

                case MES:
                    mNewLCDPanelController.setState(PanelControllerState.DisplayingMES);
                    break;

                case FIRST_TIME_SETUP:
                    mNewLCDPanelController.setState(PanelControllerState.InFirstTimeSetup);
                    break;

                case WORKOUT_SUMMARY:
                    mNewLCDPanelController.setState(PanelControllerState.DisplayingWorkoutSummary);
                    break;

                case EGYM_LOADING:
                    mNewLCDPanelController.setState(PanelControllerState.EGYMLoading);
                    break;

                case YMCA360_LOADING:
                    mNewLCDPanelController.setState(PanelControllerState.YMCA360Loading);
                    break;

                case IFIT_LOADING:
                    mNewLCDPanelController.setState(PanelControllerState.IFITLoading);
                    break;

                case OUT_OF_ORDER_SCREEN:
                    mNewLCDPanelController.setState(PanelControllerState.DisplayingOutOfOrder);
                    break;

                case UPDATE:
                    Log.w(TAG, "Update mode really isn't something I support.");
                    break;
            }
        }
    }

    private void setLedColor(LEDItem ledItem) {
        Log.d(TAG, "New Active LED " + ledItem.color + " " + ledItem.ledZone.name());
        MastboardManager.setColorLed(ledItem);

        // Always doing because console sucks.
        MastboardManager.setColorLed(new LEDItem(getColor(R.color.p1_basic_led_3), LedZone.PLAY));
        MastboardManager.setColorLed(new LEDItem(getColor(R.color.p1_basic_led_4), LedZone.STOP));
    }

    /**
     * Receiver responsible for performing LCD Touch Firmware update.
     */
    boolean isUpdatingFirmware = false;
    private final BroadcastReceiver mUpdateLCDTouchFirmwareReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // assume the console will be rebooted after an about so only support one firmware update broadcast
            if (isUpdatingFirmware) return;
            isUpdatingFirmware = true;

            // Acquire the firmware update file path.
            final String path = intent.getStringExtra("path");
            if (path != null) {
                File file = new File(path);
                if (file.isFile()) {
                    stopTouchSamplingThread();
                    boolean success = mastboard.updateTouchFirmware(file, percent -> {
                        updateLCDTouchFirmwareResult(percent, false, "");
                    });
                    // send null seem to indicate a successful update
                    updateLCDTouchFirmwareResult(100, true, success ? null : "Update Failed");
                } else {
                    Log.d(TAG, "LCD Touch Firmware File Does Not Exist!");
                }
            }
        }
    };

    void updateLCDTouchFirmwareResult(int progress, boolean finished, String result) {
        Context context = getApplicationContext();
        Intent lcdTouchResultsBroadcast = new Intent("com.jht.updatelcdtouchfirmware.result");
        lcdTouchResultsBroadcast.putExtra("lcd_touch_firmware_progress", progress);
        lcdTouchResultsBroadcast.putExtra("finished", finished);
        lcdTouchResultsBroadcast.putExtra("result", result);
        context.sendBroadcast(lcdTouchResultsBroadcast);
    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                // check to see if this is the mastboard (mVendorId=1027,mProductId=24592)
                if (device != null && device.getVendorId() == 1027 && device.getProductId() == 24592) {
                    UsbPermission.requestUsbPermission(getApplicationContext(), device);
                    mastboard.init();
                }
            }
        }
    };
}
