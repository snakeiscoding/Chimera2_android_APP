package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib;

import java.util.PriorityQueue;

public class UpdatePacketQueue extends PriorityQueue<UpdatePacket> {
    private Object _syncObject = new Object();

    public UpdatePacketQueue() {
        super(1, new UpdatePacket.UpdatePacketSort()); // PriorityQueue needs class to cast comparable or be given a comparator
    }

    @Override
    public void clear() {
        synchronized (_syncObject) {
            super.clear();
        }
    }

    @Override
    public boolean add(UpdatePacket packet) {
        synchronized (_syncObject) {
            return super.add(packet);
        }
    }

    @Override
    public UpdatePacket remove() {
        synchronized (_syncObject) {
            return super.remove();
        }
    }
}
