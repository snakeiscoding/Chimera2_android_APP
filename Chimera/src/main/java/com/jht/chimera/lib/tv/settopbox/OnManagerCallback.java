package com.jht.chimera.lib.tv.settopbox;

/**
 * Created by mutombolin on 2019/02/12.
 */

public interface OnManagerCallback {
	void onGetChannelList();
	void onResult(SetTopBoxController.SET_TOP_BOX_COMMANDS command, int response);
	void onScanning(int progress);
	void onDeviceInfo(SKNETDeviceInfo deviceInfo);
}
