package com.jht.chimera.ui.mainapp.workoutapps;

import static com.jht.chimera.lib.language.StringLookup.getString;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.SystemClock;
import android.util.Log;
import android.util.StateSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TiersKeys;
import com.jht.chimera.lib.common.i18n.LanguageProperty;
import com.jht.chimera.lib.download.MiscUtilHelper;
import com.jht.chimera.lib.widget.ScaleView;
import com.jht.chimera.lib.widget.dialog.MessageBoxDialog;
import com.jht.chimera.lib.workoutapps.AppIcon;
import com.jht.chimera.service.tiers.FeatureUpgrade;
import com.jht.chimera.service.tiers.PremiumFeature;
import com.jht.chimera.service.tiers.PremiumFeatureSet;
import com.jht.chimera.service.tiers.UpgradeSkin;
import com.jht.chimera.service.tiers.dialogs.BusyStatusDialog;
import com.jht.chimera.service.tiers.dialogs.MultiChoiceDialog;
import com.jht.chimera.ui.machinesettings.JHTTypes;
import com.jht.chimera.ui.machinesettings.dialogs.ProjectDialogFactory;
import com.jht.datamatrixlib.DataMatrix;

import java.io.File;
import java.util.List;

public class AppAdapter extends ArrayAdapter<AppIcon> {
    private static final String TAG = "AppAdapter";
    private final int resource;
    private final int textViewResourceId;
    protected List<AppIcon> appIconList;
    private static long sTimestamp = 0;
    private BusyStatusDialog busyStatusDialog;

    protected AppAdapter(Context context, int resource, int textViewResourceId, List<AppIcon> appIconList) {
        super(context, resource, textViewResourceId, appIconList);
        this.resource = resource;
        this.textViewResourceId = textViewResourceId;
        this.appIconList = appIconList;
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(resource, parent, false);
            ScaleView.scaleContents(view);
        }

        TextView textView = view.findViewById(textViewResourceId);

        textView.setText(appIconList.get(position).getAppName());

        ImageView button = view.findViewById(R.id.appIconImage);

        if (appIconList.get(position).appInfo.state == JHTTypes.FEATURE_STATE.FEATURE_DISABLED) {
            button.setActivated(true);
            textView.setActivated(true);
        } else if (appIconList.get(position).isDisabled()) {
            button.setActivated(true);
            textView.setActivated(true);
        } else if (appIconList.get(position).appInfo.state == JHTTypes.FEATURE_STATE.FEATURE_ENABLED) {
            button.setActivated(false);
            textView.setActivated(false);
        }

        //button.setIconDrawable(appIconList.get(position).getAppIcon());
        button.setBackground(appIconList.get(position).getAppIcon());
        button.setContentDescription(appIconList.get(position).getIconId());


        button.setOnClickListener(v -> {

            // only allow one click per 1 second
            if (SystemClock.uptimeMillis() - sTimestamp < 1000) {
                return;
            }
            sTimestamp = SystemClock.uptimeMillis();

            try {
                if (!button.isActivated()) {
                    AppIcon appIcon = appIconList.get(position);
                    LaunchAppUtil.launchAppByIcon(getContext(), appIcon.appInfo, v);
                } else {
                    boolean isDisabled = appIconList.get(position).isDisabled();
                    if (isDisabled) {
                        MessageBoxDialog messageBoxDialog = new MessageBoxDialog(v.getContext());
                        messageBoxDialog.setButtonsEnabled(false);
                        messageBoxDialog.setTitle(v.getContext().getString(R.string.app_disabled_tc));
                        messageBoxDialog.setMessage(v.getContext().getString(R.string.app_disabled_pg));
                        messageBoxDialog.show();
                    } else {
                        Log.d(TAG, "we clicked on a disabled feature");
                        if (DataMatrix.getBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL)) {
                            PremiumFeatureSet set = DataMatrix.getParcelable(TiersKeys.DATA_GUEST_FEATURE_SET_PARCELABLE, PremiumFeatureSet.class);
                            if (set != null) {
                                initiateUpgradeProcess(v, set, appIconList.get(position));
                            } else {
                                showDefaultUpgradeDialog();
                            }
                        } else {
                            Log.d(TAG, "we clicked on a premium feature");
                            PremiumFeatureSet set = DataMatrix.getParcelable(TiersKeys.DATA_FEATURE_SET_PARCELABLE, PremiumFeatureSet.class);
                            if (set != null) {
                                initiateUpgradeProcess(v, set, appIconList.get(position));
                            } else {
                                showDefaultUpgradeDialog();
                            }
                        }

                        Log.d(TAG, appIconList.get(position).getAppName() + " is DISABLED");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception", e);
            }
        });
        return view;
    }


    private void initiateUpgradeProcess(View iconImageButton, PremiumFeatureSet set, AppIcon appIcon) {

        Log.e(TAG, "in initiateUpgradeProcess");

        int languageId = DataMatrix.getInt(SettingKeys.General.DATA_CONSOLE_LANGUAGE_ENUM_INT);
        LanguageProperty languageProperty = LanguageProperty.getLanguagePropertyFromIndex(languageId);
        String region = languageProperty.locale;

        if (set == null || set.isEmpty()) {
            showDefaultUpgradeDialog();
            return;
        }

        PremiumFeature feature = set.getFeature(appIcon.getUuid(), JHTTypes.FEATURE_TYPE.FEATURE_TYPE_APP);
        if (feature == null) {
            showDefaultUpgradeDialog();
            return;
        }

        List<FeatureUpgrade> upgrades = set.getUpgrades(feature);
        if (upgrades.size() <= 0) {
            showDefaultUpgradeDialog();
            return;
        }

        if (upgrades.size() == 1 && upgrades.get(0).launchWebView()) {
            String url = upgrades.get(0).url();
            Log.e(TAG, "url: " + url);
            ProjectDialogFactory.webViewDialogBox(iconImageButton, url);
            return;
        }


        MultiChoiceDialog multiChoiceDialog = new MultiChoiceDialog(iconImageButton.getContext());
        multiChoiceDialog.addUpgrades(upgrades);
        multiChoiceDialog.setTitle(getContext().getResources().getString(R.string.upgrade_tc));

        UpgradeSkin skin = set.getUpgradeSkin();
        if (skin == null) {
            multiChoiceDialog.setCustomBackgroundColor(Color.BLACK);
            multiChoiceDialog.setButton0Text(getString(R.string.upgrade_tc, getContext()));
            multiChoiceDialog.setButton1Text(getString(R.string.cancel_tc, getContext()));

        } else {
            String title = skin.getTitle(region);
            if (title != null && !title.equals("")) {
                multiChoiceDialog.setTitle(title);
            }
            //Problem: getBackgroundPath is never set since the xml only has a color and no bitmap. Gotta check if null first..
            if (skin.getBackgroundPath() != null) {
                Bitmap backgroundBitmap = MiscUtilHelper.decodeBackgroundImage(new File(skin.getBackgroundPath()));
                if (backgroundBitmap != null) {
                    BitmapDrawable backgroundDrawable = new BitmapDrawable(getContext().getResources(), backgroundBitmap);
                    multiChoiceDialog.setCustomBackgroundDrawable(backgroundDrawable);
                } else {
                    multiChoiceDialog.setCustomBackgroundColor(skin.backgroundColor());
                }
            } else {
                multiChoiceDialog.setCustomBackgroundColor(skin.backgroundColor());
            }


            StateListDrawable stateListDrawableOk = getButtonDrawable(
                    MiscUtilHelper.decodeBackgroundImage(new File(skin.getOkButtonPath())),
                    MiscUtilHelper.decodeBackgroundImage(new File(skin.getOkButtonPressedPath())));
            if (stateListDrawableOk != null) {
                multiChoiceDialog.setButton0BackgroundDrawable(stateListDrawableOk);
                multiChoiceDialog.setButton0Text("");
            } else {
                multiChoiceDialog.setButton0Text(getString(R.string.upgrade_tc, getContext()));
            }

            StateListDrawable stateListDrawableCancel = getButtonDrawable(
                    MiscUtilHelper.decodeBackgroundImage(new File(skin.getCancelButtonPath())),
                    MiscUtilHelper.decodeBackgroundImage(new File(skin.getCancelButtonPressedPath())));
            if (stateListDrawableCancel != null) {
                Log.d(TAG, "UserUpgradeChoreographer#begin(): got custom skin cancel stateListDrawable");
                multiChoiceDialog.setButton1BackgroundDrawable(stateListDrawableCancel);
                multiChoiceDialog.setButton1Text("");
            } else {
                multiChoiceDialog.setButton1Text(getString(R.string.cancel_tc, getContext()));
            }
        }

        multiChoiceDialog.setButton0OnClickListener(v -> {
            if (multiChoiceDialog.getSelectedChoice() == -1) {
                Log.e(TAG, "trying to upgrade nothing selected");
            } else {
                FeatureUpgrade upgrade = upgrades.get(multiChoiceDialog.getSelectedChoice());
                Log.e(TAG, "found this upgrade: " + upgrade.getTitle(languageProperty.locale));

                if (upgrade.launchWebView()) {
                    Log.e(TAG, "this upgrade has a webview: " + upgrade.url());
                    multiChoiceDialog.hide();
                    ProjectDialogFactory.webViewDialogBox(iconImageButton, upgrade.url());
                } else {
                   ProjectDialogFactory.confirm(iconImageButton, getContext().getResources().getString(R.string.confirm_upgrade_pg), () -> {
                        multiChoiceDialog.hide();
                        busyStatusDialog = new BusyStatusDialog(iconImageButton.getContext());
                        busyStatusDialog.setStatusMessage(getContext().getResources().getString(R.string.upgrade_in_progress_pg));
                        busyStatusDialog.show(iconImageButton);
                        DataMatrix.putParcelable(TiersKeys.DATA_UPGRADE_PARCELABLE, upgrade);
                    }, null);
                }
            }

        });

        multiChoiceDialog.setButton1OnClickListener(v -> multiChoiceDialog.hide());

        multiChoiceDialog.show(iconImageButton);


    }

    private StateListDrawable getButtonDrawable(Bitmap unpressedBitmap, Bitmap pressedBitmap) {
        if (unpressedBitmap == null || pressedBitmap == null) {
            return null;
        }
        StateListDrawable stateListDrawable = new StateListDrawable();
        BitmapDrawable unpressedDrawable = new BitmapDrawable(getContext().getResources(), unpressedBitmap);
        BitmapDrawable pressedDrawable = new BitmapDrawable(getContext().getResources(), pressedBitmap);
        stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, pressedDrawable);
        stateListDrawable.addState(StateSet.WILD_CARD, unpressedDrawable);
        return stateListDrawable;
    }

    private void showDefaultUpgradeDialog() {
        MessageBoxDialog messageBoxDialog = new MessageBoxDialog(getContext());
        messageBoxDialog.setTitle(getContext().getString(R.string.upgrade_unavailable_tc));
        messageBoxDialog.setMessage(getContext().getString(R.string.upgrade_unavailable_pg));
        messageBoxDialog.setCanceledOnTouchOutside(true);
        messageBoxDialog.show();
    }

}
