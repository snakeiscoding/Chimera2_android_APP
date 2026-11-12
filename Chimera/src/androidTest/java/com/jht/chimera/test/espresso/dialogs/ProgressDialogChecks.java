package com.jht.chimera.test.espresso.dialogs;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import com.jht.androidcommonalgorithms.events.JHTDataNotification;
import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androiduiwidgets.dialog.ProgressDialog;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ProgressDialogChecks {

    public static ViewAction monitorProgressDialog(@NonNull List<String> expectedMessages, int timeout, boolean dismissWhenDone) {
        return new DialogMonitor(expectedMessages, timeout, dismissWhenDone);
    }

    public static Matcher<View> findProgressDialog() {
        return new IsProgressDialog();
    }


    private static class DialogMonitor implements ViewAction {

        private final int timeout;
        private final List<String> expectedMessages;
        private final boolean dismissWhenDone;

        DialogMonitor(@NonNull List<String> expectedMessages, int timeout, boolean dismissWhenDone) {
            this.expectedMessages = expectedMessages;
            this.timeout = timeout;
            this.dismissWhenDone = dismissWhenDone;
        }


        @Override
        public Matcher<View> getConstraints() {
            return new IsProgressDialog();
        }

        @Override
        public String getDescription() {
            return "Monitor the progress dialog make sure it shows what we expect.";
        }

        @Override
        public void perform(UiController uiController, View view) {

            Assert.assertTrue(view instanceof ProgressDialog);
            AtomicBoolean actionFinished = new AtomicBoolean(false);
            JHTDataNotification<String> failureMessage = new JHTDataNotification<>();
            AtomicBoolean timeoutReached = new AtomicBoolean(false);
            JHTTimer timeoutMonitor = new JHTTimer(timeout, () -> timeoutReached.set(true));
            timeoutMonitor.start();

            new Thread( () -> {
                AtomicInteger messageIndex = new AtomicInteger(0);
                ProgressDialog dialog = (ProgressDialog)view;

                if(expectedMessages.size() < 1 ) {
                    failureMessage.setData("Expected messages <= 1");
                    actionFinished.set(true);
                    return;
                }
                if(!expectedMessages.get(0).startsWith(dialog.getMessage())) {
                    failureMessage.setData("Expected message to start with \"" + expectedMessages.get(0) + "\", but got " + dialog.getMessage());
                    actionFinished.set(true);
                    return;
                }

                if(expectedMessages.size() == 1) {
                    actionFinished.set(true);
                    return;
                }

                dialog.monitorProgress((progress, done, error) -> {
                    if(actionFinished.get()) return;
                    if(dialog.getMessage() != null && messageIndex.get() < expectedMessages.size() && !dialog.getMessage().startsWith(expectedMessages.get(messageIndex.get()))) {
                        if(dialog.getMessage() != null) {
                            // We may have missed a message if things go too fast so check the next message if needed.
                            while(messageIndex.get() < expectedMessages.size() && !dialog.getMessage().startsWith(expectedMessages.get(messageIndex.get()))) {
                                messageIndex.incrementAndGet();
                            }
                            //Log.d("bdc_monitor", "Checking " + dialog.getMessage() + " " + expectedMessages.get(messageIndex.get()));
                            if(expectedMessages.size() > messageIndex.get()) {
                                if(!dialog.getMessage().startsWith(expectedMessages.get(messageIndex.get()))) {
                                    failureMessage.setData("Checking " + dialog.getMessage() + " " + expectedMessages.get(messageIndex.get()));
                                    actionFinished.set(true);
                                }
                            }
                        }
                    }
                    if(done) {
                        Assert.assertTrue(dialog.isOkShown());
                        if(dismissWhenDone) {
                            dialog.clickOk();
                        }
                        actionFinished.set(true);
                    }
                });

            }).start();
            while(!actionFinished.get() && !timeoutReached.get()) {
                uiController.loopMainThreadForAtLeast(100);
            }
            Assert.assertTrue(actionFinished.get());
            String result = failureMessage.waitForData(1);
            Assert.assertNull(result, result);
            Assert.assertFalse(timeoutReached.get());
        }
    }


    static final class IsProgressDialog extends TypeSafeMatcher<View> {

        @Override
        protected boolean matchesSafely(View view) {
            return view instanceof ProgressDialog;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("is Progress Dialog ");

        }
    }

}
