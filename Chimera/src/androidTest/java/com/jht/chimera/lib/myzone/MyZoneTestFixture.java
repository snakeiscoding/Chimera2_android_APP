package com.jht.chimera.lib.myzone;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.test.fixtures.SystemTestFixture;
import com.jht.datamatrixlib.DataMatrix;

import java.util.ArrayList;
import java.util.List;

 class MyZoneTestFixture extends SystemTestFixture {
    private final List<BroadcastReceiver> receivers = new ArrayList<>();

    MyZoneTestFixture() {
        super(false);
        DataMatrix.putBoolean(HALKeys.DATA_USE_MYZONE_SIMULATOR_BOOL, true);
    }

    void registerReceiver(BroadcastReceiver receiver, IntentFilter filter){
        getTargetContext().registerReceiver(receiver,filter);
        receivers.add(receiver);
    }

    @Override
    public void close(){
        super.close();
        for(BroadcastReceiver receiver : receivers){
            getTargetContext().unregisterReceiver(receiver);
        }
        receivers.clear();
    }
}
