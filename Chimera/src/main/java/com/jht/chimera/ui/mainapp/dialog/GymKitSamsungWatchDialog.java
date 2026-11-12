package com.jht.chimera.ui.mainapp.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ChimeraEnums;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.mastboard.data.UiEventKeys;
import com.jht.datamatrixlib.DataMatrix;

public class GymKitSamsungWatchDialog extends Dialog {

    private TextView title;
    private TextView message;
    private ImageView badge;
    private final boolean isApple;

    public GymKitSamsungWatchDialog(Context context, boolean isApple) {
        super(context);
        this.isApple = isApple;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setType(ChimeraEnums.TYPE_SYSTEM_ERROR); // TYPE_SYSTEM_ALERT causes improper behavior with newly added gravity below
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        setContentView(R.layout.gymkit_samsung_watch_dialog);

        title = findViewById(R.id.watch_dialog_title);
        message = findViewById(R.id.watch_dialog_message);
        badge = findViewById(R.id.watch_dialog_badge);
        findViewById(R.id.watch_dialog_close_button).setOnClickListener(v -> dismiss());
        ImageView connectWatchImageView = findViewById(R.id.watch_dialog_icon_symbol);

        // probably not needed? projectOne is new UI
        if (ProductConfig.isProjectOne()) {
            switch (FrameConfigManager.getInstance().getFrameConfig().machineType) {
                case Stepper:
                case Climbmill:
                    connectWatchImageView.setImageResource(R.drawable.connect_watch_climbmill_p1);
                    break;
                case Ascent:
                    connectWatchImageView.setImageResource(R.drawable.connect_watch_ascent_p1);
                    break;
                case RecumbentBike:
                    connectWatchImageView.setImageResource(R.drawable.connect_watch_recumbent_bike_p1);
                    break;
                case UprightBike:
                    connectWatchImageView.setImageResource(R.drawable.connect_watch_upright_bike_p1);
                    break;
                case Treadmill:
                default:
                    connectWatchImageView.setImageResource(R.drawable.connect_watch_treadmill_p1);
                    break;
            }
        } else {
            switch (ProductConfig.getConsoleType()) {
                case athena:
                case athena2:
                    connectWatchImageView.setImageResource(R.drawable.connect_watch_athena);
                    break;
                case chimera_16:
                case chimera2_16:
                    connectWatchImageView.setImageResource(R.drawable.connect_watch_touch);
                    break;
                case chimera_22:
                case chimera2_22:
                    connectWatchImageView.setImageResource(R.drawable.connect_watch_touchxl);
                    break;
                default:
                    if (isApple) {
                        connectWatchImageView.setImageResource(R.drawable.ic_help_hand_apple_watch);
                    } else {
                        connectWatchImageView.setImageResource(R.drawable.ic_help_hand_galaxy_watch);
                    }
                    break;
            }
        }

        WindowManager window = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = window.getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        int widthPixels = size.x;
        int heightPixels = size.y;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(getWindow().getAttributes());
        lp.width = (int) (widthPixels * .350);
        lp.height = (int) (heightPixels * .475);
        switch (ProductConfig.getConsoleType().rfidLocation) {
            case TOP_RIGHT:
                lp.gravity = Gravity.TOP | Gravity.RIGHT;
                break;
            case BOTTOM_LEFT:
                lp.gravity = Gravity.BOTTOM | Gravity.LEFT;
                break;
            default:
        }
        getWindow().setAttributes(lp);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isApple) {
            title.setText(R.string.connect_watch_apple_tc);
            message.setText(getContext().getResources().getString(R.string.apple_help_pg));
            badge.setImageResource(R.drawable.ic_apple_watch);
        } else {
            title.setText(R.string.connect_watch_samsung_tc);
            message.setText(getContext().getResources().getString(R.string.samsung_help_pg));
            badge.setImageResource(R.drawable.ic_samsung_watch);
        }
        // Publish message indicating that the connect to watch is opening.
        DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.OPEN_CONNECT_TO_WATCH.name());
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Publish message indicating that the connect to watch is closing.
        DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.CLOSE_CONNECT_TO_WATCH.name());
    }
}
