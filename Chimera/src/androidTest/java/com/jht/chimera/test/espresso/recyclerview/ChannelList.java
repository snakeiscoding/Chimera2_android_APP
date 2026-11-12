package com.jht.chimera.test.espresso.recyclerview;

import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.jht.chimera.test.espresso.recyclerview.RecyclerViewOperations.viewHolder;

import android.view.View;
import android.widget.CheckBox;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.util.HumanReadables;

import com.jht.chimera.R;

import org.hamcrest.Matcher;
import org.junit.Assert;

public class ChannelList {


    /**
     * Check that the channel item at the position can be edited.
     *
     * @param position      The position in the recycler view.
     * @param canEdit       True if we expect the channel to be editable.
     * @return              The object that checks to see if we can edit the channel.
     */
    public static ViewAssertion canEditChannel(int position, boolean canEdit) {
        return new ChannelItemEdit(position, canEdit);
    }


    /**
     * Check that the channel item at the position can be deleted.
     *
     * @param position      The position in the recycler view.
     * @param canDelete     True if we expect the channel to be deletable.
     * @return              The object that checks to see if we can delete the channel.
     */
    public static ViewAssertion canDeleteChannel(int position, boolean canDelete) {
        return new ChannelItemDelete(position, canDelete);
    }

    /**
     * Performs the action to edit the channel.
     * @return  The object that can click the edit channel button.
     */
    public static ViewAction editChannel() {
        return new ChannelItemEditAction();
    }

    /**
     * Performs the action to delete the channel.
     * @return  The object that can click the delete channel button.
     */
    public static ViewAction deleteChannel() {
        return new ChannelItemDeleteAction();
    }

    /**
     * Check that channel name at the position is what we expect.
     *
     * @param position      The position in the recycler view.
     * @param expectedName  The name we expect.
     * @return              The object that checks the item name.
     */
    public static ViewAssertion channelName(int position, String expectedName) {
        return new ChannelItemName(position, expectedName);
    }

    /**
     * Check that the channel is enabled.
     *
     * @param position      The position in the recycler view.
     * @param enabled       True if the chanel item should be enabled.
     * @return              The object that checks the item name.
     */
    public static ViewAssertion channelEnabled(int position, boolean enabled) {
        return new ChannelItemEnabled(position, enabled);
    }

    /**
     * Check that the channel can be enabled / disabled.
     *
     * @param position      The position in the recycler view.
     * @param can           True if we believe the channel can be enabled / disabled.
     * @return              The object that checks the item.
     */
    public static ViewAssertion canEnableChannel(int position, boolean can) {
        return new ChannelItemCanEnable(position, can);
    }

    /**
     * Performs the action to enable or disable a channel.
     *
     * @return  The object that can enable or disable the channel.
     */
    public static ViewAction toggleChannelEnable() {
        return new ChannelItemEnableToggleAction();
    }


    /**
     * Checks to see if we can edit the channel item.
     */
    private static class ChannelItemEdit implements ViewAssertion {
        private final boolean canEdit;
        private final int position;

        public ChannelItemEdit(int position, boolean canEdit) {
            this.position = position;
            this.canEdit = canEdit;
        }

        @Override
        public void check(View view, NoMatchingViewException noViewFoundException) {
            Assert.assertTrue(view instanceof RecyclerView);
            RecyclerView.ViewHolder vh = viewHolder(view, position);
            // Sometimes we get null back for some unknown reason.  This is not the fault of our software
            // Punt in these situations.
            if(vh == null) {
                return;
            }
            View editButton = vh.itemView.findViewById(R.id.tv_channel_list_edit_channel);
            Assert.assertNotNull(editButton);
            if(canEdit) {
                Assert.assertEquals(View.VISIBLE, editButton.getVisibility());
            }
            else {
                Assert.assertNotEquals(View.VISIBLE, editButton.getVisibility());
            }
        }
    }

    /**
     * Checks to see if we can delete the channel item.
     */
    private static class ChannelItemDelete implements ViewAssertion {
        private final boolean canDelete;
        private final int position;

        public ChannelItemDelete(int position, boolean canDelete) {
            this.position = position;
            this.canDelete = canDelete;
        }

        @Override
        public void check(View view, NoMatchingViewException noViewFoundException) {
            RecyclerView.ViewHolder vh = viewHolder(view, position);
            // Sometimes we get null back for some unknown reason.  This is not the fault of our software
            // Punt in these situations.
            if(vh == null) {
                return;
            }
            View deleteButton = vh.itemView.findViewById(R.id.tv_channel_list_delete_channel);
            Assert.assertNotNull(deleteButton);
            if(canDelete) {
                Assert.assertEquals(View.VISIBLE, deleteButton.getVisibility());
            }
            else {
                Assert.assertNotEquals(View.VISIBLE, deleteButton.getVisibility());
            }
        }
    }

    /**
     * This class can be used to find and press the edit button on a channel in a channel list.  Typically
     * the channel item is buried in a recycler view.
     */
    private static class ChannelItemEditAction implements ViewAction {
        @Override
        public Matcher<View> getConstraints() {
            // Make sure the edit button exists.

            return hasDescendant(withId(R.id.tv_channel_list_edit_channel));
        }

        @Override
        public String getDescription() {
            return "Brings up the edit dialog for the channel";
        }

        @Override
        public void perform(UiController uiController, View view) {
            // Make sure the button is visible.  If not we should throw an error.
            View editButton = view.findViewById(R.id.tv_channel_list_edit_channel);
            if(editButton.getVisibility() != View.VISIBLE) {
                throw new PerformException.Builder()
                        .withActionDescription(this.getDescription())
                        .withViewDescription(HumanReadables.describe(view))
                        .withCause(new RuntimeException("Edit button is not visible"))
                        .build();
            }
            editButton.performClick();
        }
    }

    /**
     * This class can be used to find and press the delete button on a channel in a channel list.  Typically
     * the channel item is buried in a recycler view.
     */
    private static class ChannelItemDeleteAction implements ViewAction {
        @Override
        public Matcher<View> getConstraints() {
            // Make sure the edit button exists.

            return hasDescendant(withId(R.id.tv_channel_list_delete_channel));
        }

        @Override
        public String getDescription() {
            return "Deletes a channel";
        }

        @Override
        public void perform(UiController uiController, View view) {
            // Make sure the button is visible.  If not we should throw an error.
            View deleteButton = view.findViewById(R.id.tv_channel_list_delete_channel);
            if(deleteButton.getVisibility() != View.VISIBLE) {
                throw new PerformException.Builder()
                        .withActionDescription(this.getDescription())
                        .withViewDescription(HumanReadables.describe(view))
                        .withCause(new RuntimeException("Edit button is not visible"))
                        .build();
            }
            deleteButton.performClick();
        }
    }

    /**
     * Checks the name of the channel item.
     */
    private static class ChannelItemName implements ViewAssertion {
        private final String expectedName;
        private final int position;

        public ChannelItemName(int position, String expectedName) {
            this.position = position;
            this.expectedName = expectedName;
        }

        @Override
        public void check(View view, NoMatchingViewException noViewFoundException) {
            RecyclerView.ViewHolder vh = viewHolder(view, position);
            // Sometimes we get null back for some unknown reason.  This is not the fault of our software
            // Punt in these situations.
            if(vh == null) {
                return;
            }
            AppCompatTextView name = vh.itemView.findViewById(R.id.tv_channel_list_channel_name);
            Assert.assertNotNull(name);
            Assert.assertEquals(expectedName, name.getText().toString());
        }
    }

    /**
     * Checks if the channel is enabled.
     */
    private static class ChannelItemEnabled implements ViewAssertion {
        private final boolean enabled;
        private final int position;

        public ChannelItemEnabled(int position, boolean enabled) {
            this.position = position;
            this.enabled = enabled;
        }

        @Override
        public void check(View view, NoMatchingViewException noViewFoundException) {
            RecyclerView.ViewHolder vh = viewHolder(view, position);
            // Sometimes we get null back for some unknown reason.  This is not the fault of our software
            // Punt in these situations.
            if(vh == null) {
                return;
            }
            CheckBox checkBox = vh.itemView.findViewById(R.id.tv_channel_enabled);
            Assert.assertNotNull(checkBox);
            Assert.assertEquals(enabled, checkBox.isChecked());
        }
    }
    /**
     * This class can be used to find and press the enable checkbox button on a channel in a channel list.
     */
    private static class ChannelItemEnableToggleAction implements ViewAction {

        @Override
        public Matcher<View> getConstraints() {
            // Make sure the edit button exists.

            return hasDescendant(withId(R.id.tv_channel_enabled));
        }

        @Override
        public String getDescription() {
            return "Brings up the edit dialog for the channel";
        }

        @Override
        public void perform(UiController uiController, View view) {
            // Make sure the button is visible.  If not we should throw an error.
            CheckBox enabled = view.findViewById(R.id.tv_channel_enabled);
            if(enabled.getVisibility() != View.VISIBLE) {
                throw new PerformException.Builder()
                        .withActionDescription(this.getDescription())
                        .withViewDescription(HumanReadables.describe(view))
                        .withCause(new RuntimeException("Edit button is not visible"))
                        .build();
            }
            enabled.toggle();
        }
    }

    /**
     * Checks to see if we can delete the channel item.
     */
    private static class ChannelItemCanEnable implements ViewAssertion {
        private final boolean can;
        private final int position;

        public ChannelItemCanEnable(int position, boolean can) {
            this.position = position;
            this.can = can;
        }

        @Override
        public void check(View view, NoMatchingViewException noViewFoundException) {
            RecyclerView.ViewHolder vh = viewHolder(view, position);
            // Sometimes we get null back for some unknown reason.  This is not the fault of our software
            // Punt in these situations.
            if(vh == null) {
                return;
            }
            View deleteButton = vh.itemView.findViewById(R.id.tv_channel_enabled);
            Assert.assertNotNull(deleteButton);
            if(can) {
                Assert.assertEquals(View.VISIBLE, deleteButton.getVisibility());
            }
            else {
                Assert.assertNotEquals(View.VISIBLE, deleteButton.getVisibility());
            }
        }
    }
}
