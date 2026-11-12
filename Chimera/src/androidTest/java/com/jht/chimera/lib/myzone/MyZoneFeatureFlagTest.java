package com.jht.chimera.lib.myzone;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.jht.chimera.R;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.platform.Emulator;
import com.jht.chimera.test.annotations.runner.MachineSettings;
import com.jht.chimera.test.espresso.recyclerview.RecyclerViewOperations;
import com.jht.chimera.test.fixtures.UITestFixture;
import com.jht.chimera.ui.machinesettings.EngineeringScreen;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MyZoneFeatureFlagTest {

   @Test
   @Emulator
   @Chimera
   @MachineSettings
   public void testFeatureFlag() {

      try (UITestFixture ignored = new UITestFixture(EngineeringScreen.class)) {
         onView(withId(R.id.engineering_page_selector)).perform(RecyclerViewOperations.actionOnStandardItemWithText("Experimental Features", androidx.test.espresso.action.ViewActions.click()));
      }
   }
}
