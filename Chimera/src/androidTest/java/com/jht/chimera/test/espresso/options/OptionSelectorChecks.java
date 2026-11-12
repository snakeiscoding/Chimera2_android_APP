package com.jht.chimera.test.espresso.options;

import android.view.View;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.util.HumanReadables;

import com.jht.androiduiwidgets.selector.OptionSelectorItem;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;

public class OptionSelectorChecks {

    public static ViewAssertion selectorHasOptions(String[] list) {
        return new OptionSelectionHasOptions(list);
    }

    public static ViewAction selectOption(int option) {
        return new OptionSelectorSelectItem(option);
    }

    private static class OptionSelectionHasOptions implements ViewAssertion {

        String[] list;

        public OptionSelectionHasOptions(String[] list) {
            this.list = list;
        }

        @Override
        public void check(View view, NoMatchingViewException noViewFoundException) {
            Assert.assertTrue(view instanceof OptionSelectorItem);
            OptionSelectorItem item = (OptionSelectorItem)view;

            if(list == null) {
                Assert.assertTrue(item.getOptions() == null || item.getOptions().length == 0);
            }
            else {
                Assert.assertEquals(list.length, item.getOptions().length);
                for(int i = 0; i < list.length; i++) {
                    Assert.assertEquals(list[i], item.getOptions()[i]);
                }
            }
        }
    }
    /**
     * This class can be used to find and press the edit button on a channel in a channel list.  Typically
     * the channel item is buried in a recycler view.
     */
    private static class OptionSelectorSelectItem implements ViewAction {
        private int item;

        OptionSelectorSelectItem(int item) {
            this.item = item;
        }

        @Override
        public Matcher<View> getConstraints() {
            // Make sure the edit button exists.

            return new IsOptionSelector();
        }

        @Override
        public String getDescription() {
            return "Select an option";
        }

        @Override
        public void perform(UiController uiController, View view) {
            if(view instanceof OptionSelectorItem) {
                ((OptionSelectorItem)view).selectItem(item);
            }
            else {
                throw new PerformException.Builder()
                        .withActionDescription(this.getDescription())
                        .withViewDescription(HumanReadables.describe(view))
                        .withCause(new RuntimeException("Not an option selector input"))
                        .build();
            }
        }
    }

    static final class IsOptionSelector extends TypeSafeMatcher<View> {

        @Override
        public void describeTo(Description description) {
            description.appendText("is OptionSelectorItem ");
        }

        @Override
        public boolean matchesSafely(final View view) {
            return view instanceof OptionSelectorItem;
        }
    }
}
