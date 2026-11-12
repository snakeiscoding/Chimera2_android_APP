package com.jht.chimera.ui.machinesettings.dialogs;

import android.view.View;

import com.jht.chimera.lib.language.StringHelper;
import com.jht.chimera.lib.network.ethernet.SystemNetworkSettings;


/**
 * Similar to the dialog factory in the common library, but this is for Engineering specific dialogs.
 */
public class EngDialogFactory {

    public static ServiceLogEntry serviceLogEntry(View anchor, StringHelper.ServiceTypes serviceType, ServiceLogEntry.IOnEntryComplete onEntryComplete) {
        ServiceLogEntry dialog = new ServiceLogEntry(anchor.getContext());
        dialog.init(serviceType, onEntryComplete);
        dialog.show(anchor);
        return dialog;
    }


    public static NetworkIPSettings ipSettings(View anchor, SystemNetworkSettings networkSettings) {
        NetworkIPSettings dialog = new NetworkIPSettings(anchor.getContext());
        dialog.init(networkSettings);
        dialog.show(anchor);
        return dialog;
    }


}
