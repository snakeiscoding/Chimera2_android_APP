package com.jht.chimera.ui.machinesettings.adapters.analytic;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.jht.androiduiwidgets.selector.IOnItemSelected;
import com.jht.androiduiwidgets.selector.ISelectorItem;
import com.jht.androiduiwidgets.selector.SelectorAdapterBase;
import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.androiduiwidgets.selector.SelectorItemHolderBase;
import com.jht.chimera.R;


public class AnalyticItem extends SelectorItemBase {

    class AnalyticItemHolder extends SelectorItemHolderBase {

        private View selectorView;
        private TextView keyTextView;
        private TextView valueTextView;

        AnalyticItemHolder(View v) {
            super(v);
            selectorView = v;
            keyTextView = selectorView.findViewById(R.id.key_text);
            valueTextView = selectorView.findViewById(R.id.value_text);
        }

        @Override
        protected void update(final SelectorAdapterBase adapter, final ISelectorItem item, final IOnItemSelected onItemSelected) {
            if (item instanceof AnalyticItem) {
                selectorView.setBackgroundColor(selectorView.getContext().getResources().getColor(item.getIndex() % 2 == 0 ? R.color.table_even_row_background_color : R.color.table_odd_row_background_color));
                AnalyticItem analyticItem = (AnalyticItem) item;
                keyTextView.setText(analyticItem.getKey());
                valueTextView.setText(analyticItem.getValue());
            }
        }
    }

    private String key;
    private String value;

    public AnalyticItem(@NonNull String key, @NonNull String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    @NonNull
    public SelectorItemHolderBase createViewHolder(Context context, @NonNull ViewGroup parent) {
        View v = LayoutInflater.from(context).inflate(R.layout.analytic_row, parent, false);
        return new AnalyticItemHolder(v);
    }

}
