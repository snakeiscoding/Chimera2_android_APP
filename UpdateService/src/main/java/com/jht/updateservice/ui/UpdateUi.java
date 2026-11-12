package com.jht.updateservice.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;

import com.jht.interfaces.IInstallEvent;
import com.jht.updateservice.BackgroundActivity;

import org.jetbrains.annotations.NonNls;

import java.util.List;

import updatepackages.UpdatePackage;

/**
 * Handles the updater UI. This code was all ported from the update manager when it was switched to
 * events with update information.
 * <p>
 * Created by chris.foyer on 2/20/2017.
 */
public class UpdateUi implements IInstallEvent {
    @NonNls private static final String TAG = "UpdateUi";
    private InstallDialog installDialog;
    private Handler handler = new Handler();
    private Context context;

    public UpdateUi(Context context, IUpdateUi updateUiInterface) {
        InstallDialog.UiEvents mUiEvents = this::hideAllScreens;
        installDialog = new InstallDialog(context, mUiEvents, updateUiInterface);
        this.context = context;
    }

    public interface IUpdateUi {
        void cancelUpdate();
    }

    public void updateInstallMessage(@NonNls String message) {
        installDialog.updateInstallProgress(message);
    }

    @Override
    public void setupCompleted(boolean inUpdate) {
        Log.e(TAG, "setupCompleted received " + inUpdate);
        //TODO: If needed, this can be set up so the system will not load if an update is underway
    }

    @Override
    public void showUpdateScreen(boolean firstTimeInstaller) {
        Log.e(TAG, "showUpdateScreen received " + firstTimeInstaller);
        if (firstTimeInstaller) {
            startFirstTimeSoftwareInstaller();
        } else {
            startSoftwareInstaller();
        }
    }

    @Override
    public void hideUpdateScreen() {
        Log.e(TAG, "hideUpdateScreen received");
        hideAllScreens();
    }

    @Override
    public void updateManagerVersionSet(String version) {
        Log.e(TAG, "updateManagerVersionSet received");
        final String versionText = "version " + version; // NON-NLS
        installDialog.setVersionText(versionText);
        refreshInstallDialogScreen();
    }

    @Override
    public void downloadInformation(List<UpdatePackage> updatesToDownload, boolean isStorage) {
        Log.d(TAG, "downloadInformation received");
        if (isStorage) {
            Log.i(TAG, "Downloading from storage, but do not show download dialog");
            installDialog.showDownloading(true);
            refreshInstallDialogScreen();
        } else {
            Log.i(TAG, "Downloading RSCU updates, showing download UI");
            installDialog.showDownloading(false);
            refreshInstallDialogScreen();
        }
    }

    @Override
    public void downloadProgressUpdate(int progress, String progressSize) {
        Log.d(TAG, "Download progress: " + progress);
        installDialog.updateDownloadProgress(progress);
    }

    @Override
    public void installingUpdate(String packageName, String packageVersion) {
        Log.i(TAG, "installingUpdate received " + packageName + " " + packageVersion);
        installDialog.showInstalling(packageName);
    }

    @Override
    public boolean updateComplete() {
        Log.e(TAG, "updateComplete received");
        installDialog.showSuccess();
        return true; // Tells the updater to reboot the console for us
    }

    @Override
    public void errorOccurred(String errorMessage, boolean canReboot) {
        Log.e(TAG, "errorOccurred received " + errorMessage);
        installDialog.showError(errorMessage, canReboot);
    }

    @Override
    public void displayManualReboot() {
        Log.d(TAG, "Showing screen to manually reset the console");
        installDialog.showSystemResetScreen();
    }

    private void hideAllScreens() {
        handler.post(() -> {
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            try {
                windowManager.removeView(installDialog);
                isSoftwareInstallerAdded = false;
                hideBackgroundActivity();
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Could not remove view.");
            }
        });
    }

    private boolean isSoftwareInstallerAdded = false;

    private void startSoftwareInstaller() {
        handler.post(() -> {
            Log.d(TAG, "starting software installer screen");
            addSoftwareInstallScreen();
            refreshInstallDialogScreen();
        });
    }

    private void refreshInstallDialogScreen() {
        handler.post(() -> {
            if (isSoftwareInstallerAdded) {
                WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                windowManager.updateViewLayout(installDialog, getSoftwareInstallerParams());
            }
        });
    }

    private void addSoftwareInstallScreen() {
        handler.post(() -> {
            if (!isSoftwareInstallerAdded) {
                isSoftwareInstallerAdded = true;
                WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                windowManager.addView(installDialog, getSoftwareInstallerParams());
            }
        });
        handler.postDelayed(this::startBackgroundActivity, 3000);
    }

    private void startFirstTimeSoftwareInstaller() {
        Log.d(TAG, "starting first time installer");
        installDialog.showInitialUpdateScreen();
        addSoftwareInstallScreen();
        refreshInstallDialogScreen();
    }

    private void startBackgroundActivity() {
        Intent dialogIntent = new Intent(context, BackgroundActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(dialogIntent);
    }

    private void hideBackgroundActivity() {
        Intent closeIntent = new Intent(BackgroundActivity.CLOSE_ACTION);
        context.sendBroadcast(closeIntent);
    }

    private WindowManager.LayoutParams getSoftwareInstallerParams() {
        WindowManager.LayoutParams installerParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.FIRST_SYSTEM_WINDOW + 21, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_TOUCHABLE_WHEN_WAKING
                | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH, PixelFormat.TRANSPARENT);
        installerParams.gravity = Gravity.NO_GRAVITY;
        installerParams.x = 0;
        installerParams.y = 0;
        return installerParams;
    }
}
