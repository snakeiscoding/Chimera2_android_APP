package com.jht.chimera.test.espresso.userinput;

import android.view.View;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.util.HumanReadables;

import com.jht.androiduiwidgets.edittext.UserTextInput;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;

public class UserInputActions {



    public static ViewAssertion checkUserInputText(String expectedText) {
        return new CheckUserInputText(expectedText);
    }

    public static ViewAction setUserInputText(String textToSet) {
        return new UserInputSetText(textToSet);
    }

    static final class IsUserInput extends TypeSafeMatcher<View> {

        @Override
        public void describeTo(Description description) {
            description.appendText("is UserInput ");
        }

        @Override
        public boolean matchesSafely(final View view) {
            return view instanceof UserTextInput;
        }
    }

    /**
     * This class can be used to find and press the edit button on a channel in a channel list.  Typically
     * the channel item is buried in a recycler view.
     */
    private static class UserInputSetText implements ViewAction {
        private final String textToSet;

        UserInputSetText(String textToSet) {
            this.textToSet = textToSet;
        }

        @Override
        public Matcher<View> getConstraints() {
            // Make sure the edit button exists.

            return new IsUserInput();
        }

        @Override
        public String getDescription() {
            return "Set the text in the user input";
        }

        @Override
        public void perform(UiController uiController, View view) {
            if(view instanceof UserTextInput) {
                ((UserTextInput)view).setText(textToSet);
            }
            else {
                throw new PerformException.Builder()
                        .withActionDescription(this.getDescription())
                        .withViewDescription(HumanReadables.describe(view))
                        .withCause(new RuntimeException("Not a user input"))
                        .build();
            }
        }
    }

    private static class CheckUserInputText implements ViewAssertion {
        private final String expectedText;

        public CheckUserInputText(String expectedText) {
            this.expectedText = expectedText;
        }

        @Override
        public void check(View view, NoMatchingViewException noViewFoundException) {
            Assert.assertTrue(view instanceof UserTextInput);
            Assert.assertEquals(expectedText, ((UserTextInput)view).getText());
        }
    }
}
