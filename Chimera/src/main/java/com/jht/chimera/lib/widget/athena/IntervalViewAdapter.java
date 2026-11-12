package com.jht.chimera.lib.widget.athena;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.jht.chimera.R;

import java.util.List;

public class IntervalViewAdapter extends RecyclerView.Adapter<IntervalViewAdapter.IntervalLineViewHolder>
{
    private List<IntervalViewDataHolder> mDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class IntervalLineViewHolder extends RecyclerView.ViewHolder
    {
        // each data item is just a string in this case
        public TextView splitNumber;
        public TextView splitTime;
        public TextView splitDistance;
        public TextView splitType;

        public IntervalLineViewHolder(View view)
        {
            super(view);
            splitNumber = (TextView) view.findViewById(R.id.splitNumberTextView);
            splitTime = (TextView) view.findViewById(R.id.splitTimeTextView);
            splitDistance = (TextView) view.findViewById(R.id.splitDistanceTextView);
            splitType = (TextView) view.findViewById(R.id.splitTypeTextView);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public IntervalViewAdapter(List<IntervalViewDataHolder> data)
    {
        mDataset = data;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public IntervalLineViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        // create a new view
        View v = (View) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.interval_line_view, parent, false);

        IntervalLineViewHolder vh = new IntervalLineViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder (IntervalLineViewHolder holder, int position)
    {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        IntervalViewDataHolder ivdh = mDataset.get(position);
        holder.splitNumber.setText (""+ivdh.intervalNumber);
        holder.splitTime.setText (ivdh.timeString);
        holder.splitDistance.setText (ivdh.distanceString);
        holder.splitType.setText (ivdh.typeString);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount()
    {
        return mDataset.size();
    }
}
