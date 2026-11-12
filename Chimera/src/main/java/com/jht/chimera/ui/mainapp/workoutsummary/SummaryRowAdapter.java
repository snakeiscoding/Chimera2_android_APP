package com.jht.chimera.ui.mainapp.workoutsummary;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.jht.chimera.R;
import com.jht.chimera.lib.widget.ScaleView;

import java.util.List;

/**
 * Contains the information for the rows of information in the workout summary.
 * <p>
 * Created by chris.foyer on 1/11/2018.
 */
@SuppressWarnings("WeakerAccess")
public class SummaryRowAdapter extends ArrayAdapter<SummaryRowItem> {
    private static final String TAG = "SummaryRowAdapter";
    private Context mContext;
    private int mResource;
    private List<SummaryRowItem> mRowItems;
    private String mTotalTextColor;

    public SummaryRowAdapter(Context context, int resource, List<SummaryRowItem> summaryRowItems, String totalTextColor) {
        super(context, resource, summaryRowItems);
        mContext = context;
        mResource = resource;
        mRowItems = summaryRowItems;
        mTotalTextColor = totalTextColor;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View rowView = convertView;
        if (null == rowView) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(mResource, parent, false);
            ScaleView.scaleContents(rowView);
        }

        LinearLayout summaryInfo = rowView.findViewById(R.id.summaryInfo);
        LinearLayout divider = rowView.findViewById(R.id.divider);

        if (mRowItems.get(position).isDivider()) {
            summaryInfo.setVisibility(View.VISIBLE);
            divider.setVisibility(View.VISIBLE);
        } else {
            summaryInfo.setVisibility(View.VISIBLE);
            divider.setVisibility(View.GONE);
        }

        if ((position + 1) % 2 != 0) {
            rowView.setBackgroundColor(Color.parseColor("#3F000000"));
        } else {
            rowView.setBackgroundColor(Color.parseColor("#7F000000"));
        }

        TextView rowLabel = rowView.findViewById(R.id.summaryRowLabel);
        rowLabel.setText(mRowItems.get(position).getLabel());

        TextView warmUp = rowView.findViewById(R.id.summaryRowWarmup);
        warmUp.setText(mRowItems.get(position).getWarmUp());

        TextView rowWorkout = rowView.findViewById(R.id.summaryRowWorkout);
        rowWorkout.setText(mRowItems.get(position).getWorkout());

        TextView rowCooldown = rowView.findViewById(R.id.summaryRowCooldown);
        rowCooldown.setText(mRowItems.get(position).getCoolDown());

        TextView rowTotal = rowView.findViewById(R.id.summaryRowTotal);
        rowTotal.setText(mRowItems.get(position).getTotal());
        rowTotal.setTextColor(Color.parseColor(mTotalTextColor));

        rowView.setContentDescription("row" + position); //NON-NLS

        return rowView;
    }
}