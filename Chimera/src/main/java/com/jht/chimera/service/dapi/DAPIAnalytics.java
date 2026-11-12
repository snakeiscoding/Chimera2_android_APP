package com.jht.chimera.service.dapi;

import com.jhtp.dapiservice.DapiResult;
import com.jhtp.dapiservice.an.IDapiAnalyticsResponse;
import com.jhtp.dapiservice.comm.CborCommands;

public class DAPIAnalytics implements IDapiAnalyticsResponse {

    private DAPIAnalytics() {
    }

    private static class SingletonHolder {
        private static final DAPIAnalytics INSTANCE = new DAPIAnalytics();
    }

    public static DAPIAnalytics getInstance() {
        return DAPIAnalytics.SingletonHolder.INSTANCE;
    }

    @Override
    public void genericAnalyticsHandler(CborCommands cborCommands, DapiResult dapiResult) {

    }

    @Override
    public void reportCommandTimingHandler(CborCommands cborCommands, int i, int i1) {

    }
}
