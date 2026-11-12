package com.jht.chimera.lib.common.config;


import android.util.SparseArray;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LcbToFramelMapper {

    private static final String TAG = "LcbToFramelMapper";
    private static SparseArray<List<FrameConfigEnum>> sModelMap;
    public static final int DEMO_LCB_TYPE = 0xffff;


    public static List<FrameConfigEnum> getValidModels(Integer lcbType) {
        List<FrameConfigEnum> retValue = new ArrayList<FrameConfigEnum>();
        if (sModelMap.get(lcbType) != null) {
            retValue = sModelMap.get(lcbType);
        }
        return retValue;
    }

    public static boolean doesLcbMatchFrame(Integer lcbType, FrameConfigEnum frameType) {
        List<FrameConfigEnum> list = getValidModels(lcbType);
        for (FrameConfigEnum frame : list) {
            if (frame == frameType) return true;
        }
        return false;
    }


    public static boolean isKnownLcbType(Integer lcbType) {
        return (sModelMap.get(lcbType) != null);
    }

    static {
        sModelMap = new SparseArray<>();

        //
        FrameConfigEnum.findSupportedFrameConfig((frameType, frameConfig) ->{
            for (int lcbType : frameConfig.supportLcbTypeList) {
                List<FrameConfigEnum> frameTypeList = sModelMap.get(lcbType);
                if (frameTypeList == null) {
                    frameTypeList = new LinkedList<>();
                    sModelMap.put(lcbType, frameTypeList);
                }
                frameTypeList.add(frameType);
            }
            return false; // iterate all configs (so keep searching)
        });

    }

    public static boolean checkAvailableLcb(int lcb) {
        return sModelMap.get(lcb) != null;
    }

    public static List<JHTCommonTypes.MachineType> getAvailableMachines(int lcb) {
        List<JHTCommonTypes.MachineType> values = new LinkedList<>();
        List<FrameConfigEnum> frameList = sModelMap.get(lcb);
        if (frameList != null) {
            for (FrameConfigEnum frameType : frameList) {
                FrameConfig frameConfig = frameType.createFrameConfig();
                if(!values.contains(frameConfig.machineType)) {
                    values.add(frameConfig.machineType);
                }
            }
        }

        if (values.size() == 0) {
            FrameConfigEnum.findSupportedFrameConfig((frameType, frameConfig) ->{
                values.add(frameConfig.machineType);
                return false;
            });
        }

        return values;
    }

    public static List<FrameConfigEnum> getAvailableModels(JHTCommonTypes.MachineType machineType, int lcb) {
        List<FrameConfigEnum> models = new LinkedList<>();
        List<FrameConfigEnum> frameList = sModelMap.get(lcb);
        if (frameList != null) {
            for (FrameConfigEnum frameType : frameList) {
                FrameConfig frameConfig = frameType.createFrameConfig();
                if (frameConfig.machineType == machineType) models.add(frameType);
            }
        }

        if (models.size() == 0) {
            FrameConfigEnum.findSupportedFrameConfig((frameType, frameConfig) ->{
                if (frameConfig.machineType == machineType) models.add(frameType);
                return false;
            });
        }

        return models;
    }


    public static List<String> getLcbModelStringList() {
        List<String> lcbModelList = new LinkedList<>();
        for (int i = 0; i < sModelMap.size(); i++) {
            int key = sModelMap.keyAt(i);
            lcbModelList.add(String.format("0x%02X", key));
        }
        return lcbModelList;
    }

    public static boolean isCurrentMachineCompatible(FrameConfigEnum frameType, int lcb) {
        // NOTE!!!! If we do not recognize the LCB we need to assume the machine is compatible.
        // Previous products would go to machine setup in this use case and it cost Johnson over
        // $40,000 US.  This code is correct as is.
        return !checkAvailableLcb(lcb) || doesLcbMatchFrame(lcb, frameType);
    }

}
