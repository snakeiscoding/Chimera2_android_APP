package com.jht.chimera.lib.tv.switft.jhttvservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


/**
 * Created by mutombolin on 2018/05/22.
 */

public class JHTTVModuleCommandImpl implements JHTTVModuleCommand {
    private static final String ACTION_NAME = "com.jht.tv_tuner_command"; //NON-NLS
    private static final String POWER_COMMAND = "tv_power_data"; //NON-NLS
    private static final String TV_COMMAND = "tv_command_data"; //NON-NLS
    private static final String V4l2_STREAM_FAILED = "v4l2.CameraFrame.failed"; //NON-NLS

    private Context context;
//	private IServiceCallback callback;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
/*
			if (JHTTVModuleServiceWrapper.ON_TV_TUNER_GMS.compareTo(intent.getAction()) == 0) {
				Bundle extra = intent.getExtras();
				if (extra != null) {
					byte state = extra.getByte(JHTTVModuleServiceWrapper.MUTE_STATE);
					onTVTunerGMS(state == 0x55);
				}
			} else if (JHTTVModuleServiceWrapper.ON_TV_TUNER_GVL.compareTo(intent.getAction()) == 0) {
				Bundle extra = intent.getExtras();
				if (extra != null) {
					int vol = extra.getInt(JHTTVModuleServiceWrapper.VOLUME_VALUE);
					onTVTunerGVL(vol);
				}
			} else if (JHTTVModuleServiceWrapper.ON_TV_TUNER_PCN.compareTo(intent.getAction()) == 0) {
				Bundle extra = intent.getExtras();
				if (extra != null) {
					byte[] data = extra.getByteArray(JHTTVModuleServiceWrapper.CURRENT_CHANNEL);
					onTVTunerPCN(new String(data, Charset.forName("UTF-8")));
				}
			} else if (JHTTVModuleServiceWrapper.ON_TV_TUNER_GOH.compareTo(intent.getAction()) == 0) {
				Bundle extra = intent.getExtras();
				if (extra != null) {
					int mode = extra.getInt(JHTTVModuleServiceWrapper.OUTPUT_HDMI_MODE);
					onTVTunerGOH(mode);
				}
			} else if (JHTTVModuleServiceWrapper.ON_TV_TUNER_MPC.compareTo(intent.getAction()) == 0) {
				Bundle extra = intent.getExtras();
				if (extra != null) {
					int productCode = extra.getInt(JHTTVModuleServiceWrapper.PRODUCT_CODE);
					onTVTunerMPC(productCode);
				}
			} else if (JHTTVModuleServiceWrapper.ON_TV_TUNER_GCH.compareTo(intent.getAction()) == 0) {
				Bundle extra = intent.getExtras();
				if (extra != null) {
					byte[] data = extra.getByteArray(JHTTVModuleServiceWrapper.CURRENT_CHANNEL);
					ByteBuffer buffer = ByteBuffer.wrap(data);
					String major = String.valueOf(buffer.getShort());
					String minor = String.valueOf(buffer.getShort());
					onTVTunerPCN(ATSCUtils.getInstance().getChannelNumbers(major, minor));
				}
			} else if (JHTTVModuleServiceWrapper.ON_TV_TUNER_GVR.compareTo(intent.getAction()) == 0) {
				Bundle extra = intent.getExtras();
				if (extra != null) {
					int version = extra.getInt(JHTTVModuleServiceWrapper.CURRENT_VERSION);
					onTVTunerGVR(version);
				}
			}
*/
        }
    };

    private BroadcastReceiver streamFailedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//			if (callback != null) {
//				try {
//					callback.callbackStreamFailed();
//				} catch (RemoteException e) {
//				}
//			}
        }
    };

    private void onTVTunerPCN(String channel) {
//		if (callback != null) {
//			try {
//				callback.onTVTunerPCN(channel);
//			} catch (RemoteException e) {
//			}
//		}
    }

    private void onTVTunerGVL(int vol) {
//		if (callback != null) {
//			try {
//				callback.onTVTunerGVL(vol);
//			} catch (RemoteException e) {
//			}
//		}
    }

    private void onTVTunerGMS(boolean state) {
//		if (callback != null) {
//			try {
//				callback.onTVTunerGMS(state);
//			} catch (RemoteException e) {
//			}
//		}
    }

    private void onTVTunerGOH(int mode) {
//		if (callback != null) {
//			try {
//				callback.onTVTunerGOH(mode);
//			} catch (RemoteException e) {
//			}
//		}
    }

    private void onTVTunerMPC(int productCode) {
//		if (callback != null) {
//			try {
//				callback.onTVTunerMPC(productCode);
//			} catch (RemoteException e) {
//
//			}
//		}
    }

    private void onTVTunerGVR(int version) {
//		if (callback != null) {
//			try {
//				callback.onTVTunerGVR(version);
//			} catch (RemoteException e) {
//			}
//		}
    }

    public JHTTVModuleCommandImpl(Context context/*, IServiceCallback callback*/) {
        this.context = context;
//		this.callback = callback;
    }

    @Override
    public void sendCommand(CommandData data) {

        Intent intent = new Intent(ACTION_NAME);

        if (data.isPowerCommand()) {
            intent.putExtra(POWER_COMMAND, data.getCommand() == 0x1 ? new boolean[]{true} : new boolean[]{false});
        } else {
            byte[] commandData = new byte[]{data.getCommand()};
            intent.putExtra(TV_COMMAND, commandData);
        }

        context.sendBroadcast(intent);
    }

    @Override
    public void initialize() {
        registerBroadcasts();
    }

    @Override
    public void uninitialize() {
        unRegisterBroadcasts();
    }


    private void registerBroadcasts() {
/*
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(JHTTVModuleServiceWrapper.ON_TV_TUNER_GMS);
		intentFilter.addAction(JHTTVModuleServiceWrapper.ON_TV_TUNER_GVL);
		intentFilter.addAction(JHTTVModuleServiceWrapper.ON_TV_TUNER_PCN);
		intentFilter.addAction(JHTTVModuleServiceWrapper.ON_TV_TUNER_GOH);
		intentFilter.addAction(JHTTVModuleServiceWrapper.ON_TV_TUNER_MPC);
		intentFilter.addAction(JHTTVModuleServiceWrapper.ON_TV_TUNER_GCH);
		intentFilter.addAction(JHTTVModuleServiceWrapper.ON_TV_TUNER_GVR);
		
		context.registerReceiver(broadcastReceiver, intentFilter);
		context.registerReceiver(streamFailedReceiver, new IntentFilter(V4l2_STREAM_FAILED));
*/
    }

    private void unRegisterBroadcasts() {
        context.unregisterReceiver(streamFailedReceiver);
        context.unregisterReceiver(broadcastReceiver);
    }
}
