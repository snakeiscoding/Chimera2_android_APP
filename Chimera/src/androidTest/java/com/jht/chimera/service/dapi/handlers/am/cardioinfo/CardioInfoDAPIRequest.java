package com.jht.chimera.service.dapi.handlers.am.cardioinfo;

import com.jht.chimera.service.dapi.handlers.DAPIRequest;
import com.jhtp.dapiservice.am.CardioInfo;
import com.jhtp.dapiservice.comm.CborCommands;

public class CardioInfoDAPIRequest extends DAPIRequest {
    private final CardioInfo cardioInfo;

    public CardioInfoDAPIRequest(int version, CardioInfo cardioInfo) {
        super(CborCommands.CardioInfo, version);
        this.cardioInfo = cardioInfo;
    }

    @Override
    public boolean equals(Object object) {
        if(object instanceof CardioInfoDAPIRequest) {
            CardioInfoDAPIRequest o = (CardioInfoDAPIRequest)object;
            return super.equals(object) && cardioInfo.equals(o.cardioInfo);
        }
        return false;
    }

    @Override public String toString() {
        return super.toString();
    }

}
