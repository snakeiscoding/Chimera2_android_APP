package com.jht.chimera.ui.machinesettings.pages;

import android.animation.Animator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jht.androidcommonalgorithms.timer.PeriodicTimer;
import com.jht.chimera.R;
import com.jht.chimera.service.hal.lib.hardware.ParkerDriver;
import com.jht.chimera.ui.machinesettings.viewpager.SettingPageBase;
import com.jht.datamatrixlib.DataMatrix;

import java.util.Arrays;

public class PortTest extends SettingPageBase {
    DataMatrix dataMatrix = new DataMatrix();
    private View view;
    private int fd;
    private final ParkerDriver driver = new ParkerDriver();
    private final PeriodicTimer readTimer = new PeriodicTimer();
    private static final String TAG = "PortTest";

    @Override
    protected View onCreate(Context context, ViewGroup container) {
        view = LayoutInflater.from(context).inflate(R.layout.port_test, null);
        container.addView(view);

        return view;
    }

    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);
        // TODO: this was specific to test chimera 3 but at some point it should reference productConfig.java to bring in port mapping dynamically.
        fd = driver.safeOpen("/dev/ttyS4"); //NON-NLS
        if (fd < 0) {
            Log.e(TAG, "Failed to open driver");
        }
        else {
            Log.d(TAG, "Open driver port：" + fd);
        }

        TextView hr_label = view.findViewById(R.id.hr_test_label_insert_a);

        byte[] data = new byte[] { 0x55, '\n' };

        view.findViewById(R.id.hr_port_test_button).setOnClickListener(v -> {
            hr_label.setText(R.string.hr_port_test_start_pg);
            int len = driver.write(fd, data);

            if (len <= 0) {
                hr_label.setText(R.string.hr_port_write_fail_pg);
                return;
            }

            new Thread(() -> {
                int ret = driver.waitForRead(fd, 500); // 最多等 500ms
                if (ret <= 0) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        // UI code
                        hr_label.setText(R.string.hr_port_test_fail_pg);
                    });
                    Log.e(TAG, "Loopback FAIL " + ret);
                    return;
                }

                byte[] buf = new byte[64];
                int rlen = driver.read(fd, buf);
                if (rlen <= 0) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        hr_label.setText(R.string.hr_port_test_read_fail_pg);
                    });
                    Log.e(TAG, "Loopback FAIL " + rlen);
                    return;
                }

                byte[] received = Arrays.copyOf(buf, rlen);
                boolean match = Arrays.equals(received, data);

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (match) {
                        hr_label.setText(R.string.hr_port_test_pass_pg);
                        Log.d(TAG, "Loopback PASS " + Arrays.toString(received));
                    } else {
                        hr_label.setText(R.string.hr_port_test_mismatch_pg);
                        Log.e(TAG, "Loopback FAIL " + Arrays.toString(received));
                    }
                });
            }).start();
        });
    }


    @Override
    public void hide(Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);
        readTimer.stop();
        driver.safeClose(fd);
        dataMatrix.finish();
    }
}

