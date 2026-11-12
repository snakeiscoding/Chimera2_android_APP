package com.jht.chimera.test.espresso.view;

import android.view.View;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAssertion;

import org.junit.Assert;

public class ViewChecks {

    public static ViewAssertion isActive(boolean isActive) {
        return new ViewIsActivated(isActive);
    }

    private static class ViewIsActivated implements ViewAssertion {

        boolean isActive;

        public ViewIsActivated(boolean isActive) {
            this.isActive = isActive;
        }

        @Override
        public void check(View view, NoMatchingViewException noViewFoundException) {
            Assert.assertEquals(isActive, view.isActivated());
        }
    }
}
