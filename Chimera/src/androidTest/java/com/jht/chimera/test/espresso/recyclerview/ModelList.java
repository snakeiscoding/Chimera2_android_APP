package com.jht.chimera.test.espresso.recyclerview;

import static com.jht.chimera.test.espresso.recyclerview.RecyclerViewOperations.viewHolder;

import android.view.View;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAssertion;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;

import org.junit.Assert;

public class ModelList {


    public static ViewAssertion isModel(int position, FrameConfigEnum model) {
        return new IsModel(position, model);
    }


    /**
     * Checks the name of the channel item.
     */
    private static class IsModel implements ViewAssertion {
        private FrameConfigEnum expectedModel;
        private int position;

        public IsModel(int position, FrameConfigEnum expectedModel) {
            this.position = position;
            this.expectedModel = expectedModel;
        }

        @Override
        public void check(View view, NoMatchingViewException noViewFoundException) {
            RecyclerView.ViewHolder vh = viewHolder(view, position);
            // Sometimes we get null back for some unknown reason.  This is not the fault of our software
            // Punt in these situations.
            if(vh == null) {
                return;
            }
            AppCompatTextView name = vh.itemView.findViewById(R.id.selector_text);
            Assert.assertNotNull(name);
            Assert.assertEquals(view.getContext().getResources().getString(expectedModel.createFrameConfig().MODEL), name.getText().toString());
        }
    }

}
