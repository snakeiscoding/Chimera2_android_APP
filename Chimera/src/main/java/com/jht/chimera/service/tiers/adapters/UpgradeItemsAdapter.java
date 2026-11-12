package com.jht.chimera.service.tiers.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.i18n.LanguageProperty;
import com.jht.chimera.service.tiers.FeatureUpgrade;
import com.jht.datamatrixlib.DataMatrix;

import java.util.List;

public class UpgradeItemsAdapter extends RecyclerView.Adapter<UpgradeItemsAdapter.UpgradeItemViewHolder> {

    private List<FeatureUpgrade> upgrades;
    private OnUpgradeListener mOnUpgradeListener;
    private Integer itemTextColor = null;


    public static class UpgradeItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // each data item is just a string in this case
        public TextView textView;
        OnUpgradeListener onUpgradeListener;

        public UpgradeItemViewHolder(TextView v, OnUpgradeListener onUpgradeListener) {
            super(v);
            textView = v;
            this.onUpgradeListener = onUpgradeListener;
            itemView.setOnClickListener(this);
        }


        @Override
        public void onClick(View v) {
            onUpgradeListener.onUpgradeClick(getAdapterPosition());
        }
    }

    public UpgradeItemsAdapter(List<FeatureUpgrade> upgrades, OnUpgradeListener onUpgradeListener) {
        this.upgrades = upgrades;
        this.mOnUpgradeListener = onUpgradeListener;
    }

    @NonNull
    @Override
    public UpgradeItemsAdapter.UpgradeItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView v = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.upgrade_item, parent, false);

        UpgradeItemViewHolder vh = new UpgradeItemViewHolder(v, mOnUpgradeListener);
        return vh;
    }

    @Override
    public void onBindViewHolder(UpgradeItemViewHolder holder, int position) {
        int languageId = DataMatrix.getInt(SettingKeys.General.DATA_CONSOLE_LANGUAGE_ENUM_INT);
        LanguageProperty languageProperty = LanguageProperty.getLanguagePropertyFromIndex(languageId);
        holder.textView.setText(upgrades.get(position).getTitle(languageProperty.locale));

        if(itemTextColor != null) {
            holder.textView.setTextColor(itemTextColor);
        }

    }

    public void setUpgradeItemTextColor(int color) {
        this.itemTextColor = color;
    }

    @Override
    public int getItemCount() {
        return upgrades.size();
    }

    public interface OnUpgradeListener {
        void onUpgradeClick(int position);
    }

    public interface OnUpdateColor {
        Color onUpdateColor();
    }
}
