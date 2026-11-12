package com.jht.chimera.lib.common.analytic;

import com.jht.chimera.BuildConfig;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.dapi.DapiUtil;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataMatrix;

public class AnalyticsMachineInfo {

    public String version = BuildConfig.VERSION_NAME;
    public String serialConsole = DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING);
    public String serialFrame = DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING);
    public int clubId = DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT);
    public int dapiEnv = DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT);
    public String machineId = DapiUtil.getMachineId();
    public String machineType = FrameConfigManager.getInstance().getFrameConfig().machineClassType.name();
    public String tvType = DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING);
    public boolean isRemoteTVEnabled = DataMatrix.getBoolean(SettingKeys.TV.DATA_TV_REMOTE_TV_BOOL);
}
