package com.jht.chimera.ui.machinesettings.adapters.update;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.jht.androiduiwidgets.selector.ISelectorItem;
import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.androiduiwidgets.selector.SelectorItemHolderBase;
import com.jht.chimera.R;

import java.util.List;

@SuppressWarnings({"unused", "WeakerAccess"})
public class UpdateSelectorItem extends SelectorItemBase {

    private String version;

    public interface IOnApplyUpdate {
        void onApplyUdpate(int updateIndex);
    }

    private IOnApplyUpdate onApplyUpdate;


    public UpdateSelectorItem(@NonNull Context context, String version, IOnApplyUpdate onApplyUpdate) {
        this(context, version, onApplyUpdate, null);
    }

    public UpdateSelectorItem(@NonNull Context context, String version, IOnApplyUpdate onApplyUpdate, List<ISelectorItem> subItems) {
        super(subItems);
        this.onApplyUpdate = onApplyUpdate;
        this.version = version;
    }


    @Override
    public SelectorItemHolderBase createViewHolder(Context context, @NonNull ViewGroup parent) {

        View v = LayoutInflater.from(context).inflate(R.layout.available_update_row, parent, false);
        return new UpdateSelectorItemHolder(v, onApplyUpdate);
    }

    public String version() {
        return version;
    }

}
