package com.jht.chimera.service.hal.lib.lcb.lcbcommlib;

import java.util.Iterator;
import java.util.PriorityQueue;

public class PacketQueue extends PriorityQueue<LcbPacket> {
    private static final String TAG = "PacketQueue";
    private Object _syncObject = new Object();

    PacketQueue() {
        super(1, new LcbPacket.LcbPacketSort());
    }

    @Override
    public void clear() {
        synchronized (_syncObject) {
            super.clear();
        }
    }

    @Override
    public boolean add(LcbPacket packet) {
        synchronized (_syncObject) {
            return super.add(packet);
        }
    }

    @Override
    public LcbPacket remove() {
        synchronized (_syncObject) {
            return super.remove();
        }
    }

    @Override
    public LcbPacket poll() {
        synchronized (_syncObject) {
            return super.poll();
        }
    }

    public void replaceCommand(LcbPacket newPacket) {
        // removes occurrences of same command (but potentially different data) and re-adds at highest priority of existing or new packet(s)
        LcbPacket highest = newPacket; // assume we are adding the new packet or it is the highest priority version of the command
        LcbPacket.LcbPacketSort sorter = new LcbPacket.LcbPacketSort();
        synchronized (_syncObject) {
            for (Iterator<LcbPacket> i = this.iterator(); i.hasNext(); ) {
                LcbPacket pack = i.next();
                if (pack.getCommand() == newPacket.getCommand()) {
                    if (sorter.compare(highest, pack) > 0) {    // returns a positive integer if the first packet has lower priority
                        highest = pack;
                        highest.setData(newPacket.getData()); // copy the data portion of packet (shallow copy should be fine)
                    }
                    i.remove(); // remove all matches, since PriorityQueue is ordered by Comparator, the add will insert it accordingly
                }
            }
            super.add(highest); // this will either be the new packet or the highest priority of existing packets
        }
    }

    public boolean addIfSameNotQueued(LcbPacket packet) {
        synchronized (_syncObject) {
            for (LcbPacket lcbPacket : this) {
                if (lcbPacket.equals(packet)) return false;
            }
            return super.add(packet);
        }
    }

}
