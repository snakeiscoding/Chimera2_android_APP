package com.jht.chimera.service.hal;

import android.os.SystemClock;
import android.util.SparseArray;

import com.jht.chimera.lib.common.math.RunningAverageHelper;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbCommands;

public class LCBCommStat {

    public static class MessageStat {
        public long statCountFail;
        public long statCountSuccessful;
        public long statCountInvalidCRC;
        public long statCountRetries;
        public RunningAverageHelper statRoundTripMsAve = new RunningAverageHelper();
        public long statRoundTripMin = Long.MAX_VALUE;
        public long statRoundTripMax = 0;

        public void incrementFail() {
            statCountFail++;
        }

        public void incrementSuccessful() {
            statCountSuccessful++;
        }

        public void incrementInvalidCRC() {
            statCountInvalidCRC++;
        }

        public void incrementRetries() {
            statCountRetries++;
        }

        public void addRoundTrip(long roundTripMs) {
            statRoundTripMsAve.addValue(roundTripMs);
            statRoundTripMin = Math.min(roundTripMs, statRoundTripMin);
            statRoundTripMax = Math.max(roundTripMs, statRoundTripMax);
        }
    }

    long mStatTimestampMs;
    int mCommandId;
    public MessageStat totalMessageStat;
    SparseArray<MessageStat> mMessageStatMap;

    private LCBCommStat() {
        reset();
    }

    private static class SingletonHolder {
        private static final LCBCommStat INSTANCE = new LCBCommStat();
    }

    public static LCBCommStat getInstance() {
        return LCBCommStat.SingletonHolder.INSTANCE;
    }


    public synchronized void reset() {
        totalMessageStat = new MessageStat();
        mMessageStatMap = new SparseArray<MessageStat>();
    }

    //TODO: Call when the LCB sends a message
    public synchronized void sendingMessage(LcbCommands command) {
        mCommandId = command.Value;
        mStatTimestampMs = SystemClock.uptimeMillis();
    }

    //TODO: Call if the message fail
    public synchronized void sentMessageFail() {
        totalMessageStat.incrementFail();

        MessageStat messageStat = getMessageStat();
        if (messageStat != null) {
            messageStat.incrementFail();
        }
    }

    //TODO: Call if it succeeds
    public synchronized void sentMessageSuccessful() {
        long roundTripMs = SystemClock.uptimeMillis() - mStatTimestampMs;
        totalMessageStat.addRoundTrip(roundTripMs);
        totalMessageStat.incrementSuccessful();

        MessageStat messageStat = getMessageStat();
        if (messageStat != null) {
            messageStat.addRoundTrip(roundTripMs);
            messageStat.incrementSuccessful();
        }
    }

    //TODO: Call if it's still retrying
    public synchronized void sentMessageRetry() {
        totalMessageStat.incrementRetries();

        MessageStat messageStat = getMessageStat();
        if (messageStat != null) {
            messageStat.incrementRetries();
        }
    }

    //TODO: Call if the message being parsed has an invalid CRC
    public synchronized void sentMessageInvalidCRC() {
        totalMessageStat.incrementInvalidCRC();

        MessageStat messageStat = getMessageStat();
        if (messageStat != null) {
            messageStat.incrementInvalidCRC();
        }
    }

    MessageStat getMessageStat() {
        if (mCommandId <= 0) return null;

        MessageStat stat = mMessageStatMap.get(mCommandId);
        if (stat == null) {
            stat = new MessageStat();
            mMessageStatMap.put(mCommandId, stat);
        }
        return stat;
    }

    @Override
    public synchronized String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("LCB Communication\n");
        sb.append(String.format("Total Successful: %d\n", totalMessageStat.statCountSuccessful));
        sb.append(String.format("Total Fail: %d\n", totalMessageStat.statCountFail));
        sb.append(String.format("Total Invalid CRC: %d\n", totalMessageStat.statCountInvalidCRC));
        sb.append(String.format("Total Retries: %d\n", totalMessageStat.statCountRetries));
        sb.append(String.format("Round Trip Ave (ms): %.0f\n", totalMessageStat.statRoundTripMsAve.getValue()));
        sb.append(String.format("Round Trip Min (ms): %d\n", totalMessageStat.statRoundTripMin == Long.MAX_VALUE ? 0 : totalMessageStat.statRoundTripMin));
        sb.append(String.format("Round Trip Max (ms): %d\n", totalMessageStat.statRoundTripMax));

        sb.append("-----------------------------------------------------------------\n");
        for (int i = 0; i < mMessageStatMap.size(); i++) {
            int key = mMessageStatMap.keyAt(i);
            MessageStat stat = mMessageStatMap.get(key);
            if (stat == null) continue;
            String row = String.format("0x%02x - %s | %d, %d, %d, %d, %.0f, %d, %d\n",
                    key,
                    LcbCommands.fromInt(key).toString(),
                    stat.statCountSuccessful,
                    stat.statCountFail,
                    stat.statCountInvalidCRC,
                    stat.statCountRetries,
                    stat.statRoundTripMsAve.getValue(),
                    stat.statRoundTripMin == Long.MAX_VALUE ? 0 : stat.statRoundTripMin,
                    stat.statRoundTripMax
            );
            sb.append(row);
        }

        return sb.toString();
    }

}
