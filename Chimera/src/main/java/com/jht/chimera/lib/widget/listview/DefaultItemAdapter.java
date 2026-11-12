package com.jht.chimera.lib.widget.listview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.chimera.R;

import java.util.List;


/**
 * Created by AlexN on 11/3/2018.
 */

public class DefaultItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    static class DefaultItemViewHolder extends RecyclerView.ViewHolder {
        TextView itemText;
        DefaultItemViewHolder(View itemView) {
            super(itemView);
            itemText = itemView.findViewById(R.id.default_item_text_view);
        }
    }

    public interface ICustomWorkoutAdapter {
        void onItemClicked(int position);
    }

    private List<String> mItems;
    private int selectedPosition;
    private TextView selectedText;
    private ICustomWorkoutAdapter iCustomWorkoutAdapter;

    public DefaultItemAdapter(List<String> items, int defaultPosition, ICustomWorkoutAdapter iCustomWorkoutAdapter) {
        mItems = items;
        this.iCustomWorkoutAdapter = iCustomWorkoutAdapter;
        selectedPosition = defaultPosition;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View itemView;
        RecyclerView.ViewHolder viewHolder;
                itemView = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.default_recycler_item, viewGroup, false);
                viewHolder = new DefaultItemViewHolder(itemView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
        ((DefaultItemViewHolder)holder).itemText.setText(mItems.get(position));
        ((DefaultItemViewHolder)holder).itemText.setOnClickListener(v -> selectItem(holder));
        ((DefaultItemViewHolder)holder).itemText.setActivated(selectedPosition == position);
        if(selectedPosition == position){
            selectedText = ((DefaultItemViewHolder)holder).itemText;
        }

    }

    private void selectItem(RecyclerView.ViewHolder holder){
        selectedPosition = holder.getAdapterPosition();
        if (selectedText != null) {
            selectedText.setActivated(false);
        }
        selectedText = ((DefaultItemViewHolder)holder).itemText;
        selectedText.setActivated(true);
        iCustomWorkoutAdapter.onItemClicked(holder.getAdapterPosition());
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

}
