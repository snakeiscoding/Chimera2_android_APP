package com.jht.chimera.ui.machinesettings.adapters.management;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.jht.chimera.R;

/**
 * Created by Giancarlo.Cuadros on 12/28/2018.
 */

public class ManagementItemHolder extends RecyclerView.ViewHolder {
    View container;
    TextView content;
    TextView status;

    public ManagementItemHolder(View itemView) {
        super(itemView);
        container = itemView;
        content = container.findViewById(R.id.console_info);
        status = container.findViewById(R.id.console_status);
    }
}
