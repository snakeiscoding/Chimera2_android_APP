package com.jht.chimera.ui.machinesettings.adapters.service;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.androiduiwidgets.selector.SelectorItemHolderBase;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.log.ServiceLog;

import java.util.LinkedList;
import java.util.List;

/**
 * This contains the data needed for a row in the service log table.
 */
public class ServiceLogItem extends SelectorItemBase {


    private List<ServiceLog> logArray = new LinkedList<>();
    private ServiceLog log;
    private View.OnClickListener onDelete;

    public ServiceLogItem(ServiceLog serviceLog, View.OnClickListener onDelete) {
        this.log = serviceLog;
        this.onDelete = onDelete;
    }

    /**
     * Used to get the item's label.
     *
     * @return Return the label
     */
    @Override
    public String toString() {
        return log.message;
    }


    public List<ServiceLog> logArray() {
        return logArray;
    }

    public ServiceLog log() {
        return log;
    }

    public View.OnClickListener onDelete() {
        return onDelete;
    }

    @Override
    @NonNull
    public SelectorItemHolderBase createViewHolder(Context context, @NonNull ViewGroup parent) {

        View v = LayoutInflater.from(context).inflate(R.layout.service_log, parent, false);
        return new ServiceLogItemHolder(v);
    }
}
