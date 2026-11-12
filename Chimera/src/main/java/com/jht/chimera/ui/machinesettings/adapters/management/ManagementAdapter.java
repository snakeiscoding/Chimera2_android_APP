package com.jht.chimera.ui.machinesettings.adapters.management;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.chimera.R;

import java.util.ArrayList;

/**
 * Created by Giancarlo.Cuadros on 12/28/2018.
 */

public class ManagementAdapter extends RecyclerView.Adapter<ManagementItemHolder> {

    private ArrayList<ConsoleItem> mDataset;

    public ManagementAdapter(ArrayList<ConsoleItem> mDataset) {
        this.mDataset = mDataset;
    }

    @Override
    public ManagementItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.management_holder_view, parent, false);
        return new ManagementItemHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ManagementItemHolder holder, int position) {
        final ConsoleItem model = mDataset.get(position);
        holder.content.setText(model.getText());
        holder.content.setBackgroundColor(model.isSelected() ? Color.CYAN : Color.WHITE);
        holder.status.setBackgroundColor(model.isSelected() ? Color.CYAN : Color.WHITE);
        holder.content.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                model.setSelected(!model.isSelected());
                holder.content.setBackgroundColor(model.isSelected() ? Color.CYAN : Color.WHITE);
                holder.status.setBackgroundColor(model.isSelected() ? Color.CYAN : Color.WHITE);
            }
        });
        holder.status.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                model.setSelected(!model.isSelected());
                holder.content.setBackgroundColor(model.isSelected() ? Color.CYAN : Color.WHITE);
                holder.status.setBackgroundColor(model.isSelected() ? Color.CYAN : Color.WHITE);
            }
        });
        holder.status.setText(model.getStatus());

    }


    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
