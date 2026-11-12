package com.jht.chimera.ui.machinesettings.dialogs;

import android.view.View;

import java.util.List;

import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.chimera.R;
import com.jht.chimera.lib.tv.ChannelInfo;
import com.jht.chimera.lib.tv.SKNETChannelInfo;
import com.jht.chimera.lib.tv.tuner.CABTuner;
import com.jht.chimera.lib.widget.dialog.CABAdvancedDialog;
import com.jht.chimera.lib.widget.dialog.LoggingOptions;
import com.jht.chimera.lib.widget.dialog.NetworkPasswordEntry;
import com.jht.chimera.lib.widget.network.SSID;
import com.jht.chimera.service.tiers.dialogs.DefaultUpgradeDialog;
import com.jht.chimera.service.tiers.dialogs.WebViewDialogBox;
import com.jht.chimera.lib.tv.tuner.lg.channel.LgChannel;
import com.jht.chimera.lib.tv.tuner.lg.channel.LgRegion;
import com.jht.serialport.peripheral.tuner.channel.IChannel;
import com.jht.serialport.peripheral.tuner.channel.SimpleIPTVChannel;
import com.jht.serialport.peripheral.tuner.enseo.EnseoChannel;
import com.jht.serialport.peripheral.tuner.swift.SwiftChannel;


/**
 * This is a convenient class for creating dialogs.  This takes care of creating the object, initializing it,
 * and showing it.
 */
@SuppressWarnings("UnusedReturnValue")
public class ProjectDialogFactory extends com.jht.androiduiwidgets.dialog.DialogFactory {


    public static DefaultUpgradeDialog defaultUpgradeDialog(View anchor, String message, String title) {
        DefaultUpgradeDialog dialog = new DefaultUpgradeDialog(anchor.getContext());
        dialog.init(message, title);
        dialog.show(anchor);
        dialog.bringToFront();
        return dialog;
    }

    /**
     * Create the network password entry used to connect to a WiFi network.
     *
     * @param anchor            The view that caused the dialog to appear
     * @param ssid              Contains the network properties of the network we want to connect to.
     * @param entryMode         WEP, WPA, or Hidden.  This affects the dialog widgets.
     * @param onEntryComplete   The callback to call when the user has finished with the dialog.
     * @return                  The dialog.
     */
    public static NetworkPasswordEntry networkPassword(View anchor, SSID ssid, NetworkPasswordEntry.PasswordEntryMode entryMode, NetworkPasswordEntry.IOnEntryComplete onEntryComplete) {
        NetworkPasswordEntry dialog = new NetworkPasswordEntry(anchor.getContext());
        dialog.init(ssid, entryMode, onEntryComplete);
        dialog.show(anchor);
        dialog.bringToFront();
        return dialog;
    }

    public static WebViewDialogBox webViewDialogBox(View anchor, String url) {
        WebViewDialogBox dialog = new WebViewDialogBox(anchor.getContext());
        dialog.init(url);
        dialog.show(anchor);
        dialog.bringToFront();
        return dialog;
    }

    public static CABAdvancedDialog cabAdvancedDialog(View anchor, CABTuner tuner) {
        CABAdvancedDialog cabAdvancedDialog = new CABAdvancedDialog(anchor.getContext());
        cabAdvancedDialog.init(tuner);
        cabAdvancedDialog.show(anchor);
        cabAdvancedDialog.bringToFront();
        return cabAdvancedDialog;
    }

    public static CalibrateDialog calibrateDialog(View anchor, String title, String message, boolean showOk, CalibrateDialog.IOnCalibrateDialog onCalibrateDialog, CalibrateDialog.CALIBRATE_BUTTON_STATE buttonState) {
        CalibrateDialog dialog = new CalibrateDialog(anchor.getContext());
        dialog.init(title, message, showOk, onCalibrateDialog, buttonState);
        dialog.show(anchor);
        dialog.bringToFront();
        return dialog;
    }


    public static TVChannelDialog editChannelDialog(View anchor, ChannelInfo channel, DialogBase.IOnConfirm onConfirm) {
        TVChannelDialog dialog = new TVChannelDialog(anchor.getContext());
        dialog.init(channel, onConfirm);
        dialog.show(anchor);
        dialog.bringToFront();
        return dialog;
    }


    public static TVChannelDialog editChannelDialog(View anchor, SwiftChannel channel, DialogBase.IOnConfirm onConfirm) {
        TVChannelDialog dialog = new TVChannelDialog(anchor.getContext());
        dialog.init(channel, onConfirm);
        dialog.show(anchor);
        dialog.bringToFront();
        return dialog;
    }

    public static TVChannelDialog editChannelDialog(View anchor, SKNETChannelInfo channel, DialogBase.IOnConfirm onConfirm) {
        TVChannelDialog dialog = new TVChannelDialog(anchor.getContext());
        dialog.init(channel, onConfirm);
        dialog.show(anchor);
        dialog.bringToFront();
        return dialog;
    }

    public static TVChannelDialog2 editChannelDialog2(View anchor, ChannelInfo channel, DialogBase.IOnConfirm onConfirm) {
        TVChannelDialog2 dialog = new TVChannelDialog2(anchor.getContext());
        dialog.init(channel, onConfirm);
        dialog.show(anchor);
        dialog.bringToFront();
        return dialog;
    }

    public static TVChannelDialog2 editChannelDialog2(View anchor, IChannel channel, TVChannelDialog2.IOnChannelEdit onConfirm) {
        TVChannelDialog2 dialog = new TVChannelDialog2(anchor.getContext());
        dialog.init(channel, onConfirm);
        dialog.show(anchor);
        return dialog;
    }

    public static IPTVChannelDialog editIPTVChannelDialog(View anchor, ChannelInfo channel, List<ChannelInfo> existingChannels, DialogBase.IOnConfirm onConfirm) {
        IPTVChannelDialog dialog = new IPTVChannelDialog(anchor.getContext());
        dialog.init(channel, existingChannels, onConfirm);
        dialog.show(anchor);
        return dialog;
    }

    public static EnseoChannelDialog editEnseoChannelDialog(View anchor, EnseoChannel channel, boolean allowRFChannels, List<EnseoChannel> existingChannels, EnseoChannelDialog.IOnConfirm onConfirm) {
        EnseoChannelDialog dialog = new EnseoChannelDialog(anchor.getContext());
        dialog.init(channel, allowRFChannels, existingChannels, onConfirm);
        dialog.show(anchor);
        return dialog;
    }

    public static LgChannelDialog editLgChannelDialog(View anchor, LgChannel channel, List<LgChannel.ChannelType> supportedTypes, LgRegion region, List<LgChannel> existingChannels, LgChannelDialog.IOnConfirm onConfirm) {
        LgChannelDialog dialog = new LgChannelDialog(anchor.getContext());
        dialog.init(channel, supportedTypes, region, existingChannels, onConfirm);
        dialog.show(anchor);
        return dialog;
    }

    public static SimpleIPTVChannelDialog editSimpleIPTVChannelDialog(View anchor, SimpleIPTVChannel channel, List<SimpleIPTVChannel> existingChannels, DialogBase.IOnConfirm onConfirm) {
        SimpleIPTVChannelDialog dialog = new SimpleIPTVChannelDialog(anchor.getContext());
        dialog.init(channel, existingChannels, onConfirm);
        dialog.show(anchor);
        return dialog;
    }

    public static TermsAndConditionsDialog termsAndConditionsDialog(View anchor, String message, TermsAndConditionsDialog.IOnYesNo onYesNo) {
        TermsAndConditionsDialog dialog = new TermsAndConditionsDialog(anchor.getContext());
        dialog.init(message, onYesNo);
        dialog.show(anchor);
        dialog.bringToFront();
        return dialog;
    }

    public static SpinBikeCalibrationDialog spindownWelcomeDialog(View anchor) {
        SpinBikeCalibrationDialog dialog = new SpinBikeCalibrationDialog(anchor.getContext());
        dialog.init();
        dialog.show(anchor);
        dialog.bringToFront();
        return dialog;
    }

    public static LoggingOptions showLoggingOptions(View anchor) {
        LoggingOptions dialog = new LoggingOptions(anchor.getContext());
        dialog.show(anchor);
        dialog.bringToFront();
        return dialog;
    }

}
