package com.jht.chimera.service.dapi.handlers.am.getmachinestats;


public class MachineStatsData {
    private final float distance;
    private final int time;

    public MachineStatsData(float distance, int time) {
        this.distance = distance;
        this.time = time;
    }

    @Override
    public boolean equals(Object object) {
        if(object instanceof MachineStatsData) {
            MachineStatsData data = (MachineStatsData)object;
            return Math.abs(data.distance - distance) < 0.001 && data.time == time;
        }
        return false;
    }

    public float distance() {
        return distance;
    }

    public int time() {
        return time;
    }

}
