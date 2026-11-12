package com.jht.chimera.test.espresso;

import android.content.ContentResolver;
import android.provider.Settings;

import androidx.test.platform.app.InstrumentationRegistry;

public class SystemAnimationHelper {
    private final int transtionAnimationScale;
    private final int windowAnimationScale;
    private final int animatorDurationScale;
    private final ContentResolver contentResolver = InstrumentationRegistry.getInstrumentation().getTargetContext().getContentResolver();

    public SystemAnimationHelper() {

        transtionAnimationScale = Settings.Global.getInt(contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 0);
        windowAnimationScale = Settings.Global.getInt(contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE, 0);
        animatorDurationScale = Settings.Global.getInt(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0);

        Settings.Global.putInt(contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 0);
        Settings.Global.putInt(contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE, 0);
        Settings.Global.putInt(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0);
    }

    public void restore() {

        Settings.Global.putInt(contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, transtionAnimationScale);
        Settings.Global.putInt(contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE, windowAnimationScale);
        Settings.Global.putInt(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, animatorDurationScale);
    }
}
