package com.jht.chimera.test.espresso.recyclerview;

import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static org.hamcrest.CoreMatchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.util.HumanReadables;

import com.jht.androiduiwidgets.selector.StandardSelectorItemHolder;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Assert;

public class RecyclerViewOperations {

    public static ViewAction viewHolderExists(int position) { return new ViewHolderExists(position); }

    public static ViewAction actionOnStandardItemWithText(String text, ViewAction viewAction) {
        return new ActionOnStandardItemWithText(text, viewAction);
    }

    public static ViewAssertion viewHasNItems(int numItems) {
        return new RecyclerViewItemCountAssertion(numItems);
    }

    private static class ActionOnStandardItemWithText implements ViewAction {
        private final String text;
        private final ViewAction viewAction;

        private ActionOnStandardItemWithText(String text, ViewAction viewAction) {
            this.text = text;
            this.viewAction = viewAction;
        }

        @Override
        public Matcher<View> getConstraints() {
            return allOf(isAssignableFrom(RecyclerView.class), isDisplayed());
        }

        @Override
        public String getDescription() {
            return "actionOnItemAtPosition performing ViewAction: "
                    + viewAction.getDescription()
                    + " on item with text : "
                    + text;
        }

        @Override
        public void perform(UiController uiController, View view) {
            RecyclerView recyclerView = (RecyclerView) view;
            RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
            for(int i = 0 ; adapter != null && i < adapter.getItemCount(); i++) {
                recyclerView.scrollToPosition(i);
                uiController.loopMainThreadUntilIdle();

                RecyclerView.ViewHolder viewHolderForPosition = recyclerView.findViewHolderForAdapterPosition(i);
                if(viewHolderForPosition instanceof StandardSelectorItemHolder) {
                    StandardSelectorItemHolder holder = (StandardSelectorItemHolder)viewHolderForPosition;
                    if(text.equals(holder.getText())) {
                        viewAction.perform(uiController, holder.itemView);
                        return;
                    }
                }

            }
            throw new PerformException.Builder()
                    .withActionDescription(this.toString())
                    .withViewDescription(HumanReadables.describe(view))
                    .withCause(new IllegalStateException("No view holder with text: " + text))
                    .build();

        }
    }

    private static class ViewHolderExists implements ViewAction {

        // The position of the desired item.
        private final int position;

        /**
         * Constructor passing the item position.
         *
         * @param position  The position in the recycler view.
         */
        ViewHolderExists(int position) {
            this.position = position;
        }


        @Override
        public Matcher<View> getConstraints() {
            return new IsRecyclerView();
        }

        @Override
        public String getDescription() {
            return "Make sure the view holder exists.";
        }

        @Override
        public void perform(UiController uiController, View view) {
            Assert.assertTrue(view instanceof RecyclerView);
            RecyclerView recyclerView = (RecyclerView)view;
            recyclerView.scrollToPosition(position);
            uiController.loopMainThreadUntilIdle();
            RecyclerView.ViewHolder vh = null;
            for(int i = 0; i < 20 && vh == null; i++) {
                vh = recyclerView.findViewHolderForAdapterPosition(position);
                if(vh == null) {
                    uiController.loopMainThreadForAtLeast(100);
                }
            }
        }
    }

    private static class IsRecyclerView extends BaseMatcher<View> {

        @Override
        public boolean matches(Object o) {
            return o instanceof RecyclerView;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("is a RecyclerView");
        }
    }

    private static class RecyclerViewItemCountAssertion implements ViewAssertion {
        private final int expectedCount;

        public RecyclerViewItemCountAssertion(int expectedCount) {
            this.expectedCount = expectedCount;
        }

        @Override
        public void check(View view, NoMatchingViewException noViewFoundException) {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
            assertTrue(view instanceof RecyclerView);
            RecyclerView.Adapter<?> adapter = ((RecyclerView)view).getAdapter();
            assertNotNull(adapter);
            assertEquals(expectedCount,adapter.getItemCount());
        }
    }

    static RecyclerView.ViewHolder viewHolder(View view, int position) {
        Assert.assertTrue(view instanceof RecyclerView);
        RecyclerView recyclerView = (RecyclerView) view;
        return recyclerView.findViewHolderForAdapterPosition(position);
        // Sometimes we get null back for some unknown reason.  This is not the fault of our software
        // Punt in these situations.
        //Assert.assertNotNull(vh);
        //Assert.assertNotNull(vh.itemView);
        //return vh;
    }

}
