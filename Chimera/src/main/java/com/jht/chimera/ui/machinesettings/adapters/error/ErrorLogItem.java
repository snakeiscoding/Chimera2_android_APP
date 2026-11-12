package com.jht.chimera.ui.machinesettings.adapters.error;


import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.androiduiwidgets.selector.SelectorItemHolderBase;
import com.jht.chimera.ui.machinesettings.pages.error.ErrorLogRow;
import com.jht.liberror.ErrorLog;

import java.util.Locale;

/**
 * This is contains data needed to create a row in the error log table.
 */
public class ErrorLogItem extends SelectorItemBase {
    private boolean checked = false;
    private final ErrorLog log;
    private final String errorDescription;

    /**
     * Initialize the error log item.
     */
    public ErrorLogItem(@NonNull final ErrorLog log, @NonNull final String errorDescription) {
        this.log = log;
        this.errorDescription = errorDescription;
    }

    /**
     * Used to get the item's label.
     *
     * @return Return the label
     */
    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "%04X", log.errorCode);
    }

    // Gettters for the data.
    public ErrorLog log() {
        return log;
    }

    public boolean isChecked() { return checked; }

    public void setIsChecked(boolean checked) {
        this.checked = checked;
    }

    public String getErrorDescription () { return this.errorDescription; }

    @Override
    @NonNull
    public SelectorItemHolderBase createViewHolder(Context context, @NonNull ViewGroup parent) {
        return new ErrorLogItemHolder(new ErrorLogRow(context));
    }
}
