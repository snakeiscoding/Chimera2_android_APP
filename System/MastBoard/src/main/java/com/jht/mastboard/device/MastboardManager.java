package com.jht.mastboard.device;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.jht.mastboard.data.LEDItem;
import com.jht.mastboard.lib.PanelSide;
import com.jht.util.ContextHelper;

import java.util.Objects;

public class MastboardManager {
    private static final String TAG = "MastboardManager";
    private final Mastboard mastboard = new Mastboard(Objects.requireNonNull(ContextHelper.INSTANCE.getContext()));

    private MastboardManager() {
    }

    private static class SingletonHolder {
        private static final MastboardManager INSTANCE = new MastboardManager();
    }

    public static MastboardManager getInstance() {
        return MastboardManager.SingletonHolder.INSTANCE;
    }

    public Mastboard getMastboard() {
        return mastboard;
    }

    public static void drawBitmap(Bitmap bitmap, @NonNull final PanelSide side) {
        getInstance().getMastboard().drawBitmap(bitmap, side);
    }

    public static void setColorLed(LEDItem ledItem) {
        switch (ledItem.ledZone) {
            case ALL:
                getInstance().getMastboard().setAllLEDColor(ledItem.color);
                break;
            case BACK:
                getInstance().getMastboard().setConsoleLEDColor(ledItem.color);
                break;
            case STOP:
                getInstance().getMastboard().setButton1LEDColor(ledItem.color);
                break;
            case PLAY:
                getInstance().getMastboard().setButton2LEDColor(ledItem.color);
                break;
            case SIDE:
                getInstance().getMastboard().setSideLEDColor(ledItem.color);
                break;
            case ALL_EXCEPT_PLAY_STOP:
                getInstance().getMastboard().setConsoleLEDColor(ledItem.color);
                getInstance().getMastboard().setSideLEDColor(ledItem.color);
                break;
            default:
                Log.i(TAG, "Zone was unknown: " + ledItem.ledZone);
                break;
        }
    }

    public static void buzzHapticMotor(int effectID, @NonNull final PanelSide side) {
        getInstance().getMastboard().buzz(effectID, side);
    }

    public static void updateBacklight(int percent) {
        if (percent > 0) {
            if (percent > 100) percent = 100;
            // need to invert percentage because it runs inverted to expected
            percent = 100 - percent;
            getInstance().getMastboard().setLCDBacklight(percent);
        } else {
            getInstance().getMastboard().setLCDBacklight(0);
        }
    }

    public static void updateLEDBrightness(int percent) {
        getInstance().getMastboard().setLEDBrightness(percent);
    }
}
